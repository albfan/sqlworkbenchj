/*
 * H2ConstantReaderTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.h2database;

import workbench.db.DbObject;
import java.util.List;
import workbench.TestUtil;
import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import org.junit.AfterClass;
import org.junit.Test;
import workbench.WbTestCase;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class H2ConstantReaderTest
	extends WbTestCase
{

	public H2ConstantReaderTest()
	{
		super("H2ConstantReaderTest");
	}

	@AfterClass
	public static void tearDown()
	{
		ConnectionMgr.getInstance().disconnectAll();
	}

	@Test
	public void testGetConstantsList()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();

		String script = "CREATE CONSTANT THE_ANSWER VALUE 42;";
		TestUtil.executeScript(con, script);

		List<TableIdentifier> objects = con.getMetadata().getObjectList(null, new String[] { "CONSTANT" });
		assertNotNull(objects);
		assertEquals(1, objects.size());

		DbObject dbo = con.getMetadata().getObjectDefinition(objects.get(0));
		assertTrue(dbo instanceof H2Constant);
		H2Constant constant = (H2Constant)dbo;
		assertEquals("THE_ANSWER", constant.getObjectName());
		assertEquals("INTEGER", constant.getDataType());
		assertEquals("42", constant.getValue());
	}

}
