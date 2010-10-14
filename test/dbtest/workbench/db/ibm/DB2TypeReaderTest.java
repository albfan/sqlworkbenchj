/*
 * DB2TypeReaderTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.ibm;

import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class DB2TypeReaderTest
{

	public DB2TypeReaderTest()
	{
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		Db2TestUtil.initTestCase();
		WbConnection con = Db2TestUtil.getDb2Connection();
		if (con == null) return;

		String sql =
			"create type wbjunit.address_type as  \n" +
      "( \n" +
      "  street varchar(50),  \n" +
      "  city varchar(50), \n" +
      "  nr integer \n" +
      ") \n" +
      "MODE db2sql; \n" +
      "commit;\n";
		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		WbConnection con = Db2TestUtil.getDb2Connection();
		if (con == null) return;

		String sql =
			"drop type wbjunit.address_type; \n" +
      "commit;\n";
		TestUtil.executeScript(con, sql);
		Db2TestUtil.cleanUpTestCase();
	}

	@Test
	public void testGetTypes()
		throws Exception
	{
		WbConnection con = Db2TestUtil.getDb2Connection();
		if (con == null) return;
		List<TableIdentifier> objects = con.getMetadata().getObjectList(Db2TestUtil.getSchemaName(), new String[] {"TYPE"} );
		assertNotNull(objects);
		assertEquals(1, objects.size());
		assertEquals("TYPE", objects.get(0).getType());

		DB2TypeReader reader = new DB2TypeReader();
		List<DB2ObjectType> types = reader.getTypes(con, Db2TestUtil.getSchemaName(), null);
		assertNotNull(types);
		assertEquals(1, types.size());

		DB2ObjectType type = types.get(0);
		assertEquals("ADDRESS_TYPE", type.getObjectName());
		String src = type.getSource(con).toString().trim();
		String sql =
			"CREATE TYPE ADDRESS_TYPE AS\n" +
      "(\n" +
      "  STREET  VARCHAR(50),\n" +
      "  CITY    VARCHAR(50),\n" +
      "  NR      INTEGER\n" +
      ");";
		assertEquals(src, sql);
	}

}
