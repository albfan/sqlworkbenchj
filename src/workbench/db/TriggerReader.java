/*
 * TriggerReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
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
public class TriggerReader
{
	private WbConnection dbConnection;
	private DbMetadata dbMeta;

	public TriggerReader(WbConnection conn)
	{
		this.dbMeta = conn.getMetadata();
		this.dbConnection = conn;
	}

	/**
	 *	The column index in the DataStore returned by getTableTriggers which identifies
	 *  the name of the trigger.
	 */
	public static final int COLUMN_IDX_TABLE_TRIGGERLIST_TRG_NAME = 0;
	/**
	 *	The column index in the DataStore returned by getTableTriggers which identifies
	 *  the type (INSERT, UPDATE etc) of the trigger.
	 */
	public static final int COLUMN_IDX_TABLE_TRIGGERLIST_TRG_TYPE = 1;
	/**
	 *	The column index in the DataStore returned by getTableTriggers which identifies
	 *  the event (before, after) of the trigger.
	 */
	public static final int COLUMN_IDX_TABLE_TRIGGERLIST_TRG_EVENT = 2;

	/**
	 *	The column index in the DataStore returned by getTableTriggers which identifies
	 *  the table of the trigger.
	 */
	public static final int COLUMN_IDX_TABLE_TRIGGERLIST_TRG_TABLE = 3;

	/**
	 *	The column index in the DataStore returned by getTableTriggers which identifies
	 *  the comment of the trigger.
	 */
	public static final int COLUMN_IDX_TABLE_TRIGGERLIST_TRG_COMMENT = 4;

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

	public static final String TRIGGER_NAME_COLUMN = "TRIGGER";
	public static final String TRIGGER_TYPE_COLUMN = "TYPE";
	public static final String TRIGGER_EVENT_COLUMN = "EVENT";
	public static final String TRIGGER_TABLE_COLUMN = "TABLE";
	public static final String TRIGGER_COMMENT_COLUMN = "REMARKS";

	public static final String[] LIST_COLUMNS = {TRIGGER_NAME_COLUMN, TRIGGER_TYPE_COLUMN, TRIGGER_EVENT_COLUMN, TRIGGER_TABLE_COLUMN, TRIGGER_COMMENT_COLUMN};

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
			boolean hasTriggerName = rs.getMetaData().getColumnCount() >= 4;
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

				if (hasTriggerName)
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
	
	public String getTriggerSource(TriggerDefinition trigger)
		throws SQLException
	{
		return getTriggerSource(trigger.getCatalog(), trigger.getSchema(), trigger.getObjectName(), trigger.getRelatedTable(), trigger.getComment());
	}

	/**
	 * Retrieve the SQL Source of the given trigger.
	 *
	 * @param aCatalog The catalog in which the trigger is defined. This should be null if the DBMS does not support catalogs
	 * @param aSchema The schema in which the trigger is defined. This should be null if the DBMS does not support schemas
	 * @param aTriggername
	 * @param triggerTable the table for which the trigger is defined
	 * @throws SQLException
	 * @return the trigger source
	 */
	public String getTriggerSource(String aCatalog, String aSchema, String aTriggername, TableIdentifier triggerTable, String trgComment)
		throws SQLException
	{
		StringBuilder result = new StringBuilder(500);

		if ("*".equals(aCatalog)) aCatalog = null;
		if ("*".equals(aSchema)) aSchema = null;

		GetMetaDataSql sql = dbMeta.metaSqlMgr.getTriggerSourceSql();
		if (sql == null) return StringUtil.EMPTY_STRING;

		sql.setSchema(aSchema);
		sql.setCatalog(aCatalog);
		sql.setObjectName(aTriggername);

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
			// for some DBMS (e.g. SQL Server)
			// we need to run a exec which might not work
			// when using executeQuery() (depending on the JDBC driver)
			stmt.execute(query);
			rs = stmt.getResultSet();

			if (rs != null)
			{
				int colCount = rs.getMetaData().getColumnCount();
				while (rs.next())
				{
					for (int i=1; i <= colCount; i++)
					{
						result.append(rs.getString(i));
					}
				}
			}
			CharSequence warn = SqlUtil.getWarnings(this.dbConnection, stmt);
			if (warn != null)
			{
				if (result.length() > 0) result.append(nl + nl);
				result.append(warn);
			}

			DelimiterDefinition delim = Settings.getInstance().getAlternateDelimiter(dbConnection);
			if (result.length() > 0 && delim != null && !delim.isStandard())
			{
				result.append(nl);
				result.append(delim.getDelimiter());
			}
			CommentSqlManager mgr = new CommentSqlManager(this.dbConnection.getMetadata().getDbId());
			String ddl = mgr.getCommentSqlTemplate("trigger");
			if (result.length() > 0 && StringUtil.isNonBlank(ddl) && StringUtil.isNonBlank(trgComment))
			{
				String commentSql = ddl.replace(CommentSqlManager.COMMENT_OBJECT_NAME_PLACEHOLDER, aTriggername);
				commentSql = commentSql.replace(CommentSqlManager.COMMENT_SCHEMA_PLACEHOLDER, aSchema);
				commentSql = commentSql.replace(MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, triggerTable.getTableExpression(dbConnection));
				commentSql = commentSql.replace(CommentSqlManager.COMMENT_PLACEHOLDER, SqlUtil.escapeQuotes(trgComment));
				result.append(nl);
				result.append(commentSql);
				result.append(nl);
				if (!delim.isStandard())
				{
					result.append(delim.getDelimiter());
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

}
