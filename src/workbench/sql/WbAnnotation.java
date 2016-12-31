/*
 * WbAnnotation.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;

import workbench.util.StringUtil;

/**
 * A class to extract the value of an "annotation" from a statement's comment, similar to Javadoc tags.
 *
 * @author Thomas Kellerer
 */
public class WbAnnotation
{
	private final String keyword;
	private String value;

	public WbAnnotation(String key)
	{
		if (key.charAt(0) == '@')
		{
			keyword = key;
		}
		else
		{
			keyword = getTag(key);
		}
	}

	public static String getTag(String key)
	{
		return "@" + key.toLowerCase();
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String annotationValue)
	{
		this.value = annotationValue;
	}

	public String getKeyWord()
	{
		return keyword;
	}

	public String getAnnotationValue(String sql)
	{
		SQLToken token = getAnnotationToken(sql);

		if (token == null)
		{
			return null;
		}
		return extractAnnotationValue(token);
	}

	public static List<WbAnnotation> readAllAnnotations(String sql, Set<String> keys)
	{
		if (sql == null) return Collections.emptyList();
    if (!sql.startsWith("--") && !sql.startsWith("/*")) return Collections.emptyList();

		SQLLexer lexer = SQLLexerFactory.createLexer(sql);
		SQLToken token = lexer.getNextToken(true, false);

		List<WbAnnotation> result = new ArrayList<>();

		while (token != null && token.isComment())
		{
			String comment = token.getText();
			comment = stripCommentChars(comment.trim());
			for (String key : keys)
			{
				if (key == null) continue;
				int pos = comment.toLowerCase().indexOf(key);
				if (pos >= 0)
				{
					pos += key.length();
					if (pos >= comment.length()) continue ;
					if (Character.isWhitespace(comment.charAt(pos)))
					{
						WbAnnotation annotation = new WbAnnotation(key);
						String value = extractAnnotationValue(token, annotation.getKeyWord());
						annotation.setValue(value);
						result.add(annotation);
					}
				}
			}
			token = lexer.getNextToken(true, false);
		}
		return result;
	}

	public boolean containsAnnotation(String sql)
	{
		if (StringUtil.isBlank(sql)) return false;
		SQLLexer lexer = SQLLexerFactory.createLexer(sql);

		SQLToken token = lexer.getNextToken(true, false);

		while (token != null && token.isComment())
		{
			String comment = token.getText();
			comment = stripCommentChars(comment.trim());
			int pos = comment.toLowerCase().indexOf(keyword);
			if (pos >= 0)
			{
				return true;
			}
			token = lexer.getNextToken(true, false);
		}
		return false;
	}

	protected SQLToken getAnnotationToken(String sql)
	{
		if (StringUtil.isBlank(sql)) return null;
		if (sql.indexOf('@') == -1) return null;

		SQLLexer lexer = SQLLexerFactory.createLexer(sql);
		SQLToken token = lexer.getNextToken(true, false);

		while (token != null && token.isComment())
		{
			String comment = token.getText();
			comment = stripCommentChars(comment.trim());
			int pos = comment.toLowerCase().indexOf(keyword);
			if (pos >= 0)
			{
				pos += keyword.length();
				if (pos >= comment.length()) return null;
				if (Character.isWhitespace(comment.charAt(pos)))
				{
					return token;
				}
			}
			token = lexer.getNextToken(true, false);
		}
		return null;
	}

	protected String extractAnnotationValue(SQLToken token)
	{
		return extractAnnotationValue(token, keyword);
	}

	protected static String extractAnnotationValue(SQLToken token, String annotation)
	{
		if (token == null) return null;

		String comment = token.getText();
		comment = stripCommentChars(comment.trim());
		int pos = comment.toLowerCase().indexOf(annotation.toLowerCase());
		if (pos >= 0)
		{
			pos += annotation.length();
			if (pos >= comment.length()) return null;
			if (Character.isWhitespace(comment.charAt(pos)))
			{
				int nameEnd = StringUtil.findPattern(StringUtil.PATTERN_CRLF, comment, pos + 1);
				if (nameEnd == -1) nameEnd = comment.length();
				return comment.substring(pos + 1, nameEnd);
			}
		}
		return null;
	}

	private static String stripCommentChars(String sql)
	{
		if (sql.startsWith("--")) return sql.substring(2);
		if (sql.startsWith("/*")) return sql.substring(2, sql.length() - 2);
		return sql;
	}

  @Override
  public int hashCode()
  {
    return this.keyword.toLowerCase().hashCode();
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj) return true;
    if (obj instanceof WbAnnotation)
    {
      final WbAnnotation other = (WbAnnotation)obj;
      return this.keyword.equalsIgnoreCase(other.keyword);
    }
    return false;
  }

}
