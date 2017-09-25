/*
 * PostgresSequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.progress;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * @author  Thomas Kellerer
 */
public class OpenEdgeSequenceReader
  implements SequenceReader
{
  private WbConnection dbConn;

  // TODO: how to find the "multi tenant" attribute
  private final String SELECT_SEQUENCE_DEF =
    "SELECT \"SEQ-OWNER\" as owner, \n" +
    "       \"SEQ-NAME\" as sequence_name, \n" +
    "       \"SEQ-INIT\" as initial, \n" +
    "       \"SEQ-INCR\" as increment, \n" +
    "       \"SEQ-MIN\" as \"MINVALUE\", \n" +
    "       \"SEQ-MAX\" as \"MAXVALUE\", \n" +
    "       case when \"CYCLE-OK\" = 1 then 'CYCLE' ELSE 'NOCYCLE' end as \"CYCLE\" \n" +
    "FROM sysprogress.syssequences \n";

  public OpenEdgeSequenceReader(WbConnection conn)
  {
    dbConn = conn;
  }

  @Override
  public List<SequenceDefinition> getSequences(String catalog, String owner, String namePattern)
  {
    StringBuilder sql = new StringBuilder(SELECT_SEQUENCE_DEF);

    boolean whereAdded = false;
    int ownerIndex = -1;
    int nameIndex = -1;

    owner = StringUtil.trimToNull(owner);
    namePattern = StringUtil.trimToNull(namePattern);

    if (StringUtil.isNonBlank(owner))
    {
      whereAdded = true;
      ownerIndex = 1;
      sql.append(" WHERE \"SEQ-OWNER\" = ?");
    }

    if (StringUtil.isNonBlank(namePattern))
    {
      if (whereAdded)
      {
        sql.append(" AND ");
        nameIndex = 2;
      }
      else
      {
        sql.append(" WHERE ");
        nameIndex = 1;
      }
      if (namePattern.indexOf('%') > 0)
      {
        sql.append(" \"SEQ-NAME\" LIKE ? ");
        SqlUtil.appendEscapeClause(sql, this.dbConn, namePattern);
        namePattern = SqlUtil.escapeUnderscore(namePattern, dbConn);
      }
      else
      {
        sql.append(" \"SEQ-NAME\" = ? ");
      }
    }

    ResultSet rs = null;
    PreparedStatement stmt = null;
    List<SequenceDefinition> result = new ArrayList<>();

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo("ProgressSequenceReader.getSquences()", "Query to retrieve sequence: \n" + SqlUtil.replaceParameters(sql, owner, namePattern));
    }

    try
    {
      stmt = this.dbConn.getSqlConnection().prepareStatement(sql.toString());
      if (ownerIndex != -1) stmt.setString(ownerIndex, SqlUtil.removeObjectQuotes(owner));
      if (nameIndex != -1) stmt.setString(nameIndex, SqlUtil.removeObjectQuotes(namePattern));
      rs = stmt.executeQuery();
      DataStore ds = new DataStore(rs);
      ds.initData(rs);

      for (int i=0; i < ds.getRowCount(); i++)
      {
        result.add(createDefinition(ds, i, ds.getValueAsString(i, 0), ds.getValueAsString(i, 1)));
      }
    }
    catch (Throwable e)
    {
      LogMgr.logError("ProgressSequenceReader.getSequences()", "Error when retrieving sequences using: \n" + sql, e);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return result;
  }

  @Override
  public SequenceDefinition getSequenceDefinition(String catalog, String owner, String sequence)
  {
    DataStore ds = getRawSequenceDefinition(catalog, owner, sequence);
    if (ds == null || ds.getRowCount() == 0) return null;
    return createDefinition(ds, 0, owner, sequence);
  }

  private SequenceDefinition createDefinition(DataStore ds, int row, String owner, String sequence)
  {
    SequenceDefinition def = new SequenceDefinition(owner.trim(), sequence.trim());
    def.setSequenceProperty(PROP_MIN_VALUE, ds.getValue(row, "MINVALUE"));
    def.setSequenceProperty(PROP_MAX_VALUE, ds.getValue(row, "MAXVALUE"));
    def.setSequenceProperty(PROP_START_VALUE, ds.getValue(row, "INITIAL"));
    def.setSequenceProperty(PROP_INCREMENT, ds.getValue(row, "INCREMENT"));
    def.setSequenceProperty(PROP_CYCLE, ds.getValue(row, "CYCLE"));
    def.setSource(buildSource(def));
    return def;
  }

  @Override
  public DataStore getRawSequenceDefinition(String catalog, String owner, String sequence)
  {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    DataStore result = null;
    String sql = SELECT_SEQUENCE_DEF + " WHERE \"SEQ-OWNER\" = ? AND \"SEQ-NAME\" = ?";
    try
    {
      stmt = this.dbConn.getSqlConnection().prepareStatement(sql);
      stmt.setString(1, owner.trim());
      stmt.setString(2, sequence.trim());
      rs = stmt.executeQuery();
      result = new DataStore(rs);
      result.initData(rs);
    }
    catch (Throwable e)
    {
      LogMgr.logError("ProgressSequenceReader.getRawSequenceDefinition()", "Error when retrieving sequence definition", e);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }

    return result;
  }

  @Override
  public void readSequenceSource(SequenceDefinition def)
  {
    if (def == null) return;
    CharSequence s = getSequenceSource(null, def.getSequenceOwner(), def.getSequenceName());
    def.setSource(s);
  }

  private String buildSource(SequenceDefinition def)
  {
    StringBuilder result = new StringBuilder(100);
    Number minvalue = (Number)def.getSequenceProperty(PROP_MIN_VALUE);
    Number maxvalue = (Number)def.getSequenceProperty(PROP_MAX_VALUE);
    Number init = (Number)def.getSequenceProperty(PROP_START_VALUE);
    Number increment = (Number)def.getSequenceProperty(PROP_INCREMENT);
    String cycle = (String)def.getSequenceProperty(PROP_CYCLE);

    result.append("CREATE SEQUENCE ");
    result.append(def.getObjectExpression(dbConn));

    boolean hasElement = false;

    if (increment != null)
    {
      result.append("\n     INCREMENT BY ");
      result.append(increment);
      hasElement = true;
    }

    if (init != null)
    {
      if (hasElement) result.append(',');
      result.append("\n     START WITH ");
      result.append(init);
      hasElement = true;
    }

    if (minvalue != null && minvalue.intValue() != 0)
    {
      if (hasElement) result.append(',');
      result.append("\n     MINVALUE ");
      result.append(minvalue);
      hasElement = true;
    }

    if (maxvalue != null)
    {
      if (hasElement) result.append(',');
      result.append("\n     MAXVALUE ");
      result.append(maxvalue);
      hasElement = true;
    }

    if (StringUtil.equalString(cycle, "CYCLE"))
    {
      if (hasElement) result.append(',');
      result.append("\n     CYCLE");
      hasElement = true;
    }

    result.append(";\n");
    return result.toString();
  }

  @Override
  public String getSequenceSource(String catalog, String owner, String sequenceName)
  {
    SequenceDefinition sequence = getSequenceDefinition(catalog, owner, sequenceName);
    if (sequence == null) return null;
    return sequence.getSource().toString();
  }

  @Override
  public String getSequenceTypeName()
  {
    return SequenceReader.DEFAULT_TYPE_NAME;
  }

}

