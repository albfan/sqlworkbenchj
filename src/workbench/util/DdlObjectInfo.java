/*
 * DdlObjectInfo.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer.
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
package workbench.util;

import java.util.Set;

import workbench.log.LogMgr;

import workbench.db.WbConnection;
import workbench.db.oracle.OracleUtils;

import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLLexerFactory;
import workbench.sql.formatter.SQLToken;

/**
 *
 * @author Thomas Kellerer
 */
public class DdlObjectInfo
{
	private String objectType;
	private String objectName;

	public DdlObjectInfo(CharSequence sql)
	{
		parseSQL(sql, null);
	}

	public DdlObjectInfo(CharSequence sql, WbConnection conn)
	{
		parseSQL(sql, conn);
	}

	@Override
	public String toString()
	{
		return "Type: " + objectType + ", name: " + objectName;
	}

	public String getDisplayType()
	{
		return StringUtil.capitalize(objectType);
	}

	public boolean isValid()
	{
		return objectType != null;
	}

	public String getObjectType()
	{
		return objectType;
	}

	public String getObjectName()
	{
		return objectName;
	}

	private void parseSQL(CharSequence sql, WbConnection conn)
	{
		SQLLexer lexer = SQLLexerFactory.createLexer(conn, sql);
		SQLToken t = lexer.getNextToken(false, false);

		if (t == null) return;
		String verb = t.getContents();
		Set<String> verbs = CollectionUtil.caseInsensitiveSet("DROP", "RECREATE", "ALTER", "ANALYZE");

		if (!verb.startsWith("CREATE") && !verbs.contains(verb)) return;

		try
		{
			boolean typeFound = false;
			SQLToken token = lexer.getNextToken(false, false);
			while (token != null)
			{
				String c = token.getContents();
				if (SqlUtil.getKnownTypes().contains(c))
				{
					typeFound = true;
					this.objectType = c.toUpperCase();
					break;
				}
				token = lexer.getNextToken(false, false);
			}

			if (!typeFound) return;

			// if a type was found we assume the next keyword is the name
			if (!SqlUtil.getTypesWithoutNames().contains(this.objectType))
			{
				SQLToken name = lexer.getNextToken(false, false);
				if (name == null) return;
				String content = name.getContents();
				if (content.equals("IF NOT EXISTS") || content.equals("IF EXISTS") || content.equals(OracleUtils.KEYWORD_EDITIONABLE))
				{
					name = lexer.getNextToken(false, false);
					if (name == null) return;
				}

				SQLToken next = lexer.getNextToken(false, false);
				if (next != null && next.getContents().equals("."))
				{
					next = lexer.getNextToken(false, false);
					if (next != null) name = next;
				}

				if (!name.isReservedWord())
				{
					this.objectName = name.getContents();
				}

				if (this.objectName != null)
				{
					this.objectName = SqlUtil.removeObjectQuotes(this.objectName);
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DdlObjectInfo.parseSQL()", "Error finding object info", e);
			this.objectName = null;
			this.objectType = null;
		}
	}
}
