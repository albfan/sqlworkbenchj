/*
 * DefaultTriggerReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.sql.DelimiterDefinition;
import workbench.storage.DataStore;
import workbench.util.CollectionUtil;
import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to read triggers from the database.
 * The reading is done by Statements configured in XML files.
 *
 * @author Thomas Kellerer
 * @see MetaDataSqlManager
 */
public class DefaultTriggerReader
	implements TriggerReader
{
	protected WbConnection dbConnection;
	protected DbMetadata dbMeta;

	public DefaultTriggerReader(WbConnection conn)
	{
		this.dbMeta = conn.getMetadata();
		this.dbConnection = conn;
	}

	/**
	 * Return a list of triggers available in the given schema.
	 */
	public DataStore getTriggers(String catalog, String schema)
		throws SQLException
	{
		return getTriggers(catalog, schema, null);
	}

	public List<TriggerDefinition> getTriggerList(String catalog, String schema, String baseTable)
		throws SQLException
	{
		DataStore triggers = getTriggers(catalog, schema, baseTable);
		List<TriggerDefinition> result = new ArrayList<TriggerDefinition>(triggers.getRowCount());
		for (int row = 0; row < triggers.getRowCount(); row ++)
		{
			String trgName = triggers.getValueAsString(row, COLUMN_IDX_TABLE_TRIGGERLIST_TRG_NAME);
			String trgType = triggers.getValueAsString(row, COLUMN_IDX_TABLE_TRIGGERLIST_TRG_TYPE);
			String trgEvent = triggers.getValueAsString(row, COLUMN_IDX_TABLE_TRIGGERLIST_TRG_EVENT);
			String tableName = triggers.getValueAsString(row, COLUMN_IDX_TABLE_TRIGGERLIST_TRG_TABLE);
			String comment = triggers.getValueAsString(row, COLUMN_IDX_TABLE_TRIGGERLIST_TRG_COMMENT);
			TriggerDefinition trg = new TriggerDefinition(catalog, schema, trgName);
			trg.setTriggerType(trgType);
			trg.setTriggerEvent(trgEvent);
			trg.setComment(comment);
			if (tableName != null)
			{
				TableIdentifier tbl = new TableIdentifier(tableName);
				trg.setRelatedTable(tbl);
			}
			result.add(trg);
		}
		return result;
	}
	/**
	 *	Return the list of defined triggers for the given table.
	 */
	public DataStore getTableTriggers(TableIdentifier table)
		throws SQLException
	{
		TableIdentifier tbl = table.createCopy();
		tbl.adjustCase(this.dbConnection);
		return getTriggers(tbl.getCatalog(), tbl.getSchema(), tbl.getTableName());
	}

	protected DataStore getTriggers(String catalog, String schema, String tableName)
		throws SQLException
	{
		final int[] types =   {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		final int[] sizes =   {30, 30, 20, 20, 20};

		DataStore result = new DataStore(LIST_COLUMNS, types, sizes);

		GetMetaDataSql sql = dbMeta.metaSqlMgr.getListTriggerSql();
		if (sql == null)
		{
			LogMgr.logWarning("TriggerReader.getTriggers()", "getTriggers() called but no SQL configured");
			return result;
		}

		sql.setSchema(schema);
		sql.setCatalog(catalog);
		sql.setObjectName(tableName);

		Statement stmt = this.dbConnection.createStatementForQuery();
		String query = sql.getSql();

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("DbMetadata.getTableTriggers()", "Using query=\n" + query);
		}
		ResultSet rs = stmt.executeQuery(query);
		try
		{
			boolean hasTableName = rs.getMetaData().getColumnCount() >= 4;
			boolean hasComment = rs.getMetaData().getColumnCount() >= 5;

			while (rs.next())
			{
				int row = result.addRow();
				String value = rs.getString(1);
				if (!rs.wasNull() && value != null) value = value.trim();
				result.setValue(row, COLUMN_IDX_TABLE_TRIGGERLIST_TRG_NAME, value);

				value = rs.getString(2);
				if (!rs.wasNull() && value != null) value = value.trim();
				result.setValue(row, COLUMN_IDX_TABLE_TRIGGERLIST_TRG_TYPE, value);

				value = rs.getString(3);
				if (!rs.wasNull() && value != null) value = value.trim();
				result.setValue(row, COLUMN_IDX_TABLE_TRIGGERLIST_TRG_EVENT, value);

				if (hasTableName)
				{
					value = rs.getString(4);
					if (!rs.wasNull() && value != null) value = value.trim();
					result.setValue(row, COLUMN_IDX_TABLE_TRIGGERLIST_TRG_TABLE, value);
				}

				if (hasComment)
				{
					value = rs.getString(5);
					if (!rs.wasNull() && value != null) value = value.trim();
					result.setValue(row, COLUMN_IDX_TABLE_TRIGGERLIST_TRG_COMMENT, value);
				}
			}
			result.resetStatus();
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	public TriggerDefinition findTrigger(String catalog, String schema, String name)
		throws SQLException
	{
		List<TriggerDefinition> triggers = getTriggerList(catalog, schema, null);
		if (CollectionUtil.isEmpty(triggers)) return null;
		for (TriggerDefinition trg : triggers)
		{
			if (trg.getObjectName().equalsIgnoreCase(name))
			{
				return trg;
			}
		}
		return null;
	}
	
	public String getTriggerSource(TriggerDefinition trigger, boolean includeDependencies)
		throws SQLException
	{
		return getTriggerSource(trigger.getCatalog(), trigger.getSchema(), trigger.getObjectName(), trigger.getRelatedTable(), trigger.getComment(), includeDependencies);
	}

	/**
	 * Retrieve the SQL Source of the given trigger.
	 *
	 * @param triggerCatalog The catalog in which the trigger is defined. This should be null if the DBMS does not support catalogs
	 * @param triggerSchema The schema in which the trigger is defined. This should be null if the DBMS does not support schemas
	 * @param triggerName
	 * @param triggerTable the table for which the trigger is defined
	 * @throws SQLException
	 * @return the trigger source
	 */
	public String getTriggerSource(String triggerCatalog, String triggerSchema, String triggerName, TableIdentifier triggerTable, String trgComment, boolean includeDependencies)
		throws SQLException
	{
		StringBuilder result = new StringBuilder(500);

		if ("*".equals(triggerCatalog)) triggerCatalog = null;
		if ("*".equals(triggerSchema)) triggerSchema = null;

		GetMetaDataSql sql = dbMeta.metaSqlMgr.getTriggerSourceSql();
		if (sql == null) return StringUtil.EMPTY_STRING;

		sql.setSchema(triggerSchema);
		sql.setCatalog(triggerCatalog);
		sql.setObjectName(triggerName);

		if (triggerTable != null)
		{
			sql.setBaseObjectName(triggerTable.getTableName());
		}
		Statement stmt = this.dbConnection.createStatementForQuery();
		String query = sql.getSql();

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("DbMetadata.getTriggerSource()", "Using query=\n" + query);
		}

		String nl = Settings.getInstance().getInternalEditorLineEnding();

		ResultSet rs = null;
		try
		{
			stmt.execute(query);
			rs = stmt.getResultSet();

			if (rs != null)
			{
				int colCount = rs.getMetaData().getColumnCount();
				while (rs.next())
				{
					for (int i=1; i <= colCount; i++)
					{
						String line = rs.getString(i);
						result.append(line);
					}
				}
			}
			
			CharSequence warn = SqlUtil.getWarnings(this.dbConnection, stmt);
			if (warn != null)
			{
				if (result.length() > 0) result.append(nl + nl);
				result.append(warn);
			}

			if (includeDependencies)
			{
				if (dbConnection.getDbSettings().createTriggerNeedsAlternateDelimiter())
				{
					DelimiterDefinition delim = Settings.getInstance().getAlternateDelimiter(dbConnection);

					if (result.length() > 0 && delim != null && !delim.isStandard())
					{
						result.append(nl);
						result.append(delim.getDelimiter());
					}
				}
				
				CommentSqlManager mgr = new CommentSqlManager(this.dbConnection.getMetadata().getDbId());
				String ddl = mgr.getCommentSqlTemplate("trigger");
				if (result.length() > 0 && StringUtil.isNonBlank(ddl) && StringUtil.isNonBlank(trgComment))
				{
					result.append(nl);
					String commentSql = ddl.replace(TriggerDefinition.PLACEHOLDER_TRIGGER_NAME, triggerName);
					commentSql = commentSql.replace(TriggerDefinition.PLACEHOLDER_TRIGGER_SCHEMA, triggerSchema);
					commentSql = commentSql.replace(TriggerDefinition.PLACEHOLDER_TRIGGER_TABLE, triggerTable.getTableExpression(dbConnection));
					commentSql = commentSql.replace(CommentSqlManager.COMMENT_PLACEHOLDER, SqlUtil.escapeQuotes(trgComment));
					result.append(nl);
					result.append(commentSql);
					result.append(';');
					result.append(nl);
				}

				CharSequence dependent = getDependentSource(triggerCatalog, triggerSchema, triggerName, triggerTable);
				if (dependent != null)
				{
					result.append(nl);
					result.append(dependent);
				}
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("DbMetadata.getTriggerSource()", "Error reading trigger source", e);
			if (this.dbMeta.isPostgres()) try { this.dbConnection.rollback(); } catch (Throwable th) {}
			result.append(ExceptionUtil.getDisplay(e));
			SqlUtil.closeAll(rs, stmt);
			return result.toString();
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		boolean replaceNL = Settings.getInstance().getBoolProperty("workbench.db." + dbMeta.getDbId() + ".replacenl.triggersource", false);

		String source = result.toString();
		if (replaceNL && source.length() > 0)
		{
			source = StringUtil.replace(source, "\\n", nl);
		}
		return source;
	}

	public CharSequence getDependentSource(String triggerCatalog, String triggerSchema, String triggerName, TableIdentifier triggerTable)
		throws SQLException
	{
		return null;
	}
}
