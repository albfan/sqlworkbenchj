/*
 * OracleTablePartitionTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleTablePartitionTest
	extends WbTestCase
{
	public OracleTablePartitionTest()
	{
		super("OraclePartitionReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		String sql = 
			"CREATE TABLE wb_list_partition_test \n" +
			"( \n" +
			"  test_id integer not null primary key, \n" +
			"  tenant_id integer not null \n" +
			") \n" +
			"PARTITION BY LIST (tenant_id)  \n" +
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
			");";
		
		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;
		TestUtil.executeScript(con, sql);
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
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;
		
		TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("WB_LIST_PARTITION_TEST"));
		assertNotNull(tbl);
		OracleTablePartition reader = new OracleTablePartition(tbl, con);
		assertTrue(reader.isPartitioned());
		assertEquals("LIST", reader.getPartitionType());
		List<String> columns = reader.getColumns();
		assertEquals(1, columns.size());
		List<OraclePartitionDefinition> partitions = reader.getPartitions();
		assertEquals(4, partitions.size());
		for (int i=0; i < 4; i++)
		{
			assertEquals("WB_LIST_PART_" + Integer.toString(i+1), partitions.get(i).getName());
			assertEquals(i + 1, partitions.get(i).getPosition());
		}
		assertEquals("1", partitions.get(0).getPartitionValue());
		assertEquals("4", partitions.get(1).getPartitionValue());
		assertEquals("8", partitions.get(2).getPartitionValue());
		assertEquals("16", partitions.get(3).getPartitionValue());
		String expected =	"PARTITION BY LIST (TENANT_ID)\n" +
			"(\n" +
			"  PARTITION WB_LIST_PART_1 VALUES (1),\n" +
			"  PARTITION WB_LIST_PART_2 VALUES (4),\n" +
			"  PARTITION WB_LIST_PART_3 VALUES (8),\n" +
			"  PARTITION WB_LIST_PART_4 VALUES (16)\n" +
			")";
		assertEquals(expected, reader.getSource().trim());
	}
	
	@Test
	public void testRetrieveHashPartition()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;
		
		TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("WB_HASH_PARTITION_TEST"));
		assertNotNull(tbl);
		OracleTablePartition reader = new OracleTablePartition(tbl, con);
		assertTrue(reader.isPartitioned());
		assertEquals("HASH", reader.getPartitionType());
		List<String> columns = reader.getColumns();
		assertEquals(2, columns.size());
		List<OraclePartitionDefinition> partitions = reader.getPartitions();
		assertEquals(5, partitions.size());
		for (int i=0; i < 5; i++)
		{
			assertEquals("WB_HASH_PART_" + Integer.toString(i+1), partitions.get(i).getName());
			assertEquals(i + 1, partitions.get(i).getPosition());
			assertNull(partitions.get(i).getPartitionValue());
		}
		String expected =	"PARTITION BY HASH (TENANT_ID, REGION_ID)\n" +
			"(\n" +
			"  PARTITION WB_HASH_PART_1,\n" +
			"  PARTITION WB_HASH_PART_2,\n" +
			"  PARTITION WB_HASH_PART_3,\n" +
			"  PARTITION WB_HASH_PART_4,\n" +
			"  PARTITION WB_HASH_PART_5\n" +
			")";
		assertEquals(expected, reader.getSource().trim());		
	}	
}