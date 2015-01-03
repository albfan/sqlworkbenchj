/*
 * VariablePool.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.sql;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.interfaces.JobErrorHandler;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.DataStore;
import workbench.storage.DmlStatement;
import workbench.storage.RowData;

import workbench.util.StringUtil;
import workbench.util.WbProperties;



/**
 * A class to store workbench specific variables.
 * This is a singleton which stores the variables inside a Map.
 * When the Pool is created it looks for any variable definition
 * passed through the system properties.
 * Any system property that starts with wbp. is used to define a variable.
 * The name of the variable is the part after the <tt>wbp.</tt> prefix.
 *
 * @see workbench.sql.wbcommands.WbDefineVar
 *
 * @author  Thomas Kellerer
 */
public class VariablePool
	implements PropertyChangeListener
{
	public static final String PROP_PREFIX = "wbp.";
	private final Map<String, String> data = new LinkedHashMap<>();
	private final Map<String, List<String>> lookups = new HashMap<>();

	private final Object lock = new Object();
	private String prefix;
	private String suffix;

	private final Pattern validNamePattern = Pattern.compile("[\\w\\.]*");;
	private Pattern promptPattern;
	private Pattern variablePattern;

	public static VariablePool getInstance()
	{
		return InstanceHolder.INSTANCE;
	}

	protected static class InstanceHolder
	{
		protected static final VariablePool INSTANCE = new VariablePool();
	}

	private VariablePool()
	{
		initPromptPattern();
		initFromProperties(System.getProperties());
		Settings.getInstance().addPropertyChangeListener(this, Settings.PROPERTY_VAR_PREFIX, Settings.PROPERTY_VAR_SUFFIX);
	}

	private void initPromptPattern()
	{
		// The promptPattern is cached as this is evaluated each time a SQL is executed
		// rebuild the pattern each time would slow down execution of large SQL scripts too much.
		synchronized (lock)
		{
			String pre = getPrefix();
			String sfx = getSuffix();
			String expr = StringUtil.quoteRegexMeta(pre) + "[\\?&][\\w\\.]+" + StringUtil.quoteRegexMeta(sfx);
			this.promptPattern = Pattern.compile(expr);

			expr = StringUtil.quoteRegexMeta(pre) + "[\\?&]{0,1}[\\w\\.]+" + StringUtil.quoteRegexMeta(sfx);
			variablePattern = Pattern.compile(expr);
		}
	}

	private String getPrefix()
	{
		synchronized (lock)
		{
			if (prefix == null)
			{
				this.prefix = Settings.getInstance().getSqlParameterPrefix();
			}
			return prefix;
		}
	}

	private String getSuffix()
	{
		synchronized (lock)
		{
			if (suffix == null)
			{
				this.suffix = Settings.getInstance().getSqlParameterSuffix();
				if (StringUtil.isEmptyString(this.suffix)) this.suffix = StringUtil.EMPTY_STRING;
			}
			return suffix;
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		synchronized (lock)
		{
			this.prefix = null;
			this.suffix = null;
			initPromptPattern();
		}
		LogMgr.logDebug("VariablePool.propertyChange()", "Update prompter patter because " + evt.getPropertyName() + " changed.");
	}

	/**
	 * For testing purposes only.
	 */
	void reset()
	{
		clear();
		synchronized (lock)
		{
			this.prefix = null;
			this.suffix = null;
			initPromptPattern();
		}
	}

	/**
	 * Set new values for the prefix and the suffix.
	 *
	 * @param newPrefix  the new prefix, if <tt>null</tt> the built-in default is used
	 * @param newSuffix  the new suffix, if <tt>null</tt> the built-in default is used
	 */
	public void setPrefixSuffix(String newPrefix, String newSuffix)
	{
		synchronized (lock)
		{
			this.prefix = newPrefix;
			this.suffix = newSuffix;
			initPromptPattern();
		}
	}

	final void initFromProperties(Properties props)
	{
		synchronized (this.data)
		{
			this.data.clear();
			for (Map.Entry<Object, Object> entry : props.entrySet())
			{
				String key = (String)entry.getKey();
				if (key.startsWith(PROP_PREFIX))
				{
					String varName = key.substring(PROP_PREFIX.length());
					String value = (String)entry.getValue();
					try
					{
						this.setParameterValue(varName, value);
					}
					catch (IllegalArgumentException e)
					{
						LogMgr.logError("SqlParameterPool.init()", "Error setting variable", e);
					}
				}
			}
		}
	}

	/**
	 * Removes all variables from the pool.
	 *
	 */
	public void clear()
	{
		synchronized (this.data)
		{
			this.data.clear();
			this.lookups.clear();
		}
	}

	/**
	 * Returns a set of prompt variables defined in the
	 * SQL string. If a variable is not yet defined it will
	 * be created in the internal pool with an empty value.
	 * and returned in the result set.
	 *
	 * @return a Set containing variable names (String objects)
	 */
	public Set<String> getVariablesNeedingPrompt(String sql)
	{
		return this.getPromptVariables(sql, false);
	}

	public DataStore getParametersToBePrompted(String sql)
	{
		Set<String> toPrompt = getVariablesNeedingPrompt(sql);
		if (toPrompt.isEmpty()) return null;
		return getVariablesDataStore(toPrompt, Settings.getInstance().getSortPromptVariables());
	}

	public boolean hasPrompt(String sql)
	{
		if (sql == null) return false;
		Matcher m = this.promptPattern.matcher(sql);
		if (m == null) return false;
		return m.find();
	}

	private Set<String> getPromptVariables(String sql, boolean includeConditional)
	{
		if (sql == null) return Collections.emptySet();
		Matcher m = this.promptPattern.matcher(sql);
		if (m == null) return Collections.emptySet();
		Set<String> variables = new TreeSet<>();
		synchronized (this.data)
		{
			while (m.find())
			{
				int start = m.start() + this.getPrefix().length();
				int end = m.end() - this.getSuffix().length();
				char type = sql.charAt(start);
				String var = sql.substring(start + 1, end);
				if (!includeConditional)
				{
					if ('&' == type)
					{
						String value = this.getParameterValue(var);
						if (value != null && value.length() > 0) continue;
					}
				}
				variables.add(var);
				if (!this.data.containsKey(var))
				{
					this.data.put(var, "");
				}
			}
		}
		return Collections.unmodifiableSet(variables);
	}

	public DataStore getVariablesDataStore()
	{
		synchronized (this.data)
		{
			return this.getVariablesDataStore(data.keySet(), true);
		}
	}

	public DataStore getVariablesDataStore(Set<String> varNames, boolean doSort)
	{
		DataStore vardata = new VariableDataStore();

		synchronized (this.data)
		{
			for (String key : data.keySet())
			{
				if (varNames.contains(key))
				{
					String value = this.data.get(key);
					int row = vardata.addRow();
					vardata.setValue(row, 0, key);
					vardata.setValue(row, 1, value);
				}
			}
		}

		if (doSort)
		{
			vardata.sortByColumn(0, true);
		}
		vardata.resetStatus();
		return vardata;
	}

	public boolean isDefined(String varName)
	{
		return StringUtil.isNonBlank(getParameterValue(varName));
	}

	public String getParameterValue(String varName)
	{
		if (varName == null) return null;
		synchronized (this.data)
		{
			return data.get(varName);
		}
	}

	/**
	 *	Returns the number of parameters currently defined.
	 */
	public int getParameterCount()
	{
		synchronized (this.data)
		{
			return data.size();
		}
	}

	public String replaceAllParameters(String sql)
	{
		if (data.isEmpty()) return sql;
		synchronized (this.data)
		{
			return this.replaceParameters(this.data.keySet(), sql, false);
		}
	}

	private String replaceParameters(Set<String> varNames, String sql, boolean forPrompt)
	{
		if (sql == null) return null;
		if (StringUtil.isBlank(sql)) return StringUtil.EMPTY_STRING;
		if (sql.indexOf(this.getPrefix()) == -1) return sql;
		StringBuilder newSql = new StringBuilder(sql);
		for (String name : varNames)
		{
			String var = this.buildVarNamePattern(name, forPrompt);
			String value = this.data.get(name);
			if (value == null) continue;
			replaceVarValue(newSql, var, value);
		}
		return newSql.toString();
	}

	public String removeVariables(String data)
	{
		if (data == null) return data;
		Matcher m = variablePattern.matcher(data);
		if (m == null) return data;
		return m.replaceAll("");
	}

	/**
	 * Replaces the variable defined through pattern with the replacement string
	 * inside the string original.
	 * String.replaceAll() cannot be used, because it parses escape sequences
	 */
	private void replaceVarValue(StringBuilder original, String pattern, String replacement)
	{
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(original);
		while (m != null && m.find())
		{
			int start = m.start();
			int end = m.end();
			original.replace(start, end, replacement);
			m = p.matcher(original.toString());
		}
	}

	public String buildVarName(String varName, boolean forPrompt)
	{
		StringBuilder result = new StringBuilder(varName.length() + 5);
		result.append(this.getPrefix());
		if (forPrompt) result.append('?');
		result.append(varName);
		String sufx = getSuffix();
		if (StringUtil.isNonEmpty(sufx))
		{
			result.append(sufx);
		}
		return result.toString();
	}

	public String buildVarNamePattern(String varName, boolean forPrompt)
	{
		StringBuilder result = new StringBuilder(varName.length() + 5);
		result.append(StringUtil.quoteRegexMeta(getPrefix()));
		if (forPrompt)
		{
			result.append("[\\?\\&]{1}");
		}
		else
		{
			result.append("[\\?\\&]?");
		}
		result.append(varName);
		result.append(StringUtil.quoteRegexMeta(getSuffix()));
		return result.toString();
	}

	public boolean removeValue(String varName)
	{
		if (varName == null) return false;
		synchronized (this.data)
		{
			Object old = this.data.remove(varName);
			return (old != null);
		}
	}

	public List<String> getLookupValues(String varName)
	{
		if (varName == null) return null;

		synchronized (this.data)
		{
			return this.lookups.get(varName);
		}
	}

	public void setLookupValues(String varName, List<String> values)
	{
		synchronized (this.data)
		{
			this.lookups.put(varName, values);
			if (!this.data.containsKey(varName))
			{
				this.data.put(varName, "");
			}
		}
	}

	public void setParameterValue(String varName, String value)
		throws IllegalArgumentException
	{
		if (this.isValidVariableName(varName))
		{
			synchronized (this.data)
			{
				this.data.put(varName, value);
			}
		}
		else
		{
			String msg = ResourceMgr.getString("ErrIllegalVariableName");
			msg = StringUtil.replace(msg, "%varname%", varName);
			msg = msg + "\n" + ResourceMgr.getString("ErrVarDefWrongName");
			throw new IllegalArgumentException(msg);
		}
	}

	public boolean isValidVariableName(String varName)
	{
		return this.validNamePattern.matcher(varName).matches();
	}

	/**
	 *	Initialize the variables from a commandline parameter.
	 *
	 *	If the parameter starts with the # character
	 *  assumed that the parameter contains a list of variable definitions
	 *  enclosed in brackets. e.g. <tt>-vardef="#var1=value1,var2=value2"</tt>
	 *  The list needs to be quoted on the commandline!
	 */
	public void readDefinition(String parameter)
		throws Exception
	{
		if (StringUtil.isBlank(parameter)) return;
		if (parameter.charAt(0) == '#')
		{
			readNameList(parameter.substring(1));
		}
		else
		{
			readFromFile(parameter, null);
		}
	}

	private void readNameList(String list)
	{
		List<String> defs = StringUtil.stringToList(list, ",");
		for (String line : defs)
		{
			int pos = line.indexOf('=');
			if (pos == -1) return;
			String key = line.substring(0, pos);
			String value = line.substring(pos + 1);
			try
			{
				this.setParameterValue(key, value);
			}
			catch (IllegalArgumentException e)
			{
				LogMgr.logWarning("SqlParameterPool.readNameList()", "Ignoring definition: "+ line);
			}
		}
	}

	/**
	 * Read the variable defintions from an external file.
	 * The file has to be a regular Java properties file, but does not support
	 * line continuation.
	 */
	public void readFromFile(String filename, String encoding)
		throws IOException
	{
		WbProperties props = new WbProperties(this);
		File f = new File(filename);
		if (!f.exists()) return;

		props.loadTextFile(filename, encoding);
		for (Entry entry : props.entrySet())
		{
			Object key = entry.getKey();
			Object value = entry.getValue();
			if (key != null && value != null)
			{
				this.setParameterValue((String)key, (String)value);
			}
		}
		String msg = ResourceMgr.getString("MsgVarDefFileLoaded");
		msg = StringUtil.replace(msg, "%file%", f.getAbsolutePath());
		LogMgr.logInfo("SqlParameterPool.readFromFile", msg);
	}

}
class VariableDataStore
	extends DataStore
{
	private static final String[] cols = {ResourceMgr.getString("LblVariableName"), ResourceMgr.getString("LblVariableValue") };
	private static final int[] types =   {Types.VARCHAR, Types.VARCHAR};
	private static final int[] sizes =   {20, 50};
	private static final TableIdentifier TABLE_ID = new TableIdentifier("WB$VARIABLE_DEFINITION");

	VariableDataStore()
	{
		super(cols, types, sizes);
		this.setUpdateTable(TABLE_ID);
	}

	@Override
	public List<DmlStatement> getUpdateStatements(WbConnection aConn)
	{
		return Collections.emptyList();
	}

	@Override
	public boolean hasPkColumns()
	{
		return true;
	}

	@Override
	public boolean isUpdateable()
	{
		return true;
	}

	@Override
	public boolean hasUpdateableColumns()
	{
		return true;
	}

	@Override
	public boolean checkUpdateTable()
	{
		return true;
	}

	@Override
	public boolean checkUpdateTable(WbConnection aConn)
	{
		return true;
	}

	@Override
	public int updateDb(WbConnection aConnection, JobErrorHandler errorHandler)
		throws SQLException, IllegalArgumentException
	{
		int rowcount = this.getRowCount();
		this.resetUpdateRowCounters();

		VariablePool pool = VariablePool.getInstance();
		for (int i=0; i < rowcount; i++)
		{
			String key = this.getValueAsString(i, 0);
			String oldkey = (String)this.getOriginalValue(i, 0);
			if (oldkey != null && !key.equals(oldkey))
			{
				pool.removeValue(oldkey);
			}
			String value = this.getValueAsString(i, 1);
			// Treat null as an empty value
			pool.setParameterValue(key, value == null ? "" : value);
		}

		RowData row = this.getNextDeletedRow();
		while (row != null)
		{
			String key = (String)row.getValue(0);
			pool.removeValue(key);
			row = this.getNextDeletedRow();
		}
		this.resetStatus();
		return rowcount;
	}

}
