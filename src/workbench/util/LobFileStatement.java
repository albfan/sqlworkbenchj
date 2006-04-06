/*
 * LobFileStatement.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 * A class to analyze the {$blobfile= } and {$clobfile= } 
 * parameters in a SQL statement. This class supports INSERT and UPDATE
 * statements. To retrieve a blob from the database {@link workbench.sql.wbcommands.WbSelectBlob}
 * has to be used.
 * @author support@sql-workbench.net
 */
public class LobFileStatement
{
	private final String MARKER = "\\{\\$[cb]lobfile=";
	private final Pattern MARKER_PATTERN = Pattern.compile(MARKER, Pattern.CASE_INSENSITIVE);
	private String sqlToUse;
	private ParameterEntry[] parameters;
	private int parameterCount = 0;
	private String baseDir;
	
	public LobFileStatement(String sql)
		throws FileNotFoundException
	{
		this(sql, null);
	}
	public LobFileStatement(String sql, String dir)
		throws FileNotFoundException
	{
		Matcher m = MARKER_PATTERN.matcher(sql);
		if (!m.find()) return;
		
		this.baseDir = dir;
		
		// Calculate number of parameters
		parameterCount ++;
		while (m.find())
		{
			parameterCount ++;
		}
		m.reset();
		parameters = new ParameterEntry[parameterCount];
		int index = 0;
		StringBuffer newSql = new StringBuffer(sql.length());
		WbStringTokenizer tok = new WbStringTokenizer(" \t", false, "\"'", false);
		int lastStart = 0;
		while (m.find())
		{
			int start = m.start();
			int end = sql.indexOf("}", start + 1);
			if (end > -1)
			{
				newSql.append(sql.substring(lastStart, start));
				newSql.append(" ? ");
				lastStart = end + 1; 
				String parm = sql.substring(start + 2, end);
				tok.setSourceString(parm);
				parameters[index] = new ParameterEntry();
				while (tok.hasMoreTokens())
				{
					String s = tok.nextToken();
					String arg = null;
					String value = null;
					int pos = s.indexOf("=");
					if (pos > -1)
					{
						arg = s.substring(0, pos);
						value = s.substring(pos + 1);
					}
					if ("encoding".equals(arg))
					{
						parameters[index].encoding = value;
					}
					else
					{
						// only other parameter allowed is [cb]lobfile
						File f = new File(value);
						if (!f.isAbsolute() && this.baseDir != null)
						{
							f = new File(this.baseDir, value);
							value = f.getAbsolutePath();
						}
						parameters[index].filename = value;
						parameters[index].binary = "blobfile".equals(arg);
					}
				}
				if (parameters[index].filename == null)
				{
					String msg = ResourceMgr.getString("ErrUpdateBlobNoFileParameter".replaceAll("%parm%",sql.substring(start + 2, end)));
					throw new FileNotFoundException(msg);
				}
				File f = new File(parameters[index].filename);
				if (f.isDirectory() || !f.exists())
				{
					String msg = ResourceMgr.getString("ErrUpdateBlobFileNotFound").replaceAll("%filename%", parameters[index].filename);
					throw new FileNotFoundException(msg);
				}
			}
			index ++;
		}
		newSql.append(sql.substring(lastStart));
		this.sqlToUse = newSql.toString();
		//LogMgr.logDebug("LobFileStatement.<init>", "Using SQL: " + sqlToUse);
	}

	public int getParameterCount()
	{
		return parameterCount;
	}
	
	public boolean containsParameter()
	{
		return (parameterCount > 0);
	}

	public PreparedStatement prepareStatement(Connection conn)
		throws SQLException, IOException
	{
		if (this.parameters == null) return null;
		if (this.parameters.length == 0) return null;
		PreparedStatement pstmt = conn.prepareStatement(sqlToUse);
		final int buffSize = 64*1024;
		for (int i = 0; i < parameters.length; i++)
		{
			File f = new File(parameters[i].filename);
			int length = (int)f.length(); 
			if (parameters[i].binary)
			{
				InputStream in = new BufferedInputStream(new FileInputStream(f), buffSize);
				parameters[i].dataStream = new CloseableDataStream(in);
				pstmt.setBinaryStream(i+1, in, length);
			}
			else if (parameters[i].encoding == null)
			{
				InputStream in = new BufferedInputStream(new FileInputStream(f), buffSize);
				parameters[i].dataStream = new CloseableDataStream(in);
				pstmt.setAsciiStream(i+1, in, length);
			}
			else
			{
				Reader in = EncodingUtil.createBufferedReader(f, parameters[i].encoding);
				parameters[i].dataStream = new CloseableDataStream(in);
				pstmt.setCharacterStream(i+1, in, length);
			}
		}
		return pstmt;
	}
	
	public void done()
	{
		if (this.parameters == null) return;
		for (int i = 0; i < parameters.length; i++)
		{
			if (parameters[i].dataStream != null)
			{
				parameters[i].dataStream.close();
			}
		}
	}
	
	class ParameterEntry
	{
		CloseableDataStream dataStream;
		String filename;
		String encoding;
		boolean binary;
		public String toString()
		{
			return "filename=[" + filename + "], binary=" + binary + ", encoding=" + encoding;
		}
	}
	
}
