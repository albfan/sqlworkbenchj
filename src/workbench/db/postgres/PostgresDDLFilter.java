/*
 * PostgresDDLFilter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.db.DDLFilter;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;

/**
 * @author support@sql-workbench.net
 */
public class PostgresDDLFilter
	implements DDLFilter
{
	private Pattern createFunc = Pattern.compile("CREATE\\s*(OR\\s*REPLACE|)\\s*FUNCTION", Pattern.CASE_INSENSITIVE);
	
	public PostgresDDLFilter()
	{
	}

	/**
	 * PG's documentation shows CREATE FUNCTION samples that use
	 * a "dollar quoting" to avoid the nested single quotes
	 * e.g. http://www.postgresql.org/docs/8.0/static/plpgsql-structure.html
	 * but the JDBC driver does not (yet) understand this as well (this 
	 * seems to be only implemented in the psql command line tool
	 * So we'll replace the "dollar quotes" with regular single quotes
	 * Every single quote inside the function body will be replaced with 
	 * two single quotes in properly "escape" them
	 */
	public String adjustDDL(String sql)
	{
		int bodyStart = -1;
		int bodyEnd = -1;
		
		// Only valid for CREATE ... FUNCTION or PROCEDURE statements
		Matcher m = createFunc.matcher(sql);
		if (!m.find()) return sql;
		
		SQLLexer lexer = new SQLLexer(sql);
		
		try
		{
			SQLToken t = lexer.getNextToken(false, false);
			
			while (t != null)
			{
				String value = t.getContents();
				if ("$$".equals(value))
				{
					if (bodyStart == -1) bodyStart = t.getCharEnd();
					else bodyEnd = t.getCharBegin();
				}
				t = lexer.getNextToken(false, false);
			}
		}
		catch (Exception e)
		{
			
		}
		
		if (bodyStart == -1 || bodyEnd == -1) return sql;
		
		String body = sql.substring(bodyStart, bodyEnd);
		body = body.replaceAll("'", "''");

		StringBuffer newSql = new StringBuffer(sql.length() + 10);
		newSql.append(sql.substring(0, bodyStart - 2));
		newSql.append('\'');
		newSql.append(body);
		newSql.append('\'');
		newSql.append(sql.substring(bodyEnd + 2));
		return newSql.toString();
	}
	
}
