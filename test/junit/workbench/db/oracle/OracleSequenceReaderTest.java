/*
 * OracleSequenceReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

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
 * @author support@sql-workbench.net
 */
public class OracleSequenceReaderTest
	extends TestCase
{
	private WbConnection db;
	
	public OracleSequenceReaderTest(String testName)
	{
		super(testName);
	}

	@Override
	protected void setUp()
		throws Exception
	{
		super.setUp();
		TestUtil util = new TestUtil(getName());
		db = util.getHSQLConnection("fake_ora");
		TestUtil.executeScript(db,
			"create sequence seq_aaa;\n" +
			"create sequence seq_bbb;\n" +
			"commit;\n" +
			"create view all_sequences \n" +
			"AS \n" +
			"select 'PUBLIC' AS SEQUENCE_OWNER,  \n" +
			"       sequence_name,  \n" +
			"       cast(minimum_value as integer) as min_value, \n" +
			"       cast(maximum_value as bigint) as max_value, \n" +
			"       cycle_option as cycle_flag, \n" +
			"       'N' as order_flag, \n" +
			"       0 as cache_size,  \n" +
			"       0 as last_number, \n " +
			"       cast(increment as integer) as increment_by \n" +
			"from information_schema.system_sequences; \n" +
			"commit;\n"
		);
	}

	@Override
	protected void tearDown()
		throws Exception
	{
		db.disconnect();
	}

	public void testGetSequenceList()
		throws Exception
	{
		OracleSequenceReader instance = new OracleSequenceReader(db);
		List<String> result = instance.getSequenceList("PUBLIC");
		Collections.sort(result);
		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals("SEQ_AAA", result.get(0));
		assertEquals("SEQ_BBB", result.get(1));
	}

	public void testGetSequences()
	{
		OracleSequenceReader instance = new OracleSequenceReader(db);
		List<SequenceDefinition> result = instance.getSequences("PUBLIC");
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
		assertTrue(sql.toString().startsWith("CREATE SEQUENCE"));
	}
}
