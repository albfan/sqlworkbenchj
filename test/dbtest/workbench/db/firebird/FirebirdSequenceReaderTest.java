/*
 * FirebirdSequenceReaderTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.firebird;

import java.util.List;
import workbench.TestUtil;
import workbench.db.TableIdentifier;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class FirebirdSequenceReaderTest
{

	public FirebirdSequenceReaderTest()
	{
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		FirebirdTestUtil.initTestCase();
		WbConnection con = FirebirdTestUtil.getFirebirdConnection();
		if (con == null) return;
		
		TestUtil.executeScript(con, "CREATE SEQUENCE seq_one;");
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		FirebirdTestUtil.cleanUpTestCase();
	}

	@Test
	public void retrieveSequences()
		throws Exception
	{
		WbConnection con = FirebirdTestUtil.getFirebirdConnection();
		if (con == null) return;
		List<TableIdentifier> objects = con.getMetadata().getObjectList(null, new String[] { "SEQUENCE" });
		assertEquals(1, objects.size());
		TableIdentifier seq = objects.get(0);
		assertEquals("SEQUENCE", seq.getObjectType());
		String sql = seq.getSource(con).toString();
		String expected = "CREATE SEQUENCE SEQ_ONE;";
		assertEquals(expected, sql.trim());
	}

}
