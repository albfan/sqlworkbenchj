/*
 * PkTemplate.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.sqltemplates;

/**
 *
 * @author Thomas Kellerer
 */
public class PkTemplate
	extends TemplateHandler
{
	private final String defaultSQL =
		"ALTER TABLE %table_name%\n" +
		"   ADD CONSTRAINT %pk_name% PRIMARY KEY (%columnlist%)";

	private final String defaultInlineSQL =
		"CONSTRAINT %pk_name% PRIMARY KEY (%columnlist%)";

	private String sql;

	public PkTemplate(String dbid, boolean forInlineUse)
	{
		if (forInlineUse)
		{
			this.sql = getStringProperty("workbench.db." + dbid + ".pk.inline.sql", defaultInlineSQL);
		}
		else
		{
			this.sql = getStringProperty("workbench.db." + dbid + ".pk.sql", defaultSQL);
		}
	}

	public String getSQLTemplate()
	{
		return sql;
	}

}
