/*
 * PostgresColumnEnhancer
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresColumnEnhancer
	implements ColumnDefinitionEnhancer
{

	@Override
	public void updateColumnDefinition(TableDefinition table, WbConnection conn)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
