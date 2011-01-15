/*
 * SqlLiteralFormatter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.io.File;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Date;
import workbench.db.DbSettings;
import workbench.db.WbConnection;
import workbench.interfaces.DataFileWriter;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  Thomas Kellerer
 */
public class SqlLiteralFormatter
{
	/**
	 * The "product" for the jdbc date literal format
	 */
	public static final String JDBC_DATE_LITERAL_TYPE = "jdbc";

	/**
	 * The "product" for the ansi date literal format
	 */
	public static final String ANSI_DATE_LITERAL_TYPE = "ansi";

	/**
	 * The "product" for the standard date literal format
	 */
	public static final String STANDARD_DATE_LITERAL_TYPE = "default";

	/**
	 * The "product" for the dbms specific date literal format
	 */
	public static final String DBMS_DATE_LITERAL_TYPE = "dbms";
	
	private SimpleDateFormat dateFormatter;
	private SimpleDateFormat timestampFormatter;
	private SimpleDateFormat timeFormatter;
	private BlobLiteralFormatter blobFormatter;
	private DataFileWriter blobWriter;
	private DataFileWriter clobWriter;
	private boolean treatClobAsFile = false;
	private String clobEncoding = Settings.getInstance().getDefaultFileEncoding();
	private boolean isDbId;
	private DbSettings dbSettings;
	
	/**
	 * Create a new formatter with default formatting. 
	 */
	public SqlLiteralFormatter()
	{
		this(null);
	}
	
	/**
	 * Create  new formatter specifically for the DBMS identified
	 * by the connection.
	 * The type of date literals used, can be changed to a different
	 * "product" using {@link #setDateLiteralType(String)}
	 * 
	 * @param con the connection identifying the DBMS
	 * 
	 * @see workbench.db.DbMetadata#getProductName()
  */
	public SqlLiteralFormatter(WbConnection con)
	{
		String product = null;
		isDbId = false;
		if (con != null && con.getMetadata() != null)
		{
			product = con.getMetadata().getDbId();
			isDbId = true;
		}
		setDateLiteralType(product);
	}
	
	/**
	 * Select the DBMS specific date literal according to the 
	 * DBMS identified by the connection.
	 * @param con the connection to identify the DBMS
	 * @see #setDateLiteralType(String)
	 */
	public void setProduct(WbConnection con)
	{
		if (con != null)
		{
			String product = con.getMetadata().getDbId();
			isDbId = true;
			this.setDateLiteralType(product);
			this.dbSettings = con.getDbSettings();
		}
	}
	
	/**
	 * Use a specific product name for formatting date and timestamp values.
	 * This call is ignored if the passed value is DBMS and this instance has 
	 * been initialised with a Connection (thus the DBMS specific formatter is already
	 * selected).
	 * 
	 * @param type the literal type to use. This is the key to the map defining the formats
	 * 
	 * @see workbench.db.DbMetadata#getProductName()
	 */
	public void setDateLiteralType(String type)
	{
		// If the DBMS specific format is selected and we already have a DBID
		// then this call is simply ignored.
		if (DBMS_DATE_LITERAL_TYPE.equalsIgnoreCase(type))
		{
			if (this.isDbId)
			{
				return;
			}
			type = null;
		}
		
		dateFormatter = createFormatter(type, "date", "''yyyy-MM-dd''");
		timestampFormatter = createFormatter(type, "timestamp", "''yyyy-MM-dd HH:mm:ss''");
		timeFormatter = createFormatter(type, "time", "''HH:mm:ss''");
	}


	/**
	 * Do not write BLOBs as SQL Literals.
	 */
	public void noBlobHandling()
	{
		this.blobWriter = null;
		this.blobFormatter = null;
	}

	public void setBlobFormat(BlobLiteralType type)
	{
		blobWriter = null;
		blobFormatter = BlobFormatterFactory.createInstance(type);
	}
	/**
	 * Create ANSI compatible BLOB literals
	 */
	public void createAnsiBlobLiterals()
	{
		blobFormatter = BlobFormatterFactory.createAnsiFormatter();
		this.blobWriter = null;
	}
	
	/**
	 * Create BLOB literals that are compatible with the 
	 * DBMS identified by the connection.
	 * If no specific formatter for the given DMBS can be found, the generic
	 * ANSI formatter will be used. 
	 * @param con the connection (i.e. the DBMS) for which the literals should be created
	 */
	public void createDbmsBlobLiterals(WbConnection con)
	{
		if (con != null)
		{
			blobFormatter = BlobFormatterFactory.createInstance(con.getMetadata());
			this.blobWriter = null;
		}
	}

	/**
	 * Create external BLOB files instead of BLOB literals.
	 * This will reset any literal formatting selected with createAnsiBlobLiterals()
	 * or createDbmsBlobLiterals().
	 * The generated SQL Literal will be compatible with SQL Workbench extended
	 * blob handling and will generate literals in the format <code>{$blobfile=...}</code>
	 * 
	 * @param bw the writer to be used for writing the BLOB content
	 */
	public void createBlobFiles(DataFileWriter bw)
	{
		this.blobFormatter = null;
		this.blobWriter = bw;
	}
	
	/**
	 * Create external files for CLOB columns (instead of String literals).
	 * The generated SQL Literal will be compatible with SQL Workbench extended
	 * LOB handling and will generate literals in the format <code>{$clobfile='...' encoding='encoding'}</code>
	 * 
	 * @param writer the writer to be used for writing the BLOB content
	 * @param encoding the encoding to be used to write the CLOB files
	 */
	public void setTreatClobAsFile(DataFileWriter writer, String encoding)
	{
		this.treatClobAsFile = true;
		this.clobWriter = writer;
		if (!StringUtil.isEmptyString(encoding)) this.clobEncoding = encoding;
	}
	
	public static SimpleDateFormat createFormatter(String format, String type, String defaultPattern)
	{
		String key = "workbench.sql.literals." + (format == null ? STANDARD_DATE_LITERAL_TYPE : format) + "." + type + ".pattern";
		SimpleDateFormat f = null;
		String pattern = null;
		try
		{
			pattern = Settings.getInstance().getProperty(key, null);
			if (pattern == null)
			{
				key = "workbench.sql.literals." + STANDARD_DATE_LITERAL_TYPE + "." + type + ".pattern";
				pattern = Settings.getInstance().getProperty(key, defaultPattern);
			}
			f = new SimpleDateFormat(pattern);
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlLiteralFormatter.createFormatter()", "Could not create formatter with pattern [" + pattern + "], using default [" + defaultPattern + "]", e);
			f = new SimpleDateFormat(defaultPattern);
		}
		return f;
	}
	
	private String quoteString(String t)
	{
		if (t == null) return t;
		return "'" + t.replace("'", "''") + "'";
	}
	
	/**
	 * Return the default literal for the given column data.
	 * Date and Timestamp data will be formatted according to the 
	 * syntax defined by the {@link #setDateLiteralType(String)} method
	 * or through the connection provided in the constructor.
	 * @param data the data to be converted into a literal.
	 * @return the literal to be used in a SQL statement
	 * @see #setDateLiteralType(String)
	 */
	public CharSequence getDefaultLiteral(ColumnData data)
	{
		if (data.isNull()) return "NULL";
		
		Object value = data.getValue();
		if (value == null) return "NULL";
		
		int type = data.getIdentifier().getDataType();
		String dbmsType = data.getIdentifier().getDbmsType();
		
		if (value == null)
		{
			return "NULL";
		}
		else if (type == Types.STRUCT)
		{
			return value.toString();
		}
		else if (value instanceof String)
		{
			String t = (String)value;
			if (this.treatClobAsFile  && clobWriter != null && SqlUtil.isClobType(type, dbmsType, dbSettings))
			{
				try
				{
					File f = clobWriter.generateDataFileName(data);
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
		else if (value instanceof Time)
		{
			return this.timeFormatter.format((Time)value);
		}
		else if (value instanceof Timestamp)
		{
			return this.timestampFormatter.format((Timestamp)value);
		}
		else if (value instanceof Date)
		{
			return this.dateFormatter.format((Date)value);
		}
		else if (value instanceof File)
		{
			File f = (File)value;
			String path = null;
			try
			{
				path = f.getCanonicalPath();
			}
			catch (Exception e)
			{
				path = f.getAbsolutePath();
			}
			if (SqlUtil.isBlobType(type))
				return "{$blobfile='" + path + "'}";
			else if (SqlUtil.isClobType(type))
				return "{$clobfile='" + path + "' encoding='" + this.clobEncoding + "'}";
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
				try
				{
					File f = blobWriter.generateDataFileName(data);
					blobWriter.writeBlobFile(value, f);
					return "{$blobfile='" + f.getName() + "'}";
				}
				catch (Exception e)
				{
					LogMgr.logError("SqlLiteralFormatter.getDefaultLiteral", "Could not write BLOB file", e);
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
				}
			}
		}
		
		// Fallback, let the JDBC driver format the value
		return value.toString();
	}
	
}
