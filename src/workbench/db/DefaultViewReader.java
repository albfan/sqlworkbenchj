/*
 * DefaultViewReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.sql.formatter.WbSqlFormatter;

import workbench.util.CollectionUtil;
import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to read the source of a database view.
 * <br/>
 * The source is retrieved by using SQL statements defined in the file
 * <literal>ViewSourceStatements.xml</literal>.
 * <br/>
 *
 * @author Thomas Kellerer
 * @see MetaDataSqlManager#getViewSourceSql()
 */
public class DefaultViewReader
	implements ViewReader
{
	protected WbConnection connection;

	public DefaultViewReader(WbConnection con)
	{
		this.connection = con;
	}

	@Override
	public CharSequence getExtendedViewSource(TableIdentifier tbl)
		throws SQLException
	{
		return getExtendedViewSource(new TableDefinition(tbl), DropType.none, false);
	}

	@Override
	public CharSequence getExtendedViewSource(TableIdentifier tbl, DropType dropType)
		throws SQLException
	{
		return getExtendedViewSource(new TableDefinition(tbl), dropType, false);
	}

  @Override
	public CharSequence getFullViewSource(TableDefinition view)
		throws SQLException, NoConfigException
  {
    return createFullViewSource(view, DropType.none, false);
  }

	protected CharSequence createFullViewSource(TableDefinition view, DropType dropType, boolean includeCommit)
		throws SQLException, NoConfigException
  {
		TableIdentifier viewTable = view.getTable();
		CharSequence source = this.getViewSource(viewTable);

		List<ColumnIdentifier> columns = view.getColumns();

		if (CollectionUtil.isEmpty(columns))
		{
			view = this.connection.getMetadata().getTableDefinition(view.getTable());
			columns = view.getColumns();
		}

		if (StringUtil.isEmptyString(source)) return StringUtil.EMPTY_STRING;

		StringBuilder result = new StringBuilder(source.length() + 100);

		String lineEnding = Settings.getInstance().getInternalEditorLineEnding();
		String verb = connection.getParsingUtil().getSqlVerb(source.toString());

		TableSourceBuilder builder = TableSourceBuilderFactory.getBuilder(connection);

		// SQL Server and DB2 return the full CREATE VIEW statement
		// DB2 even returns the CREATE OR REPLACE if the view was created that way.
		// Teradata returns a complete REPLACE VIEW ... statement
		// therefor the verb is compared with startsWith() rather than equals()
    if (verb.startsWith("CREATE") || verb.equals("REPLACE"))
		{
			if (dropType != DropType.none && !verb.equals("CREATE OR REPLACE") && !verb.equalsIgnoreCase("REPLACE"))
			{
				result.append(builder.generateDrop(viewTable, dropType));
				result.append(lineEnding);
				result.append(lineEnding);
			}
			result.append(source);
			if (this.connection.getDbSettings().ddlNeedsCommit() && includeCommit)
			{
				result.append(lineEnding);
				result.append("COMMIT;");
				result.append(lineEnding);
			}
		}
		else
		{
      if  (dropType != DropType.none && viewTable.getType().equalsIgnoreCase(DbMetadata.MVIEW_NAME))
      {
        result.append(builder.generateDrop(viewTable, dropType));
        result.append(lineEnding);
        result.append(lineEnding);
      }

			result.append(builder.generateCreateObject(DropType.none, viewTable, null));

			if (connection.getDbSettings().generateColumnListInViews())
			{
				result.append(lineEnding);
				result.append('(');
				result.append(lineEnding);

				int colCount = columns.size();
				for (int i=0; i < colCount; i++)
				{

					String colName = columns.get(i).getColumnName();
					result.append("  ");
					result.append(connection.getMetadata().quoteObjectname(colName));
					if (i < colCount - 1)
					{
						result.append(',');
						result.append(lineEnding);
					}
				}
				result.append(lineEnding);
				result.append(')');
			}
			result.append(lineEnding);
			result.append("AS ");
			result.append(lineEnding);
			result.append(source);
			result.append(lineEnding);
		}
    return result;
  }

	/**
	 * Returns a complete SQL statement to (re)create the given view.
	 *
	 * This method will extend the stored source to a valid CREATE VIEW.
	 *
	 * @param view The view for which thee source should be created
	 * @param includeCommit if true, terminate the whole statement with a COMMIT
	 * @param includeDrop if true, add a DROP statement before the CREATE statement
	 *
	 * @see #getViewSource(workbench.db.TableIdentifier)
	 */
	@Override
	public CharSequence getExtendedViewSource(TableDefinition view, DropType dropType, boolean includeCommit)
		throws SQLException
	{
		TableIdentifier viewTable = view.getTable();

		CharSequence source = null;
		try
		{
			source = createFullViewSource(view, dropType, includeCommit);
		}
		catch (NoConfigException no)
		{
			SourceStatementsHelp help = new SourceStatementsHelp(this.connection.getMetadata().getMetaDataSQLMgr());
			return help.explainMissingViewSourceSql();
		}

		if (StringUtil.isEmptyString(source)) return StringUtil.EMPTY_STRING;

		StringBuilder result = new StringBuilder(source.length() + 100);
    result.append(source);

		String lineEnding = Settings.getInstance().getInternalEditorLineEnding();

		ViewGrantReader grantReader = ViewGrantReader.createViewGrantReader(connection);
		if (grantReader != null)
		{
			CharSequence grants = grantReader.getViewGrantSource(connection, view.getTable());
			if (grants != null && grants.length() > 0)
			{
				result.append(Settings.getInstance().getInternalEditorLineEnding());
				result.append(Settings.getInstance().getInternalEditorLineEnding());
				result.append(grants);
				result.append(Settings.getInstance().getInternalEditorLineEnding());
			}
		}

		TableCommentReader commentReader = new TableCommentReader();
		String viewComment = commentReader.getTableCommentSql(this.connection, view.getTable());
		if (StringUtil.isNonBlank(viewComment))
		{
			result.append(viewComment);
			if (!viewComment.endsWith(";")) result.append(';');
		}

		StringBuilder colComments = commentReader.getTableColumnCommentsSql(this.connection, view.getTable(), view.getColumns());
		if (StringUtil.isNonBlank(colComments))
		{
			result.append(lineEnding);
			result.append(colComments);
			result.append(lineEnding);
		}

		// Oracle and MS SQL Server support materialized views. For those
		// the index definitions are of interest as well.
		List<IndexDefinition> indexInfo = connection.getMetadata().getIndexReader().getTableIndexList(viewTable);
		if (indexInfo.size() > 0)
		{
			StringBuilder idx = this.connection.getMetadata().getIndexReader().getIndexSource(viewTable, indexInfo);
			if (idx != null && idx.length() > 0)
			{
				result.append(lineEnding);
				result.append(lineEnding);
				result.append(idx);
				result.append(lineEnding);
			}
		}

		if (this.connection.getDbSettings().ddlNeedsCommit() && includeCommit)
		{
			result.append("COMMIT;");
		}
		return result;
	}

	/**
	 *	Return the source of a view definition as it is stored in the database.
	 *  <br/>
	 *	Usually (depending on how the meta data is stored in the database) the DBMS
	 *	only stores the underlying SELECT statement (but not a full CREATE VIEW),
	 *  and that will be returned by this method.
	 *  <br/>
	 *	To create a complete SQL to re-create a view, use {@link #getExtendedViewSource(workbench.db.TableIdentifier) }
	 *
	 *	@return the view source as stored in the database.
	 *  @throws NoConfigException if no SQL was configured in ViewSourceStatements.xml
	 */
	@Override
	public CharSequence getViewSource(TableIdentifier viewId)
		throws NoConfigException
	{
		if (viewId == null) return null;

		GetMetaDataSql sql = connection.getMetadata().getMetaDataSQLMgr().getViewSourceSql();
		if (sql == null) throw new NoConfigException("No SQL to retrieve the VIEW source");

		StringBuilder source = new StringBuilder(500);
		Statement stmt = null;
		ResultSet rs = null;
		Savepoint sp = null;
		String query = null;

		try
		{
			if (connection.getDbSettings().useSavePointForDML())
			{
				sp = connection.setSavepoint();
			}
			TableIdentifier tbl = viewId.createCopy();
			tbl.adjustCase(connection);
			sql.setSchema(tbl.getRawSchema());
			sql.setObjectName(tbl.getRawTableName());
			sql.setCatalog(tbl.getRawCatalog());
			stmt = connection.createStatementForQuery();
			query = sql.getSql();
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logInfo("DbMetadata.getViewSource()", "Retrieving view source using query=\n" + query);
			}
			rs = stmt.executeQuery(query);
			while (rs.next())
			{
				String line = rs.getString(1);
				if (line != null)
				{
					source.append(line);
				}
			}

			if (source.length() > 0)
			{
				StringUtil.trimTrailingWhitespace(source);
				if (this.connection.getDbSettings().getFormatViewSource())
				{
					WbSqlFormatter f = new WbSqlFormatter(source, connection.getDbId());
					source = new StringBuilder(f.getFormattedSql());
				}

				if (!StringUtil.endsWith(source, ';'))
				{
					source.append(';');
					source.append(Settings.getInstance().getInternalEditorLineEnding());
				}
			}
			connection.releaseSavepoint(sp);
		}
		catch (Exception e)
		{
			LogMgr.logError("DefaultViewReader.getViewSource()", "Could not retrieve view definition for " + viewId.getTableExpression() + " using SQL:\n" + query, e);
			source = new StringBuilder(ExceptionUtil.getDisplay(e));
			connection.rollback(sp);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		return source;
	}

}
