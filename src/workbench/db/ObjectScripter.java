/*
 * ObjectScripter.java
 *
 * Created on September 4, 2003, 5:26 PM
 */

package workbench.db;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import workbench.exception.WbException;
import workbench.storage.DataStore;

/**
 *
 * @author  workbench@kellerer.org
 */
public class ObjectScripter
{
	public interface ScriptGenerationMonitor
	{
		void currentTable(String aTableName);
	}
	
	private Map objectList;
	private DbMetadata meta;
	private StringBuffer script;
	private ScriptGenerationMonitor progressMonitor;
	
	public ObjectScripter(Map objectList, WbConnection aConnection)
	{
		this.objectList = objectList;
		this.meta = aConnection.getMetadata();
	}
	
	public void setProgressMonitor(ScriptGenerationMonitor aMonitor)
	{
		this.progressMonitor = aMonitor;
	}
	
	public String getScript()
	{
		if (this.script == null) 
		{
			this.script = new StringBuffer(this.objectList.size() * 500);
			this.appendObjectType("sequence");
			this.script.append("\n");
			this.appendObjectType("table");
			this.appendObjectType("view");
			this.appendObjectType("synonym");
		}
		return this.script.toString();
	}
	
	private void appendObjectType(String typeFilter)
	{
		Iterator itr = this.objectList.entrySet().iterator();
		while (itr.hasNext())
		{
			Map.Entry entry = (Map.Entry)itr.next();
			String object = (String)entry.getKey();
			String type = (String)entry.getValue();
			if (type.equalsIgnoreCase(typeFilter))
			{
				if (this.progressMonitor != null)
				{
					this.progressMonitor.currentTable(object);
				}
				try
				{
					if ("table".equalsIgnoreCase(type))
					{
						this.script.append(this.getTableScript(object));
					}
					else if ("view".equalsIgnoreCase(type))
					{
						this.script.append(this.getViewSource(object));
					}
					else if ("synonym".equalsIgnoreCase(type))
					{
						this.script.append(this.getSynonymSource(object));
					}
					else if ("sequence".equalsIgnoreCase(type))
					{
						String source = this.meta.getSequenceSource(object);
						this.script.append(source);
					}
				}
				catch (Exception e)
				{
					this.script.append("\nError creating script for " + object);
				}
				this.script.append("\n");
			}
		}
	}

	private String getSynonymSource(String tableName)
		throws SQLException, WbException
	{
		String table = tableName;
		String owner = null;
		int pos = tableName.indexOf('.');
		if (pos > 0)
		{
			owner = tableName.substring(0, pos);
			table = tableName.substring(pos + 1);
		}
		return this.meta.getSynonymSource(owner, table);
	}	
	
	private String getTableScript(String tableName)
		throws SQLException, WbException
	{
		String table = tableName;
		String owner = null;
		int pos = tableName.indexOf('.');
		if (pos > 0)
		{
			owner = tableName.substring(0, pos);
			table = tableName.substring(pos + 1);
		}
		String source = this.meta.getTableSource(null, owner, table);
		return source;
	}
	
	private String getViewSource(String viewName)
		throws SQLException, WbException
	{
		String view = viewName;
		String owner = null;
		int pos = viewName.indexOf('.');
		if (pos > 0)
		{
			owner = viewName.substring(0, pos);
			view = viewName.substring(pos + 1);
		}
		return this.meta.getExtendedViewSource(null, owner, view, false);
	}
	
}

