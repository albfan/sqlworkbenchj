/*
 * PostgresConstraintReader.java
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
package workbench.db.postgres;

import workbench.db.AbstractConstraintReader;


/**
 * Read table level constraints for Postgres
 * (column constraints are stored on table level...)
 * @author  Thomas Kellerer
 */
public class PostgresConstraintReader
	extends AbstractConstraintReader
{
	private final String TABLE_SQL =
				"select rel.conname,  \n" +
				"       case  \n" +
				"         when rel.consrc is null then pg_get_constraintdef(rel.oid) \n" +
				"         else rel.consrc \n" +
				"       end as src, \n" +
				"       obj_description(t.oid) as remarks  \n" +
				"from pg_class t \n" +
				"  join pg_constraint rel on t.oid = rel.conrelid   \n" +
				"  join pg_namespace nsp on t.relnamespace = nsp.oid \n" +
				"where rel.contype in ('c', 'x') \n" +
				" and t.relname = ? \n" +
				" and nsp.nspname = ? ";

	public PostgresConstraintReader(String dbId)
	{
		super(dbId);
	}


	@Override
	public String getColumnConstraintSql()
	{
		return null;
	}

	@Override
	public String getTableConstraintSql()
	{
		return TABLE_SQL;
	}

	@Override
	public int getIndexForTableNameParameter()
	{
		return 1;
	}

	@Override
	public int getIndexForSchemaParameter()
	{
		return 2;
	}

}
