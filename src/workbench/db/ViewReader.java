/*
 * ViewReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.sql.formatter.SqlFormatter;
import workbench.storage.DataStore;
import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class ViewReader
{
	private WbConnection connection;

	public ViewReader(WbConnection con)
	{
		this.connection = con;
	}
	
	public CharSequence getExtendedViewSource(TableIdentifier tbl, boolean includeDrop)
		throws SQLException
	{
		return this.getExtendedViewSource(tbl, null, includeDrop);
	}

	public CharSequence getExtendedViewSource(TableIdentifier view, DataStore viewTableDefinition, boolean includeDrop)
		throws SQLException
	{
		return getExtendedViewSource(view, viewTableDefinition, includeDrop, true);
	}

	/**
	 * Returns a complete SQL statement to (re)create the given view.
	 */
	public CharSequence getExtendedViewSource(TableIdentifier view, DataStore viewTableDefinition, boolean includeDrop, boolean includeCommit)
		throws SQLException
	{
		GetMetaDataSql sql = connection.getMetadata().metaSqlMgr.getViewSourceSql();
		if (sql == null)
		{
			SourceStatementsHelp help = new SourceStatementsHelp();
			return help.explainMissingViewSourceSql(this.connection.getMetadata().getProductName());
		}

		if (viewTableDefinition == null)
		{
			viewTableDefinition = this.connection.getMetadata().getTableDefinition(view);
		}
		CharSequence source = this.getViewSource(view);

		if (StringUtil.isEmptyString(source)) return StringUtil.EMPTY_STRING;

		StringBuilder result = new StringBuilder(source.length() + 100);

		String lineEnding = Settings.getInstance().getInternalEditorLineEnding();
		String verb = SqlUtil.getSqlVerb(source);

		// ThinkSQL and DB2 return the full CREATE VIEW statement
		if (verb.equalsIgnoreCase("CREATE"))
		{
			if (includeDrop)
			{
				String type = SqlUtil.getCreateType(source);
				result.append("DROP ");
				result.append(type);
				result.append(' ');
				result.append(view.getTableName());
				result.append(';');
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
			return result.toString();
		}

		result.append(connection.getMetadata().generateCreateObject(includeDrop, view.getType(), view.getTableName()));

		if (!DbMetadata.MVIEW_NAME.equalsIgnoreCase(view.getType()))
		{
			result.append(lineEnding + "(" + lineEnding);
			int rows = viewTableDefinition.getRowCount();
			for (int i=0; i < rows; i++)
			{
				String colName = viewTableDefinition.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
				result.append("  ");
				result.append(connection.getMetadata().quoteObjectname(colName));
				if (i < rows - 1)
				{
					result.append(',');
					result.append(lineEnding);
				}
			}
			result.append(lineEnding + ")");
		}

		result.append(lineEnding + "AS " + lineEnding);
		result.append(source);
		result.append(lineEnding);

		// Oracle and MS SQL Server support materialized views. For those
		// the index definitions are of interest as well.
		DataStore indexInfo = connection.getMetadata().getIndexReader().getTableIndexInformation(view);
		if (indexInfo.getRowCount() > 0)
		{
			StringBuilder idx = this.connection.getMetadata().getIndexReader().getIndexSource(view, indexInfo, null);
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
	 *	Usually (depending on how the meta data is stored in the database) the DBMS
	 *	only stores the underlying SELECT statement, and that will be returned by this method.
	 *	To create a complete SQL to re-create a view, use {@link #getExtendedViewSource(TableIdentifier, DataStore, boolean)}
	 *
	 *	@return the view source as stored in the database.
	 */
	public CharSequence getViewSource(TableIdentifier viewId)
	{
		if (viewId == null) return null;

		if (connection.getMetadata().isOracle() && DbMetadata.MVIEW_NAME.equalsIgnoreCase(viewId.getType()))
		{
			return connection.getMetadata().getOracleMeta().getSnapshotSource(viewId);
		}

		StringBuilder source = new StringBuilder(500);
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			GetMetaDataSql sql = connection.getMetadata().metaSqlMgr.getViewSourceSql();
			if (sql == null) return StringUtil.EMPTY_STRING;
			TableIdentifier tbl = viewId.createCopy();
			tbl.adjustCase(connection);
			sql.setSchema(tbl.getSchema());
			sql.setObjectName(tbl.getTableName());
			sql.setCatalog(tbl.getCatalog());
			stmt = connection.createStatementForQuery();
			String query = this.connection.getMetadata().adjustHsqlQuery(sql.getSql());
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logInfo("DbMetadata.getViewSource()", "Using query=\n" + query);
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
			StringUtil.trimTrailingWhitespace(source);
			if (this.connection.getDbSettings().getFormatViewSource())
			{
				SqlFormatter f = new SqlFormatter(source);
				source = new StringBuilder(f.getFormattedSql());
			}
			if (!StringUtil.endsWith(source, ';')) source.append(';');
			source.append(Settings.getInstance().getInternalEditorLineEnding());

			ViewGrantReader grantReader = ViewGrantReader.createViewGrantReader(connection);
			if (grantReader != null)
			{
				CharSequence grants = grantReader.getViewGrantSource(connection, viewId);
				if (grants != null && grants.length() > 0)
				{
					source.append(Settings.getInstance().getInternalEditorLineEnding());
					source.append(grants);
					source.append(Settings.getInstance().getInternalEditorLineEnding());
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logWarning("DbMetadata.getViewSource()", "Could not retrieve view definition for " + viewId.getTableExpression(), e);
			source = new StringBuilder(ExceptionUtil.getDisplay(e));
			if (this.connection.getMetadata().isPostgres()) try { this.connection.rollback(); } catch (Throwable th) {}
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		return source;
	}

}
