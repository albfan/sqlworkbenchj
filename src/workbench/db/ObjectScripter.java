/*
 * ObjectScripter.java
 *
 * Created on September 4, 2003, 5:26 PM
 */

package workbench.db;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

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
			String source = null;
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
						source = this.getTableScript(object);
					}
					else if ("view".equalsIgnoreCase(type))
					{
						source = this.getViewSource(object);
					}
					else if ("synonym".equalsIgnoreCase(type))
					{
						source = this.getSynonymSource(object);
					}
					else if ("sequence".equalsIgnoreCase(type))
					{
						source = this.meta.getSequenceSource(object);
					}
				}
				catch (Exception e)
				{
					this.script.append("\nError creating DDL for " + object);
				}
				if (source != null && source.length() > 0)
				{
					this.script.append("-- BEGIN " + type + " " + object + "\n");
					this.script.append(source);
					this.script.append("-- END " + type + " " + object + "\n");
					this.script.append("\n");
				}
			}
		}
	}

	private String getSynonymSource(String tableName)
		throws SQLException
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
		throws SQLException
	{
		String table = tableName;
		String owner = null;
		int pos = tableName.indexOf('.');
		if (pos > 0)
		{
			owner = tableName.substring(0, pos);
			table = tableName.substring(pos + 1);
		}
		String source = this.meta.getTableSource(null, owner, table, true);
		return source;
	}

	private String getViewSource(String viewName)
		throws SQLException
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
