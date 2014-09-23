/*
 * WbCommandAnalyzer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import workbench.liquibase.ChangeSetIdentifier;
import workbench.liquibase.LiquibaseParser;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.db.importer.SpreadsheetReader;

import workbench.sql.CommandMapper;
import workbench.sql.ParserType;
import workbench.sql.SqlCommand;
import workbench.sql.VariablePool;
import workbench.sql.wbcommands.CommonArgs;
import workbench.sql.wbcommands.WbDescribeObject;
import workbench.sql.wbcommands.WbExport;
import workbench.sql.wbcommands.WbGrepSource;
import workbench.sql.wbcommands.WbImport;
import workbench.sql.wbcommands.WbRunLB;
import workbench.sql.wbcommands.WbTableSource;
import workbench.sql.wbcommands.WbXslt;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.CaseInsensitiveComparator;
import workbench.util.FileUtil;
import workbench.util.MessageBuffer;
import workbench.util.NumberStringCache;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

import static workbench.gui.completion.BaseAnalyzer.*;


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

	// Maps a Wb command to a map of recently used directories for each parameter
	private static final Map<String, Map<String, String>> LRU_DIR_MAP = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);

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
		if (context == CONTEXT_WB_PARAMVALUES)
		{
			return wordDelimiters + "=";
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

		ArgumentParser args = cmd.getArgumentParser();
		if (args == null)
		{
			if (showTableList(cmd, args))
			{
				this.context = CONTEXT_TABLE_LIST;
			}
			else
			{
				this.context = NO_CONTEXT;
			}
			this.elements = null;
			return;
		}

		args.parse(this.sql);

		String parameter = getCurrentParameter();
		this.isParameter = false;

		if (args.isRegistered(parameter))
		{
			context = CONTEXT_WB_PARAMVALUES;
			title = ResourceMgr.getString("LblCompletionListParmValues");

			ArgumentType type = args.getArgumentType(parameter);
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
				this.elements = new ArrayList(args.getAllowedValues(parameter));
			}
			else if (type == ArgumentType.ObjectTypeArgument)
			{
				this.elements  = new ArrayList<>(dbConnection.getMetadata().getObjectTypes());
				if (verb.equalsIgnoreCase(WbGrepSource.VERB))
				{
					elements.add("FUNCTION");
					elements.add("PROCEDURE");
					elements.add("TRIGGER");
				}
			}
			else if (type == ArgumentType.SchemaArgument)
			{
				this.elements  = new ArrayList<>(dbConnection.getMetadata().getSchemas(dbConnection.getSchemaFilter()));
			}
			else if (type == ArgumentType.CatalogArgument)
			{
				this.elements  = new ArrayList<>(dbConnection.getMetadata().getCatalogInformation(dbConnection.getCatalogFilter()));
			}
			else if (type == ArgumentType.ProfileArgument)
			{
				this.elements = ConnectionMgr.getInstance().getProfileKeys();
				changeCase = false;
			}
			else if (type == ArgumentType.Filename)
			{
				this.elements = getFiles(args, parameter, false);
				this.setOverwriteCurrentWord(true);
			}
			else if (type == ArgumentType.DirName)
			{
				this.elements = getFiles(args, parameter, true);
				this.setOverwriteCurrentWord(true);
			}
			else if (parameter.equalsIgnoreCase(WbImport.ARG_SHEET_NR) || parameter.equalsIgnoreCase(WbExport.ARG_TARGET_SHEET_IDX))
			{
				this.useSheetIndex = true;
				this.elements = getSheetnames(cmd, args, true);
				changeCase = false;
			}
			else if (parameter.equalsIgnoreCase(WbRunLB.ARG_CHANGESET))
			{
				this.elements = getChangeSets(cmd, args);
				changeCase = false;
			}
			else if (parameter.equalsIgnoreCase(WbImport.ARG_SHEET_NAME) || parameter.equalsIgnoreCase(WbExport.ARG_TARGET_SHEET_NAME))
			{
				this.useSheetIndex = false;
				this.elements = getSheetnames(cmd, args, false);
				changeCase = false;
			}
			else
			{
				this.context = NO_CONTEXT;
				this.elements = null;
			}
		}
		else if (showTableList(cmd, args))
		{
			this.context = CONTEXT_TABLE_LIST;
			this.elements = null;
		}
		else
		{
			context = CONTEXT_WB_PARAMS;
			List<String> arguments = args.getRegisteredArguments();
			this.elements = arguments;
			String params = SqlUtil.stripVerb(this.sql);
			args.parse(params);
			List<String> argsPresent = args.getArgumentsOnCommandLine();
			this.elements.removeAll(argsPresent);
			Collections.sort(this.elements, CaseInsensitiveComparator.INSTANCE);
			isParameter = args.needsSwitch();
			changeCase = false;
		}
	}

	private boolean showTableList(SqlCommand cmd, ArgumentParser args)
	{
		if (cmd == null) return false;

		int numArgs = args == null ? 0 : args.getArgumentCount();
		if (numArgs > 0) return false;

		if (cmd instanceof WbDescribeObject) return true;
		if (cmd instanceof WbTableSource) return true;

		return false;
	}

	@Override
	protected void buildResult()
	{
		if (this.context == CONTEXT_TABLE_LIST)
		{
			super.buildResult();
		}
	}

	private WbFile getCurrentDir(String parameter)
	{
		String lastDir = getLastDirectory(parameter);
		if (StringUtil.isNonBlank(lastDir))
		{
			WbFile dir = new WbFile(lastDir);
			if (dir.exists())
			{
				return dir;
			}
		}

		if (parameter.equalsIgnoreCase(WbXslt.ARG_STYLESHEET))
		{
			File dir = Settings.getInstance().getDefaultXsltDirectory();
			if (dir.exists())
			{
				return new WbFile(dir);
			}
		}
		return new WbFile(".");
	}

	private String getLastDirectory(String parameter)
	{
		if (parameter == null) return null;
		Map<String, String> parameterMap = LRU_DIR_MAP.get(getSqlVerb());
		if (parameterMap == null) return null;
		return parameterMap.get(parameter);
	}

	private void saveLastDirectory(WbFile file)
	{
		if (file == null) return;

		Map<String, String> parameterMap = LRU_DIR_MAP.get(getSqlVerb());
		if (parameterMap == null)
		{
			parameterMap = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);
			LRU_DIR_MAP.put(getSqlVerb(), parameterMap);
		}
		String parameter = getCurrentParameter();
		if (file.isDirectory())
		{
			parameterMap.put(parameter, file.getFullPath());
		}
		else
		{
			WbFile dir = new WbFile(file.getParentFile());
			parameterMap.put(parameter, dir.getFullPath());
		}
	}

	private List<WbFile> getFiles(ArgumentParser cmdLine, String parameter, boolean dirsOnly)
	{
		String name = cmdLine.getValue(parameter);
		File[] files;

		if (StringUtil.isNonBlank(name))
		{
			WbFile f = new WbFile(name);
			if (!f.isDirectory())
			{
				return null;
			}
			files = f.listFiles();
			this.title = f.getFullPath();
		}
		else
		{
			WbFile dir = getCurrentDir(parameter);
			files = dir.listFiles();
			this.title = dir.getFullPath();
		}

		if (files == null)
		{
			return null;
		}

		List<WbFile> result = new ArrayList<>(files.length);
		for (File f : files)
		{
			if (!dirsOnly || f.isDirectory())
			{
				WbFile wb = new TooltipFile(f);
				result.add(wb);
			}
		}
		FileUtil.sortFiles(result);

		return result;
	}

	@Override
	public String getPasteValue(Object selectedObject)
	{
		if (selectedObject instanceof SheetEntry)
		{
			SheetEntry entry = (SheetEntry)selectedObject;
			if (useSheetIndex)
			{
				return NumberStringCache.getNumberString(entry.sheetIndex);
			}
			else
			{
				if (entry.sheetName.indexOf(' ') > -1 || entry.sheetName.indexOf('-') > -1)
				{
					return "\"" + entry.sheetName + "\"";
				}
				return entry.sheetName;
			}
		}
		if (selectedObject instanceof WbFile)
		{
			WbFile file = (WbFile)selectedObject;
			saveLastDirectory(file);
			return file.getFullPath();
		}
		return null;
	}

	private List getChangeSets(SqlCommand wbRunLb, ArgumentParser cmdLine)
	{
		cmdLine.parse(this.sql);
		WbFile file = wbRunLb.evaluateFileArgument(cmdLine.getValue(WbRunLB.ARG_FILE));
		String encoding = cmdLine.getValue(CommonArgs.ARG_ENCODING, "UTF-8");
		try
		{
			MessageBuffer messages = new MessageBuffer(1);
			LiquibaseParser parser = new LiquibaseParser(file, encoding, messages, ParserType.Standard);
			List<ChangeSetIdentifier> changeSets = parser.getChangeSets();
			List result = new ArrayList();
			for (final ChangeSetIdentifier set : changeSets)
			{
				TooltipElement element = new TooltipElement()
				{
					@Override
					public String getTooltip()
					{
						return set.getComment();
					}
					@Override
					public String toString()
					{
						return set.toString();
					}
				};
				result.add(element);
			}
			return result;
		}
		catch (Throwable th)
		{
			return null;
		}
	}

	private List getSheetnames(SqlCommand wbImport, ArgumentParser cmdLine, boolean displayIndex)
	{
		cmdLine.parse(this.sql);
		String fname = cmdLine.getValue(WbImport.ARG_FILE);
		if (fname == null)
		{
			fname = cmdLine.getValue(WbExport.ARG_OUTPUT_FILENAME);
		}
		fname = VariablePool.getInstance().replaceAllParameters(fname);
		WbFile input = wbImport.evaluateFileArgument(fname);

		List result = new ArrayList();
		if (input == null) return result;

		SpreadsheetReader reader = SpreadsheetReader.Factory.createReader(input, -1, null);
		if (reader == null) return result;

		try
		{
			List<String> sheets = reader.getSheets();
			for (int index = 0; index < sheets.size(); index++ )
			{
				String display = null;
				String name = sheets.get(index);
				if (displayIndex)
				{
					display = NumberStringCache.getNumberString(index + 1) + " - " + name;
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
			LogMgr.logError("WbCommandAnalyzer.getSheetnames()", "Could not read spreadsheet: " + input.getFullPath(), e);
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
		implements TooltipElement
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
		public String getTooltip()
		{
			return sheetName + " - " + NumberStringCache.getNumberString(sheetIndex);
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
