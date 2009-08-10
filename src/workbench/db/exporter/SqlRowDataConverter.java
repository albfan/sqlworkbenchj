/*
 * SqlRowDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.sql.SQLException;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.TableSourceBuilderFactory;
import workbench.db.WbConnection;
import workbench.interfaces.Committer;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.DmlStatement;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.SqlLiteralFormatter;
import workbench.storage.StatementFactory;
import workbench.util.CollectionUtil;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 * Export data as SQL INSERT statements.
 *
 * @author Thomas Kellerer
 */
public class SqlRowDataConverter
	extends RowDataConverter
{
	// This instance can be re-used for several
	// table exports from DataExporter, to prevent
	// that one failed export for the requested type
	// resets the export type for subsequent tables
	// the requested sqlType is stored in sqlType
	// Upon setting the ResultInfo in setResultInfo()
	// the sqlTypeToUse is set accordingly and then
	// used in convertRowData()
	private ExportType sqlTypeToUse = ExportType.SQL_INSERT;
	private ExportType sqlType = ExportType.SQL_INSERT;

	private boolean createTable = false;
	private TableIdentifier alternateUpdateTable;
	private int commitEvery;
	private String concatString;
	private String chrFunction;
	private String concatFunction;
	private StatementFactory statementFactory;
	private List<String> keyColumnsToUse;
	private String lineTerminator = "\n";
	private String doubleLineTerminator = "\n\n";
	private boolean includeOwner = true;
	private boolean doFormatting = true;
	private SqlLiteralFormatter literalFormatter;
	private boolean ignoreRowStatus = true;

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
		this.statementFactory = new StatementFactory(meta, this.originalConnection);
		this.needsUpdateTable = meta.getUpdateTable() == null;
		this.statementFactory.setIncludeTableOwner(this.includeOwner);
		this.statementFactory.setTableToUse(this.alternateUpdateTable);

		boolean keysPresent = this.checkKeyColumns();
		this.sqlTypeToUse = this.sqlType;
		if (!keysPresent && (this.sqlType == ExportType.SQL_DELETE_INSERT || this.sqlType == ExportType.SQL_UPDATE))
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
			this.sqlTypeToUse = ExportType.SQL_INSERT;
		}

	}

	public void setSqlLiteralType(String type)
	{
		if (this.literalFormatter != null)
		{
			this.literalFormatter.setDateLiteralType(type);
		}
	}

	public StrBuffer getEnd(long totalRows)
	{
		boolean writeCommit = true;
		if ( (commitEvery == Committer.NO_COMMIT_FLAG) || (commitEvery > 0 && (totalRows % commitEvery == 0)))
		{
			writeCommit = false;
		}

		StrBuffer end = null;
		if (writeCommit && totalRows > 0 || this.createTable && this.originalConnection.getDbSettings().ddlNeedsCommit())
		{
			end = new StrBuffer();
			end.append(lineTerminator);
			end.append("COMMIT;");
			end.append(lineTerminator);
		}
		return end;
	}

	public void setIgnoreColumnStatus(boolean flag)
	{
		this.ignoreRowStatus = flag;
	}

	public void setType(ExportType type)
	{
		switch (type)
		{
			case SQL_INSERT:
				setCreateInsert();
				break;
			case SQL_UPDATE:
				this.setCreateUpdate();
				break;
			case SQL_DELETE_INSERT:
				this.setCreateInsertDelete();
				break;
			default:
				throw new IllegalArgumentException("Invalid type specified");
		}
	}

	public StrBuffer convertRowData(RowData row, long rowIndex)
	{
		StrBuffer result = new StrBuffer();
		DmlStatement dml = null;
		this.statementFactory.setIncludeTableOwner(this.includeOwner);

		if (this.sqlTypeToUse == ExportType.SQL_DELETE_INSERT)
		{
			dml = this.statementFactory.createDeleteStatement(row, true);
			result.append(dml.getExecutableStatement(this.literalFormatter));
			result.append(';');
			result.append(lineTerminator);
		}
		if (this.sqlTypeToUse == ExportType.SQL_DELETE_INSERT || this.sqlType == ExportType.SQL_INSERT)
		{
			dml = this.statementFactory.createInsertStatement(row, ignoreRowStatus, "\n", this.exportColumns);
		}
		else // implies sqlType == SQL_UPDATE
		{
			dml = this.statementFactory.createUpdateStatement(row, ignoreRowStatus, "\n", this.exportColumns);
		}
		if (dml == null) return null;

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
		TableIdentifier updateTable = this.metaData.getUpdateTable();
		if (updateTable == null && alternateUpdateTable == null)
		{
			LogMgr.logError("SqlRowDataConverter.getStart()", "Cannot write create table without update table!",null);
			return null;
		}
		else if (updateTable == null)
		{
			updateTable = alternateUpdateTable;
		}

		List<ColumnIdentifier> cols = CollectionUtil.arrayList(this.metaData.getColumns());
		TableSourceBuilder builder = TableSourceBuilderFactory.getBuilder(originalConnection);
		String source = builder.getTableSource(updateTable, cols, (alternateUpdateTable == null ? updateTable.getTableName() : alternateUpdateTable.getTableName()));
		StrBuffer createSql = new StrBuffer(source);
		createSql.append(doubleLineTerminator);
		return createSql;
	}

	public boolean isCreateInsert()
	{
		return this.sqlTypeToUse == ExportType.SQL_INSERT;
	}

	public boolean isCreateUpdate()
	{
		return this.sqlTypeToUse == ExportType.SQL_UPDATE;
	}

	public boolean isCreateInsertDelete()
	{
		return this.sqlTypeToUse == ExportType.SQL_DELETE_INSERT;
	}

	public void setCreateInsert()
	{
		this.sqlType = ExportType.SQL_INSERT;
		this.sqlTypeToUse = this.sqlType;
		this.doFormatting = Settings.getInstance().getBoolProperty("workbench.sql.generate.insert.doformat",true);
	}

	public void setCreateUpdate()
	{
		this.sqlType = ExportType.SQL_UPDATE;
		this.sqlTypeToUse = this.sqlType;
		this.doFormatting = Settings.getInstance().getBoolProperty("workbench.sql.generate.update.doformat",true);
	}

	public void setCreateInsertDelete()
	{
		this.sqlType = ExportType.SQL_DELETE_INSERT;
		this.sqlTypeToUse = this.sqlType;
		this.doFormatting = Settings.getInstance().getBoolProperty("workbench.sql.generate.insert.doformat",true);
	}

	private boolean checkKeyColumns()
	{
		boolean keysPresent = metaData.hasPkColumns();

		if (this.keyColumnsToUse != null && this.keyColumnsToUse.size() > 0)
		{
			// make sure the default key columns are not used
			this.metaData.resetPkColumns();

			for (String col : keyColumnsToUse )
			{
				this.metaData.setIsPkColumn(col, true);
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

	public void setCommitEvery(int interval)
	{
		this.commitEvery = interval;
	}

	public void setConcatString(String concat)
	{
		if (concat == null) return;
		this.concatString = concat;
		this.concatFunction = null;
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

	public void setChrFunction(String function)
	{
		this.chrFunction = function;
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
			if (this.statementFactory != null) this.statementFactory.setTableToUse(this.alternateUpdateTable);
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
	 * @param cols New value of property keyColumnsToUse.
	 */
	public void setKeyColumnsToUse(List<String> cols)
	{
		this.keyColumnsToUse = cols;
	}

	public void setLineTerminator(String lineEnd)
	{
		this.lineTerminator = lineEnd;
		this.doubleLineTerminator = lineEnd + lineEnd;
	}

	public void setIncludeTableOwner(boolean flag)
	{
		this.includeOwner = flag;
		if (this.statementFactory != null) this.statementFactory.setIncludeTableOwner(flag);
	}

	public void setBlobMode(BlobMode type)
	{
		if (this.literalFormatter == null) return;

		if (type == BlobMode.DbmsLiteral)
		{
			this.literalFormatter.createDbmsBlobLiterals(originalConnection);
		}
		else if (type == BlobMode.AnsiLiteral)
		{
			literalFormatter.createAnsiBlobLiterals();
		}
		else if (type == BlobMode.SaveToFile)
		{
			literalFormatter.createBlobFiles(this);
		}
		else
		{
			this.literalFormatter.noBlobHandling();
		}
	}

	public void setClobAsFile(String encoding)
	{
		if (StringUtil.isEmptyString(encoding)) return;
		if (this.literalFormatter != null)
		{
			literalFormatter.setTreatClobAsFile(this, encoding);
		}
	}

}
