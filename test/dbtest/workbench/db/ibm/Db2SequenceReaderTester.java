/*
 * Db2SequenceReaderTest
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
import workbench.WbTestCase;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2SequenceReaderTester
	extends WbTestCase
{

	public Db2SequenceReaderTester()
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

		String sql =
			"create sequence wbjunit.wb_sequence_a;\n"+
			"create sequence wbjunit.wb_sequence_b increment by 2 start with 42;\n"+
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
			"drop sequence wbjunit.wb_sequence_a;\n"+
			"drop sequence wbjunit.wb_sequence_b;\n"+
			"commit;\n";
		TestUtil.executeScript(con, sql);
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

		List<SequenceDefinition> seqs = reader.getSequences(null, Db2TestUtil.getSchemaName(), null);
		assertNotNull(seqs);
		assertEquals(2, seqs.size());
		assertEquals("WB_SEQUENCE_A", seqs.get(0).getSequenceName());
		String sql = seqs.get(1).getSource(con).toString();
		String src = "CREATE SEQUENCE WBJUNIT.WB_SEQUENCE_B\n" +
    "       START WITH 42\n" +
    "       INCREMENT BY 2\n" +
    "       MINVALUE 42\n" +
    "       NO MAXVALUE\n" +
    "       CACHE 20\n" +
    "       NO CYCLE\n" +
    "       NO ORDER;";
		assertEquals(src, sql.trim());
	}
}
