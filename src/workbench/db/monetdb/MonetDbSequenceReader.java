/*
 * MonetDbSequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.monetdb;

import java.util.ArrayList;
import java.util.Collections;
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
 * SequenceReader for <a href="http://www.monetdb.org">MonetDB</a>
 *
 * @author  Thomas Kellerer
 */
public class MonetDbSequenceReader
  implements SequenceReader
{
  private WbConnection dbConn;
  private String dbName;

  private String baseQuery =
    "SELECT sh.name as sequence_schema, \n" +
    "       sq.name as sequence_name, \n" +
    "       sq.\"start\", \n" +
    "       sq.\"minvalue\", \n" +
    "       sq.\"maxvalue\", \n" +
    "       sq.\"increment\", \n" +
    "       sq.\"cycle\", \n" +
    "       sq.cacheinc \n" +
    "FROM sys.sequences sq \n" +
    "  JOIN sys.schemas sh on sh.id = sq.schema_id";


  public MonetDbSequenceReader(WbConnection conn)
  {
    this.dbConn = conn;
    this.dbName = conn.getCurrentCatalog();
  }

  @Override
  public void readSequenceSource(SequenceDefinition def)
  {
    if (def == null) return;
    CharSequence s = getSequenceSource(def.getCatalog(), def.getSequenceOwner(), def.getSequenceName());
    def.setSource(s);
  }

  @Override
  public DataStore getRawSequenceDefinition(String catalog, String schema, String namePattern)
  {
    StringBuilder query = new StringBuilder(baseQuery.length() + 20);
    query.append(baseQuery);
    boolean whereAdded = false;

    if (StringUtil.isNonBlank(namePattern))
    {
      whereAdded = true;
      query.append(" WHERE ");
      SqlUtil.appendExpression(query, "sq.name", StringUtil.trimQuotes(namePattern), dbConn);
    }

    if (StringUtil.isNonBlank(schema))
    {
      if (!whereAdded)
      {
        query.append(" WHERE ");
      }
      else
      {
        query.append(" AND ");
      }
      SqlUtil.appendExpression(query, "sh.name", StringUtil.trimQuotes(schema), null);
    }

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logInfo("MonetDbSequenceReader.getRawSequenceDefinition()", "Query to retrieve sequences:\n" + query);
    }

    DataStore result = null;
    try
    {
      result = SqlUtil.getResultData(dbConn, query.toString(), true);
    }
    catch (Throwable e)
    {
      LogMgr.logError("MonetDbSequenceReader.getSequenceDefinition()", "Error when retrieving sequence definition", e);
    }

    return result;
  }


  @Override
  public List<SequenceDefinition> getSequences(String catalog, String owner, String namePattern)
  {
    DataStore ds = getRawSequenceDefinition(catalog, owner, namePattern);
    if (ds == null) return Collections.emptyList();

    List<SequenceDefinition> result = new ArrayList<>();

    for (int row = 0; row < ds.getRowCount(); row++)
    {
      result.add(createSequenceDefinition(ds, row));
    }
    return result;
  }

  private SequenceDefinition createSequenceDefinition(DataStore ds, int row)
  {
    SequenceDefinition result = null;

    if (ds == null || ds.getRowCount() == 0) return null;

    String name = ds.getValueAsString(row, "sequence_name");
    String schema = ds.getValueAsString(row, "sequence_schema");
    result = new SequenceDefinition(schema, name);
    result.setCatalog(dbName);

    result.setSequenceProperty("START_WITH", ds.getValue(row, "start"));
    result.setSequenceProperty("MAXIMUM_VALUE", ds.getValue(row, "maxvalue"));
    result.setSequenceProperty("MINIMUM_VALUE", ds.getValue(row, "minvalue"));
    result.setSequenceProperty("INCREMENT", ds.getValue(row, "increment"));
    result.setSequenceProperty("CYCLE_OPTION", ds.getValue(row, "cycle"));
    result.setSequenceProperty("DATA_TYPE", "integer");  // TODO: how to retrieve the real datatype?
    result.setSource(buildSource(result));
    return result;
  }

  @Override
  public SequenceDefinition getSequenceDefinition(String catalog, String owner, String sequence)
  {
    DataStore ds = getRawSequenceDefinition(catalog, owner, sequence);
    if (ds == null) return null;
    return createSequenceDefinition(ds, 0);
  }

  @Override
  public CharSequence getSequenceSource(String catalog, String owner, String sequence)
  {
    SequenceDefinition def = getSequenceDefinition(catalog, owner, sequence);
    return buildSource(def);
  }

  protected CharSequence buildSource(SequenceDefinition def)
  {
    if (def == null) return StringUtil.EMPTY_STRING;

    StringBuilder result = new StringBuilder(100);
    result.append("CREATE SEQUENCE ");
    String nl = Settings.getInstance().getInternalEditorLineEnding();
    result.append(def.getSequenceName());
    String type = (String)def.getSequenceProperty("DATA_TYPE");

    if (!"INTEGER".equals(type))
    {
      result.append(" AS ");
      result.append(type);
    }

    Number start = (Number)def.getSequenceProperty("START_WITH");
    if (start != null && start.longValue() > 1)
    {
      result.append(nl);
      result.append("       START WITH ");
      result.append(start);
    }

    Number inc = (Number)def.getSequenceProperty("INCREMENT");
    if (inc != null && inc.longValue() != 1)
    {
      result.append(nl);
      result.append("       INCREMENT BY ");
      result.append(inc);
    }

    Number cache = (Number)def.getSequenceProperty("CACHE");
    if (cache != null && cache.longValue() > 0)
    {
      result.append(nl);
      result.append("       CACHE ");
      result.append(cache.toString());
    }

    Boolean cycle = (Boolean)def.getSequenceProperty("CYCLE");
    if (cycle != null && cycle.booleanValue())
    {
      result.append(nl);
      result.append("       CYCLE");
    }
    result.append(nl);
    result.append(';');
    result.append(nl);
    return result;
  }

  @Override
  public String getSequenceTypeName()
  {
    return SequenceReader.DEFAULT_TYPE_NAME;
  }
}
