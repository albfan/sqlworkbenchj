/*
 * MySqlEnumReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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
