/*
 * HsqlSequenceReaderTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.hsqldb;

import java.util.List;
import org.junit.AfterClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class HsqlSequenceReaderTest
	extends WbTestCase
{

	public HsqlSequenceReaderTest()
	{
		super("HsqlSequenceReaderTest");
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		ConnectionMgr.getInstance().disconnectAll();
	}

	@Test
	public void testReader()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getHSQLConnection("sequence_test");
		TestUtil.executeScript(con,
			"create sequence aaa_sequence as bigint start with 42;\n" +
			"create sequence bbb_sequence start with 12 increment by 5;\n");
		SequenceReader reader = con.getMetadata().getSequenceReader();
		assertTrue(reader instanceof HsqlSequenceReader);
		List<SequenceDefinition> sequences = reader.getSequences(null, "PUBLIC", null);
		assertNotNull(sequences);
		assertEquals(2, sequences.size());

		SequenceDefinition one = sequences.get(0);
		assertEquals("AAA_SEQUENCE", one.getSequenceName());
		String sql = one.getSource().toString();
		assertTrue(sql.indexOf("AS BIGINT") > -1);
		assertTrue(sql.indexOf("START WITH 42") > -1);

		SequenceDefinition two = sequences.get(1);
		assertEquals("BBB_SEQUENCE", two.getSequenceName());
		sql = two.getSource().toString();
		assertTrue(sql.indexOf("AS BIGINT") == -1);
		assertTrue(sql.indexOf("START WITH 12") > -1);
		assertTrue(sql.indexOf("INCREMENT BY 5") > -1);

		// Since 2.0 there is a system sequence in the schema "SYSTEM_LOBS", so without a schema
		// we expect three sequences in the list.
		sequences = reader.getSequences(null, null, null);
		assertEquals(3, sequences.size());
	}

}
