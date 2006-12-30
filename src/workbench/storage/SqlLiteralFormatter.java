/*
 * SqlLiteralFormatter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.io.BufferedInputStream;
import java.io.File;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import workbench.WbManager;
import workbench.db.WbConnection;
import workbench.interfaces.DataFileWriter;

import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbPersistence;

/**
 *
 * @author  support@sql-workbench.net
 */
public class SqlLiteralFormatter
{
	public static final String GENERAL_SQL = "All";
	
	private Map<String, DbDateFormatter> dateLiteralFormats;
	private Map<String, DbDateFormatter> timestampFormats;
	private DbDateFormatter defaultDateFormatter;
	private DbDateFormatter defaultTimestampFormatter;
	private BlobLiteralFormatter blobFormatter;
	private DataFileWriter blobWriter;
	private DataFileWriter clobWriter;
	private boolean treatClobAsFile = false;
	private String clobEncoding = Settings.getInstance().getDefaultFileEncoding();
	
	public SqlLiteralFormatter()
	{
		this(null);
	}
	
	public SqlLiteralFormatter(WbConnection con)
	{
		String product = "*";
			
		if (con != null)
		{
			product = con.getMetadata().getProductName();
		}
		dateLiteralFormats = readStatementTemplates("DateLiteralFormats.xml");
		if (dateLiteralFormats == null) dateLiteralFormats = Collections.EMPTY_MAP;
		defaultDateFormatter = getDateLiteralFormatter(product);
	
		timestampFormats = readStatementTemplates("TimestampLiteralFormats.xml");
		if (timestampFormats == null) timestampFormats = Collections.EMPTY_MAP;
		defaultTimestampFormatter = getDateLiteralFormatter(product);
	}
	
	public void noBlobHandling()
	{
		this.blobWriter = null;
		this.blobFormatter = null;
	}
	
	public void createAnsiBlobLiterals()
	{
		blobFormatter = BlobFormatterFactory.createAnsiFormatter();
		this.blobWriter = null;
	}
	
	public void createDbmsBlobLiterals(WbConnection con)
	{
		if (con != null)
		{
			blobFormatter = BlobFormatterFactory.createInstance(con.getMetadata());
			this.blobWriter = null;
		}
	}
	
	public void createBlobFiles(DataFileWriter bw)
	{
		this.blobFormatter = null;
		this.blobWriter = bw;
	}
	
	public void setTreatClobAsFile(DataFileWriter writer, String encoding)
	{
		this.treatClobAsFile = true;
		this.clobWriter = writer;
		if (!StringUtil.isEmptyString(encoding)) this.clobEncoding = encoding;
	}
	
	private Map<String, DbDateFormatter> readStatementTemplates(String aFilename)
	{
		Map result = null;
		
		BufferedInputStream in = new BufferedInputStream(this.getClass().getResourceAsStream(aFilename));
		
		try
		{
			WbPersistence reader = new WbPersistence();
			Object value = reader.readObject(in);
			if (value instanceof Map)
			{
				result = (Map<String, DbDateFormatter>)value;
			}
		}
		catch (Exception e)
		{
			result = null;
			LogMgr.logError("SqlSyntaxFormatter.readStatementTemplates()", "Error reading template file " + aFilename,e);
		}
		
		Map customizedMap = null;
		
		// try to read additional definitions from local file
		try
		{
			File f = new File(WbManager.getInstance().getJarPath(), aFilename);
			if (f.exists())
			{
				WbPersistence reader = new WbPersistence(f.getAbsolutePath());
				customizedMap = (Map<String, DbDateFormatter>)reader.readObject();
			}
		}
		catch (Exception e)
		{
			customizedMap = null;
		}
		
		if (customizedMap != null)
		{
			if (result != null)
			{
				result.putAll(customizedMap);
			}
			else
			{
				result = customizedMap;
			}
		}
		return result;
	}

	public DbDateFormatter getTimestampFormatter(String product)
	{
		return getFormatter(timestampFormats, product);
	}
	
	public DbDateFormatter getDateLiteralFormatter(String aProductname)
	{
		return getFormatter(dateLiteralFormats, aProductname);
	}
	
	private DbDateFormatter getFormatter(Map formats, String product)
	{
		DbDateFormatter format = dateLiteralFormats.get(product == null ? GENERAL_SQL : product);
		if (format == null)
		{
			format = dateLiteralFormats.get(GENERAL_SQL);
			
			// Just in case someone messed around with the XML file
			if (format == null) format = DbDateFormatter.DEFAULT_FORMATTER;
		}
		return format;
	}
	
	private String quoteString(String t)
	{
		StringBuilder realValue = new StringBuilder(t.length() + 10);
		// Single quotes in a String must be "quoted"...
		realValue.append('\'');
		// replace to Buffer writes the result of into the passed buffer
		// so this appends the correct literal to realValue
		StringUtil.replaceToBuffer(realValue, t, "'", "''");
		realValue.append('\'');
		return realValue.toString();
	}
	
	public String getDefaultLiteral(ColumnData data)
	{
		if (data.isNull()) return "NULL";
		
		Object value = data.getValue();
		int type = data.getIdentifier().getDataType();
		
		if (value instanceof String)
		{
			String t = (String)value;
			if (this.treatClobAsFile && SqlUtil.isClobType(type) && clobWriter != null)
			{
				File f = clobWriter.generateDataFileName(data);
				try
				{
					clobWriter.writeClobFile(t, f, this.clobEncoding);
					return "{$clobfile='" + f.getName() + "' encoding='" + this.clobEncoding + "'}";
				}
				catch (Exception e)
				{
					LogMgr.logError("SqlLiteralFormatter.getDefaultLiteral", "Could not write CLOB file", e);
					return quoteString(t);
				}
			}
			else
			{
				return quoteString(t);
			}
		}
		else if (value instanceof Timestamp)
		{
			return this.defaultTimestampFormatter.getLiteral((Date)value);
		}
		else if (value instanceof Date)
		{
			return this.defaultDateFormatter.getLiteral((Date)value);
		}
		else if (value instanceof File)
		{
			if (SqlUtil.isBlobType(type))
				return "{$blobfile='" + value.toString() + "'}";
			else if (SqlUtil.isClobType(type))
				return "{$clobfile='" + value.toString() + "'}";
		}
		else if (value instanceof NullValue)
		{
			return "NULL";
		}
		else if (type == java.sql.Types.BIT && "bit".equalsIgnoreCase(data.getIdentifier().getDbmsType()))
		{
			// this is for MS SQL Server
			// we cannot convert all values denoted as Types.BIT to 0/1 as
			// e.g. Postgres only accepts the literals true/false for boolean columns
			// which are reported as Types.BIT as well.
			// that's why I compare to the DBMS data type bit (hoping that
			// other DBMS's that are also using 'bit' work the same way
			boolean flag = ((java.lang.Boolean)value).booleanValue();
			return (flag ? "1" : "0");
		}
		else if (SqlUtil.isBlobType(type))
		{
			if (blobWriter != null)
			{
				File f = blobWriter.generateDataFileName(data);
				try
				{
					blobWriter.writeBlobFile(value, f);
					return "{$blobfile='" + f.getName() + "'}";
				}
				catch (Exception e)
				{
					LogMgr.logError("SqlLiteralFormatter.getDefaultLiteral", "Could not write BLOB file", e);
					return null;
				}
			}
			else if (blobFormatter != null)
			{
				try
				{
					return blobFormatter.getBlobLiteral(value);
				}
				catch (Exception e)
				{
					LogMgr.logError("SqlLiteralFormatter.getDefaultLiteral", "Error converting BLOB value", e);
					return null;
				}
			}
		}
		
		// Fallback, let the JDBC driver format the value
		return value.toString();
	}
	
}
