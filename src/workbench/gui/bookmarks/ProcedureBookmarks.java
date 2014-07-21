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

package workbench.gui.bookmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ProcedureBookmarks
{
	private enum GlobalState
	{
		none,
		packageSpec,
		packageBody;
	}

	private enum ParseState
	{
		none,
		createKeyword,
		procType,
		procName,
		parameterList;
	}

	private Set<String> typeKeywords = CollectionUtil.caseInsensitiveSet("PROCEDURE", "FUNCTION");
	private Set<String> createKeywords = CollectionUtil.caseInsensitiveSet("CREATE", "ALTER", "CREATE OR REPLACE");
	private Set<String> parameterStart = CollectionUtil.caseInsensitiveSet("(");
	private Set<String> parameterEnd = CollectionUtil.caseInsensitiveSet(")");
	private Set<String> createTerminal = CollectionUtil.caseInsensitiveSet("AS", "RETURN", "IS", "BEGIN", ";");
	private List<NamedScriptLocation> procedures = new ArrayList<NamedScriptLocation>();

	private final String id;

	private GlobalState globalState;
	private ParseState parseState;
	private SQLToken createToken;
	private String currentIdentifier;
	private String type;
	private String parameterList;

	public ProcedureBookmarks()
	{
		this("script");
	}

	public ProcedureBookmarks(String panelId)
	{
		this.id = panelId;
		reset();
	}

	public void processToken(SQLToken token)
	{
		if (token == null) return;
		if (token.isComment()) return;
		if (token.isWhiteSpace()) return;

		String content = token.getContents();

		switch (parseState)
		{
			case none:
				if (createKeywords.contains(content))
				{
					parseState = ParseState.createKeyword;
					createToken = token;
				}
				break;
			case createKeyword:
				if (typeKeywords.contains(content))
				{
					parseState = ParseState.procType;
					type = content;
				}
				else if (content.equals("PACKAGE BODY"))
				{
					globalState = GlobalState.packageSpec;
				}
				else if (content.equals("PACKAGE"))
				{
					globalState = GlobalState.packageBody;
				}
				break;
			case procType:
				if (token.isIdentifier())
				{
					parseState = ParseState.procName;
					currentIdentifier = content;
				}
				break;
			case procName:
				if (parameterStart.contains(content))
				{
					parseState = ParseState.parameterList;
					parameterList = "";
				}
				else if (createTerminal.contains(content))
				{
					parseState = ParseState.none;
					addCurrentProc();
				}
				else
				{
					currentIdentifier += content;
				}
				break;
			case parameterList:
				if (parameterEnd.contains(content) || createTerminal.contains(content))
				{
					parseState = ParseState.none;
					addCurrentProc();
				}
				else
				{
					if (parameterList.length() > 0) parameterList += " ";
					parameterList += token.getText();
				}
				break;
		}
	}

	private void addCurrentProc()
	{
		if (createToken != null && this.currentIdentifier != null)
		{
			String name = currentIdentifier;
			if (StringUtil.isNonEmpty(type))
			{
				name = type + " " + name;
			}
			if (StringUtil.isNonEmpty(parameterList))
			{
				name += " (" + parameterList + ")";
			}
			if (globalState == GlobalState.packageSpec)
			{
				name = "Spec: " + name;
			}
			NamedScriptLocation bookmark = new NamedScriptLocation(name, createToken.getCharBegin(), id);
			this.procedures.add(bookmark);
			parameterList = null;
			type = null;
			currentIdentifier = null;
		}
	}


	private void reset()
	{
		parseState = ParseState.none;
		globalState = GlobalState.none;
		type = null;
		procedures.clear();
		currentIdentifier = null;
	}

	public List<NamedScriptLocation> getBookmarks()
	{
		return procedures;
	}

	public void parseScript(String script)
	{
		reset();
		SQLLexer lexer = new SQLLexer(script);
		SQLToken token = lexer.getNextToken(false, false);
		while (token != null)
		{
			processToken(token);
			token = lexer.getNextToken(false, false);
		}
	}

}
