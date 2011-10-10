/*
 * DropScriptGeneratorTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import java.util.List;
import org.junit.*;
import workbench.sql.ScriptParser;
import static org.junit.Assert.*;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class DropScriptGeneratorTest
	extends WbTestCase
{

	public DropScriptGeneratorTest()
	{
		super("DropScriptGeneratorTest");
	}

	@Test
	public void testGetScript()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection conn = util.getConnection();

		String sql =
			"create table customer (cust_id integer not null primary key);\n" +
			"create table orders (order_id integer not null primary key, cust_id integer not null);\n" +
			"create table order_item (item_id integer not null primary key, order_id integer not null, currency_id integer not null);\n" +
			"create table currency (currency_id integer not null primary key);\n" +
			"create table delivery (deliv_id integer not null primary key, item_id integer not null);\n" +
			"create table invoice (invoice_id integer not null primary key, order_id integer not null);\n" +
			"alter table orders add constraint fk_orders_cust foreign key (cust_id) references customer (cust_id);\n" +
			"alter table order_item add constraint fk_oi_orders foreign key (order_id) references orders(order_id);\n" +
			"alter table order_item add constraint fk_oi_currency foreign key (currency_id) references currency(currency_id);\n" +
			"alter table delivery add constraint fk_del_oi foreign key (item_id) references order_item (item_id);\n" +
			"alter table invoice add constraint fk_inv_order foreign key (order_id) references orders (order_id);\n" +
			"commit;\n";
		TestUtil.executeScript(conn, sql);

		DropScriptGenerator generator = new DropScriptGenerator(conn);

		TableIdentifier cust = conn.getMetadata().findTable(new TableIdentifier("CUSTOMER"));
		generator.setTable(cust);
//		String result = generator.getScript();
//		System.out.println(result);
		List<String> drop = generator.getDropConstraintStatements(cust);
		assertEquals(1, drop.size());
		assertEquals("ALTER TABLE ORDERS DROP CONSTRAINT FK_ORDERS_CUST;", drop.get(0));

		List<String> restore = generator.getRestoreStatements(cust);
		assertEquals(1, restore.size());
		assertTrue(restore.get(0).startsWith("ALTER TABLE ORDERS"));
		assertTrue(restore.get(0).contains("ADD CONSTRAINT FK_ORDERS_CUST FOREIGN KEY (CUST_ID)"));

		TableIdentifier orders = conn.getMetadata().findTable(new TableIdentifier("ORDERS"));
		generator.setTable(orders);
//		String ordersScript = generator.getScript();
//		System.out.println(ordersScript);

		drop = generator.getDropConstraintStatements(orders);
		assertEquals(2, drop.size());
		assertEquals("ALTER TABLE INVOICE DROP CONSTRAINT FK_INV_ORDER;", drop.get(0));
		assertEquals("ALTER TABLE ORDER_ITEM DROP CONSTRAINT FK_OI_ORDERS;", drop.get(1));

		restore = generator.getRestoreStatements(orders);
		assertEquals(3, restore.size());
		assertTrue(restore.get(0).contains("ADD CONSTRAINT FK_INV_ORDER FOREIGN KEY (ORDER_ID)"));
		assertTrue(restore.get(1).contains("ADD CONSTRAINT FK_OI_ORDERS FOREIGN KEY (ORDER_ID)"));
		assertTrue(restore.get(2).startsWith("ALTER TABLE ORDERS"));
		assertTrue(restore.get(2).contains("ADD CONSTRAINT FK_ORDERS_CUST FOREIGN KEY (CUST_ID)"));

		TableIdentifier orderItem = conn.getMetadata().findTable(new TableIdentifier("ORDER_ITEM"));
		generator.setTable(orderItem);
//		String itemScript = generator.getScript();
//		System.out.println(itemScript);

		drop = generator.getDropConstraintStatements(orderItem);
		assertEquals(1, drop.size());
		assertEquals("ALTER TABLE DELIVERY DROP CONSTRAINT FK_DEL_OI;", drop.get(0));

		restore = generator.getRestoreStatements(orderItem);
		assertEquals(3, restore.size());
		assertTrue(restore.get(0).startsWith("ALTER TABLE DELIVERY"));
		assertTrue(restore.get(0).contains("ADD CONSTRAINT FK_DEL_OI FOREIGN KEY (ITEM_ID)"));

		assertTrue(restore.get(1).startsWith("ALTER TABLE ORDER_ITEM"));
		assertTrue(restore.get(1).contains("ADD CONSTRAINT FK_OI_CURRENCY FOREIGN KEY (CURRENCY_ID)"));

		assertTrue(restore.get(2).startsWith("ALTER TABLE ORDER_ITEM"));
		assertTrue(restore.get(2).contains("ADD CONSTRAINT FK_OI_ORDERS FOREIGN KEY (ORDER_ID)"));

		generator.setTables(CollectionUtil.arrayList(cust,orders,orderItem));
		String custScript = generator.getScriptFor(cust);
//		System.out.println(custScript);

		ScriptParser p = new ScriptParser(custScript);
		p.setScript(custScript);
		int count = p.getSize();
		assertEquals(3, count);
		assertEquals("ALTER TABLE ORDERS DROP CONSTRAINT FK_ORDERS_CUST", p.getCommand(0));
		assertEquals("DROP TABLE CUSTOMER", p.getCommand(1));
		assertTrue(p.getCommand(2).startsWith("ALTER TABLE ORDERS"));
		assertTrue(p.getCommand(2).contains("ADD CONSTRAINT FK_ORDERS_CUST FOREIGN KEY (CUST_ID)"));
	}

}
