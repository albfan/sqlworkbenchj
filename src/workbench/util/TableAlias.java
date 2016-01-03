/*
 * TableAlias.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.util;

import workbench.db.TableIdentifier;

/**
 * @author Thomas Kellerer
 */
public class TableAlias
	extends Alias
{
	private TableIdentifier table;

	public TableAlias(String value)
	{
		super(value);
    checkTable('.', '.');
	}

	public TableAlias(String objectName, char catalogSeparator, char schemaSeparator)
	{
		super(objectName);
    checkTable(catalogSeparator, schemaSeparator);
  }

	public TableAlias(String objectName, String alias, char catalogSeparator, char schemaSeparator)
	{
		super(objectName, alias);
    checkTable(catalogSeparator, schemaSeparator);
  }

  private void checkTable(char catalogSeparator, char schemaSeparator)
  {
		if (getObjectName() != null)
		{
			this.table = new TableIdentifier(getObjectName(), catalogSeparator, schemaSeparator);
		}
  }

	public final TableIdentifier getTable()
	{
		return this.table;
	}

	/**
	 * Compares the given name to this TableAlias checking
	 * if the name either references this table or its alias
	 */
	public boolean isTableOrAlias(String name, char catalogSeparator, char schemaSeparator)
	{
		if (StringUtil.isEmptyString(name))
		{
			return false;
		}

		TableIdentifier tbl = new TableIdentifier(name, catalogSeparator, schemaSeparator);
		return (table.getTableName().equalsIgnoreCase(tbl.getTableName()) || name.equalsIgnoreCase(getAlias()));
	}
}
