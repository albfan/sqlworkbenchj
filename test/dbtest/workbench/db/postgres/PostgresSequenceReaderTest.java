/*
 * PostgresSequenceReaderTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import workbench.WbTestCase;
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
public class PostgresSequenceReaderTest
	extends WbTestCase
{

	private static final String TEST_ID = "ruletest";

	public PostgresSequenceReaderTest()
	{
		super("PostgresSequenceReaderTest");
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
			"CREATE SEQUENCE seq_one;\n" +
			"CREATE SEQUENCE seq_two cache 25 minvalue 100 increment by 10;\n" +
			"CREATE TABLE seq_table (id integer);\n" +
			"ALTER SEQUENCE seq_one OWNED BY seq_table.id;\n" +
			"COMMIT; \n");
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase(TEST_ID);
	}

	@Test
	public void retrieveSequences()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null)
		{
			System.out.println("No PostgreSQL connection available. Skipping test...");
			return;
		}
		List<TableIdentifier> objects = con.getMetadata().getObjectList(TEST_ID, new String[] { "SEQUENCE" });
		assertEquals(2, objects.size());
		TableIdentifier seq = objects.get(0);
		assertEquals("SEQUENCE", seq.getObjectType());
		String sql = seq.getSource(con).toString();
		String expected = "CREATE SEQUENCE seq_one\n" +
             "       INCREMENT BY 1\n" +
             "       MINVALUE 1\n" +
             "       CACHE 1\n" +
             "       NO CYCLE;\n\n" +
						 "ALTER SEQUENCE seq_one OWNER TO seq_table.id;";
//		System.out.println(sql + "\n-------------\n" + expected);
		
		assertEquals(expected, sql.trim());

		seq = objects.get(1);
		sql = seq.getSource(con).toString();
//		System.out.println(sql);
		expected = "CREATE SEQUENCE seq_two\n" +
             "       INCREMENT BY 10\n" +
             "       MINVALUE 100\n" +
             "       CACHE 25\n" +
             "       NO CYCLE;";
		assertEquals(expected, sql.trim());
	}

}
