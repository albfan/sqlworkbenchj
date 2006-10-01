/*
 * SqlRowDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.sql.SQLException;
import java.util.List;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.DmlStatement;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.SqlLiteralFormatter;
import workbench.storage.StatementFactory;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class SqlRowDataConverter
	extends RowDataConverter
{
	private static final int BLOB_ANSI_LITERAL = 1;
	private static final int BLOB_DBMS_LITERAL = 2;
	private static final int BLOB_FILE = 4;
	
	public static final int SQL_INSERT = 1;
	public static final int SQL_UPDATE = 2;
	public static final int SQL_DELETE_INSERT = 3;
	
	// This instance can be re-used for several 
	// table exports from DataExporter, to prevent 
	// that one failed export for the requested type
	// resets the export type for subsequent tables
	// the requested sqlType is stored in sqlType
	// Upon setting the ResultInfo in setResultInfo()
	// the sqlTypeToUse is set accordingly and then 
	// used in convertRowData()
	private int sqlTypeToUse = SQL_INSERT;
	private int sqlType = SQL_INSERT;
	
	private boolean createTable = false;
	private TableIdentifier alternateUpdateTable;
	private int commitEvery;
	private String concatString;
	private String chrFunction;
	private String concatFunction;
	private String sql;
	private StatementFactory factory;
	private List keyColumnsToUse;
	private String lineTerminator = "\n";
	private String doubleLineTerminator = "\n\n";
	private boolean includeOwner = true;
	private boolean doFormatting = true; 
	private int blobType = BLOB_ANSI_LITERAL;
	private SqlLiteralFormatter literalFormatter;
	
	public SqlRowDataConverter(WbConnection con)
	{
		super();
		setOriginalConnection(con);
	}

	public void setOriginalConnection(WbConnection con)
	{
		super.setOriginalConnection(con);
		this.literalFormatter = new SqlLiteralFormatter(con);
	}

	public void setResultInfo(ResultInfo meta)
	{
		super.setResultInfo(meta);
		this.factory = new StatementFactory(meta, this.originalConnection);
		this.needsUpdateTable = meta.getUpdateTable() == null;
		this.factory.setIncludeTableOwner(this.includeOwner);
		this.factory.setTableToUse(this.alternateUpdateTable);
		
		boolean keysPresent = this.checkKeyColumns();
		this.sqlTypeToUse = this.sqlType;
		if (!keysPresent && (this.sqlType == SQL_DELETE_INSERT || this.sqlType == SQL_UPDATE))
		{
			String tbl = "";
			if (meta.getUpdateTable() != null)
			{
				tbl = " (" + meta.getUpdateTable().getTableName() + ")";
			}
			
			if (this.errorReporter != null)
			{
				String msg = ResourceMgr.getString("ErrExportNoKeys") + tbl;
				this.errorReporter.addWarning(msg);
			}
			
			LogMgr.logWarning("SqlRowDataConverter.setResultInfo()", "No key columns found" + tbl + " reverting back to INSERT generation");
			this.sqlTypeToUse = SQL_INSERT;
		}
		
	}
	
	public StrBuffer getEnd(long totalRows)
	{
		boolean writeCommit = true;
		if (commitEvery > 0 && (totalRows % commitEvery == 0))
		{
			writeCommit = false;
		}

		StrBuffer end = null;
		if (writeCommit)
		{
			end = new StrBuffer();
			end.append(lineTerminator);
			end.append("COMMIT;");
			end.append(lineTerminator);
		}
		return end;
	}

	public void setType(int type)
	{
		if (type == SQL_INSERT)
			this.setCreateInsert();
		else if (type == SQL_UPDATE)
			this.setCreateUpdate();
		else if (type == SQL_DELETE_INSERT)
			this.setCreateInsertDelete();
		else
			throw new IllegalArgumentException("Invalid type specified");
	}
	
	public String getFormatName()
	{
		if (isCreateInsert())
			return "SQL INSERT";
		else if (sqlType == SQL_UPDATE)
			return "SQL UPDATE";
		else
			return "SQL DELETE/INSERT";
	}

	public StrBuffer convertRowData(RowData row, long rowIndex)
	{
		StrBuffer result = new StrBuffer();
		DmlStatement dml = null;
		String db = null;
		if (this.originalConnection != null)
		{
			db = this.originalConnection.getDatabaseProductName();
		}
		this.factory.setIncludeTableOwner(this.includeOwner);
		
		if (this.sqlTypeToUse == SQL_DELETE_INSERT)
		{
			dml = this.factory.createDeleteStatement(row, true);
			result.append(dml.getExecutableStatement(this.literalFormatter));
			result.append(';');
			result.append(lineTerminator);
		}
		if (this.sqlTypeToUse == SQL_DELETE_INSERT || this.sqlType == SQL_INSERT)
		{
			dml = this.factory.createInsertStatement(row, true, "\n", this.exportColumns);
		}
		else // implies sqlType == SQL_UPDATE
		{
			dml = this.factory.createUpdateStatement(row, true, "\n", this.exportColumns);
		}
		dml.setChrFunction(this.chrFunction);
		dml.setConcatString(this.concatString);
		dml.setConcatFunction(this.concatFunction);
		
		// Needed for formatting BLOBs in the literalFormatter
		this.currentRow = rowIndex;
		this.currentRowData = row;
		
		result.append(dml.getExecutableStatement(this.literalFormatter));
		
		result.append(';');
		if (doFormatting)
			result.append(doubleLineTerminator);
		else
			result.append(lineTerminator);

		if (this.commitEvery > 0 && ((rowIndex + 1) % commitEvery) == 0)
		{
			result.append("COMMIT;");
			result.append(doubleLineTerminator);
		}
		return result;
	}

	public StrBuffer getStart()
	{
		if (!this.createTable) return null;
		TableIdentifier updatetable = this.metaData.getUpdateTable();
		if (updatetable == null && alternateUpdateTable == null)
		{
			LogMgr.logError("SqlRowDataConverter.getStart()", "Cannot write create table without update table!",null);
			return null;
		}
		ColumnIdentifier[] cols = this.metaData.getColumns();
		DbMetadata db = this.originalConnection.getMetadata();
		String source = db.getTableSource(updatetable, cols, (alternateUpdateTable == null ? updatetable.getTableName() : alternateUpdateTable.getTableName()));
		StrBuffer createSql = new StrBuffer(source);
		createSql.append(doubleLineTerminator);
		return createSql;
	}

	public boolean isCreateInsert()
	{
		return this.sqlTypeToUse == SQL_INSERT;
	}

	public boolean isCreateUpdate()
	{
		return this.sqlTypeToUse == SQL_UPDATE;
	}
	
	public boolean isCreateInsertDelete()
	{
		return this.sqlTypeToUse == SQL_DELETE_INSERT;
	}
	
	public void setCreateInsert()
	{
		this.sqlType = SQL_INSERT;
		this.sqlTypeToUse = this.sqlType;
		this.doFormatting = Settings.getInstance().getBoolProperty("workbench.sql.generate.insert.doformat",true);
	}
	
	public void setCreateUpdate()
	{
		this.sqlType = SQL_UPDATE;
		this.sqlTypeToUse = this.sqlType;
		this.doFormatting = Settings.getInstance().getBoolProperty("workbench.sql.generate.update.doformat",true);
	}

	public void setCreateInsertDelete()
	{
		this.sqlType = SQL_DELETE_INSERT;
		this.sqlTypeToUse = this.sqlType;
		this.doFormatting = Settings.getInstance().getBoolProperty("workbench.sql.generate.insert.doformat",true);
	}
	
	private boolean checkKeyColumns()
	{
		boolean keysPresent = metaData.hasPkColumns();
		
		if (this.keyColumnsToUse != null && this.keyColumnsToUse.size() > 0)
		{
			int keyCount = this.keyColumnsToUse.size();
			
			// make sure the default key columns are not used
			this.metaData.resetPkColumns();

			for (int i=0; i < keyCount; i++)
			{
				this.metaData.setIsPkColumn((String)this.keyColumnsToUse.get(i), true);
			}
			keysPresent = true;
		}

		if (!keysPresent)
		{
			try
			{
				this.metaData.readPkDefinition(this.originalConnection);
				keysPresent = this.metaData.hasPkColumns();
			}
			catch (SQLException e)
			{
				LogMgr.logError("SqlRowDataConverter.setCreateInsert", "Could not read PK columns for update table", e);
			}
		}
		return keysPresent;
	}
	
	public int getCommitEvery()
	{
		return commitEvery;
	}

	public void setCommitEvery(int commitEvery)
	{
		this.commitEvery = commitEvery;
	}

	public String getConcatString()
	{
		return concatString;
	}

	public void setConcatString(String concat)
	{
		if (concat == null) return;
		this.concatString = concat;
		this.concatFunction = null;
	}

	public String getConcatFunction()
	{
		return concatFunction;
	}

	public void setConcatFunction(String func)
	{
		if (func == null) return;
		this.concatFunction = func;
		this.concatString = null;
	}

	public String getChrFunction()
	{
		return chrFunction;
	}

	public void setChrFunction(String chrFunction)
	{
		this.chrFunction = chrFunction;
	}

	public String getSql()
	{
		return sql;
	}

	public void setSql(String sql)
	{
		this.sql = sql;
	}

	/**
	 * Getter for property createTable.
	 * @return Value of property createTable.
	 */
	public boolean isCreateTable()
	{
		return createTable;
	}

	/**
	 * Setter for property createTable.
	 * @param flag New value of property createTable.
	 */
	public void setCreateTable(boolean flag)
	{
		this.createTable = flag;
	}

	/**
	 * Setter for property alternateUpdateTable.
	 * @param table New value of property alternateUpdateTable.
	 */
	public void setAlternateUpdateTable(TableIdentifier table)
	{
		if (table != null) 
		{
			this.alternateUpdateTable = table;
			this.needsUpdateTable = false;
			if (this.factory != null) this.factory.setTableToUse(this.alternateUpdateTable);
		}
		else
		{
			this.alternateUpdateTable = null;
			this.needsUpdateTable = true;
		}
	}

	/**
	 * Getter for property keyColumnsToUse.
	 * @return Value of property keyColumnsToUse.
	 */
	public List getKeyColumnsToUse()
	{
		return keyColumnsToUse;
	}

	/**
	 * Setter for property keyColumnsToUse.
	 * @param keyColumnsToUse New value of property keyColumnsToUse.
	 */
	public void setKeyColumnsToUse(List keyColumnsToUse)
	{
		this.keyColumnsToUse = keyColumnsToUse;
	}

	public void setLineTerminator(String lineEnd)
	{
		this.lineTerminator = lineEnd;
		this.doubleLineTerminator = lineEnd + lineEnd;
	}
	
	public void setIncludeTableOwner(boolean flag)
	{
		this.includeOwner = flag;
		if (this.factory != null) this.factory.setIncludeTableOwner(flag);
	}

	public void setBlobTypeNone()
	{
		this.literalFormatter.noBlobHandling();
	}
	
	public void setBlobTypeDbmsLiteral()
	{
		this.literalFormatter.createDbmsBlobLiterals(originalConnection);
	}
	
	public void setBlobTypeAnsiLiteral()
	{
		literalFormatter.createAnsiBlobLiterals();
	}
	
	public void setBlobTypeFile()
	{
		literalFormatter.createBlobFiles(this);
	}
	
	public void setClobAsFile(String encoding)
	{
		if (StringUtil.isEmptyString(encoding)) return;
		literalFormatter.setTreatClobAsFile(this, encoding);
	}
	
}
