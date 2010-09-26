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
import workbench.db.EnumIdentifier;
import workbench.db.WbConnection;
import workbench.sql.ScriptParser;
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
		PostgresTestUtil.initTestCase(TEST_ID);
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		TestUtil.executeScript(con,
			"CREATE TYPE stimmung AS ENUM ('sad','ok','happy');\n" +
			"COMMENT ON TYPE stimmung IS 'my enum';\n" +
			"COMMIT;\n");
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase(TEST_ID);
	}

	@Test
	public void testSupportedTypes()
	{
		PostgresEnumReader reader = new PostgresEnumReader();
		List<String> expResult = CollectionUtil.arrayList("ENUM");
		List<String> result = reader.supportedTypes();
		assertEquals(expResult, result);
		assertTrue(reader.handlesType("ENUM"));
		assertTrue(reader.handlesType("enum"));
		assertFalse(reader.handlesType("enumeration"));
	}

	@Test
	public void testEnumRetrieval()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		PostgresEnumReader reader = new PostgresEnumReader();
		Collection<EnumIdentifier> enums = reader.getDefinedEnums(con, TEST_ID, null);
		assertEquals(1, enums.size());
		EnumIdentifier enumId = enums.iterator().next();
		assertEquals("stimmung", enumId.getObjectName());
		assertEquals("my enum", enumId.getComment());

		String sql = enumId.getSource(con).toString();
		ScriptParser parser = new ScriptParser(sql.toString());
		assertEquals(2, parser.getSize());
		String create = parser.getCommand(0);
		assertEquals(create, "CREATE TYPE stimmung AS ENUM ('sad','ok','happy')");
		String comment = parser.getCommand(1);
		assertEquals(comment, "COMMENT ON TYPE stimmung IS 'my enum'");
	}

}
