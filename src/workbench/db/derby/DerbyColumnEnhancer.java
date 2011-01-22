/*
 * DerbyColumnEnhancer
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.derby;

import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class DerbyColumnEnhancer
	implements ColumnDefinitionEnhancer
{

	@Override
	public void updateColumnDefinition(TableDefinition table, WbConnection conn)
	{
		for (ColumnIdentifier col : table.getColumns())
		{
			String defaultValue = col.getDefaultValue();
			if (StringUtil.isNonBlank(defaultValue))
			{
				if (defaultValue.startsWith("GENERATED ALWAYS AS"))
				{
					col.setComputedColumnExpression(defaultValue);
				}
				if (defaultValue.startsWith("AUTOINCREMENT:") || defaultValue.equals("GENERATED_BY_DEFAULT"))
				{
					col.setIsAutoincrement(true);
				}
			}
		}
	}
	
}
