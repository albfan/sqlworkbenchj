/*
 * OracleViewGrantReaderTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import java.util.Collection;
import workbench.db.TableGrant;
import java.util.List;
import workbench.TestUtil;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleViewGrantReaderTest
{

	public OracleViewGrantReaderTest()
	{
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		String sql = "create table person (id integer primary key, first_name varchar(100), last_name varchar(100), check (id > 0));\n" +
			"create view v_person (id, full_name) as select id, first_name || ' ' || last_name from person;\n" +
			"grant select on v_person to public;\n";
		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testRetrieveGrants()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		List<TableIdentifier> views = con.getMetadata().getObjectList("WBJUNIT", new String[] { "VIEW" });
		assertEquals(1, views.size());
		TableIdentifier v = views.get(0);
		assertEquals("VIEW", v.getType());
		assertEquals("V_PERSON", v.getTableName());

		String sql = views.get(0).getSource(con).toString();
		assertTrue(sql.contains("GRANT SELECT ON V_PERSON TO PUBLIC"));

		OracleViewGrantReader reader = new OracleViewGrantReader();
		Collection<TableGrant> grants = reader.getViewGrants(con, views.get(0));
		assertNotNull(grants);
		assertEquals(1, grants.size());
		TableGrant grant = grants.iterator().next();
		assertEquals("SELECT", grant.getPrivilege());
		assertEquals("PUBLIC", grant.getGrantee());
	}

}
