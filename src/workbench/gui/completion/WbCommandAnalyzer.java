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

import workbench.log.LogMgr;

import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.db.importer.SpreadsheetReader;

import workbench.sql.CommandMapper;
import workbench.sql.SqlCommand;
import workbench.sql.VariablePool;
import workbench.sql.wbcommands.WbGrepSource;
import workbench.sql.wbcommands.WbImport;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.CaseInsensitiveComparator;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

import static workbench.gui.completion.BaseAnalyzer.CONTEXT_WB_PARAMS;
import workbench.sql.wbcommands.WbDescribeObject;


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
	private boolean useSheetIndex = true;

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
		if (context == CONTEXT_WB_COMMANDS)
		{
			return wordDelimiters;
		}
		return super.getWordDelimiters();
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

		p.parse(this.sql);

		String parameter = getCurrentParameter();
		this.isParameter = false;

		if (p.isRegistered(parameter))
		{
			context = CONTEXT_WB_PARAMVALUES;
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
				if (verb.equalsIgnoreCase(WbGrepSource.VERB))
				{
					elements.add("FUNCTION");
					elements.add("PROCEDURE");
					elements.add("TRIGGER");
				}
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
			else if (parameter.equals(WbImport.ARG_SHEET_NR))
			{
				this.useSheetIndex = true;
				this.elements = getSheetnames(cmd, p);
				changeCase = false;
			}
			else if (parameter.equals(WbImport.ARG_SHEET_NAME))
			{
				this.useSheetIndex = false;
				this.elements = getSheetnames(cmd, p);
				changeCase = false;
			}
			else
			{
				this.context = NO_CONTEXT;
				this.elements = null;
			}
		}
		else if (cmd instanceof WbDescribeObject && p.getArgumentCount() == 0)
		{
			this.context = CONTEXT_TABLE_LIST;
			this.elements = null;
		}
		else
		{
			context = CONTEXT_WB_PARAMS;
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
	public String getPasteValue(Object selectedObject)
	{
		if (selectedObject instanceof SheetEntry)
		{
			SheetEntry entry = (SheetEntry)selectedObject;
			if (useSheetIndex)
			{
				return Integer.toString(entry.sheetIndex);
			}
			else
			{
				return entry.sheetName;
			}
		}
		return null;
	}

	private List getSheetnames(SqlCommand wbImport, ArgumentParser cmdLine)
	{
		cmdLine.parse(this.sql);
		String fname = cmdLine.getValue(WbImport.ARG_FILE);
		fname = VariablePool.getInstance().replaceAllParameters(fname);
		WbFile input = wbImport.evaluateFileArgument(fname);

		List result = new ArrayList();
		if (input == null) return result;

		SpreadsheetReader reader = SpreadsheetReader.Factory.createReader(input, -1, null);
		try
		{
			List<String> sheets = reader.getSheets();
			for (int index = 0; index < sheets.size(); index++ )
			{
				String display = null;
				String name = sheets.get(index);
				if (useSheetIndex)
				{
					display = Integer.toString(index + 1) + " - " + name;
				}
				else
				{
					display = name;
				}
				SheetEntry entry = new SheetEntry(index + 1, name, display);
				result.add(entry);
			}
		}
		catch (Exception e)
		{
			LogMgr.logWarning("WbCommandAnalyzer.getSheetnames()", "Could not read spreadsheet: " + input.getFullPath(), e);
		}
		finally
		{
			reader.done();
		}
		return result;
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

	private static class SheetEntry
	{
		private final int sheetIndex;
		private final String sheetName;
		private final String displayString;

		SheetEntry(int index, String name, String display)
		{
			this.sheetIndex = index;
			this.sheetName = name;
			this.displayString = display;
		}

		@Override
		public String toString()
		{
			return displayString;
		}
	}

	@Override
	public boolean needsCommaForMultipleSelection()
	{
		return (context != CONTEXT_WB_PARAMS);
	}

}
