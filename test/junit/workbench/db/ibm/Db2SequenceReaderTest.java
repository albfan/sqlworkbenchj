/*
 * Db2SequenceReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.ibm;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.db.DbObject;
import workbench.db.SequenceDefinition;
import workbench.db.WbConnection;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2SequenceReaderTest
	extends TestCase
{

	private WbConnection db;

	public Db2SequenceReaderTest(String testName)
	{
		super(testName);
	}

	@Override
	protected void setUp()
		throws Exception
	{
		super.setUp();
		TestUtil util = new TestUtil(getName());
		db = util.getConnection("fake_db2");
		TestUtil.executeScript(db,
			"create schema syscat;\n" +
			"set schema syscat;\n" +
			"create view sequences \n" +
			"AS \n" +
			"select sequence_name as SEQNAME,  \n" +
			"       sequence_catalog as seqschema, \n " +
			"       0 as START,  \n" +
			"       0 as MINVALUE,  \n" +
			"       9999999 as MAXVALUE,  \n" +
			"       increment, \n" +
			"       'N' as CYCLE, \n" +
			"       'N' as \"ORDER\",  \n" +
			"       cache, \n" +
			"       42 as DATATYPEID," +
			"       REMARKS \n" +
			"from information_schema.sequences; \n" +
			"create schema sysibm;\n" +
			"set schema sysibm;\n" +
			"create view SYSSEQUENCES \n" +
			"AS \n" +
			"select sequence_name as NAME,  \n" +
			"       sequence_catalog as SCHEMA, \n " +
			"       0 as START,  \n" +
			"       0 as MINVALUE,  \n" +
			"       9999999 as MAXVALUE,  \n" +
			"       increment, \n" +
			"       'N' as CYCLE, \n" +
			"       'N' as \"ORDER\",  \n" +
			"       cache, \n" +
			"       42 as DATATYPEID," +
			"       REMARKS \n" +
			"from information_schema.sequences; \n" +
			"set schema public;" +
			"create sequence seq_aaa;\n" +
			"comment on sequence seq_aaa IS 'aaa comment';\n" +
			"create sequence seq_bbb;\n" +
			"comment on sequence seq_bbb IS 'bbb comment';\n" +
			"commit;");
	}

	@Override
	protected void tearDown()
		throws Exception
	{
		db.disconnect();
	}

	public void testLUW()
	{
		Db2SequenceReader reader = new Db2SequenceReader(db, "db2");
		reader.setQuoteKeyword(true);
		List<SequenceDefinition> result = reader.getSequences("FAKE_DB2", null);
		Collections.sort(result, new Comparator<DbObject>()
		{
			public int compare(DbObject o1, DbObject o2)
			{
				return StringUtil.compareStrings(o1.getObjectName(), o2.getObjectName(), true);
			}
		});

		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals("SEQ_AAA", result.get(0).getSequenceName());
		assertEquals("SEQ_BBB", result.get(1).getSequenceName());

		CharSequence sql = result.get(0).getSource();
		assertNotNull(sql);
		String source = sql.toString();

		assertTrue(source.startsWith("CREATE SEQUENCE"));
		assertTrue(source.indexOf("COMMENT ON SEQUENCE SEQ_AAA IS 'aaa comment'") > -1);
	}

	public void testDB2Host()
	{
		Db2SequenceReader reader = new Db2SequenceReader(db, "db2h");
		reader.setQuoteKeyword(true);
		List<SequenceDefinition> result = reader.getSequences("FAKE_DB2", null);
		Collections.sort(result, new Comparator<DbObject>()
		{
			public int compare(DbObject o1, DbObject o2)
			{
				return StringUtil.compareStrings(o1.getObjectName(), o2.getObjectName(), true);
			}
		});

		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals("SEQ_AAA", result.get(0).getSequenceName());
		assertEquals("SEQ_BBB", result.get(1).getSequenceName());

		CharSequence sql = result.get(0).getSource();
		assertNotNull(sql);
		String source = sql.toString();

		assertTrue(source.startsWith("CREATE SEQUENCE"));
		assertTrue(source.indexOf("COMMENT ON SEQUENCE SEQ_AAA IS 'aaa comment'") > -1);
	}

}
