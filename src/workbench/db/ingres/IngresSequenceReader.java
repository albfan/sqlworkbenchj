/*
 * IngresSequenceReader.java
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
package workbench.db.ingres;

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
 * A sequence reader for Ingres
 *
 * @author  Thomas Kellerer
 */
public class IngresSequenceReader
  implements SequenceReader
{
  private WbConnection dbConn;
  private final String SELECT_SEQUENCE_DEF =
             "SELECT SEQ_NAME,  \n" +
             "       SEQ_OWNER, \n" +
             "       MIN_VALUE,  \n" +
             "       MAX_VALUE,  \n" +
             "       INCREMENT_VALUE,  \n" +
             "       CYCLE_FLAG,  \n" +
             "       ORDER_FLAG,  \n" +
             "       CACHE_SIZE  \n" +
             "FROM   iisequences \n";

  public IngresSequenceReader(WbConnection conn)
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

    if (StringUtil.isNonBlank(owner))
    {
      whereAdded = true;
      ownerIndex = 1;
      sql.append(" WHERE seq_owner = ?");
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
        sql.append(" seq_name LIKE ? ");
        SqlUtil.appendEscapeClause(sql, this.dbConn, namePattern);
        namePattern = SqlUtil.escapeUnderscore(namePattern, dbConn);
      }
      else
      {
        sql.append(" seq_name = ? ");
      }
    }

    ResultSet rs = null;
    PreparedStatement stmt = null;
    List<SequenceDefinition> result = new ArrayList<>();

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo("IngresSequenceReqder.getSquences()", "Query to retrieve sequence:" + sql);
    }

    try
    {
      stmt = this.dbConn.getSqlConnection().prepareStatement(sql.toString());
      if (ownerIndex != -1) stmt.setString(ownerIndex, owner.trim());
      if (nameIndex != -1) stmt.setString(nameIndex, namePattern.trim());
      rs = stmt.executeQuery();
      DataStore ds = new DataStore(rs);
      ds.setTrimCharData(true);
      ds.initData(rs);

      for (int i=0; i < ds.getRowCount(); i++)
      {
        result.add(createDefinition(ds, i, ds.getValueAsString(i, 1), ds.getValueAsString(i, 0)));
      }
    }
    catch (Throwable e)
    {
      LogMgr.logError("IngresMetaData.getSequences()", "Error when retrieving sequences",e);
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
    def.setSequenceProperty(PROP_MIN_VALUE, ds.getValue(row, "MIN_VALUE"));
    def.setSequenceProperty(PROP_MAX_VALUE, ds.getValue(row, "MAX_VALUE"));
    def.setSequenceProperty(PROP_INCREMENT, ds.getValue(row, "INCREMENT_VALUE"));
    def.setSequenceProperty(PROP_CYCLE, ds.getValue(row, "CYCLE_FLAG"));
    def.setSequenceProperty(PROP_ORDERED, ds.getValue(row, "ORDER_FLAG"));
    def.setSequenceProperty(PROP_CACHE_SIZE, ds.getValue(row, "CACHE_SIZE"));
    def.setSource(buildSource(def));
    return def;
  }

  @Override
  public DataStore getRawSequenceDefinition(String catalog, String owner, String sequence)
  {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    DataStore result = null;
    String sql = SELECT_SEQUENCE_DEF + " WHERE seq_owner = ? AND seq_name = ?";
    try
    {
      stmt = this.dbConn.getSqlConnection().prepareStatement(sql);
      stmt.setString(1, owner.trim());
      stmt.setString(2, sequence.trim());
      rs = stmt.executeQuery();
      result = new DataStore(rs);
      result.setTrimCharData(true);
      result.initData(rs);
    }
    catch (Throwable e)
    {
      LogMgr.logError("IngresMetaData.getRawSequenceDefinition()", "Error when retrieving sequence definition", e);
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
    String increment = def.getSequenceProperty(PROP_INCREMENT).toString();
    String cycle = def.getSequenceProperty(PROP_CYCLE).toString();
    String order = def.getSequenceProperty(PROP_ORDERED).toString();
    Number cache = (Number)def.getSequenceProperty(PROP_CACHE_SIZE);

    result.append("CREATE SEQUENCE ");
    result.append(def.getSequenceName());

    result.append("\n       INCREMENT BY ");
    result.append(increment);

    if (minvalue.intValue() == 0)
    {
      result.append("\n       NO MINVALUE");
    }
    else
    {
      result.append("\n       MINVALUE ");
      result.append(minvalue);
    }

    if (maxvalue.intValue() < Integer.MAX_VALUE)
    {
      result.append("\n       MAXVALUE ");
      result.append(maxvalue);
    }
    else
    {
      result.append("\n       NO MAXVALUE");
    }
    if (cache.intValue() > 0)
    {
      result.append("\n       CACHE ");
      result.append(cache);
    }
    else
    {
      result.append("\n       NO CACHE");
    }
    result.append("\n       ");
    result.append(cycle.equals("Y") ? "CYCLE" : "NO CYCLE");

    result.append("\n       ");
    result.append(order.equals("Y") ? "ORDER" : "NO ORDER");

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

