/*
 * OracleSequenceReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import java.util.Collection;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
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
public class OracleSequenceReaderTest
	extends WbTestCase
{

	public OracleSequenceReaderTest()
	{
		super("OracleSynonymReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		TestUtil.executeScript(con,
			"CREATE SEQUENCE seq_one;\n"  +
			"CREATE SEQUENCE seq_two MINVALUE 33 increment by 12 CACHE 42;\n" +
			"CREATE SEQUENCE seq_three MINVALUE 2 maxvalue 42 cycle;\n" +
			"CREATE SEQUENCE seq_four MINVALUE 2 nocycle cache 12 order;"
    );
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testGetSequenceDefinition()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull("Oracle not available", con);

		SequenceReader reader = con.getMetadata().getSequenceReader();
		assertNotNull(reader);
		Collection<String> types = con.getMetadata().getObjectTypes();
		assertTrue(types.contains("SEQUENCE"));
		List<TableIdentifier> objects = con.getMetadata().getObjectList(null, new String[] { "SEQUENCE"});
		assertNotNull(objects);
		assertEquals(4, objects.size());

		SequenceDefinition one = reader.getSequenceDefinition(null, "WBJUNIT", "SEQ_ONE");
		assertNotNull(one);
		String sql = one.getSource(con).toString().trim();
		String expected =
      "CREATE SEQUENCE SEQ_ONE\n" +
      "       INCREMENT BY 1\n" +
      "       NOMINVALUE\n" +
      "       NOMAXVALUE\n" +
      "       CACHE 20\n" +
      "       NOCYCLE\n" +
      "       NOORDER;";
//		System.out.println("--------- expected:\n" + expected + "\n--------- got:\n" + sql);
		assertEquals(expected, sql);

		SequenceDefinition two = reader.getSequenceDefinition(null, "WBJUNIT", "SEQ_TWO");
		assertNotNull(two);
		sql = two.getSource(con).toString().trim();
		expected =
      "CREATE SEQUENCE SEQ_TWO\n" +
      "       INCREMENT BY 12\n" +
      "       MINVALUE 33\n" +
      "       NOMAXVALUE\n" +
      "       CACHE 42\n" +
      "       NOCYCLE\n" +
      "       NOORDER;";
//		System.out.println("--------- expected:\n" + expected + "\n--------- got:\n" + sql);
		assertEquals(expected, sql);

		SequenceDefinition three = reader.getSequenceDefinition(null, "WBJUNIT", "SEQ_THREE");
		assertNotNull(three);
		sql = three.getSource(con).toString().trim();
		expected =
      "CREATE SEQUENCE SEQ_THREE\n" +
      "       INCREMENT BY 1\n" +
      "       MINVALUE 2\n" +
      "       MAXVALUE 42\n" +
      "       CACHE 20\n" +
      "       CYCLE\n" +
      "       NOORDER;";
//		System.out.println("--------- expected:\n" + expected + "\n--------- got:\n" + sql);
		assertEquals(expected, sql);

		SequenceDefinition four = reader.getSequenceDefinition(null, "WBJUNIT", "SEQ_FOUR");
		assertNotNull(four);
		sql = four.getSource(con).toString().trim();
		expected =
      "CREATE SEQUENCE SEQ_FOUR\n" +
      "       INCREMENT BY 1\n" +
      "       MINVALUE 2\n" +
      "       NOMAXVALUE\n" +
      "       CACHE 12\n" +
      "       NOCYCLE\n" +
      "       ORDER;";
//		System.out.println("--------- expected:\n" + expected + "\n--------- got:\n" + sql);
		assertEquals(expected, sql);
	}

}
