/*
 * IngresMetadataTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.ingres;

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
public class IngresMetadataTest
	extends TestCase
{

	private WbConnection db;

	public IngresMetadataTest(String testName)
	{
		super(testName);
	}

	@Override
	protected void setUp()
		throws Exception
	{
		super.setUp();
		TestUtil util = new TestUtil(getName());
		db = util.getConnection("fake_ingres");
		TestUtil.executeScript(db,
			"create view iisequences \n" +
			"AS \n" +
			"select sequence_name as seq_name,  \n" +
			"       sequence_schema as seq_owner,  \n" +
			"       0 as min_value,  \n" +
			"       9999999 as max_value,  \n" +
			"       increment as increment_value, \n" +
			"       'N' as cycle_flag, \n" +
			"       'N' as order_flag,  \n" +
			"       cache as cache_size \n" +
			"from information_schema.sequences; \n" +
			"create sequence seq_aaa;\n" +
			"create sequence seq_bbb;\n" +
			"commit;");
	}

	@Override
	protected void tearDown()
		throws Exception
	{
		db.disconnect();
	}

	public void testGetSequenceList()
	{
		IngresMetadata instance = new IngresMetadata(db.getSqlConnection());
		List<String> result = instance.getSequenceList("PUBLIC");
		Collections.sort(result);
		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals("SEQ_AAA", result.get(0));
		assertEquals("SEQ_BBB", result.get(1));
	}

	public void testGetSequences()
	{
		IngresMetadata instance = new IngresMetadata(db.getSqlConnection());
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
