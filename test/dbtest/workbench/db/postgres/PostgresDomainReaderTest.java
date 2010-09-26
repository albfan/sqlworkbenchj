/*
 * PostgresDomainReaderTest
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
import workbench.db.DomainIdentifier;
import workbench.db.GenericObjectDropper;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresDomainReaderTest
	extends WbTestCase
{

	private static final String TEST_ID = "domaintest";

	public PostgresDomainReaderTest()
	{
		super("PostgresDomainReaderTest");
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TEST_ID);
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null)
		{
			return;
		}
		TestUtil.executeScript(con,
			"CREATE DOMAIN salary AS numeric(12,2) CHECK (value > 0);\n" +
			"COMMIT; \n");
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase(TEST_ID);
	}

	@Test
	public void testGetDomainRetrieval()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null)
		{
			System.out.println("No PostgreSQL connection available. Skipping test...");
			return;
		}
		Collection<String> types = con.getMetadata().getObjectTypes();
		assertTrue(types.contains("DOMAIN"));
		List<TableIdentifier> objects = con.getMetadata().getObjectList(TEST_ID, new String[] { "DOMAIN" });
		assertEquals(1, objects.size());

		PostgresDomainReader reader = new PostgresDomainReader();

		List<DomainIdentifier> domains = reader.getDomainList(con, "%", "%");
		assertEquals(1, domains.size());

		DbObject domain = objects.get(0);

		assertEquals("DOMAIN", domain.getObjectType());
		String sql = domain.getSource(con).toString().trim();
		String expected = "CREATE DOMAIN salary AS numeric(12,2)\n" +
                      "   CONSTRAINT NOT NULL CHECK (VALUE > 0::numeric);";
		assertEquals(expected, sql);
		GenericObjectDropper dropper = new GenericObjectDropper();
		dropper.setCascade(true);
		dropper.setObjects(objects);
		dropper.setConnection(con);

		String drop = dropper.getScript().toString().trim();
		assertTrue(drop.startsWith("DROP DOMAIN salary"));
	}


}
