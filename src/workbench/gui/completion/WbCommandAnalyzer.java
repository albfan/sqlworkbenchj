/*
 * WbCommandAnalyzer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.completion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.sql.CommandMapper;
import workbench.sql.SqlCommand;
import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.CaseInsensitiveComparator;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * @author support@sql-workbench.net
 */
public class WbCommandAnalyzer
	extends BaseAnalyzer
{
	// True if the the parameters are put into the 
	// elements list. This is used by the CompletionPopup
	// to check if the selected value should be enhanced with - and =
	private boolean isParameter;
	
	public WbCommandAnalyzer(WbConnection conn, String statement, int cursorPos)
	{	
		super(conn, statement, cursorPos);
	}
	
	public boolean isWbParam()
	{
		return this.isParameter;
	}
	
	public char quoteCharForValue(String value)
	{
		if (this.isParameter) return 0;
		if (value.indexOf('-') > -1 || value.indexOf(' ') > -1)
		{
			if (value.indexOf('\'') > -1) return '"';
			else return '\'';
		}
		return 0;
	}
	
	@SuppressWarnings("unchecked")
	public void checkContext()
	{
		CommandMapper mapper = new CommandMapper();
		SqlCommand cmd = mapper.getCommandToUse(this.sql);
		ArgumentParser p = cmd.getArgumentParser();
		if (p == null)
		{
			this.context = NO_CONTEXT;
			this.elements = null;
			return;
		}
		
		this.context = CONTEXT_WB_PARAMS;
		
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
				this.schemaForTableList = this.dbConnection.getCurrentSchema();
			}
			else if (type == ArgumentType.ListArgument)
			{
				this.elements = new ArrayList(p.getAllowedValues(parameter));
			}
			else if (type == ArgumentType.ProfileArgument)
			{
				List<ConnectionProfile> profiles = ConnectionMgr.getInstance().getProfiles();
				this.elements = new ArrayList(profiles.size());
				for (ConnectionProfile profile : profiles)
				{
					this.elements.add(profile.getKey().toString());
				}
				Collections.sort(this.elements, new CaseInsensitiveComparator());
			}
			else
			{
				this.context = NO_CONTEXT;
				this.elements = null;
			}
		}
		else
		{
			this.elements = p.getRegisteredArguments();
			String params = SqlUtil.stripVerb(this.sql);
			p.parse(params);
			List argsPresent = p.getArgumentsOnCommandLine();
			this.elements.removeAll(argsPresent);
			Collections.sort(this.elements, new CaseInsensitiveComparator());
			isParameter = p.needsSwitch();
		}
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
