/*
 * WbSelectBlob.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.ExceptionUtil;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbSelectBlob
	extends SqlCommand
{
	public static final String VERB = "WBSELECTBLOB";

	public WbSelectBlob()
	{
		this.isUpdatingCommand = false;
	}
	
	public String getVerb() { return VERB; }
	
	public StatementRunnerResult execute(final String sqlCommand)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		SQLLexer lexer = new SQLLexer(sqlCommand);
		
		StringBuilder sql = new StringBuilder(sqlCommand.length());
		
		WbFile outputFile = null;
		
		SQLToken token  = lexer.getNextToken(false, false);
		if (!token.getContents().equals("WBSELECTBLOB"))
		{
			result.addMessage(ResourceMgr.getString("ErrSelectBlobSyntax"));
			result.setFailure();
			return result;
		}
		sql.append("SELECT ");
		while (token != null)
		{
			token = lexer.getNextToken(false, true);
			if (token.getContents().equals("INTO"))
			{
				break;
			}
			sql.append(token.getContents());
		}

		if (token != null && !token.getContents().equals("INTO"))
		{
			result.addMessage(ResourceMgr.getString("ErrSelectBlobSyntax"));
			result.setFailure();
			return result;
		}
		else
		{
			// Next token must be the filename
			token = lexer.getNextToken(false, false);
			String filename = token.getContents();
			outputFile = new WbFile(StringUtil.trimQuotes(filename));
			sql.append(' ');
			sql.append(sqlCommand.substring(token.getCharEnd() + 1));
		}
		
		LogMgr.logDebug("WbSelectBlob.execute()", "Using SQL=" + sql + " for file: " + outputFile.getFullPath());
		ResultSet rs = null;
		OutputStream out = null;
		InputStream in = null;
		long filesize = 0;
		
		File outputDir = outputFile.getParentFile();
		String baseFilename = outputFile.getFileName();
		String extension = outputFile.getExtension();
		if (StringUtil.isEmptyString(extension)) extension = "";
		else extension = "." + extension;
		
		try
		{
			currentStatement = currentConnection.createStatementForQuery();
			rs = currentStatement.executeQuery(sql.toString());
			int row = 0;
			while (rs.next())
			{
				WbFile currentFile = null;
				
				in = rs.getBinaryStream(1);
				if (in == null)
				{
					//result.setFailure();
					String msg = ResourceMgr.getString("ErrSelectBlobNoStream");
					result.addMessage(StringUtil.replace(msg, "%row%", Integer.toString(row)));
					result.setWarning(true);
					continue;
				}
				
				if (row == 0)
				{
					currentFile = outputFile;
				}
				else
				{
					currentFile = new WbFile(outputDir, baseFilename + "_" + Integer.toString(row) + extension);
				}
				
				out = new FileOutputStream(currentFile);
				filesize = FileUtil.copy(in, out);
				String msg = ResourceMgr.getString("MsgBlobSaved");
				msg = msg.replace("%filename%", currentFile.getFullPath());
				msg = msg.replace("%filesize%", Long.toString(filesize));
				result.addMessage(msg);
				result.setSuccess();
				row ++;
			}
			this.appendSuccessMessage(result);
		}
		catch (IOException e)
		{
			String msg = StringUtil.replace(ResourceMgr.getString("ErrSelectBlobFileError"), "%filename%", outputFile.getFullPath());
			result.addMessage(msg);
			result.setFailure();
			return result;
		}
		catch (SQLException e)
		{
			String msg = StringUtil.replace(ResourceMgr.getString("ErrSelectBlobSqlError"), "%filename%", outputFile.getFullPath());
			result.addMessage(msg);
			result.addMessage(ExceptionUtil.getDisplay(e));
			result.setFailure();
			return result;
		}
		finally
		{
			SqlUtil.closeAll(rs, currentStatement);
		}

		return result;
	}
	
}
