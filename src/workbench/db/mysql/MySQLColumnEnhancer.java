/*
 * MySQLColumnEnhancer.java
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
package workbench.db.mysql;

import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;

/**
 * A class to retrieve enum and collation definitions for the columns of a MySQL table.
 *
 * @author  Thomas Kellerer
 * @see workbench.db.DbMetadata#getTableDefinition(workbench.db.TableIdentifier)
 * @see MySQLEnumReader
 * @see MySQLColumnCollationReader
 */
public class MySQLColumnEnhancer
	implements ColumnDefinitionEnhancer
{

	@Override
	public void updateColumnDefinition(TableDefinition tbl, WbConnection connection)
	{
		MySQLColumnCollationReader collationReader = new MySQLColumnCollationReader();
		collationReader.readCollations(tbl, connection);

		MySQLEnumReader enumReader = new MySQLEnumReader();
		enumReader.readEnums(tbl, connection);
	}

}
