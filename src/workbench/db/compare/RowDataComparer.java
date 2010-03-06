/*
 * RowDataComparer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.compare;

import java.util.Collection;
import workbench.db.WbConnection;
import workbench.db.exporter.BlobMode;
import workbench.db.exporter.ExportType;
import workbench.db.exporter.SqlRowDataConverter;
import workbench.db.exporter.XmlRowDataConverter;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.util.StrBuffer;
import workbench.util.WbFile;

/**
 * Compare two {@link workbench.storage.RowData} objects to check for equality.
 * Used to generate the approriate SQL scripts when comparing the data from
 * two tables.
 *
 * @author Thomas Kellerer
 */
public class RowDataComparer
{
	private RowData migrationData;
	private boolean targetWasNull;
	private WbConnection targetDb;
	private ResultInfo resultInfo;
	private BlobMode blobMode;
	private SqlRowDataConverter sqlConverter;
	private XmlRowDataConverter xmlConverter;
	private String sqlDateLiteral;
	private WbFile baseDir;

	/**
	 * Compares two database rows.
	 */
	public RowDataComparer()
	{
	}

	public void setBaseDir(WbFile dir)
	{
		baseDir = dir;
		if (sqlConverter != null)
		{
			sqlConverter.setOutputFile(dir);
		}
		if (xmlConverter != null)
		{
			xmlConverter.setOutputFile(dir);
		}
	}

	public void setSqlDateLiteralType(String type)
	{
		sqlDateLiteral = type;
		if (sqlConverter != null)
		{
			sqlConverter.setDateLiteralType(type);
		}
	}

	public void setTypeSql()
	{
		sqlConverter = new SqlRowDataConverter(targetDb);
		sqlConverter.setBlobMode(blobMode);
		if (resultInfo != null) sqlConverter.setResultInfo(resultInfo);
		if (sqlDateLiteral != null) sqlConverter.setDateLiteralType(sqlDateLiteral);
		if (blobMode != null)
		{
			sqlConverter.setBlobMode(blobMode);
		}
		sqlConverter.setOutputFile(baseDir);
		xmlConverter = null;
	}

	public boolean isTypeXml()
	{
		return xmlConverter != null;
	}

	public void setTypeXml(boolean useCDATA)
	{
		xmlConverter = new XmlRowDataConverter();
		xmlConverter.setUseVerboseFormat(false);
		xmlConverter.setUseDiffFormat(true);
		xmlConverter.setWriteClobToFile(false);
		xmlConverter.setUseCDATA(useCDATA);
		xmlConverter.setOriginalConnection(targetDb);
		if (resultInfo != null) xmlConverter.setResultInfo(resultInfo);
		sqlConverter = null;
		xmlConverter.setOutputFile(baseDir);
	}

	/**
	 * Define the Blob mode when generating SQL statements.
	 */
	public void setSqlBlobMode(BlobMode mode)
	{
		blobMode = mode;
		if (sqlConverter != null)
		{
			sqlConverter.setBlobMode(mode);
		}
	}

	public void setConnection(WbConnection target)
	{
		targetDb = target;
	}

	public void setResultInfo(ResultInfo ri)
	{
		resultInfo = ri;
		if (sqlConverter != null)
		{
			sqlConverter.setResultInfo(ri);
		}

		if (xmlConverter != null)
		{
			xmlConverter.setResultInfo(ri);
		}
	}

	public void setRows(RowData referenceRow, RowData targetRow)
	{
		int cols = referenceRow.getColumnCount();
		if (targetRow == null)
		{
			targetWasNull = true;
			migrationData = referenceRow.createCopy();
			migrationData.resetStatus();
			migrationData.setNew();
		}
		else
		{
			targetWasNull = false;
			migrationData = targetRow.createCopy();
			migrationData.resetStatus();

			int tcols = migrationData.getColumnCount();
			if (cols != tcols) throw new IllegalArgumentException("Column counts must match!");

			for (int i=0; i < cols; i++)
			{
				// if the value passed to the target row is
				// identical to the existing value, this will
				// not change the state of the RowData
				migrationData.setValue(i, referenceRow.getValue(i));
			}
		}
	}

	public void ignoreColumns(Collection<String> columnNames, ResultInfo info)
	{
		if (columnNames == null || columnNames.isEmpty()) return;
		for (int i=0; i < info.getColumnCount(); i++)
		{
			if (columnNames.contains(info.getColumnName(i)))
			{
				migrationData.resetStatusForColumn(i);
			}
		}
	}

	/**
	 * Returns the representation for the changes between the rows
	 * defined by setRows().
	 * <br/>
	 * Depending on the type this might be a SQL statement (INSERT, UPDATE)
	 * or a XML fragment as returned by XmlRowDataConverter.
	 *
	 * @param rowNumber
	 * @return
	 */
	public String getMigration(long rowNumber)
	{
		StrBuffer result = null;
		if (sqlConverter != null)
		{
			if (targetWasNull)
			{
				sqlConverter.setIgnoreColumnStatus(true);
				sqlConverter.setType(ExportType.SQL_INSERT);
			}
			else
			{
				sqlConverter.setIgnoreColumnStatus(false);
				sqlConverter.setType(ExportType.SQL_UPDATE);
			}
			result = sqlConverter.convertRowData(migrationData, rowNumber);
		}
		if (xmlConverter != null)
		{
			if (targetWasNull)
			{
				xmlConverter.convertModifiedColumnsOnly(false);
				StrBuffer row = xmlConverter.convertRowData(migrationData, rowNumber);
				result = new StrBuffer(row.length() + 20);
				result.append("<insert>");
				result.append(row);
				result.append("</insert>");
			}
			else if (migrationData.isModified())
			{
				xmlConverter.convertModifiedColumnsOnly(true);
				StrBuffer row = xmlConverter.convertRowData(migrationData, rowNumber);
				result = new StrBuffer(row.length() + 20);
				result.append("<update>");
				result.append(row);
				result.append("</update>");
			}
		}
		if (result == null) return null;
		return result.toString();
	}

}
