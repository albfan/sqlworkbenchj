/*
 * TableDefinitionTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class TableDefinitionTest
{

	public TableDefinitionTest()
	{
	}

	@Test
	public void testToString()
	{
		TableIdentifier tbl = new TableIdentifier("PERSON");
		List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>(3);
		cols.add(new ColumnIdentifier("ID", Types.INTEGER));
		ColumnIdentifier firstname = new ColumnIdentifier("FIRSTNAME", Types.VARCHAR);
		firstname.setColumnSize(50);
		cols.add(firstname);

		ColumnIdentifier lastname = new ColumnIdentifier("LASTNAME", Types.VARCHAR);
		lastname.setColumnSize(50);
		cols.add(lastname);

		TableDefinition def = new TableDefinition(tbl, cols);
		assertEquals("PERSON(ID, FIRSTNAME, LASTNAME)", def.toString());
	}
}
