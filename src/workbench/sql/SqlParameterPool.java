/*
 * SqlParameterPool.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
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
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.db.WbConnection;
import workbench.interfaces.JobErrorHandler;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.storage.RowData;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;
import workbench.util.WbProperties;


/**
 *
 * @author  info@sql-workbench.net
 */
public class SqlParameterPool
{
	private HashMap data = new HashMap(); 
	private static final SqlParameterPool POOL = new SqlParameterPool();
	private String prefix;
	private String suffix;
	private int prefixLen = 0;
	private int suffixLen = 0;
	private Pattern validNamePattern = Pattern.compile("[\\w]*");;
	private Pattern promptPattern;
	private Pattern variablePattern;
	
	public static SqlParameterPool getInstance()
	{
		return POOL;
	}
	
	private SqlParameterPool()
	{
		this.prefix = Settings.getInstance().getSqlParameterPrefix();
		this.suffix = Settings.getInstance().getSqlParameterSuffix();
		
		if (this.suffix == null) this.suffix = StringUtil.EMPTY_STRING;
		
		String expr = StringUtil.quoteRegexMeta(prefix) + "[\\?\\&][\\w]*" + StringUtil.quoteRegexMeta(suffix);
		this.promptPattern = Pattern.compile(expr);
		
		expr = StringUtil.quoteRegexMeta(prefix) + "[\\?\\&]?[\\w]*" + StringUtil.quoteRegexMeta(suffix);
		this.variablePattern = Pattern.compile(expr);
		
		this.initFromSystemProperties();
	}
	
	private void initFromSystemProperties()
	{
		Iterator itr = System.getProperties().entrySet().iterator();
		while (itr.hasNext())
		{
			Entry entry = (Entry)itr.next();
			String key = (String)entry.getKey();
			if (key.startsWith("wbp."))
			{
				String varName = key.substring(4);
				String value = (String)entry.getValue();
				try
				{
					if (LogMgr.isDebugEnabled()) LogMgr.logDebug("SqlParameterPool", "Found parameter=[" + varName + "] in system properties with value=[" + value + "]");
					this.setParameterValue(varName, value);
				}
				catch (IllegalArgumentException e)
				{
					LogMgr.logError("SqlParameterPool.init()", "Error setting variable", e);
				}
			}
		}
	}
	
	public boolean hasPrompts(String sql)
	{
		Matcher m = this.promptPattern.matcher(sql);
		return m.find();
	}

	public String replacePrompts(String sql)
	{
		Set vars = this.getPromptVariables(sql, false);
		return this.replaceParameters(vars, sql, true);
	}
	
	public String replacePrompts(Set vars, String sql)
	{
		return this.replaceParameters(vars, sql, true);
	}
	
	/**
	 *	Returns a set of prompt variables defined in the 
	 *	SQL string. If a variable is not yet defined it will
	 *  be created in the internal pool.
	 */
	public Set getVariablesNeedingPrompt(String sql)
	{
		return this.getPromptVariables(sql, false);
	}
	
	private Set getPromptVariables(String sql, boolean includeConditional)
	{
		if (sql == null) return Collections.EMPTY_SET;
		Matcher m = this.promptPattern.matcher(sql);
		if (m == null) return Collections.EMPTY_SET;
		Set variables = new TreeSet();
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
		return Collections.unmodifiableSet(variables);
	}
	
	public Pattern getPromptPattern()
	{
		return this.promptPattern;
	}
	public DataStore getVariablesDataStore()
	{
		return this.getVariablesDataStore(Collections.synchronizedSet(this.data.keySet()));
	}

	/**
	 *	Ensure that all variable names contained in the passed set
	 *	are actually declared. If a variable is not defined, it will 
	 *	be defined with no value
	 */
	public void defineVariables(Set names)
	{
		Iterator itr = names.iterator();
		while (itr.hasNext())
		{
			String var = (String)itr.next();
			if (!this.data.containsKey(var))
			{
				this.data.put(var, "");
			}
		}
	}
	public DataStore getVariablesDataStore(Set varNames)
	{
		final String cols[] = {"VARIABLE", "VALUE"};
		final int types[] =   {Types.VARCHAR, Types.VARCHAR};
		final int sizes[] =   {20, 50};
		
		DataStore vardata = new VariableDataStore();
		
		Iterator itr = varNames.iterator();
		while (itr.hasNext())
		{
			String key = (String)itr.next();
			if (!this.data.containsKey(key)) continue;
			String value = (String)this.data.get(key);
			int row = vardata.addRow();
			vardata.setValue(row, 0, key);
			vardata.setValue(row, 1, value);
		}
		vardata.sortByColumn(0, true);
		vardata.resetStatus();
		return vardata;
	}
	
	public String getParameterValue(String varName)
	{
		return (String)this.data.get(varName);
	}

	public String replaceAllParameters(String sql)
	{
		Set vars = Collections.synchronizedSet(this.data.keySet());
		return this.replaceParameters(vars, sql, false);
	}
	
	private String replaceParameters(Set varNames, String sql, boolean forPrompt)
	{
		if (sql == null) return null;
		if (sql.trim().length() == 0) return StringUtil.EMPTY_STRING;
		if (sql.indexOf(this.prefix) == -1) return sql;
		Iterator itr = varNames.iterator();
		String newSql = sql;
		while (itr.hasNext())
		{
			String name = (String)itr.next();
			String var = this.buildVarNamePattern(name, forPrompt);
			String value = (String)this.data.get(name);
			if (value == null) continue;
			if (LogMgr.isDebugEnabled()) LogMgr.logDebug("SqlParameterPool", "Using value=[" + value + "] for parameter=" + name);
			newSql = replaceVarValue(newSql, var, value);
		}
		return newSql;
	}
	
	/**
	 *	Replaces the variable defined through pattern with the replacement string
	 * inside the string original. 
	 * String.replaceAll() cannot be used, because it parses escape sequences
	 */
	private String replaceVarValue(String original, String pattern, String replacement)
	{
		StringBuffer result = new StringBuffer(original);
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(original);
		while (m.find())
		{
			int start = m.start();
			int end = m.end();
			result.replace(start, end, replacement);
		}
		return result.toString();
	}
		
	public String buildVarName(String varName, boolean forPrompt)
	{
		StrBuffer result = new StrBuffer(varName.length() + this.prefixLen + this.suffixLen + 1);
		result.append(this.prefix);
		if (forPrompt) result.append("?");
		result.append(varName);
		result.append(this.suffix);
		return result.toString();
	}
	
	public String buildVarNamePattern(String varName, boolean forPrompt)
	{
		StrBuffer result = new StrBuffer(varName.length() + this.prefixLen + this.suffixLen + 1);
		
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
	
	public boolean isVariableDefined(String varName)
	{
		String value = this.getParameterValue(varName);
		return (value != null);
	}
	
	public synchronized boolean removeValue(String varName)
	{
		if (LogMgr.isDebugEnabled()) 	LogMgr.logDebug("SqlParameterPool", "Removing parameter definition [" + varName + "]");
		Object old = this.data.remove(varName);
		return (old != null);
	}
	
	public synchronized void setParameterValue(String varName, String value)
		throws IllegalArgumentException
	{
		if (LogMgr.isDebugEnabled()) 	LogMgr.logDebug("SqlParameterPool", "Defining parameter=[" + varName + "] with value=[" + value + "]");
		if (this.isValidVariableName(varName))
		{
			this.data.put(varName, value);
		}
		else
		{
			String msg = ResourceMgr.getString("ErrorIllegalVariableName");
			msg = StringUtil.replace(msg, "%varname%", varName);
			msg = msg + "\n" + ResourceMgr.getString("ErrorVarDefWrongName");
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
		if (parameter == null || parameter.trim().length() == 0) return;
		if (parameter.startsWith("#"))
		{
			readNameList(parameter.substring(1));
		}
		else
		{
			readFromFile(parameter);
		}
	}
	
	public void readNameList(String list)
	{
		List defs = StringUtil.stringToList(list, ",");
		for (int i=0; i < defs.size(); i++)
		{
			String line = (String)defs.get(i);
			int pos = line.indexOf("=");
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
	
	public void readFromFile(String filename)
		throws IOException
	{
		WbProperties props = new WbProperties();
		File f = new File(filename);
		props.loadTextFile(filename);
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
	private static final String cols[] = {ResourceMgr.getString("LabelVariableName"), ResourceMgr.getString("LabelVariableValue") };
	private static final int types[] =   {Types.VARCHAR, Types.VARCHAR};
	private static final int sizes[] =   {20, 50};
	private static final String TABLE_ID = "$__wb-internal-variable-list__$";
	
	public VariableDataStore()
	{
		super(cols, types, sizes);
		this.setUpdateTable(TABLE_ID);
	}
	
	public List getUpdateStatements(WbConnection aConn)
	{
		return Collections.EMPTY_LIST;
	}
	
	public boolean isUpdateable() { return true; }
	public boolean hasUpdateableColumns() { return true; }
	
	public int updateDb(WbConnection aConnection, JobErrorHandler errorHandler)
		throws SQLException, IllegalArgumentException
	{
		int rowcount = this.getRowCount();
		this.resetUpdateRowCounters();
		
		SqlParameterPool pool = SqlParameterPool.getInstance();
		for (int i=0; i < rowcount; i++)
		{
			String key = this.getValueAsString(i, 0);
			String oldkey = (String)this.getOriginalValue(i, 0);
			if (oldkey != null && !key.equals(oldkey))
			{
				pool.removeValue(oldkey);
			}
			String value = this.getValueAsString(i, 1);
			pool.setParameterValue(key, value);
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
