/*
 * FirebirdSequenceReaderTest.java
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
package workbench.db.firebird;

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
		assertNotNull("No connection available", con);

		List<TableIdentifier> objects = con.getMetadata().getObjectList(null, new String[] { "SEQUENCE" });
		assertEquals(1, objects.size());
		TableIdentifier seq = objects.get(0);
		assertEquals("SEQUENCE", seq.getObjectType());
		String sql = seq.getSource(con).toString();
		String expected = "CREATE SEQUENCE SEQ_ONE;";
		assertEquals(expected, sql.trim());
	}

}
