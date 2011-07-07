/*
 * OracleIndexPartitionTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */

package workbench.db.oracle;

import java.sql.SQLException;
import java.util.List;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.db.IndexDefinition;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleIndexPartitionTest
	extends WbTestCase
{
	private static boolean partitioningAvailable;

	public OracleIndexPartitionTest()
	{
		super("OraclePartitionedIndexTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		String sql =
			"CREATE TABLE wb_list_partition_test \n" +
			"( \n" +
			"  test_id integer not null, \n" +
			"  tenant_id integer not null \n" +
			") \n" +
			"PARTITION BY LIST (test_id)  \n" +
			"( \n" +
			"  PARTITION wb_list_part_1  VALUES (1),  \n" +
			"  PARTITION wb_list_part_2  VALUES (4),  \n" +
			"  PARTITION wb_list_part_3  VALUES (8), \n" +
			"  PARTITION wb_list_part_4  VALUES (16) \n" +
			");  \n" +
			"\n" +
			"CREATE TABLE wb_hash_partition_test \n" +
			"( \n" +
			"  test_id integer not null primary key, \n" +
			"  tenant_id integer not null, \n" +
			"  region_id integer not null \n" +
			") \n" +
			"PARTITION BY HASH (tenant_id, region_id)  \n" +
			"( \n" +
			"  PARTITION wb_hash_part_1,  \n" +
			"  PARTITION wb_hash_part_2,  \n" +
			"  PARTITION wb_hash_part_3, \n" +
			"  PARTITION wb_hash_part_4, \n" +
			"  PARTITION wb_hash_part_5 \n" +
			");\n" +
			"CREATE INDEX part_list_idx ON wb_list_partition_test (tenant_id)  \n" +
			 "LOCAL \n" +
			 "( \n" +
			 "  PARTITION wb_list_part_1,\n" +
			 "  PARTITION wb_list_part_2,\n" +
			 "  PARTITION wb_list_part_3,\n" +
			 "  PARTITION wb_list_part_4 \n" +
			 ")";

		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;
		try
		{
			TestUtil.executeScript(con, sql, false);
			partitioningAvailable = true;
		}
		catch (SQLException e)
		{
			if (e.getErrorCode() == 439)
			{
				partitioningAvailable = false;
			}
			else
			{
				throw e;
			}
		}
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testRetrieveListPartition()
		throws Exception
	{
		if (!partitioningAvailable) return;
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		IndexDefinition def = new IndexDefinition(new TableIdentifier(OracleTestUtil.SCHEMA_NAME, "WB_LIST_PARTITION_TEST"), "PART_LIST_IDX");
		OracleIndexPartition reader = new OracleIndexPartition(con);
		reader.retrieve(def, con);
		assertTrue(reader.isPartitioned());
		List<String> cols = reader.getColumns();
		assertEquals(1, cols.size());
		List<OraclePartitionDefinition> parts = reader.getPartitions();
		assertEquals(4, parts.size());
		assertEquals("LOCAL", reader.getLocality());
		String expected = "LOCAL\n" +
             "PARTITION BY LIST (TEST_ID)\n" +
             "(\n" +
             "  PARTITION WB_LIST_PART_1,\n" +
             "  PARTITION WB_LIST_PART_2,\n" +
             "  PARTITION WB_LIST_PART_3,\n" +
             "  PARTITION WB_LIST_PART_4\n" +
             ")";
		assertEquals(expected, reader.getSourceForIndexDefinition().trim());
	}

}