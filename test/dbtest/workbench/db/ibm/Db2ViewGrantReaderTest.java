/*
 * Db2ViewGrantReaderTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.ibm;

import workbench.TestUtil;
import java.util.Collection;
import workbench.db.TableGrant;
import java.util.List;
import workbench.db.TableIdentifier;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.db.ViewGrantReader;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2ViewGrantReaderTest
{

	public Db2ViewGrantReaderTest()
	{
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		Db2TestUtil.initTestCase();

		WbConnection conn = Db2TestUtil.getDb2Connection();
		if (conn == null) return;

		String schema = Db2TestUtil.getSchemaName();

		String sql =
			"CREATE TABLE " + schema + ".person (id integer, firstname varchar(50), lastname varchar(50)); \n" +
      "CREATE VIEW " + schema + ".v_person AS SELECT * FROM wbjunit.person; \n" +
      "GRANT SELECT ON " + schema + ".v_person TO PUBLIC;\n" +
			"commit;\n";
		TestUtil.executeScript(conn, sql);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		Db2TestUtil.cleanUpTestCase();
	}

	@Test
	public void testGetViewGrantSql()
		throws Exception
	{
		WbConnection conn = Db2TestUtil.getDb2Connection();
		if (conn == null) return;
		String schema = Db2TestUtil.getSchemaName();

		List<TableIdentifier> views = conn.getMetadata().getObjectList(schema, new String[] { "VIEW" });
		assertNotNull(views);
		assertEquals(1, views.size());

		ViewGrantReader reader = ViewGrantReader.createViewGrantReader(conn);
		assertTrue(reader instanceof Db2ViewGrantReader);

		Collection<TableGrant> grants = reader.getViewGrants(conn, views.get(0));
		assertNotNull(grants);
		assertEquals(1, grants.size());
		TableGrant grant = grants.iterator().next();
		assertEquals("SELECT", grant.getPrivilege());
		assertEquals("PUBLIC", grant.getGrantee());
	}

}
