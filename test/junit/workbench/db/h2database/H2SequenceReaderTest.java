/*
 * H2SequenceReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.h2database;

import org.junit.Test;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.DbObject;
import workbench.db.SequenceDefinition;
import workbench.db.WbConnection;
import workbench.util.StringUtil;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class H2SequenceReaderTest
	extends WbTestCase
{
	private WbConnection db;

	public H2SequenceReaderTest()
	{
		super("H2SequenceReaderTest");
	}


	@Before
	public void setUp()
		throws Exception
	{
		TestUtil util = new TestUtil(getName());
		db = util.getConnection("h2_seq_test");
		TestUtil.executeScript(db,
			"create sequence seq_aaa;\n" +
			"create sequence seq_bbb;\n" +
			"commit;");
	}

	@After
	public void tearDown()
		throws Exception
	{
		db.disconnect();
	}

	@Test
	public void testGetSequences()
	{
		H2SequenceReader instance = new H2SequenceReader(db.getSqlConnection());
		List<SequenceDefinition> result = instance.getSequences(null, "PUBLIC", null);
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
