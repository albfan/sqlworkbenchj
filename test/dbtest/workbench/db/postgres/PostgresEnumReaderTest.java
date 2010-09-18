/*
 * PostgresEnumReaderTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import java.util.Collection;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.DbObject;
import workbench.db.EnumIdentifier;
import workbench.db.WbConnection;
import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresEnumReaderTest
	extends WbTestCase
{
	private static final String TEST_ID = "enumtest";

	public PostgresEnumReaderTest()
	{
		super("PostgresEnumReaderTest");
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		PostgresTestCase.initTestCase(TEST_ID);
		WbConnection con = TestUtil.getPostgresConnection();
		TestUtil.executeScript(con, 
			"CREATE TYPE stimmung AS ENUM ('sad','ok','happy');\n" +
			"COMMENT ON TYPE stimmung IS 'my enum';\n" +
			"COMMIT;\n");
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		PostgresTestCase.cleanUpTestCase(TEST_ID);
	}

	@Test
	public void testSupportedTypes()
	{
		PostgresEnumReader instance = new PostgresEnumReader();
		List<String> expResult = CollectionUtil.arrayList("ENUM");
		List<String> result = instance.supportedTypes();
		assertEquals(expResult, result);
	}

	@Test
	public void testHandlesType()
	{
		PostgresEnumReader reader = new PostgresEnumReader();
		assertTrue(reader.handlesType("ENUM"));
		assertTrue(reader.handlesType("enum"));
		assertFalse(reader.handlesType("enumeration"));
	}

	@Test
	public void testEnumRetrieval()
	{
		WbConnection con = TestUtil.getPostgresConnection();
		PostgresEnumReader reader = new PostgresEnumReader();
		Collection<EnumIdentifier> enums = reader.getDefinedEnums(con, TEST_ID, null);
		assertEquals(1, enums.size());
		EnumIdentifier enumId = enums.iterator().next();
		assertEquals("stimmung", enumId.getObjectName());
		assertEquals("my enum", enumId.getComment());
	}


}
