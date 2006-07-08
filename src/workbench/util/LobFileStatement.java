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
	private final String MARKER = "\\{\\$[cb]lobfile=[^\\}]*\\}";
	private String sqlToUse;
	private LobFileParameter[] parameters;
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
		this.baseDir = dir;

		LobFileParameterParser p = new LobFileParameterParser(sql);
		
		this.parameters = p.getParameters();
		if (this.parameters == null) return;
		
		parameterCount = this.parameters.length;
		for (int index=0; index < parameterCount; index++)
		{
			if (parameters[index].getFilename() == null)
			{
				String msg = ResourceMgr.getString("ErrUpdateBlobNoFileParameter");
				throw new FileNotFoundException(msg);
			}
			File f = new File(parameters[index].getFilename());

			if (!f.isAbsolute() && this.baseDir != null)
			{
				f = new File(this.baseDir, parameters[index].getFilename());
				parameters[index].setFilename(f.getAbsolutePath());
			}
						
			if (f.isDirectory() || !f.exists())
			{
				String msg = ResourceMgr.getString("ErrUpdateBlobFileNotFound");
				msg = StringUtil.replace(msg, "%filename%", parameters[index].getFilename());
				throw new FileNotFoundException(msg);
			}
		}
		this.sqlToUse = sql.replaceAll(MARKER, " ? ");
		//LogMgr.logDebug("LobFileStatement.<init>", "Using SQL: " + sqlToUse);
	}

	public String getPreparedSql() { return sqlToUse; }
	
	public LobFileParameter[] getParameters()
	{
		return parameters;
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
			File f = new File(parameters[i].getFilename());
			int length = (int)f.length(); 
			if (parameters[i].isBinary())
			{
				InputStream in = new BufferedInputStream(new FileInputStream(f), buffSize);
				parameters[i].setDataStream(new CloseableDataStream(in));
				pstmt.setBinaryStream(i+1, in, length);
			}
			else if (parameters[i].getEncoding() == null)
			{
				InputStream in = new BufferedInputStream(new FileInputStream(f), buffSize);
				parameters[i].setDataStream(new CloseableDataStream(in));
				pstmt.setAsciiStream(i+1, in, length);
			}
			else
			{
				Reader in = EncodingUtil.createBufferedReader(f, parameters[i].getEncoding());
				parameters[i].setDataStream(new CloseableDataStream(in));
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
			if (parameters[i].getDataStream() != null)
			{
				parameters[i].getDataStream().close();
			}
		}
	}
	
}
