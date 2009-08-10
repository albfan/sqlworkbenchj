/*
 * TriggerDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
import workbench.util.SqlUtil;

/**
 * The definition of a trigger in the database.
 * 
 * @author Thomas Kellerer
 */
public class TriggerDefinition
	implements DbObject
{
	private String schema;
	private String catalog;
	private String triggerName;
	private String comment;
	private String type;
	private String event;
//	private TableIdentifier table;

	public TriggerDefinition(String cat, String schem, String name)
	{
		schema = schem;
		catalog = cat;
		triggerName = name;
	}

	public String getComment()
	{
		return comment;
	}

	public void setComment(String c)
	{
		comment = c;
	}

	public void setTriggerEvent(String evt)
	{
		event = evt;
	}

	public String getTriggerEvent()
	{
		return event;
	}

	public void setTriggerType(String typ)
	{
		type = typ;
	}

	public String getTriggerType()
	{
		return type;
	}

//	public void setRelatedTable(TableIdentifier tbl)
//	{
//		table = tbl;
//	}
//
//	public TableIdentifier getRelatedTable()
//	{
//		return table;
//	}

	public CharSequence getSource(WbConnection con)
		throws SQLException
	{
		if (con == null) return null;
		TriggerReader reader = new TriggerReader(con);
		return reader.getTriggerSource(catalog, schema, triggerName);
	}

	public String getSchema()
	{
		return schema;
	}

	public String getCatalog()
	{
		return catalog;
	}

	public String getObjectNameForDrop(WbConnection con)
	{
		return getObjectName(con);
	}

	public String getObjectName(WbConnection conn)
	{
		return conn.getMetadata().quoteObjectname(this.triggerName);
	}

	public String getFullyQualifiedName(WbConnection conn)
	{
		return getObjectExpression(conn);
	}
	
	public String getObjectExpression(WbConnection conn)
	{
		return SqlUtil.buildExpression(conn, catalog, schema, triggerName);
	}

	public String getObjectName()
	{
		return triggerName;
	}

	public String getObjectType()
	{
		return "TRIGGER";
	}

	public String toString()
	{
		return triggerName;
	}

}
