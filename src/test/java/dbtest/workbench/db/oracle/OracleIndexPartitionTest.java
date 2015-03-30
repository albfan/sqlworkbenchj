/*
 * OracleIndexPartitionTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import java.sql.SQLException;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.IndexDefinition;
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
		System.setProperty("workbench.db.oracle.partition.index.local.retrieve", "true");
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
		assertNotNull("Oracle not available", con);

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
		System.clearProperty("workbench.db.oracle.partition.index.local.retrieve");
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
		String sql = reader.getSourceForIndexDefinition().trim();
		System.out.println(sql);
		String expected = "LOCAL\n" +
             "(\n" +
             "  PARTITION WB_LIST_PART_1,\n" +
             "  PARTITION WB_LIST_PART_2,\n" +
             "  PARTITION WB_LIST_PART_3,\n" +
             "  PARTITION WB_LIST_PART_4\n" +
             ")";
		assertEquals(expected, sql);
	}

}
