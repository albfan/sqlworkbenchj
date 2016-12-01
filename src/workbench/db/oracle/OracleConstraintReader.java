/*
 * OracleConstraintReader.java
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
package workbench.db.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import workbench.db.AbstractConstraintReader;
import workbench.db.ColumnIdentifier;
import workbench.db.TableConstraint;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;
import workbench.sql.parser.ParserType;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;

/**
 * A class to read column and table constraints from Oracle.
 *
 * @author Thomas Kellerer
 */
public class OracleConstraintReader
  extends AbstractConstraintReader
{
  private final String TABLE_SQL =
    "-- SQL Workbench \n" +
    "SELECT " + OracleUtils.getCacheHint() + " constraint_name, search_condition, status, validated \n" +
     "FROM all_constraints cons   \n" +
     "WHERE constraint_type = 'C' \n" +
     " and owner = ? \n" +
     " and table_name = ?  \n";

  public OracleConstraintReader(String dbId)
  {
    super(dbId);
  }

  @Override
  public int getIndexForSchemaParameter()
  {
    return 1;
  }

  @Override
  public int getIndexForCatalogParameter()
  {
    return -1;
  }

  @Override
  public int getIndexForTableNameParameter()
  {
    return 2;
  }

  @Override
  public String getColumnConstraintSql()
  {
    return null;
  }

  @Override
  public String getTableConstraintSql()
  {
    return TABLE_SQL;
  }

  @Override
  public List<TableConstraint> getTableConstraints(WbConnection dbConnection, TableDefinition def)
  {
    String sql = this.getTableConstraintSql();
    if (sql == null) return null;
    List<TableConstraint> result = CollectionUtil.arrayList();

    TableIdentifier table = def.getTable();

    ResultSet rs = null;
    PreparedStatement stmt = null;

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logDebug("OracleConstraintReader.getTableConstraints()", "Retrieving table constraints using:\n" + SqlUtil.replaceParameters(sql, table.getRawSchema(), table.getRawTableName()));
    }

    try
    {
      long start = System.currentTimeMillis();
      stmt = dbConnection.getSqlConnection().prepareStatement(sql);
      stmt.setString(1, table.getRawSchema());
      stmt.setString(2, table.getRawTableName());

      rs = stmt.executeQuery();
      while (rs.next())
      {
        String name = rs.getString(1);
        String constraint = rs.getString(2);
        String status = rs.getString(3);
        String valid = rs.getString(4);

        if (constraint != null)
        {
          if (hideTableConstraint(name, constraint, def.getColumns())) continue;

          String expression = "(" + constraint + ")";
          if ("DISABLED".equalsIgnoreCase(status))
          {
            expression += " DISABLE";
          }
          if ("NOT VALIDATED".equalsIgnoreCase(valid))
          {
            expression += " NOVALIDATE";
          }
          TableConstraint c = new TableConstraint(name, expression);
          c.setIsSystemName(isSystemConstraintName(name));
          result.add(c);
        }
      }
      long duration = System.currentTimeMillis() - start;
      LogMgr.logDebug("OracleConstraintReader.getTableConstraints()", "Retrieving table constraints for " + table.getFullyQualifiedName(null) + " took " + duration + "ms");
    }
    catch (Exception e)
    {
      LogMgr.logError("OracleConstraintReader", "Error when reading column constraints", e);
    }
    finally
    {
      SqlUtil.closeAll(rs, stmt);
    }
    return result;
  }

  /**
   * Checks if the constraint definition is a valid Not null definition that should be displayed.
   */
  protected boolean hideTableConstraint(String name, String definition, List<ColumnIdentifier> columns)
  {
    /*
      There are four ways to create a column that is not nullable in Oracle:

      create table foo (id integer not null);
      --> the column will be marked as NOT NULL and a check constraints with a system name will be created in all_constraints

      create table foo (id integer constraint id_not_null not null);
      --> the column will be marked as NOT NULL and a check constraint with the specified name will created in all_constraints

      create table foo (id integer check (id is not null));
      --> the column will be marked as NULLABLE and a check constraint with a system name will be created in all_constraints

      create table foo (id integer constraint id_not_null check (id is not null));
      --> the column will be marked as NULLABLE and a check constraint with the specified name will be created in all_constraints
    */

    boolean systemConstraint = isSystemConstraintName(name);

    try
    {
      SQLLexer lexer = SQLLexerFactory.createLexer(ParserType.Oracle, definition);
      SQLToken tok = lexer.getNextToken(false, false);
      if (tok == null) return false;

      if (!tok.isIdentifier()) return false;
      String colName = SqlUtil.removeObjectQuotes(tok.getText());

      ColumnIdentifier colId = ColumnIdentifier.findColumnInList(columns, colName);
      if (colId == null) return false;

      // If no further tokens exist, this cannot be a not null constraint
      tok = lexer.getNextToken(false, false);
      if (tok == null) return false;

      SQLToken tok2 = lexer.getNextToken(false, false);
      if (tok2 != null) return false; // another token means this can't be a simple NOT NULL constraint

      if ("IS NOT NULL".equalsIgnoreCase(tok.getContents()))
      {
        if (colId.isNullable())
        {
          // column is nullable but has a not null constraint
          String check = "CHECK (" + definition + ")";
          if (systemConstraint)
          {
            colId.setConstraint(check);
          }
          else
          {
            colId.setConstraint("CONSTRAINT " + name + " " + check);
          }
        }
        else
        {
          if (!systemConstraint)
          {
            // only show the column constraint if it is a named constraint
            // in that case the column was defined as "col_name type constraint xxxx not null"
            colId.setConstraint("CONSTRAINT " + name + " NOT NULL");
          }
        }
      }

      // hide not null constraints at table level, always display them with the column
      return true;
    }
    catch (Exception e)
    {
      return false;
    }
  }

}
