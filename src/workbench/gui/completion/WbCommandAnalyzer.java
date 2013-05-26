/*
 * WbCommandAnalyzer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.gui.completion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;

import workbench.sql.CommandMapper;
import workbench.sql.SqlCommand;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.CaseInsensitiveComparator;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * @author Thomas Kellerer
 */
public class WbCommandAnalyzer
	extends BaseAnalyzer
{
	// True if the the parameters are put into the
	// elements list. This is used by the CompletionPopup
	// to check if the selected value should be enhanced with - and =
	private boolean isParameter;

	private boolean changeCase;
	private final String wordDelimiters = " \t";

	public WbCommandAnalyzer(WbConnection conn, String statement, int cursorPos)
	{
		super(conn, statement, cursorPos);
		changeCase = true;
	}

	@Override
	public boolean isWbParam()
	{
		return this.isParameter;
	}

	@Override
	public String getWordDelimiters()
	{
		return wordDelimiters;
	}

	@Override
	public char quoteCharForValue(String value)
	{
		if (this.isParameter) return 0;
		if (context == CONTEXT_STATEMENT_PARAMETER) return 0;

		if (value.indexOf('-') > -1 || value.indexOf(' ') > -1)
		{
			if (value.indexOf('\'') > -1) return '"';
			else return '\'';
		}
		return 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void checkContext()
	{
		CommandMapper mapper = new CommandMapper();
		String word = StringUtil.getWordLeftOfCursor(this.sql, this.cursorPos, wordDelimiters);

		changeCase = true;

		if (word != null && word.trim().toLowerCase().equals("wb"))
		{
			context = CONTEXT_WB_COMMANDS;
			elements = new ArrayList();
			elements.addAll(mapper.getAllWbCommands());
			return;
		}

		SqlCommand cmd = mapper.getCommandToUse(this.sql);

		if (cmd == null)
		{
			this.context = NO_CONTEXT;
			this.elements = null;
			return;
		}

		ArgumentParser p = cmd.getArgumentParser();
		if (p == null)
		{
			this.context = NO_CONTEXT;
			this.elements = null;
			return;
		}

		context = CONTEXT_WB_PARAMS;

		String parameter = getCurrentParameter();
		this.isParameter = false;

		if (p.isRegistered(parameter))
		{
			ArgumentType type = p.getArgumentType(parameter);
			if (type == ArgumentType.BoolArgument)
			{
				this.elements = new ArrayList(2);
				this.elements.add("true");
				this.elements.add("false");
			}
			else if (type == ArgumentType.TableArgument)
			{
				this.context = CONTEXT_TABLE_LIST;
				this.schemaForTableList = getSchemaFromCurrentWord();
			}
			else if (type == ArgumentType.ListArgument)
			{
				this.elements = new ArrayList(p.getAllowedValues(parameter));
			}
			else if (type == ArgumentType.ObjectTypeArgument)
			{
				this.elements  = new ArrayList<String>(dbConnection.getMetadata().getObjectTypes());
			}
			else if (type == ArgumentType.SchemaArgument)
			{
				this.elements  = new ArrayList<String>(dbConnection.getMetadata().getSchemas(dbConnection.getSchemaFilter()));
			}
			else if (type == ArgumentType.CatalogArgument)
			{
				this.elements  = new ArrayList<String>(dbConnection.getMetadata().getCatalogInformation(dbConnection.getCatalogFilter()));
			}
			else if (type == ArgumentType.ProfileArgument)
			{
				this.elements = ConnectionMgr.getInstance().getProfileKeys();
				changeCase = false;
			}
			else
			{
				this.context = NO_CONTEXT;
				this.elements = null;
			}
		}
		else
		{
			List<String> arguments = p.getRegisteredArguments();
			this.elements = arguments;
			String params = SqlUtil.stripVerb(this.sql);
			p.parse(params);
			List<String> argsPresent = p.getArgumentsOnCommandLine();
			this.elements.removeAll(argsPresent);
			Collections.sort(this.elements, CaseInsensitiveComparator.INSTANCE);
			isParameter = p.needsSwitch();
		}
	}

	@Override
	public boolean convertCase()
	{
		return changeCase;
	}

	/**
	 * Returns the name of the parameter where the cursor is currently located.
	 * If the previous non-whitespace character left of the cursor is the equal
	 * sign, then this is assumed to be the "current parameter" and the
	 * corresponding string is returned.
	 * Otherwise it is assumed that the cursor is "between" two parameters
	 * and the list of available parameters should be displayed.
	 *
	 * @return the value of the current parameter or null if no parameter was found
	 */
	protected String getCurrentParameter()
	{
		if (cursorPos > 1 && cursorPos <= this.sql.length())
		{
			char c = this.sql.charAt(cursorPos - 1);
			if (Character.isWhitespace(c)) return null;
		}

		String word = StringUtil.getWordLeftOfCursor(this.sql, this.cursorPos, " \t");
		if (word == null) return null;
		if (word.charAt(0) == '-' && word.length() > 2)
		{
			int end = word.indexOf('=');
			if (end == -1)
			{
				end = word.length() - 1;
			}
			return word.substring(1, end);
		}
		return null;
	}

}
