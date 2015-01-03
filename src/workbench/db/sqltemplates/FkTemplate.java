/*
 * FkTemplate.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
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
		"    %fk_update_rule%%fk_delete_rule% %deferrable%";

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
