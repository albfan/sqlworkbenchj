/*
 * TableCreator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.db;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import workbench.log.LogMgr;

import workbench.db.sqltemplates.ColumnDefinitionTemplate;

import workbench.sql.formatter.FormatterUtil;
import workbench.sql.syntax.SqlKeywordHelper;

import workbench.util.SqlUtil;

/**
 * A class to create a table in the database based on column definitions.
 *
 * The necessary CREATE TABLE is not generated using a TableSourceBuilder because dependent constraints
 * and other options should not be created as well.
 *
 * @author  Thomas Kellerer
 */
public class TableCreator
{
	private WbConnection connection;
	private List<ColumnIdentifier> columnList;
	private TableIdentifier tablename;
	private TypeMapper mapper;
	private boolean useDbmsDataType;
	private boolean useColumnAlias;
	private boolean removeDefaults;
	private String creationType;
	private ColumnDefinitionTemplate columnTemplate;
	private boolean storeSQL;
	private List<String> generatedSQL;

  private boolean useFormatterSettings;

	public TableCreator(TableIdentifier newTable, Collection<ColumnIdentifier> columns)
		throws SQLException
	{
    this(null, null, newTable, columns);
  }

	/**
	 * Create a new TableCreator.
	 *
	 * @param target the connection where to create the table
	 * @param type a keyword identifying the type of the table. This will be used to retrieve the corresponding
	 *             SQL template from {@link DbSettings#getCreateTableTemplate(java.lang.String) }
	 * @param newTable the name of the new table
	 * @param columns the columns of the new table
	 *
	 * @throws SQLException if something goes wrong
	 */
	public TableCreator(WbConnection target, String type, TableIdentifier newTable, Collection<ColumnIdentifier> columns)
		throws SQLException
	{
		this.connection = target;
		this.tablename = newTable.createCopy();

		// As we are sorting the columns we have to create a copy of the array
		// to ensure that the caller does not see a different ordering
		columnList = new ArrayList<>(columns);

		// Now sort the columns according to their DBMS position
		ColumnIdentifier.sortByPosition(columnList);

    if (connection != null)
    {
      mapper = new TypeMapper(this.connection);
      columnTemplate = new ColumnDefinitionTemplate(connection.getMetadata().getDbId());
      columnTemplate.setFixDefaultValues(!connection.getDbSettings().returnsValidDefaultExpressions());
    }
    else
    {
      mapper = new TypeMapper();
      columnTemplate = new ColumnDefinitionTemplate();
      columnTemplate.setFixDefaultValues(false);
    }
		creationType = type;
	}

	/**
	 * Controls if the column name or column display name should be used.
	 * If set to true, the column display name is used to create the table,
	 * this is useful if the ColumnDefinitions where obtained from a query with column aliases
	 *
	 * @see ColumnIdentifier#getColumnName()
	 * @see ColumnIdentifier#getDisplayName()
	 */
	public void setUseColumnAlias(boolean flag)
	{
		this.useColumnAlias = flag;
	}

	/**
	 * Controls how the column data types are used.
	 * If set to true, the DBMS type is used. If false
	 * a mapping between JDBC types and the target DBMS is used.
	 *
	 * @param flag if true the stored DBMS type is used
	 *
	 * @see ColumnIdentifier#getDbmsType()
	 * @see ColumnIdentifier#getDataType()
	 * @see TypeMapper
	 */
	public void useDbmsDataType(boolean flag)
	{
		this.useDbmsDataType = flag;
	}

	public void setRemoveDefaults(boolean flag)
	{
		this.removeDefaults = flag;
	}

  public void setUseFormatterSettings(boolean flag)
  {
    this.useFormatterSettings = flag;
  }

	public TableIdentifier getTable()
	{
		return this.tablename;
	}

  public String getCreateTableSQL()
    throws SQLException
  {
    StringBuilder columns = new StringBuilder(100);
    String template = null;
    if (connection != null)
    {
      template = connection.getDbSettings().getCreateTableTemplate(creationType);
    }
    else
    {
      template = DbSettings.DEFAULT_CREATE_TABLE_TEMPLATE;
    }

    String name = getIdentifier(tablename.getTableExpression(this.connection));

    int numCols = 0;
    List<String> pkCols = getPKColumns();

    columns.append("  ");
    for (ColumnIdentifier col : columnList)
    {
      String def = getColumnDefintionString(col);
      if (def == null) continue;

      if (numCols > 0) columns.append(",\n  ");
      columns.append(def);
      numCols++;
    }

    String sql = template.replace(MetaDataSqlManager.FQ_TABLE_NAME_PLACEHOLDER, name);
    sql = sql.replace(MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER, columns);

    int pos = sql.indexOf(MetaDataSqlManager.PK_INLINE_DEFINITION);

    if (pkCols.size() > 0 && pos > -1)
    {
      StringBuilder inlinePK = new StringBuilder(pkCols.size() * 10);
      inlinePK.append(",\n  ");
      String pkKeyword = "PRIMARY KEY";
      if (connection != null)
      {
        pkKeyword = connection.getDbSettings().getInlinePKKeyword();
      }
      inlinePK.append(pkKeyword);
      inlinePK.append(" (");
      for (int i = 0; i < pkCols.size(); i++)
      {
        if (i > 0) inlinePK.append(',');
        inlinePK.append(getIdentifier(getQuoteHandler().quoteObjectname(pkCols.get(i))));
      }
      inlinePK.append(')');
      sql = sql.replace(MetaDataSqlManager.PK_INLINE_DEFINITION, inlinePK.toString());
    }
    else if (pos > -1)
    {
      // make sure the placeholder is removed if no PK is defined.
      sql = sql.replace(MetaDataSqlManager.PK_INLINE_DEFINITION, "");
    }

    return sql;
  }

  public String getAddPKSQL()
  {
    if (connection == null) return null;

    List<String> colNames = getPKColumns();
    if (colNames.size() > 0)
    {
      TableSourceBuilder builder = TableSourceBuilderFactory.getBuilder(connection);
      PkDefinition pk = new PkDefinition(colNames);
      CharSequence pkSql = builder.getPkSource(this.tablename, pk, false, false);
      if (pkSql.length() > 0)
      {
        return pkSql.toString();
      }
    }
    return null;
  }

  private boolean addPKNeeded()
  {
    String template = connection.getDbSettings().getCreateTableTemplate(creationType);
    return !template.contains(MetaDataSqlManager.PK_INLINE_DEFINITION);
  }

  private List<String> getPKColumns()
  {
    List<String> pkCols = new ArrayList<>();
    for (ColumnIdentifier col : columnList)
    {
      if (col.isPkColumn())
      {
        pkCols.add(col.getColumnName());
      }
    }
    return pkCols;
  }


	/**
	 * Generate the necessary SQL and create the table in the database defined by the current connection.
	 *
	 * If enabled, the generated SQL statements are stored and can be obtained using
	 * getGeneratedSQL() afterwards.
	 *
	 * @throws SQLException
	 * @see #getGeneratedSQL()
	 * @see #setStoreSQL(boolean)
	 */
	public void createTable()
		throws SQLException
	{
    String sql = getCreateTableSQL();
		if (storeSQL)
		{
			generatedSQL = new ArrayList<>(2);
			generatedSQL.add(sql);
		}

		Statement stmt = this.connection.createStatement();

		boolean commitRequired = needsCommit();

		try
		{
      LogMgr.logInfo("TableCreator.createTable()", "Creating table using sql: " + sql);
			stmt.executeUpdate(sql);
      if (addPKNeeded())
      {
        String alterTable = getAddPKSQL();
        if (alterTable != null)
        {
					LogMgr.logInfo("TableCreator.createTable()", "Adding primary key using: " + alterTable);
					stmt.executeUpdate(alterTable);
				}
				if (storeSQL)
				{
					generatedSQL.add(alterTable);
				}
			}

			if (commitRequired)
			{
				LogMgr.logDebug("TableCreator.createTable()", "Commiting the CREATE TABLE");
				this.connection.commit();
			}
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
	}

	private boolean needsCommit()
	{
		return connection.getDbSettings().ddlNeedsCommit() &&
				   connection.getDbSettings().commitCreateTable(creationType) &&
				   !this.connection.getAutoCommit();
	}

	/**
	 * Return the SQL string for the column definition.
	 *
	 * If useDbmsDataType is set to true, then the data type
	 * stored in the ColumnIdentifier is used. Otherwise
	 * the TypeMapper is use to map the jdbc data type returned from
	 * ColumnIdentifier.getDataType() to the target DBMS
	 *
	 * If useColumnAlias is set to true, getDisplayName() is used instead of getColumnName()
	 *
	 * Internally a ColumnDefinitionTemplate is used to build the data type definition of the passed column
	 *
	 * @param col the column for which the SQL should be generated
	 *
	 * @see #setUseColumnAlias(boolean)
	 * @see #useDbmsDataType(boolean)
	 * @see ColumnIdentifier#getDataType()
	 * @see ColumnIdentifier#getDisplayName()
	 * @see TypeMapper#getTypeName(int, int, int)
	 * @see ColumnDefinitionTemplate#getColumnDefinitionSQL(workbench.db.ColumnIdentifier, java.lang.String, int, java.lang.String)
	 */
	private String getColumnDefintionString(ColumnIdentifier col)
	{
		if (col == null) return null;

		int type = col.getDataType();
		int size = col.getColumnSize();
		int digits = col.getDecimalDigits();

		String name = (useColumnAlias ? col.getDisplayName() : col.getColumnName());

		StringBuilder result = new StringBuilder(30);

    if (!getQuoteHandler().isQuoted(name))
    {
      // just use "simple" quoting rules for the target database (instead of checking
      // the case of the column name by using DbMetadata.quoteObjectName()
      // This is to ensure that the columns are created with the default case of the target DBMS
      boolean isReservedWord;

      if (connection != null)
      {
        isReservedWord = connection.getMetadata().isReservedWord(name);
      }
      else
      {
        SqlKeywordHelper helper = new SqlKeywordHelper();
        isReservedWord = helper.getReservedWords().contains(name);
      }

      if (isReservedWord || getQuoteHandler().isLegalIdentifier(name) == false)
      {
        name = getQuoteHandler().getIdentifierQuoteCharacter() + name + getQuoteHandler().getIdentifierQuoteCharacter();
      }
    }

		result.append(getIdentifier(name));
		result.append(' ');

		String typeName = null;
		if (!this.useDbmsDataType)
		{
			typeName = this.mapper.getTypeName(type, size, digits);
		}

    if (useFormatterSettings)
    {
      typeName = FormatterUtil.getDataType(typeName);
    }

		if (removeDefaults)
		{
			col = col.createCopy();
			col.setDefaultValue(null);
		}
		result.append(columnTemplate.getColumnDefinitionSQL(col, null, 0, typeName));

		return result.toString();
	}

  private String getIdentifier(String name)
  {
    if (useFormatterSettings)
    {
      return FormatterUtil.getIdentifier(name);
    }
    return name;
  }

  private QuoteHandler getQuoteHandler()
  {
    if (connection != null)
    {
      return connection.getMetadata();
    }
    return QuoteHandler.STANDARD_HANDLER;
  }

	/**
	 * Return the generated SQL statement(s) if any.
	 *
	 * Will return null if setStoreSQL() has not been enabled.
	 *
	 * @return the generated SQL statement(s) if any.
	 */
	public List<String> getGeneratedSQL()
	{
		return generatedSQL;
	}

	/**
	 * Controls if the generated SQL is stored for later use.
	 *
	 * Intended for testing purposes only.
	 * @see #getGeneratedSQL()
	 */
	public void setStoreSQL(boolean flag)
	{
		this.storeSQL = flag;
	}
}
