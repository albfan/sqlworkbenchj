/*
 * ImportFileColumnTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import java.util.List;
import junit.framework.TestCase;
import workbench.db.ColumnIdentifier;

/**
 *
 * @author Thomas Kellerer
 */
public class ImportFileColumnTest
	extends TestCase
{

	public ImportFileColumnTest(String testName)
	{
		super(testName);
	}

	public void testEquals()
	{
		List<ImportFileColumn> columns = ImportFileColumn.createList();
		columns.add(new ImportFileColumn(new ColumnIdentifier("firstname")));
		columns.add(new ImportFileColumn(new ColumnIdentifier("lastname")));
		columns.add(new ImportFileColumn(new ColumnIdentifier("person_id")));

		int index = columns.indexOf("firstname");
		assertEquals(0, index);

		index = columns.indexOf("person_id");
		assertEquals(2, index);

		index = columns.indexOf(new ColumnIdentifier("person_id"));
		assertEquals(2, index);

	}

}
