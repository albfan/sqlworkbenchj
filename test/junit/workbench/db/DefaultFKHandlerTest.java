/*
 * DefaultFKHandlerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;
import java.util.List;
import org.junit.AfterClass;

import org.junit.Test;
import static org.junit.Assert.*;import org.junit.BeforeClass;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.storage.DataStore;


/**
 *
 * @author Thomas Kellerer
 */
public class DefaultFKHandlerTest
	extends WbTestCase
{
	public DefaultFKHandlerTest()
	{
		super("DefaultFKHandlerTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{

	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
	}

	@Test
	public void testGetForeignKeys()
		throws Exception
	{
		try
		{
			TestUtil util = getTestUtil();
			WbConnection con = util.getConnection();
			TestUtil.executeScript(con,
				"create table pk_parent (id integer not null primary key);\n" +
				"create table pk_child ("  +
				"   id integer primary key, " +
				"   parent_id integer not null, " +
				"   constraint fk_parent_child foreign key (parent_id) references pk_parent (id) on delete cascade on update restrict" +
				");\n" +
				"commit;");

			FKHandler handler = FKHandlerFactory.createInstance(con);
			TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("PK_CHILD"));
			assertNotNull(tbl);
			DataStore fk = handler.getForeignKeys(tbl, false);
			assertNotNull(fk);
			assertEquals(1, fk.getRowCount());
			String deleteAction = fk.getValueAsString(0, FKHandler.COLUMN_IDX_FK_DEF_DELETE_RULE);
			assertEquals("CASCADE", deleteAction);
			String updateAction = fk.getValueAsString(0, FKHandler.COLUMN_IDX_FK_DEF_UPDATE_RULE);
			assertEquals("RESTRICT", updateAction);

			TestUtil.executeScript(con,
				"drop table pk_child;\n" +
				"create table pk_child ("  +
				"   id integer primary key, " +
				"   parent_id integer not null, " +
				"   constraint fk_parent_child foreign key (parent_id) references pk_parent (id) on delete no action on update cascade" +
				");\n" +
				"commit;");

			fk = handler.getForeignKeys(tbl, false);
			deleteAction = fk.getValueAsString(0, FKHandler.COLUMN_IDX_FK_DEF_DELETE_RULE);
			assertEquals("RESTRICT", deleteAction);
			updateAction = fk.getValueAsString(0, FKHandler.COLUMN_IDX_FK_DEF_UPDATE_RULE);
			assertEquals("CASCADE", updateAction);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

}
