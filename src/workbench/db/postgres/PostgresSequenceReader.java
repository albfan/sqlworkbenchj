/*
 * PostgresSequenceReader.java
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
package workbench.db.postgres;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * @author  Thomas Kellerer
 */
public class PostgresSequenceReader
  implements SequenceReader
{
  private WbConnection dbConnection;
  private static final String NAME_PLACEHOLDER = "%sequence_name%";

  private final String baseSql =
      "SELECT seq_info.*, \n" +
      "       obj_description(seq.oid, 'pg_class') as remarks, \n" +
      "       quote_ident(tab.relname)||'.'||quote_ident(col.attname) as owned_by, \n" +
      "       seq.relname as sequence_name, \n" +
      "       sn.nspname as sequence_schema \n" +
      "FROM pg_class seq   \n" +
      "  JOIN pg_namespace sn ON sn.oid = seq.relnamespace \n" +
      "  CROSS JOIN (SELECT min_value, max_value, last_value, increment_by, cache_value, is_cycled FROM " + NAME_PLACEHOLDER + ") seq_info \n" +
      "  LEFT JOIN pg_depend d ON d.objid = seq.oid AND deptype = 'a' \n" +
      "  LEFT JOIN pg_class tab ON d.objid = seq.oid AND d.refobjid = tab.oid   \n" +
      "  LEFT JOIN pg_attribute col ON (d.refobjid, d.refobjsubid) = (col.attrelid, col.attnum) \n" +
      "WHERE seq.relkind = 'S'";

  public PostgresSequenceReader(WbConnection conn)
  {
    this.dbConnection = conn;
  }

  @Override
  public void readSequenceSource(SequenceDefinition def)
  {
    readSequenceSource(def, true);
  }

  public void readSequenceSource(SequenceDefinition def, boolean includeOwner)
  {
    CharSequence source = getSequenceSource(def, includeOwner);
    def.setSource(source);
  }

  /**
   *  Return the source SQL for a PostgreSQL sequence definition.
   *
   *  @return The SQL to recreate the given sequence
   */
  @Override
  public CharSequence getSequenceSource(String catalog, String schema, String aSequence)
  {
    SequenceDefinition def = getSequenceDefinition(catalog, schema, aSequence);
    if (def == null) return "";
    return def.getSource();
  }

  @Override
  public CharSequence getSequenceSource(SequenceDefinition def, boolean includeOwner)
  {
    if (def == null) return null;

    StringBuilder buf = new StringBuilder(250);

    try
    {
      String name = def.getSequenceName();
      Long max = (Long) def.getSequenceProperty(PROP_MAX_VALUE);
      Long min = (Long) def.getSequenceProperty(PROP_MIN_VALUE);
      Long inc = (Long) def.getSequenceProperty(PROP_INCREMENT);
      Long cache = (Long) def.getSequenceProperty(PROP_CACHE_SIZE);
      Boolean cycle = (Boolean) def.getSequenceProperty(PROP_CYCLE);
      if (cycle == null) cycle = Boolean.FALSE;

      buf.append("CREATE SEQUENCE ");
      buf.append(name);
      buf.append("\n       INCREMENT BY ");
      buf.append(inc);
      buf.append("\n       MINVALUE ");
      buf.append(min);
      long maxMarker = 9223372036854775807L;
      if (max != maxMarker)
      {
        buf.append("\n       MAXVALUE ");
        buf.append(max.toString());
      }
      buf.append("\n       CACHE ");
      buf.append(cache);
      buf.append("\n       ");
      if (!cycle.booleanValue())
      {
        buf.append("NO");
      }
      buf.append(" CYCLE");
      String col = def.getRelatedColumn();
      TableIdentifier tbl = def.getRelatedTable();
      if (tbl != null && StringUtil.isNonBlank(col))
      {
        String owningColumn = tbl.getTableName() + "." + col;

        if (includeOwner)
        {
          buf.append("\n       OWNED BY ");
          buf.append(owningColumn);
        }
        def.setPostCreationSQL("ALTER SEQUENCE " + def.getObjectExpression(dbConnection) + " OWNED BY " + owningColumn + ";");
      }

      buf.append(";\n");

      if (StringUtil.isNonBlank(def.getComment()))
      {
        buf.append('\n');
        buf.append("COMMENT ON SEQUENCE ").append(def.getSequenceName()).append(" IS '").append(def.getComment().replace("'", "''")).append("';");
      }
    }
    catch (Exception e)
    {
      LogMgr.logError("PgSequenceReader.getSequenceSource()", "Error reading sequence definition", e);
    }
    return buf;
  }

  /**
   * Retrieve the list of full SequenceDefinitions from the database.
   */
  @Override
  public List<SequenceDefinition> getSequences(String catalog, String schema, String namePattern)
  {
    List<SequenceDefinition> result = new ArrayList<>();

    ResultSet rs = null;
    Savepoint sp = null;
    if (namePattern == null) namePattern = "%";

    try
    {
      sp = this.dbConnection.setSavepoint();
      DatabaseMetaData meta = this.dbConnection.getSqlConnection().getMetaData();
      rs = meta.getTables(null, schema, namePattern, new String[] { "SEQUENCE"} );
      while (rs.next())
      {
        String seqName = rs.getString("TABLE_NAME");
        String seqSchema = rs.getString("TABLE_SCHEM");
        result.add(getSequenceDefinition(null, seqSchema, seqName));
      }
      this.dbConnection.releaseSavepoint(sp);
    }
    catch (SQLException e)
    {
      this.dbConnection.rollback(sp);
      LogMgr.logError("PostgresSequenceReader.getSequences()", "Error retrieving sequences", e);
      return null;
    }
    finally
    {
      SqlUtil.closeResult(rs);
    }
    return result;
  }

  private SequenceDefinition createDefinition(String name, String schema, DataStore ds)
  {
    SequenceDefinition def = new SequenceDefinition(schema, name);
    def.setSequenceProperty(PROP_INCREMENT, ds.getValue(0, "increment_by"));
    def.setSequenceProperty(PROP_MAX_VALUE, ds.getValue(0, "max_value"));
    def.setSequenceProperty(PROP_MIN_VALUE, ds.getValue(0, "min_value"));
    def.setSequenceProperty(PROP_CACHE_SIZE, ds.getValue(0, "cache_value"));
    def.setSequenceProperty(PROP_CYCLE, ds.getValue(0, "is_cycled"));
    def.setSequenceProperty(PROP_LAST_VALUE, ds.getValue(0, "last_value"));
    String ownedBy = ds.getValueAsString(0, "owned_by");
    if (StringUtil.isNonEmpty(ownedBy))
    {
      List<String> elements = StringUtil.stringToList(ownedBy, ".", true, true, false, false);
      TableIdentifier tbl = new TableIdentifier(schema, elements.get(0));
      def.setRelatedTable(tbl, elements.get(1));
    }
    String comment = ds.getValueAsString(0, "remarks");
    def.setComment(comment);
    return def;
  }

  @Override
  public SequenceDefinition getSequenceDefinition(String catalog, String schema, String sequence)
  {
    DataStore ds = getRawSequenceDefinition(null, schema, sequence);
    if (ds == null) return null;
    SequenceDefinition result = createDefinition(sequence, schema, ds);
    readSequenceSource(result);
    return result;
  }

  @Override
  public DataStore getRawSequenceDefinition(String catalog, String schema, String sequence)
  {
    if (sequence == null) return null;

    String fullname = (schema == null ? sequence : schema + "." + sequence);

    DataStore result = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;
    Savepoint sp = null;

    String sql =
      "select min_value, max_value, last_value, increment_by, cache_value, is_cycled, remarks, owned_by \n" +
      "from ( \n" + baseSql.replace(NAME_PLACEHOLDER, fullname) + "\n) t \n" +
      "where sequence_name = ? ";

    if (schema != null)
    {
      sql += "\n  and sequence_schema = ? ";
    }

    try
    {
      if (Settings.getInstance().getDebugMetadataSql())
      {
        LogMgr.logInfo("PostgresSequenceReader.getRawSequenceDefinition()", "Retrieving sequence details using:\n" + SqlUtil.replaceParameters(sql, sequence, schema));
      }

      sp = this.dbConnection.setSavepoint();
      stmt = this.dbConnection.getSqlConnection().prepareStatement(sql);
      stmt.setString(1, sequence);
      if (schema != null)
      {
        stmt.setString(2, schema);
      }
      rs = stmt.executeQuery();
      result = new DataStore(rs, true);
      this.dbConnection.releaseSavepoint(sp);
    }
    catch (SQLException e)
    {
      this.dbConnection.rollback(sp);
      // sqlstate = 42P01 is "undefined table" which can happen if this method was called
      // for a sequence that doesn't exist. There is no need to log this
      LogMgr.logDebug("PostgresSequenceReader.getRawSequenceDefinition()", "Error reading sequence definition using:\n" +
        SqlUtil.replaceParameters(sql, sequence, schema), e);

      return null;
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }

    if (result.getRowCount() > 1)
    {
      // this can happen if a sequence is owned by more than one column
      // so collect all the "owned_by" values and then delete all but the
      // first row
      int colIndex = result.getColumnIndex("owned_by");
      String cols = "";
      for (int i=0; i < result.getRowCount(); i++)
      {
        String owned = result.getValueAsString(i, colIndex);
        if (StringUtil.isNonEmpty(owned))
        {
          if (cols.length() > 0) cols += ", ";
          cols += owned;
        }
      }
      for (int i=result.getRowCount() - 1; i > 0; i--)
      {
        result.deleteRow(i);
      }
      result.setValue(0, colIndex, cols);
    }

    result.resetStatus();

    return result;
  }

  @Override
  public String getSequenceTypeName()
  {
    return SequenceReader.DEFAULT_TYPE_NAME;
  }

  @Override
  public boolean supportsDependentSQL()
  {
    return true;
  }

}
