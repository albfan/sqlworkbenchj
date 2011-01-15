/*
 * ArgumentParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import workbench.log.LogMgr;

/**
 *
 * @author  Thomas Kellerer
 */
public class ArgumentParser
{
	private static final String ARG_PRESENT = "$WB$__ARG_PRESENT__$WB$";

	// Maps the argument to the supplied value
	private Map<String, Object> arguments;

	// Maps a registered argument to the argument type
	private Map<String, ArgumentType> argTypes;

	private List<String> unknownParameters = new ArrayList<String>();

	// Stores the allowed values for a parameter
	private Map<String, Collection<ArgumentValue>> allowedValues;
	private int argCount = 0;
	private boolean needSwitch = true;


	public ArgumentParser()
	{
		arguments = new TreeMap<String, Object>(CaseInsensitiveComparator.INSTANCE);
		argTypes = new TreeMap<String, ArgumentType>(CaseInsensitiveComparator.INSTANCE);
		allowedValues = new TreeMap<String, Collection<ArgumentValue>>(CaseInsensitiveComparator.INSTANCE);
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

	public boolean isAllowedValue(String key, String value)
	{
		StringArgumentValue argValue = new StringArgumentValue(value);
		Collection<ArgumentValue> allowed = getAllowedValues(key);
		return allowed.contains(argValue);
	}

	public Collection<ArgumentValue> getAllowedValues(String key)
	{
		return allowedValues.get(key);
	}

	public boolean hasValidValue(String parameter)
	{
		String value = getValue(parameter);
		if (value == null) return true;
		Collection<ArgumentValue> allowed = this.getAllowedValues(parameter);
		if (allowed == null || allowed.isEmpty()) return true;
		return allowed.contains(value);
	}

	public void addArgument(String key, List<String> values)
	{
		addArgument(key, ArgumentType.ListArgument);
		if (values == null) return;
		Collection<ArgumentValue> v = new TreeSet<ArgumentValue>(ArgumentValue.COMPARATOR);
		for (String value : values)
		{
			v.add(new StringArgumentValue(value));
		}
		allowedValues.put(key, v);
	}

	public void addArgumentWithValues(String key, List<? extends ArgumentValue> values)
	{
		addArgument(key, ArgumentType.ListArgument);
		if (values == null) return;
		Collection<ArgumentValue> v = new TreeSet<ArgumentValue>();
		v.addAll(values);
		allowedValues.put(key, v);
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

	public void parseProperties(File propFile)
		throws IOException
	{
		Reader in = EncodingUtil.createReader(propFile, null);
		List<String> lines = FileUtil.getLines(new BufferedReader(in));
		parse(lines);
	}

	public void parse(String[] args)
	{
		reset();
		StringBuilder line = new StringBuilder(200);
		for (int i=0; i<args.length; i++)
		{
			line.append(args[i]);
			line.append(' ');
		}
		parse(line.toString());
	}

	public void parse(String aCmdLine)
	{
		reset();
		WbStringTokenizer tok = new WbStringTokenizer('-', "\"'", true);
		tok.setDelimiterNeedsWhitspace(true);
		tok.setSourceString(aCmdLine.trim());
		List<String> entries = tok.getAllTokens();
		parse(entries);
	}

	protected void parse(List<String> entries)
	{
		try
		{
			for (String word : entries)
			{
				if (word == null || word.length() == 0) continue;
				String arg = word.trim();
				String value = null;
				int pos = word.indexOf('=');
				if (pos > -1)
				{
					arg = word.substring(0, pos).trim();
					value = word.substring(pos + 1).trim();
					char first = value.charAt(0);
					char last = value.charAt(value.length() - 1);
					if ( (first == '"' && last == '"') || (first == '\'' && last == '\''))
					{
						int otherPos = value.indexOf(first, 1);
						if (otherPos == -1 || otherPos == value.length() - 1)
						{
							value = StringUtil.trimQuotes(value);
						}
					}
				}

				if (value == null)
				{
					value = ARG_PRESENT;
				}

				if (arguments.containsKey(arg))
				{
					ArgumentType type = argTypes.get(arg);
					if (type == ArgumentType.Repeatable)
					{
						List<String> list = (List<String>)arguments.get(arg);
						if (list == null)
						{
							list = new ArrayList<String>();
							arguments.put(arg, list);
						}
						List<String> result = StringUtil.stringToList(value, ",", true, true, false);
						list.addAll(result);
					}
					else
					{
						arguments.put(arg, value);
					}
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
			LogMgr.logError("ArgumentParser.parse()", "Error when parsing entries", e);
		}
	}

	public List<String> getArgumentsOnCommandLine()
	{
		ArrayList<String> result = new ArrayList<String>(this.arguments.size());
		for (Map.Entry<String, Object> entry : arguments.entrySet())
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

	/**
	 * Checks if any arguments have been defined.
	 *
	 * @return true if at least one argument was provided
	 */
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

	public void removeArgument(String arg)
	{
		if (arguments.get(arg) != null)
		{
			argCount --;
		}
		this.arguments.remove(arg);
		this.argTypes.remove(arg);
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
		if (StringUtil.isBlank(value)) return defaultValue;
		return StringUtil.stringToBool(value);
	}

	/**
	 * Return the parameter for the give argument.
	 *
	 * If no value was specified or the parameter was not
	 * passed on the commandline null will be returned.
	 * Any leading or trailing quotes will be removed from the argument
	 * before it is returned. To check if the parameter was present
	 * but without a value isArgPresent() should be used.
	 *
	 * @param key the parameter to retrieve
	 * @return the value as provided by the user or null if no value specified
	 *
	 * @see #isArgPresent(java.lang.String)
	 * @see StringUtil#trimQuotes(String)
	 */
	public String getValue(String key)
	{
		if (getArgumentType(key) == ArgumentType.Repeatable)
		{
			List<String> list = getList(key);
			if (list == null) return null;

			if (list.size() == 1)
			{
				return list.get(0);
			}
			throw new IllegalStateException("Cannot return a single string from a List");
		}
		Object value = this.arguments.get(key);
		if (value == ARG_PRESENT) return null;
		return (String)value;
	}

	public List<String> getList(String key)
	{
		if (getArgumentType(key) != ArgumentType.Repeatable)
		{
			String value = getValue(key);
			return CollectionUtil.arrayList(value);
		}
		Object value = this.arguments.get(key);
		if (value == ARG_PRESENT) return null;
		return (List<String>)value;
	}

	public String getValue(String key, String defaultValue)
	{
		String value = getValue(key);
		if (value == null) return defaultValue;
		return value;
	}

	public List<String> getListValue(String key)
	{
		if (getArgumentType(key) == ArgumentType.Repeatable)
		{
			return getList(key);
		}
		String value = this.getValue(key);
		if (value == null) return Collections.emptyList();
		List<String> result = StringUtil.stringToList(value, ",", true, true, false);
		return result;
	}

	public Map<String, String> getMapValue(String key)
	{
		String value = this.getValue(key);
		if (value == null) return Collections.emptyMap();

		List<String> entries = StringUtil.stringToList(value, ",", true, true, false, true);
		Map<String, String> result = new HashMap<String, String>(entries.size());
		for (String entry : entries)
		{
			String[] param = entry.split("=");
			if (param == null || param.length != 2)
			{
				param = entry.split(":");
			}
			if (param != null && param.length == 2)
			{
				result.put(param[0], StringUtil.trimQuotes(param[1]));
			}
		}
		return result;
	}

	public int getIntValue(String key, int def)
	{
		return StringUtil.getIntValue(this.getValue(key),def);
	}

}
