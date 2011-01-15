/*
 * ResultNameParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import workbench.log.LogMgr;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.StringUtil;

/**
 * A class to extract a "result name" from a statement's comment, similar
 * to Javadoc tags.
 * 
 * @author Thomas Kellerer
 */
public class ResultNameParser
{
	public final String resultKeyword = "@wbresult";
	
	public ResultNameParser()
	{
	}

	public String getResultName(String sql)
	{
		if (StringUtil.isBlank(sql)) return null;
		try
		{
			SQLLexer lexer = new SQLLexer(sql);
			SQLToken token = lexer.getNextToken(true, false);
			if (token == null || !token.isComment()) return null;

			String comment = token.getText();
			comment = stripCommentChars(comment.trim());
			int pos = comment.indexOf(resultKeyword);
			if (pos == -1) return null;

			pos += resultKeyword.length();
			if (pos >= comment.length()) return null;
			if (!Character.isWhitespace(comment.charAt(pos))) return null;

			int nameEnd = StringUtil.findPattern(StringUtil.PATTERN_CRLF, comment, pos + 1);
			if (nameEnd == -1) nameEnd = comment.length();
			return comment.substring(pos + 1, nameEnd);
		}
		catch (Exception e)
		{
			LogMgr.logWarning("ResultNameParser.getResultName()", "Error when parsing sql", e);
			return null;
		}
	}

	private String stripCommentChars(String sql)
	{
		if (sql.startsWith("--")) return sql.substring(2);
		if (sql.startsWith("/*")) return sql.substring(2, sql.length() - 2);
		return sql;
	}
}
