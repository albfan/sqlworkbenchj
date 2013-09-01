/*
 * Db2SequenceReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.db.ibm;

import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2SequenceReaderTest
	extends WbTestCase
{

	public Db2SequenceReaderTest()
	{
		super("Db2SequenceReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		Db2TestUtil.initTestCase();
		WbConnection con = Db2TestUtil.getDb2Connection();
		if (con == null) return;

		String schema = Db2TestUtil.getSchemaName();

		String sql =
			"create sequence " + schema + ".wb_sequence_a;\n"+
			"create sequence " + schema + ".wb_sequence_b increment by 2 start with 42;\n"+
			"commit;\n";
		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		WbConnection con = Db2TestUtil.getDb2Connection();
		if (con == null) return;
		Db2TestUtil.cleanUpTestCase();
	}

	@Test
	public void testGetSequences()
		throws Exception
	{
		WbConnection con = Db2TestUtil.getDb2Connection();
		if (con == null)
		{
			System.out.println("DB2 not available. Skipping test");
			return;
		}

		SequenceReader reader = con.getMetadata().getSequenceReader();
		assertTrue(reader instanceof Db2SequenceReader);

		String schema = Db2TestUtil.getSchemaName();

		List<SequenceDefinition> seqs = reader.getSequences(null, schema, null);
		assertNotNull(seqs);
		assertEquals(2, seqs.size());
		assertEquals("WB_SEQUENCE_A", seqs.get(0).getSequenceName());

		String sql = seqs.get(1).getSource(con).toString();
		String src = "CREATE SEQUENCE " + schema + ".WB_SEQUENCE_B\n" +
    "       START WITH 42\n" +
    "       INCREMENT BY 2\n" +
    "       MINVALUE 42\n" +
    "       NO MAXVALUE\n" +
    "       CACHE 20\n" +
    "       NO CYCLE\n" +
    "       NO ORDER;";
		assertEquals(src, sql.trim());

		seqs = reader.getSequences(null, schema, "WB_SEQUENCE_A");
		assertNotNull(seqs);
		assertEquals(1, seqs.size());
	}
}
