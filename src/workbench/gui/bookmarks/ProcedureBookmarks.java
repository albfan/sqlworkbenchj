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

import workbench.resource.GuiSettings;

import workbench.sql.lexer.SQLToken;

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
		skip,
		createKeyword,
		procType,
		procName,
		parameterList;
	}

	private enum ParameterState
	{
		none,
		name,
		dataType,
		defaultValue;
	}

	private Set<String> modeKeywords = CollectionUtil.caseInsensitiveSet("OUT", "IN", "INOUT");
	private Set<String> typeKeywords = CollectionUtil.caseInsensitiveSet("PROCEDURE", "FUNCTION");
	private Set<String> createKeywords = CollectionUtil.caseInsensitiveSet("CREATE", "ALTER", "CREATE OR REPLACE");
	private Set<String> noParse = CollectionUtil.caseInsensitiveSet("DROP", "GRANT", "REVOKE", "RENAME");
	private Set<String> parameterStart = CollectionUtil.caseInsensitiveSet("(");
	private Set<String> parameterEnd = CollectionUtil.caseInsensitiveSet(")", "COLLATE", "NOT NULL");
	private Set<String> parameterDefault = CollectionUtil.caseInsensitiveSet("DEFAULT");
	private Set<String> createTerminal = CollectionUtil.caseInsensitiveSet("AS", "RETURN", "IS", "BEGIN", ";", "DECLARE", "RETURNS");

	private List<NamedScriptLocation> procedures = new ArrayList<>();

	private final String id;

	private GlobalState globalState;
	private ParseState parseState;
	private ParameterState parmState;

	private SQLToken currentStartToken;
	private String currentIdentifier;
	private String parameterList;

	// counts opening/closing brackets while inside the parameterlist
	// in order to cope with data types that contain commas, e.g. numeric(12,1)
	private int bracketCount;
	private boolean includeParameterNames;

	public ProcedureBookmarks()
	{
		this("script");
	}

	public ProcedureBookmarks(String panelId)
	{
		this.id = panelId;
		reset();
		this.includeParameterNames = GuiSettings.getProcBookmarksIncludeParmName();
	}

	public void setIncludeParameterNames(boolean flag)
	{
		this.includeParameterNames = flag;
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
					currentStartToken = token;
				}
				else if (typeKeywords.contains(content))
				{
					parseState = ParseState.procName;
					currentStartToken = token;
				}
				else if (noParse.contains(content))
				{
					parseState = ParseState.skip;
					currentStartToken = null;
					currentIdentifier = "";
				}
				break;

			case skip:
				if (";".equals(content))
				{
					parseState = ParseState.none;
				}
				break;

			case createKeyword:
				if (typeKeywords.contains(content))
				{
					parseState = ParseState.procType;
					currentStartToken = token;
				}
				else if (content.equals("PACKAGE BODY"))
				{
					globalState = GlobalState.packageBody;
					currentStartToken = null;
				}
				else if (content.equals("PACKAGE"))
				{
					globalState = GlobalState.packageSpec;
					currentStartToken = null;
				}
				else if (createTerminal.contains(content))
				{
					parseState = ParseState.none;
					currentStartToken = null;
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
					parmState = ParameterState.name;
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

				if ("(".equals(content))
				{
					bracketCount++;
				}

				if ((parameterEnd.contains(content) && bracketCount == 0) || createTerminal.contains(content))
				{
					parseState = ParseState.none;
					parmState = ParameterState.none;
					addCurrentProc();
				}
				else
				{
					if (",".equals(content) && bracketCount == 0)
					{
						parmState = ParameterState.name;
					}
					else if (parameterDefault.contains(content) || (content.equals("=") && (parmState == ParameterState.dataType || parmState == ParameterState.none)))
					{
						parmState = ParameterState.defaultValue;
					}
					else if (modeKeywords.contains(content))
					{
						parmState = ParameterState.dataType;
					}
					else if (parmState == ParameterState.none || parmState == ParameterState.dataType)
					{
						if (parameterList.length() > 0 && bracketCount == 0)
						{
							parameterList += includeParameterNames ? " " : ",";
						}
						parameterList += token.getText();
					}
					else if (parmState == ParameterState.name)
					{
						parmState = ParameterState.dataType;
						if (includeParameterNames)
						{
							if (parameterList.length() > 0) parameterList += ", ";
							parameterList += token.getText();
						}
					}

					if (")".equals(content) && bracketCount > 0)
					{
						bracketCount--;
						parmState = ParameterState.none;
					}
				}
				break;
		}
	}

	private void addCurrentProc()
	{
		if (currentStartToken != null && this.currentIdentifier != null)
		{
			String name = currentIdentifier;
			if (StringUtil.isNonEmpty(parameterList))
			{
				name += "(" + parameterList + ")";
			}
			if (globalState == GlobalState.packageSpec)
			{
				name = GuiSettings.getBookmarksPkgSpecPrefix() + " " + name;
			}
			NamedScriptLocation bookmark = new NamedScriptLocation(name, currentStartToken.getCharBegin(), id);
			this.procedures.add(bookmark);
			parameterList = null;
			currentIdentifier = "";
			bracketCount = 0;
			currentStartToken = null;
		}
	}


	void reset()
	{
		parseState = ParseState.none;
		globalState = GlobalState.none;
		parmState = ParameterState.none;
		procedures.clear();
		currentIdentifier = "";
		bracketCount = 0;
		currentStartToken = null;
	}

	public List<NamedScriptLocation> getBookmarks()
	{
		return procedures;
	}
}
