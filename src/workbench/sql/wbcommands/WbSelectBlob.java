/*
 * WbSelectBlob.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.ExceptionUtil;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.WbStringTokenizer;

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
	
	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		Pattern expr = Pattern.compile("WBSELECTBLOB.*\\sINTO\\s*.*\\s*FROM.*", Pattern.CASE_INSENSITIVE);
		Matcher m = expr.matcher(aSql);
		if (!m.find())
		{
			result.addMessage(ResourceMgr.getString("ErrSelectBlobSyntax"));
			result.setFailure();
			return result;
		}
		Pattern p = Pattern.compile(VERB, Pattern.CASE_INSENSITIVE);
		m = p.matcher(aSql);
		String sql = m.replaceAll("SELECT");
		p = Pattern.compile("\\sINTO\\s", Pattern.CASE_INSENSITIVE);
		m = p.matcher(sql);
		if (!m.find())
		{
			result.addMessage(ResourceMgr.getString("ErrSelectBlobParseError"));
			result.setFailure();
			return result;
		}
		int pos = m.start() + 5;// sql.toUpperCase().indexOf("INTO") + 4;
		WbStringTokenizer tok = new WbStringTokenizer(sql.substring(pos), "\t \r\n", false, "\"'", true);
		String filename = null;
		if (tok.hasMoreTokens())
		{
			filename = tok.nextToken();
		}
		sql = StringUtil.replace(sql, filename, "");
		sql = sql.replaceFirst("(?i)\\sINTO\\s", " ");
		filename = evaluateFileArgument(filename);
		File f = new File(filename);
		if (f.isDirectory())
		{
			String msg = ResourceMgr.getString("ErrUpdateBlobFileNotFound").replaceAll("%filename%", filename);
			result.addMessage(msg);
			result.setFailure();
			return result;
		}
		LogMgr.logDebug("WbSelectBlob.execute()", "Using SQL=" + sql + " for file: " + filename);
		Statement stmt = null;
		ResultSet rs = null;
		this.currentConnection = aConnection;
		OutputStream out = null;
		InputStream in = null;
		long filesize = 0;
		
		WbFile outputFile = new WbFile(filename);
		File outputDir = outputFile.getParentFile();
		String baseFilename = outputFile.getFileName();
		String extension = outputFile.getExtension();
		if (StringUtil.isEmptyString(extension)) extension = "";
		else extension = "." + extension;
		
		try
		{
			stmt = aConnection.createStatementForQuery();
			this.currentStatement = stmt;
			rs = stmt.executeQuery(sql);
			int row = 0;
			while (rs.next())
			{
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
					f = new File(filename);
				}
				else
				{
					f = new File(outputDir, baseFilename + "_" + Integer.toString(row) + extension);
				}
				out = new FileOutputStream(f);
				filesize = FileUtil.copy(in, out);
				String msg = ResourceMgr.getString("MsgBlobSaved");
				msg = StringUtil.replace(msg, "%filename%", f.getAbsolutePath());
				msg = msg.replaceAll("%filesize%", Long.toString(filesize));
				result.addMessage(msg);
				result.setSuccess();
				row ++;
			}
			this.appendSuccessMessage(result);
		}
		catch (IOException e)
		{
			String msg = ResourceMgr.getString("ErrSelectBlobFileError").replaceAll("%filename%", filename);
			result.addMessage(msg);
			result.setFailure();
			return result;
		}
		catch (SQLException e)
		{
			String msg = ResourceMgr.getString("ErrSelectBlobSqlError").replaceAll("%filename%", filename);
			result.addMessage(msg);
			result.addMessage(ExceptionUtil.getDisplay(e));
			result.setFailure();
			return result;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		return result;
	}
	
}
