/*
 * VariablePool.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.interfaces.JobErrorHandler;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
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
{
	public static final String PROP_PREFIX = "wbp.";
	private final Map<String, String> data = new HashMap<String, String>();
	private String prefix;
	private String suffix;
	private int prefixLen;
	private int suffixLen;
	private Pattern validNamePattern = Pattern.compile("[\\w]*");;
	private Pattern promptPattern;

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
		this.prefix = Settings.getInstance().getSqlParameterPrefix();
		this.suffix = Settings.getInstance().getSqlParameterSuffix();

		if (this.suffix == null) this.suffix = StringUtil.EMPTY_STRING;

		String expr = StringUtil.quoteRegexMeta(prefix) + "[\\?&][\\w]+" + StringUtil.quoteRegexMeta(suffix);
		this.promptPattern = Pattern.compile(expr);
		this.initFromProperties(System.getProperties());
	}

	void initFromProperties(Properties props)
	{
		synchronized (this.data)
		{
			this.data.clear();
			Iterator itr = props.entrySet().iterator();
			while (itr.hasNext())
			{
				Entry entry = (Entry)itr.next();
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
	 * Used to reset the pool during testing
	 */
	void clear()
	{
		synchronized (this.data)
		{
			this.data.clear();
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
		return getVariablesDataStore(toPrompt);
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
		Set<String> variables = new TreeSet<String>();
		synchronized (this.data)
		{
			while (m.find())
			{
				int start = m.start() + this.prefix.length();
				int end = m.end() - this.suffix.length();
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
			return this.getVariablesDataStore(data.keySet());
		}
	}

	public DataStore getVariablesDataStore(Set<String> varNames)
	{
		DataStore vardata = new VariableDataStore();

		synchronized (this.data)
		{
			for (String key : varNames)
			{
				if (!this.data.containsKey(key)) continue;
				String value = this.data.get(key);
				int row = vardata.addRow();
				vardata.setValue(row, 0, key);
				vardata.setValue(row, 1, value);
			}
		}
		vardata.sortByColumn(0, true);
		vardata.resetStatus();
		return vardata;
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
		if (sql.indexOf(this.prefix) == -1) return sql;
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

	/**
	 * Replaces the variable defined through pattern with the replacement string
	 * inside the string original.
	 * String.replaceAll() cannot be used, because it parses escape sequences
	 */
	private void replaceVarValue(StringBuilder original, String pattern, String replacement)
	{
		//StringBuilder result = new StringBuilder(original);
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(original);
		while (m != null && m.find())
		{
			int start = m.start();
			int end = m.end();
			original.replace(start, end, replacement);
			m = p.matcher(original.toString());
		}
		//return result.toString();
	}

	public String buildVarName(String varName, boolean forPrompt)
	{
		StringBuilder result = new StringBuilder(varName.length() + this.prefixLen + this.suffixLen + 1);
		result.append(this.prefix);
		if (forPrompt) result.append('?');
		result.append(varName);
		result.append(this.suffix);
		return result.toString();
	}

	public String buildVarNamePattern(String varName, boolean forPrompt)
	{
		StringBuilder result = new StringBuilder(varName.length() + this.prefixLen + this.suffixLen + 1);

		result.append(StringUtil.quoteRegexMeta(prefix));
		if (forPrompt)
		{
			result.append("[\\?\\&]{1}");
		}
		else
		{
			result.append("[\\?\\&]?");
		}
		result.append(varName);
		result.append(StringUtil.quoteRegexMeta(suffix));
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
	 *	If the parameter starts with the # character
	 *  assumed that the parameter contains a list of variable definitions
	 *  enclosed in brackets. e.g.
	 *  -vardef="#var1=value1,var2=value2"
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
		Iterator itr = props.entrySet().iterator();
		while (itr.hasNext())
		{
			Entry entry = (Entry)itr.next();
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

	public VariableDataStore()
	{
		super(cols, types, sizes);
		this.setUpdateTable(TABLE_ID);
	}

	public List<DmlStatement> getUpdateStatements(WbConnection aConn)
	{
		return Collections.emptyList();
	}

	public boolean hasPkColumns() { return true; }

	public boolean isUpdateable() { return true; }
	public boolean hasUpdateableColumns() { return true; }

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
