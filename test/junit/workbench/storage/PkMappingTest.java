/*
 * PkMappingTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.util.List;
import workbench.TestUtil;
import workbench.db.TableIdentifier;
import workbench.util.WbFile;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class PkMappingTest
{

	@Test
	public void testMapping()
	{
		TestUtil util = new TestUtil("PkMappingTest");
		WbFile f = new WbFile(util.getBaseDir(), "mapping_test.properties");
		PkMapping map = new PkMapping(f.getFullPath());
		TableIdentifier tbl = new TableIdentifier("PERSON");
		map.addMapping(tbl, "id");

		TableIdentifier tbl2 = new TableIdentifier("person");
		List<String> col = map.getPKColumns(tbl2);
		assertEquals(1, col.size());
		assertEquals("id", col.get(0));
	}
}
