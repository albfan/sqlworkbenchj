/*
 * ObjectScripter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import workbench.exception.ExceptionUtil;

import workbench.interfaces.ScriptGenerationMonitor;
import workbench.interfaces.Scripter;
import workbench.util.StrBuffer;

/**
 *
 * @author  support@sql-workbench.net
 */
public class ObjectScripter
	implements Scripter
{
	public static final String TYPE_SEQUENCE = "sequence";
	public static final String TYPE_TABLE = "table";
	public static final String TYPE_VIEW = "view";
	public static final String TYPE_SYNONYM = "synonym";
	public static final String TYPE_INSERT = "insert";
	public static final String TYPE_SELECT = "select";

	private Map objectList;
	private DbMetadata meta;
	private StrBuffer script;
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
			this.script = new StrBuffer(this.objectList.size() * 500);
			this.appendObjectType(TYPE_SEQUENCE);
			this.appendObjectType(TYPE_TABLE);
			this.appendObjectType(TYPE_VIEW);
			this.appendObjectType(TYPE_SYNONYM);
			this.appendObjectType(TYPE_INSERT);
			this.appendObjectType(TYPE_SELECT);
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
			TableIdentifier tbl = new TableIdentifier(object);
			String source = null;
			
			if (!type.equals(typeFilter)) continue;
			
			if (this.progressMonitor != null)
			{
				this.progressMonitor.setCurrentObject(object);
			}
			try
			{
				if (TYPE_TABLE.equals(type))
				{
					source = meta.getTableSource(null, tbl.getSchema(), tbl.getTable(), true);
				}
				else if (TYPE_VIEW.equals(type))
				{
					source = meta.getExtendedViewSource(null, tbl.getSchema(), tbl.getTable(), false);
				}
				else if (TYPE_SYNONYM.equals(type))
				{
					source = meta.getSynonymSource(tbl.getSchema(), tbl.getTable());
				}
				else if (TYPE_SEQUENCE.equals(type))
				{
					source = this.meta.getSequenceSource(object);
				}
				else if (TYPE_INSERT.equals(type))
				{
					source = this.meta.getEmptyInsert(null, null, object);
				}
				else if (TYPE_SELECT.equals(type))
				{
					source = this.meta.getDefaultSelect(null, null, object);
				}
			}
			catch (Exception e)
			{
				this.script.append("\nError creating script for " + object + " " + ExceptionUtil.getDisplay(e));
			}

			if (source != null && source.length() > 0)
			{
				boolean useSeparator = !type.equalsIgnoreCase("insert") && !type.equalsIgnoreCase("select");
				if (useSeparator) this.script.append("-- BEGIN " + type + " " + object + "\n");
				this.script.append(source);
				if (useSeparator) this.script.append("-- END " + type + " " + object + "\n");
				this.script.append("\n");
			}
		}
	}

}
