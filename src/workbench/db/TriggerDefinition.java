/*
 * ProcedureDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import workbench.util.SqlUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class TriggerDefinition
	implements DbObject
{
	private String schema;
	private String catalog;
	private String triggerName;
	
	private CharSequence source;

	public TriggerDefinition(String cat, String schem, String name)
	{
		schema = schem;
		catalog = cat;
		triggerName = name;
	}
	
	public String getObjectName(WbConnection conn)
	{
		return conn.getMetadata().quoteObjectname(this.triggerName);
	}
	
	public String getObjectExpression(WbConnection conn)
	{
		return SqlUtil.buildExpression(conn, catalog, schema, triggerName);
	}
	
	public String getDisplayName()
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

	public void setSource(CharSequence s) { this.source = s; }
	public CharSequence getSource() { return this.source; }
	
}
