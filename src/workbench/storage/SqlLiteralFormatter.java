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
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import workbench.WbManager;
import workbench.db.WbConnection;
import workbench.interfaces.DataFileWriter;
import workbench.log.LogMgr;
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
	private final int JDBC_FORMAT = 1;
	private final int ANSI_FORMAT = 2;
	private final int DEFAULT_FORMAT = 4;
	
	private int timeFormat = DEFAULT_FORMAT;
	
	/**
	 * The "product" for the jdbc date literal format
	 */
	public static final String JDBC_DATE_LITERAL_TYPE = "jdbc";

	/**
	 * The "product" for the ansi date literal format
	 */
	public static final String ANSI_DATE_LITERAL_TYPE = "ansi";

	/**
	 * The "product" for the jdbc literal format
	 */
	public static final String DEFAULT_DATE_LITERAL_TYPE = "standard";
	
	/**
	 * The "product" identifying a general (default) 
	 * formatting for date and timestamp literals
	 */
	public static final String GENERAL_SQL = "All";
	
	private Map<String, DbDateFormatter> dateLiteralFormats;
	private Map<String, DbDateFormatter> timestampFormats;
	private DbDateFormatter defaultDateFormatter;
	private DbDateFormatter defaultTimestampFormatter;
	private SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");;
	private BlobLiteralFormatter blobFormatter;
	private DataFileWriter blobWriter;
	private DataFileWriter clobWriter;
	private boolean treatClobAsFile = false;
	private String clobEncoding = Settings.getInstance().getDefaultFileEncoding();
	
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
	 * "product" using {@link #setProduct(String)}
	 * 
	 * @param con the connection identifying the DBMS
	 * 
	 * @see workbench.db.DbMetadata#getProductName()
  */
	public SqlLiteralFormatter(WbConnection con)
	{
		String product = "*";
			
		if (con != null)
		{
			product = con.getMetadata().getProductName();
		}
		dateLiteralFormats = readStatementTemplates("DateLiteralFormats.xml");
		if (dateLiteralFormats == null) dateLiteralFormats = Collections.EMPTY_MAP;
	
		timestampFormats = readStatementTemplates("TimestampLiteralFormats.xml");
		if (timestampFormats == null) timestampFormats = Collections.EMPTY_MAP;
		
		setProduct(product);
	}
	
	/**
	 * Use a specific product name for formatting date and timestamp values.
	 * <br/>
	 * Valid values are: 
	 * <ul>
	 *	<li>jdbc - to produce JDBC escape literals (e.g. {d '2010-01-02'})</li> 
	 *  <li>ansi - to produce ANSI literals (e.g. DATE '2010-01-02')</li> 
	 * </ul>
	 * Any other value will either use the formatting available in the XML files
	 * or a default literal.
	 * 
	 * @param product the product to use. This is the key to the map defining the formats
	 * 
	 * @see workbench.db.DbMetadata#getProductName()
	 */
	public void setProduct(String product)
	{
		defaultDateFormatter = getDateLiteralFormatter(product);
		defaultTimestampFormatter = getTimestampFormatter(product);
		if ("jdbc".equalsIgnoreCase(product))
		{
			this.timeFormat = JDBC_FORMAT;
		}
		else if ("ansi".equalsIgnoreCase(product))
		{
			this.timeFormat = ANSI_FORMAT;
		}
		else
		{
			this.timeFormat = DEFAULT_FORMAT;
		}
	}


	/**
	 * Do not write BLOBs as SQL Literals.
	 */
	public void noBlobHandling()
	{
		this.blobWriter = null;
		this.blobFormatter = null;
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
			LogMgr.logError("SqlSyntaxFormatter.readStatementTemplates()", "Error reading template file " + aFilename,e);
		}
		
		Map customizedMap = null;
		
		// try to read additional definitions from local file
		try
		{
			File f = new File(WbManager.getInstance().getJarPath(), aFilename);
			if (f.exists())
			{
				LogMgr.logInfo("SqlLiteralFormatter", "Adding customized literal format using file " + f.getAbsolutePath());
				WbPersistence reader = new WbPersistence(f.getAbsolutePath());
				customizedMap = (Map<String, DbDateFormatter>)reader.readObject();
			}
			else
			{
				LogMgr.logDebug("SqlLiteralFormatter", "No customized formatter file found (" + f.getAbsolutePath() + ")");
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlLiteralFormatter", "Error when reading customized format file", e);
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
	
	private DbDateFormatter getFormatter(Map<String, DbDateFormatter> formats, String product)
	{
		DbDateFormatter format = formats.get(product == null ? GENERAL_SQL : product);
		if (format == null)
		{
			format = formats.get(GENERAL_SQL);
			
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
	
	private String getTimeLiteral(Time tm)
	{
		StringBuilder result = new StringBuilder(12);
		if (this.timeFormat == JDBC_FORMAT)
		{
			result.append("{t '");
		}
		else if (this.timeFormat == ANSI_FORMAT)
		{
			result.append("TIME '");
		}
		else
		{
			result.append('\'');
		}
		synchronized (this.timeFormatter)
		{
			result.append(this.timeFormatter.format(tm));
		}
		
		result.append('\'');
		if (this.timeFormat == JDBC_FORMAT)
		{
			result.append('}');
		}
		return result.toString();
	}
	
	public String getDefaultLiteral(ColumnData data)
	{
		if (data.isNull()) return "NULL";
		
		Object value = data.getValue();
		if (value == null) return "NULL";
		
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
		else if (value instanceof Time)
		{
			return this.getTimeLiteral((Time)value);
		}
		else if (value instanceof Timestamp)
		{
			return this.defaultTimestampFormatter.getLiteral((Timestamp)value);
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
