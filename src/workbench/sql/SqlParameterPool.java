/*
 * SqlParameterPool.java
 *
 * Created on August 17, 2004, 8:47 PM
 */

package workbench.sql;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import workbench.db.WbConnection;
import workbench.interfaces.JobErrorHandler;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.storage.RowData;
import workbench.util.StringUtil;

/**
 *
 * @author  thomas
 */
public class SqlParameterPool
{
	private HashMap data = new HashMap(); 
	private static final SqlParameterPool POOL = new SqlParameterPool();
	private String prefix;
	private String suffix;
	private int prefixLen = 0;
	private int suffixLen = 0;
	
	public static SqlParameterPool getInstance()
	{
		return POOL;
	}
	
	private SqlParameterPool()
	{
		this.prefix = Settings.getInstance().getSqlParameterPrefix();
		this.suffix = Settings.getInstance().getSqlParameterSuffix();
		this.initFromSystemProperties();
	}
	
	private void initFromSystemProperties()
	{
		Iterator itr = System.getProperties().entrySet().iterator();
		while (itr.hasNext())
		{
			Map.Entry entry = (Map.Entry)itr.next();
			String key = (String)entry.getKey();
			if (key.startsWith("wbp."))
			{
				String varName = key.substring(4);
				String value = (String)entry.getValue();
				if (LogMgr.isDebugEnabled()) 	LogMgr.logDebug("SqlParameterPool", "Adding parameter=[" + varName + "] from system properties with value=[" + value + "]");
				//this.setParameterValue(varName, value);
				this.data.put(varName, value);
			}
		}
	}
	
	public DataStore getVariablesDataStore()
	{
		final String cols[] = {"VARIABLE", "VALUE"};
		final int types[] =   {Types.VARCHAR, Types.VARCHAR};
		final int sizes[] =   {20, 50};
		
		DataStore vardata = new VariableDataStore();
		
		Iterator itr = this.data.entrySet().iterator();
		while (itr.hasNext())
		{
			Map.Entry entry = (Map.Entry)itr.next();
			String key = (String)entry.getKey();
			String value = (String)entry.getValue();
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
	
	public String replaceParameter(String sql)
	{
		if (sql == null) return null;
		if (sql.trim().length() == 0) return "";
		if (sql.indexOf(this.prefix) == -1) return sql;
		Iterator itr = this.data.entrySet().iterator();
		StringBuffer result = new StringBuffer(sql);
		while (itr.hasNext())
		{
			Map.Entry entry = (Map.Entry)itr.next();
			String var = this.buildVarName((String)entry.getKey());
			String value = (String)entry.getValue();
			if (value == null) continue;
			if (LogMgr.isDebugEnabled()) LogMgr.logDebug("SqlParameterPool", "Using value=[" + value + "] for parameter=" + var);
			//result = result.replaceAll(StringUtil.quoteRegexMeta(var), StringUtil.quoteRegexMeta(value));
			
			int pos = result.indexOf(var);
			while (pos > -1)
			{
				result.replace(pos, pos + var.length(), value);
				pos = result.indexOf(var, pos + value.length());
			}
		}
		return result.toString();
	}
	
	public String buildVarName(String varName)
	{
		StringBuffer result = new StringBuffer(varName.length() + this.prefixLen + this.suffixLen);
		result.append(this.prefix);
		result.append(varName);
		result.append(this.suffix);
		return result.toString();
	}
	
	public boolean isVariableDefined(String varName)
	{
		String value = this.getParameterValue(varName);
		return (value != null);
	}
	
	public void removeValue(String varName)
	{
		if (LogMgr.isDebugEnabled()) 	LogMgr.logDebug("SqlParameterPool", "Removing parameter definition [" + varName + "]");
		this.data.remove(varName);
	}
	
	public void setParameterValue(String varName, String value)
	{
		if (LogMgr.isDebugEnabled()) 	LogMgr.logDebug("SqlParameterPool", "Defining parameter=[" + varName + "] with value=[" + value + "]");
		this.data.put(varName, value);
	}		
}

class VariableDataStore 
	extends DataStore
{
	private static final String cols[] = {"VARIABLE", "VALUE"};
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
		throws SQLException
	{
		int rowcount = this.getRowCount();
		this.resetUpdateRowCounters();
		
		SqlParameterPool pool = SqlParameterPool.getInstance();
		for (int i=0; i < rowcount; i++)
		{
			String key = this.getValueAsString(i, 0);
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
