/*
 * FkTemplate.java
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
public class FkTemplate
	extends TemplateHandler
{
	private final String defaultSQL =
		"ALTER TABLE %table_name%\n" +
		"  ADD CONSTRAINT %constraint_name% FOREIGN KEY (%columnlist%)\n" +
		"  REFERENCES %targettable% (%targetcolumnlist%)\n" +
		"  %fk_update_rule%\n" +
		"  %fk_delete_rule%\n" +
		"  %deferrable%";

	private final String defaultInlineSQL =
		"CONSTRAINT %constraint_name% FOREIGN KEY (%columnlist%) REFERENCES %targettable% (%targetcolumnlist%)\n" +
		"              %fk_update_rule%%fk_delete_rule%%deferrable%";

	private String sql;

	public FkTemplate(String dbid, boolean forInlineUse)
	{
		if (forInlineUse)
		{
			this.sql = getStringProperty("workbench.db." + dbid + ".fk.inline.sql", defaultInlineSQL);
		}
		else
		{
			this.sql = getStringProperty("workbench.db." + dbid + ".fk.sql", defaultSQL);
		}
	}

	public String getSQLTemplate()
	{
		return sql;
	}
}
