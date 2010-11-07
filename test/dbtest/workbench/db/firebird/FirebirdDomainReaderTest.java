/*
 * FirebirdDomainReaderTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.firebird;

import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.DomainIdentifier;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class FirebirdDomainReaderTest
	extends WbTestCase
{
	public FirebirdDomainReaderTest()
	{
		super("FirebirdDomainReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		FirebirdTestUtil.initTestCase();
		WbConnection con = FirebirdTestUtil.getFirebirdConnection();
		if (con == null) return;
		String sql = 
			"CREATE DOMAIN AAA_SALARY AS DECIMAL(12,2) NOT NULL;\n" +
			"CREATE DOMAIN BBB_POSITIVE_INTEGER AS BIGINT DEFAULT 42 CHECK (value > 0);\n";
		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		FirebirdTestUtil.cleanUpTestCase();
	}

	@Before
	public void setUp()
	{
	}

	@After
	public void tearDown()
	{
	}

	@Test
	public void testGetDomainList()
		throws Exception
	{
		WbConnection con = FirebirdTestUtil.getFirebirdConnection();
		if (con == null) return;

		List<TableIdentifier> domains = con.getMetadata().getObjectList(null, new String[] { "DOMAIN" });
		assertNotNull(domains);
		assertEquals(2, domains.size());

		DbObject dbo = con.getMetadata().getObjectDefinition(domains.get(0));
		assertNotNull(dbo);
		assertEquals("DOMAIN", dbo.getObjectType());
		assertTrue(dbo instanceof DomainIdentifier);
		DomainIdentifier salary = (DomainIdentifier)dbo;
		assertEquals("AAA_SALARY", salary.getObjectName());

		DomainIdentifier positive = (DomainIdentifier)con.getMetadata().getObjectDefinition(domains.get(1));
		assertEquals("CHECK (value > 0)", positive.getCheckConstraint());
	}

}
