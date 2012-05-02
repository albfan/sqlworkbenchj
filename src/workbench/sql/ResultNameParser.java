/*
 * ResultNameParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
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
		SQLLexer lexer = new SQLLexer(sql);
		SQLToken token = lexer.getNextToken(true, false);

		while (token != null && token.isComment())
		{
			String comment = token.getText();
			comment = stripCommentChars(comment.trim());
			int pos = comment.toLowerCase().indexOf(resultKeyword);
			if (pos >= 0)
			{
				pos += resultKeyword.length();
				if (pos >= comment.length()) return null;
				if (Character.isWhitespace(comment.charAt(pos)))
				{
					int nameEnd = StringUtil.findPattern(StringUtil.PATTERN_CRLF, comment, pos + 1);
					if (nameEnd == -1) nameEnd = comment.length();
					return comment.substring(pos + 1, nameEnd);
				}
			}
			token = lexer.getNextToken(true, false);
		}
		return null;
	}

	private String stripCommentChars(String sql)
	{
		if (sql.startsWith("--")) return sql.substring(2);
		if (sql.startsWith("/*")) return sql.substring(2, sql.length() - 2);
		return sql;
	}
}
