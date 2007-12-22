/*
 * ObjectScripter.java
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

import java.sql.SQLException;
import java.util.List;
import workbench.resource.ResourceMgr;
import workbench.util.ExceptionUtil;

import workbench.interfaces.ScriptGenerationMonitor;
import workbench.interfaces.Scripter;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.storage.DmlStatement;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.SqlLiteralFormatter;
import workbench.storage.StatementFactory;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class ObjectScripter
	implements Scripter
{
	public static final String TYPE_SEQUENCE = "sequence";
	public static final String TYPE_TABLE = "table";
	public static final String TYPE_VIEW = "view";
	public static final String TYPE_SYNONYM = "synonym";
	public static final String TYPE_INSERT = "insert";
	public static final String TYPE_SELECT = "select";
	public static final String TYPE_PROC = "procedure";
	public static final String TYPE_PKG = "package";
	public static final String TYPE_FUNC = "function";
	public static final String TYPE_TRG = "trigger";
	public static final String TYPE_MVIEW = DbMetadata.MVIEW_NAME.toLowerCase();

	private List<? extends DbObject> objectList;
	private DbMetadata meta;
	private StringBuilder script;
	private ScriptGenerationMonitor progressMonitor;
	private WbConnection dbConnection;
	private boolean cancel;
	private String nl = Settings.getInstance().getInternalEditorLineEnding();
	
	public ObjectScripter(List<? extends DbObject> objectList, WbConnection aConnection)
	{
		this.objectList = objectList;
		this.dbConnection = aConnection;
		this.meta = aConnection.getMetadata();
	}

	public void setProgressMonitor(ScriptGenerationMonitor aMonitor)
	{
		this.progressMonitor = aMonitor;
	}

	public String getScript()
	{
		if (this.script == null) this.generateScript();
		return this.script.toString();
	}
	
	public boolean isCancelled()
	{
		return this.cancel;
	}
	
	public void generateScript()
	{
		try
		{
			this.dbConnection.setBusy(true);
			this.cancel = false;
			this.script = new StringBuilder(this.objectList.size() * 500);
			if (!cancel) this.appendObjectType(TYPE_SEQUENCE);
			if (!cancel) this.appendObjectType(TYPE_TABLE);
			if (!cancel) this.appendForeignKeys();
			if (!cancel) this.appendObjectType(TYPE_VIEW);
			if (!cancel) this.appendObjectType(TYPE_SYNONYM);
			if (!cancel) this.appendObjectType(TYPE_MVIEW);
			if (!cancel) this.appendObjectType(TYPE_INSERT);
			if (!cancel) this.appendObjectType(TYPE_SELECT);
			if (!cancel) this.appendObjectType(TYPE_FUNC);
			if (!cancel) this.appendObjectType(TYPE_PROC);
			if (!cancel) this.appendObjectType(TYPE_PKG);
			if (!cancel) this.appendObjectType(TYPE_TRG);
		}
		finally 
		{
			this.dbConnection.setBusy(false);
		}
	}

	public void cancel()
	{
		this.cancel = true;
	}
	
	private void appendForeignKeys()
	{
		if (this.progressMonitor != null)
		{
			this.progressMonitor.setCurrentObject(ResourceMgr.getString("TxtScriptProcessFk"));
		}
		boolean first = true;
		for (DbObject dbo : objectList)
		{
			if (cancel) break;
			String type = dbo.getObjectType();
			if (!type.equalsIgnoreCase(TYPE_TABLE)) continue;
			
			TableIdentifier tbl = (TableIdentifier)dbo;
			tbl.adjustCase(this.dbConnection);
			StringBuilder source = meta.getFkSource(tbl);
			if (source != null && source.length() > 0)
			{
				if (first)
				{
					this.script.append("-- BEGIN FOREIGN KEYS --" + nl + nl);
					first = false;
				}
				script.append(source);
			}
		}	
		if (!first)
		{
			// no table was found, so no FK was added --> do not add separator
			this.script.append("-- END FOREIGN KEYS --" + nl);
		}
	}
	
	private void appendObjectType(String typeFilter)
	{
		for (DbObject dbo : objectList)
		{
			if (cancel) break;
			String type = dbo.getObjectType();
			
			if (!type.equalsIgnoreCase(typeFilter)) continue;
			
			CharSequence source = null;
			
			if (this.progressMonitor != null)
			{
				this.progressMonitor.setCurrentObject(dbo.getObjectName());
			}
			try
			{
				if (dbo instanceof ProcedureDefinition)
				{
					ProcedureDefinition procDef = (ProcedureDefinition)dbo;
					meta.readProcedureSource(procDef);
					source = procDef.getSource();
				}
				else if (dbo instanceof TriggerDefinition)
				{
					TriggerDefinition trg = (TriggerDefinition)dbo;
					source = meta.getTriggerSource(trg.getCatalog(), trg.getSchema(), trg.getObjectName());
				}
				else if (dbo instanceof TableIdentifier)
				{
					TableIdentifier tbl = (TableIdentifier)dbo;
					
					if (TYPE_TABLE.equalsIgnoreCase(type))
					{
						source = meta.getTableSource(tbl, true, false);
					}
					else if (TYPE_VIEW.equalsIgnoreCase(type) || TYPE_MVIEW.equalsIgnoreCase(type))
					{
						source = meta.getExtendedViewSource(tbl, false);
					}
					else if (TYPE_SYNONYM.equalsIgnoreCase(type))
					{
						source = meta.getSynonymSource(tbl);
					}
					else if (TYPE_INSERT.equalsIgnoreCase(type))
					{
						source = this.getEmptyInsert(tbl);
					}
					else if (TYPE_SELECT.equalsIgnoreCase(type))
					{
						source = this.getDefaultSelect(tbl);
					}
					else if (TYPE_SEQUENCE.equalsIgnoreCase(type))
					{
						String object = tbl.getTableExpression(this.dbConnection);
						source = this.meta.getSequenceSource(object);
					}
				}
				else if (dbo instanceof SequenceDefinition)
				{
					source = this.meta.getSequenceSource(dbo.getObjectExpression(dbConnection));
				}
			}
			catch (Exception e)
			{
				this.script.append("\nError creating script for " + dbo.getObjectName() + " " + ExceptionUtil.getDisplay(e));
			}

			if (source != null && source.length() > 0)
			{
				boolean useSeparator = !type.equalsIgnoreCase("insert") && !type.equalsIgnoreCase("select");
				if (useSeparator) this.script.append("-- BEGIN " + type + " " + dbo.getObjectName() + nl);
				this.script.append(source);
				if (useSeparator) this.script.append(nl + "-- END " + type + " " + dbo.getObjectName() + nl);
				this.script.append(nl);
			}
		}
	}
	
	/**
	 *	Return an "empty" INSERT statement for the given table.
	 */
	public String getEmptyInsert(TableIdentifier tbl)
		throws SQLException
	{
    boolean makePrepared = Settings.getInstance().getBoolProperty("workbench.sql.generate.defaultinsert.prepared", false);
		ResultInfo info = new ResultInfo(tbl, this.dbConnection);
		info.setUpdateTable(tbl);
		StatementFactory factory = new StatementFactory(info, this.dbConnection);
		
		SqlLiteralFormatter f = new SqlLiteralFormatter(this.dbConnection);
		
		RowData dummyData = new RowData(info.getColumnCount());

		// This is a "trick" to fool the StatementFactory which will
		// check the type of the Data. In case it does not "know" the 
		// class, it calls toString() to get the value of the column
		// this way we get a question mark for each value
		StringBuilder marker = new StringBuilder("?");
		
		for (int i=0; i < info.getColumnCount(); i++)
		{
			if (makePrepared)
			{
				dummyData.setValue(i, marker);
			}
			else
			{
				int type = info.getColumnType(i);
				StringBuilder dummy = new StringBuilder();
				if (SqlUtil.isCharacterType(type)) dummy.append('\'');
				dummy.append(info.getColumnName(i));
				dummy.append("_value");
				if (SqlUtil.isCharacterType(type)) dummy.append('\'');
				dummyData.setValue(i, dummy);
			}
		}
		DmlStatement stmt = factory.createInsertStatement(dummyData, true);
		String sql = stmt.getExecutableStatement(f) + ";" + nl;
		return sql;
	}
	
	/**
	 *	Return a default SELECT statement for the given table.
	 */
	public String getDefaultSelect(TableIdentifier tbl)
		throws SQLException
	{
		DataStore tableDef = this.meta.getTableDefinition(tbl);

		if (tableDef.getRowCount() == 0) return StringUtil.EMPTY_STRING;
		int colCount = tableDef.getRowCount();
		if (colCount == 0) return StringUtil.EMPTY_STRING;

		StringBuilder sql = new StringBuilder(colCount * 80);

		sql.append("SELECT ");
		for (int i=0; i < colCount; i++)
		{
			String column = tableDef.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_DEFINITION_COL_NAME);
			//column = SqlUtil.quoteObjectname(column);
			if (i > 0)
			{
				sql.append(',');
				sql.append(nl);
				sql.append("       ");
			}

			sql.append(column);
		}
		sql.append(nl);
		sql.append("FROM ");
		sql.append(tbl.getTableExpression(this.dbConnection));
		sql.append(';');
		sql.append(nl);

		return sql.toString();
	}


}
