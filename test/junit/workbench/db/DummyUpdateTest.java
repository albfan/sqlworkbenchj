/*
 * DummyUpdateTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.Statement;
import workbench.TestUtil;
import workbench.util.SqlUtil;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class DummyUpdateTest
{

	@Test
	public void testGetSource()
		throws Exception
	{
		TestUtil util = new TestUtil("DummyUpdateGen1");
		WbConnection con = util.getConnection();

		try
		{
			Statement stmt = con.createStatement();
			stmt.executeUpdate("create table person (nr integer not null primary key, firstname varchar(20), lastname varchar(20))");
			con.commit();
			TableIdentifier person = con.getMetadata().findTable(new TableIdentifier("PERSON"));
			DummyUpdate insert = new DummyUpdate(person);
			assertEquals("UPDATE", insert.getObjectType());
			String sql = insert.getSource(con).toString();

			String verb = SqlUtil.getSqlVerb(sql);
			assertEquals("UPDATE", verb);
			assertTrue(sql.indexOf("FIRSTNAME = 'FIRSTNAME_value'") > -1);
			assertTrue(sql.indexOf("WHERE NR = NR_value") > -1);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

}
