/*
 * ArgumentParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import workbench.log.LogMgr;

/**
 *
 * @author  support@sql-workbench.net
 */
public class ArgumentParser
{
	private static final String ARG_PRESENT = "$WB$__ARG_PRESENT__$WB$";
	private Map<String, String> arguments;
	private Map<String, ArgumentType> argTypes;
	private List<String> unknownParameters = new ArrayList<String>();
	private Map<String, List<String>> allowedValues;
	private int argCount = 0;
	private boolean needSwitch = true;

	public ArgumentParser()
	{
		Comparator<String> c = new CaseInsensitiveComparator();
		arguments = new TreeMap<String, String>(c);
		argTypes = new TreeMap<String, ArgumentType>(c);
		allowedValues = new TreeMap<String, List<String>>(c);
		
	}
	
	public ArgumentParser(boolean parameterSwitchNeeded)
	{
		this();
		this.needSwitch = parameterSwitchNeeded;
	}

	public boolean needsSwitch()
	{
		return needSwitch;
	}
	
	public List<String> getAllowedValues(String key)
	{
		return allowedValues.get(key);
	}
	
	public void addArgument(String key, List<String> values)
	{
		addArgument(key, ArgumentType.ListArgument);
		allowedValues.put(key, values);
	}
	
	public void addArgument(String key)
	{
		addArgument(key, ArgumentType.StringArgument);
	}
	
	public void addArgument(String key, ArgumentType type)
	{
		if (key == null) throw new NullPointerException("Key may not be null");
		
		this.arguments.put(key, null);
		this.argTypes.put(key, type);
	}

	public void parse(String args[])
	{
		this.reset();
		StringBuilder line = new StringBuilder(200);
		for (int i=0; i<args.length; i++)
		{
			line.append(args[i]);
			line.append(' ');
		}
		this.parse(line.toString());
	}

	public void parse(String aCmdLine)
	{
		this.reset();

		try
		{
			WbStringTokenizer tok = new WbStringTokenizer('-', "\"'", false);
			tok.setDelimiterNeedsWhitspace(true);
			tok.setSourceString(aCmdLine.trim());

			while (tok.hasMoreTokens())
			{
				String word = tok.nextToken();
				if (word == null || word.length() == 0) continue;
				String arg = null;
				String value = null;
				int pos = word.indexOf('=');
				if (pos > -1)
				{
					arg = word.substring(0, pos).trim().toLowerCase();
					value = word.substring(pos + 1).trim();
				}
				else
				{
					arg = word.trim().toLowerCase();
				}

				if (value == null)
				{
					value = ARG_PRESENT;
				}

				if (arguments.containsKey(arg))
				{
					arguments.put(arg, value);
					this.argCount ++;
				}
				else
				{
					this.unknownParameters.add(arg);
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("ArgumentParser.parse()", "Error when parsing input line: " + aCmdLine, e);
		}
	}

	public List<String> getArgumentsOnCommandLine()
	{
		ArrayList<String> result = new ArrayList<String>(this.arguments.size());
		for (Map.Entry<String, String> entry : arguments.entrySet())
		{
			if (entry.getValue() != null)
			{
				result.add(entry.getKey());
			}
		}
		return result;
	}
	
	/**
	 * Returns the list of known arguments for this ArgumentParser
	 * @return the registered argument types
	 */
	public List<String> getRegisteredArguments()
	{
		Iterator<Map.Entry<String, ArgumentType>> itr = this.argTypes.entrySet().iterator();

		List<String> result = new ArrayList<String>(this.argTypes.size());
		while (itr.hasNext())
		{
			Map.Entry<String, ArgumentType> entry = itr.next();
			if (entry.getValue() != ArgumentType.Deprecated)
			{
				result.add(entry.getKey());
			}
		}
		return result;
	}
	
	/**
	 * Returns the type of an argument
	 */
	public ArgumentType getArgumentType(String arg)
	{
		return this.argTypes.get(arg);
	}
	
	public boolean hasArguments()
	{
		return this.argCount > 0;
	}
	
	public int getArgumentCount()
	{
		return this.argCount;
	}

	public boolean hasUnknownArguments()
	{
		return this.unknownParameters.size() > 0;
	}

	/**
	 * Return a list of unknown arguments. 
	 * Each argument passed in the original command line
	 * that has not been registered using addArgument()
	 * will be listed in the result. For each argument 
	 * in this list, isRegistered() would return false.
	 * 
	 * @return a comma separated string with unknown arguments
	 */
	public String getUnknownArguments()
	{
		StringBuilder msg = new StringBuilder();
		
		if (unknownParameters.size() > 0)
		{	
			for (int i=0; i < unknownParameters.size(); i++)
			{
				if (i > 0) msg.append(' ');
				msg.append('-');
				msg.append(unknownParameters.get(i));
			}
		}
		return msg.toString();
	}
	
	/**
	 * Check if the given argument is a valid argument for the current commandline
	 * @return true if arg was registered with addArgument()
	 */
	public boolean isRegistered(String arg)
	{
		return this.arguments.containsKey(arg);
	}
	
	/**
	 * Check if the given argument was passed on the commandline. 
	 * This does not check if a value has been supplied with the 
	 * argument. This can be used for argument-less parameters
	 * e.g. -showEncodings
	 * 
	 * @return true if the given argument was part of the commandline
	 */
	public boolean isArgPresent(String arg)
	{
		if (arg == null) return false;
		// Even arguments without a value will have something 
		// in the map (the ARG_PRESENT marker object), otherwise
		// they could not be distinguished from arguments that
		// are merely registered.
		Object value = this.arguments.get(arg);
		return (value != null);
	}
	
	private void reset()
	{
		Iterator<String> keys = this.arguments.keySet().iterator();
		while (keys.hasNext())
		{
			String key = keys.next();
			this.arguments.put(key, null);
		}
		this.argCount = 0;
		this.unknownParameters.clear();
	}
	
	/**
	 * Return a parameter as a boolean.
	 * @return the value as passed on the command line
	 *         false if no value was specified
	 * 
	 * @see #getBoolean(String, boolean)
	 */
	public boolean getBoolean(String key)
	{
		return getBoolean(key, false);
	}
	
	/**
	 * Return a parameter value as a boolean. 
	 * If no value was specified the given default value will be returned
	 * 
	 * @param key the parameter key
	 * @param defaultValue the default to be returned if the parameter is not present
	 * 
	 * @return the value as passed on the commandline 
	 *         the defaultValue if the parameter was not supplied by the user
	 * 
	 * @see #getValue(String)
	 * @see #getBoolean(String)
	 * @see StringUtil#stringToBool(String)
	 */
	public boolean getBoolean(String key,boolean defaultValue)
	{
		String value = this.getValue(key);
		if (value == null || value.trim().length() == 0) return defaultValue;
		return StringUtil.stringToBool(value);
	}

	/**
	 * Return the parameter for the give argument. 
	 * 
	 * If no value was specified or the parameter was not 
	 * passed on the commandline null will be returned.
	 * Any leading or trailing quotes will be removed from the argument
	 * before it is returned.
	 * 
	 * @param key the parameter to retrieve
	 * @return the value as provided by the user or null if no value specified
	 * 
	 * @see StringUtil#trimQuotes(String)
	 */
	public String getValue(String key)
	{
		String value = this.arguments.get(key);
		if (value == ARG_PRESENT) return null;
		return value;
	}
	
	public int getIntValue(String key, int def)
	{
		return StringUtil.getIntValue(this.getValue(key),def);
	}
	
}
