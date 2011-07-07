/*
 * TriggerDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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

	public static final String PLACEHOLDER_TRIGGER_NAME = "%trigger_name%";
	public static final String PLACEHOLDER_TRIGGER_SCHEMA = "%trigger_schema%";
	public static final String PLACEHOLDER_TRIGGER_TABLE = "%trigger_table%";

	private String schema;
	private String catalog;
	private String triggerName;
	private String comment;
	private String type;
	private String event;
	private TableIdentifier table;
	private CharSequence source;
	private String status;

	public TriggerDefinition(String cat, String schem, String name)
	{
		schema = schem;
		catalog = cat;
		triggerName = name;
	}

	@Override
	public String getComment()
	{
		return comment;
	}

	@Override
	public void setComment(String c)
	{
		comment = c;
	}

	public String getStatus()
	{
		return status;
	}

	public void setStatus(String statusText)
	{
		this.status = statusText;
	}

	/**
	 * Define the event that makes this trigger fire (e.g. UPDATE, INSERT, DELETE)
	 * @param evt
	 */
	public void setTriggerEvent(String evt)
	{
		event = evt;
	}

	/**
	 * Returns the event that makes this trigger fire (e.g. UPDATE, INSERT, DELETE)
	 */
	public String getTriggerEvent()
	{
		return event;
	}

	/**
	 * Define the type of trigger (BEFORE/AFTER)
	 * @param trgType
	 */
	public void setTriggerType(String trgType)
	{
		type = trgType;
	}

	/**
	 * Return the type of the trigger (BEFORE/AFTER)
	 */
	public String getTriggerType()
	{
		return type;
	}

	public void setRelatedTable(TableIdentifier tbl)
	{
		table = tbl;
	}

	public TableIdentifier getRelatedTable()
	{
		return table;
	}

	@Override
	public String getDropStatement(WbConnection con, boolean cascade)
	{
		String ddl = con.getDbSettings().getDropDDL(getObjectType(), cascade);
		if (ddl == null) return null;

		// getDropDDL can also return a generic DROP statement that only
		// includes the %name% placeholder (because it's not based on a configured
		// property, but is created dynamically)
		ddl = ddl.replace("%name%", triggerName);

		// specialized statements have different placeholders
		ddl = ddl.replace(PLACEHOLDER_TRIGGER_NAME, triggerName);
		ddl = ddl.replace(PLACEHOLDER_TRIGGER_SCHEMA, schema);
		if (table != null)
		{
			ddl = ddl.replace(PLACEHOLDER_TRIGGER_TABLE, table.getTableExpression(con));
		}

		return ddl;
	}

	public void setSource(CharSequence src)
	{
		source = src;
	}

	public CharSequence getSource()
	{
		return source;
	}

	@Override
	public CharSequence getSource(WbConnection con)
		throws SQLException
	{
		return getSource(con, true);
	}

	public CharSequence getSource(WbConnection con, boolean includeDependencies)
		throws SQLException
	{
		if (con == null) return null;
		TriggerReader reader = TriggerReaderFactory.createReader(con);
		return reader.getTriggerSource(catalog, schema, triggerName, table, comment, includeDependencies);
	}

	@Override
	public String getSchema()
	{
		return schema;
	}

	@Override
	public String getCatalog()
	{
		return catalog;
	}

	@Override
	public String getObjectNameForDrop(WbConnection con)
	{
		return getFullyQualifiedName(con);
	}

	@Override
	public String getObjectName(WbConnection conn)
	{
		return conn.getMetadata().quoteObjectname(this.triggerName);
	}

	@Override
	public String getFullyQualifiedName(WbConnection conn)
	{
		return getObjectExpression(null);
	}

	@Override
	public String getObjectExpression(WbConnection conn)
	{
		return SqlUtil.buildExpression(conn, catalog, schema, triggerName);
	}

	@Override
	public String getObjectName()
	{
		return triggerName;
	}

	@Override
	public String getObjectType()
	{
		return "TRIGGER";
	}

	@Override
	public String toString()
	{
		return triggerName;
	}

}
