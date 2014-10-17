/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.formatter;

import java.io.Reader;

import workbench.db.DbMetadata;
import workbench.db.WbConnection;

import workbench.sql.ParserType;

/**
 *
 * @author Thomas Kellerer
 */
public class SQLLexerFactory
{
	public static SQLLexer createLexer()
	{
		SQLLexer lexer = new StandardLexer("");
		return lexer;
	}

	public static SQLLexer createLexer(WbConnection conn)
	{
		SQLLexer lexer = new StandardLexer("");
		configureLexer(lexer, conn);
		return lexer;
	}

	public static SQLLexer createLexer(CharSequence sql)
	{
		SQLLexer lexer = new StandardLexer(sql);
		return lexer;
	}

	public static SQLLexer createLexer(WbConnection conn, String sql)
	{
		SQLLexer lexer = new StandardLexer(sql);
		configureLexer(lexer, conn);
		return lexer;
	}

	public static SQLLexer createLexer(WbConnection conn, CharSequence sql)
	{
		SQLLexer lexer = new StandardLexer(sql);
		configureLexer(lexer, conn);
		return lexer;
	}

	public static SQLLexer createLexerForDbId(String dbId, CharSequence sql)
	{
		SQLLexer lexer = new StandardLexer(sql);
		configureLexer(lexer, dbId);
		return lexer;
	}

	public static SQLLexer createLexer(Reader input, ParserType type)
	{
		SQLLexer lexer = new NonStandardLexer(input);
		if (type == ParserType.SqlServer)
		{
			lexer.setCheckStupidQuoting(true);
		}
		return lexer;
	}

	public static SQLLexer createNonStandardLexer(Reader input, ParserType type)
	{
		SQLLexer lexer = new NonStandardLexer(input);
		if (type == ParserType.SqlServer)
		{
			lexer.setCheckStupidQuoting(true);
		}
		return lexer;
	}


	public static SQLLexer createNonStandardLexer(String sql, ParserType type)
	{
		SQLLexer lexer = new NonStandardLexer(sql);
		if (type == ParserType.SqlServer)
		{
			lexer.setCheckStupidQuoting(true);
		}
		return lexer;
	}

	private static void configureLexer(SQLLexer lexer, WbConnection conn)
	{
		if (conn == null) return;
		configureLexer(lexer, conn.getDbId());
	}

	private static void configureLexer(SQLLexer lexer, String dbId)
	{
		if (dbId == null) return;
		lexer.setCheckStupidQuoting(dbId.equalsIgnoreCase(DbMetadata.DBID_MS));
	}

}
