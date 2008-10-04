/*
 * PostgresDDLFilter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import workbench.db.DDLFilter;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;

/**
 * @author support@sql-workbench.net
 */
public class PostgresDDLFilter
	implements DDLFilter
{

	/**
	 * PG's documentation shows CREATE FUNCTION samples that use
	 * a "dollar quoting" to avoid the nested single quotes
	 * e.g. http://www.postgresql.org/docs/8.0/static/plpgsql-structure.html
	 *
	 * But the JDBC driver does not (yet) understand this - this
	 * seems to be only implemented in the psql command line tool.
	 *
	 * So we'll replace the "dollar quotes" with regular single quotes
	 * Every single quote inside the function body will be replaced with
	 * two single quotes to properly "escape" them.
	 *
	 * This does not mimic psql's quoting completely as basically
	 * you can also use some descriptive words between the dollar signs, such
	 * as $body$ which will not be detected by this method.
	 */
	public String adjustDDL(String sql)
	{
		int bodyStart = -1;
		int bodyEnd = -1;

		SQLLexer lexer = new SQLLexer(sql);

		try
		{
			SQLToken t = lexer.getNextToken(false, false);
			if (t != null)
			{
				String v = t.getContents();
				if (!"CREATE".equals(v) && !"CREATE OR REPLACE".equals(v)) return sql;
			}

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
		body = body.replace("'", "''");

		StringBuilder newSql = new StringBuilder(sql.length() + 10);
		newSql.append(sql.substring(0, bodyStart - 2));
		newSql.append(" '");
		newSql.append(body);
		newSql.append("' ");
		newSql.append(sql.substring(bodyEnd + 2));
		return newSql.toString();
	}

}
