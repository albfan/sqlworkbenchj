/*
 * FirebirdColumnEnhancerTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.db.firebird;

import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class FirebirdColumnEnhancerTest 
	extends WbTestCase
{

	public FirebirdColumnEnhancerTest()
	{
		super("FirebirdColumnEnhancerTest");
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		FirebirdTestUtil.initTestCase();
		WbConnection con = FirebirdTestUtil.getFirebirdConnection();
		if (con == null) return;

		TestUtil.executeScript(con, "create table foo (id integer, double_id computed by (id * 2));");
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		FirebirdTestUtil.cleanUpTestCase();
	}


	@Test
	public void testUpdateColumnDefinition()
		throws Exception
	{
		WbConnection con = FirebirdTestUtil.getFirebirdConnection();
		if (con == null) return;
		List<TableIdentifier> tables = con.getMetadata().getTableList();
		assertNotNull(tables);
		assertEquals(1, tables.size());

		TableDefinition def = con.getMetadata().getTableDefinition(tables.get(0));
		assertNotNull(def);
		List<ColumnIdentifier> cols = def.getColumns();
		assertNotNull(cols);
		assertEquals(2, cols.size());
		ColumnIdentifier computed = cols.get(1);
		assertEquals("DOUBLE_ID", computed.getColumnName());
		assertEquals("COMPUTED BY (id * 2)", computed.getComputedColumnExpression());
	}

}