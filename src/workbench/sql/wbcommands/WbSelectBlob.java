/*
 * WbSelectBlob.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.ByteArrayInputStream;
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
import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;

import workbench.util.ExceptionUtil;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 * A SQL statement that can retrieve the data from a blob column into a file
 * on the client.
 * <br/>
 * It is intended to store the contents of a single row/column into a file.
 * For that it accepts an "INTO &lt;filename&gt;" syntax.
 * <br/>
 * If the generated SELECT returns more than one row, additional files
 * are created with a sequence counter.
 * <br/>
 * As WbExport can also retrieve BLOB data and allows automated control
 * over the generated filenames (e.g. by using a different column's content)
 * WbExport should be preferred over this command.
 *
 * @author Thomas Kellerer
 * @see WbExport
 */
public class WbSelectBlob
	extends SqlCommand
{
	public static final String VERB = "WbSelectBlob";

	public WbSelectBlob()
	{
		super();
		this.isUpdatingCommand = false;
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public StatementRunnerResult execute(final String sqlCommand)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(sqlCommand);
		SQLLexer lexer = SQLLexerFactory.createLexer(currentConnection, sqlCommand);

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

				if (currentConnection.getDbSettings().useGetBytesForBlobs())
				{
					byte[] data = rs.getBytes(1);
					in = new ByteArrayInputStream(data);
				}
				else
				{
					in = rs.getBinaryStream(1);
				}

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

	@Override
	public boolean isWbCommand()
	{
		return true;
	}
}
