/*
 * DefaultScriptParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import workbench.log.LogMgr;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.EncodingUtil;

/**
 * @author support@sql-workbench.net
 */
public class DefaultScriptParser
{

	private SQLLexer lexer;
	private StringBuilder nextCommand = new StringBuilder(64);
	private String delimiter = ";";
	
	public DefaultScriptParser(String sql)
	{
		this.lexer = new SQLLexer(sql);
	}
	
	public DefaultScriptParser(File source, String encoding)
		throws IOException
	{
		Reader r = EncodingUtil.createReader(source, encoding);
		this.lexer = new SQLLexer(r);
	}

	public void setDelimiter(char c)
	{
		this.delimiter = new String(new char[] { c });
	}

	public String getNextCommand()
	{
		if (this.nextCommand == null) return null;
		try
		{
			SQLToken tok = null; ;
			while ((tok = this.lexer.getNextToken(true, true)) !=  null)
			{
				String text = tok.getText();
				if (text.equals(this.delimiter)) break;
				nextCommand.append(text);
			}
			String result = this.nextCommand.toString();
			if (tok == null)
			{
				this.nextCommand = null;
			}
			else
			{
				this.nextCommand = new StringBuilder(64);
			}
			return (result.length() == 0 ? null : result);
		}
		catch (Exception e)
		{
			LogMgr.logError("DefaultScriptParser.getNextCommand()", "Error parsing script", e);
			return null;
		}
		
	}
}
