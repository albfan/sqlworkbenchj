/*
 * WbGenerateScriptTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.wbcommands;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.resource.DbExplorerSettings;

import workbench.db.WbConnection;

import workbench.sql.StatementRunnerResult;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbGenerateScriptTest
	extends WbTestCase
{
	public WbGenerateScriptTest()
	{
		super("WbGenerateScriptTest");
	}

	@Test
	public void testExecute()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection conn = util.getHSQLConnection("genscript");

		String sql =
			"create table customer (cust_id integer not null primary key);\n" +
			"create table orders (order_id integer not null primary key, cust_id integer not null);\n" +
			"create table order_item (item_id integer not null primary key, order_id integer not null, currency_id integer not null);\n" +
			"create table currency (currency_id integer not null primary key);\n" +
			"create table invoice (invoice_id integer not null primary key, order_id integer not null);\n" +
			"alter table orders add constraint fk_orders_cust foreign key (cust_id) references customer (cust_id);\n" +
			"alter table order_item add constraint fk_oi_orders foreign key (order_id) references orders(order_id);\n" +
			"alter table order_item add constraint fk_oi_currency foreign key (currency_id) references currency(currency_id);\n" +
			"alter table invoice add constraint fk_inv_order foreign key (order_id) references orders (order_id);\n" +
			"create view v_order_items \n" +
			"as \n " +
			"select o.order_id, \n" +
			"       o.cust_id, \n" +
			"       oi.currency_id \n" +
			"from orders o \n" +
			"  join order_item oi on oi.order_id = o.order_id;\n" +
			"commit;\n";
		TestUtil.executeScript(conn, sql);

		boolean oldGenProp = DbExplorerSettings.getAutoGeneratePKName();
		DbExplorerSettings.setAutoGeneratePKName(true);

		try
		{
			WbGenerateScript genScript = new WbGenerateScript();
			genScript.setConnection(conn);

			StatementRunnerResult result = null;
			String script = null;

			result = genScript.execute("WbGenerateScript -types=view");
			script = result.getMessageBuffer().toString();
			assertNotNull(script);
			assertTrue(script.toLowerCase().startsWith("create view v_order_items"));

			result = genScript.execute("WbGenerateScript -objects=customer,orders");
			script = result.getMessageBuffer().toString();
			assertTrue(script.contains("CREATE TABLE CUSTOMER"));
			assertTrue(script.contains("ADD CONSTRAINT pk_customer"));

			assertTrue(script.contains("CREATE TABLE ORDERS"));
			assertTrue(script.contains("ADD CONSTRAINT pk_orders"));
			assertTrue(script.contains("ADD CONSTRAINT FK_ORDERS_CUST"));

			result = genScript.execute("WbGenerateScript invoice");
			script = result.getMessageBuffer().toString();

			assertTrue(script.contains("CREATE TABLE INVOICE"));
			assertTrue(script.contains("ADD CONSTRAINT pk_invoice"));
			assertTrue(script.contains("ADD CONSTRAINT FK_INV_ORDER"));

			result = genScript.execute("WbGenerateScript -objects=o%");
			script = result.getMessageBuffer().toString();

			assertTrue(script.contains("CREATE TABLE ORDERS"));
			assertTrue(script.contains("CREATE TABLE ORDER_ITEM"));
			assertFalse(script.contains("CREATE TABLE CUSTOMER"));
			assertFalse(script.contains("CREATE TABLE INVOICE"));
			assertFalse(script.contains("CREATE TABLE CURRENCY"));

			result = genScript.execute("WbGenerateScript -types=table -objects=* -exclude=*ord*");
			script = result.getMessageBuffer().toString();
			assertTrue(script.contains("CREATE TABLE CUSTOMER"));
			assertTrue(script.contains("CREATE TABLE INVOICE"));
			assertTrue(script.contains("CREATE TABLE CURRENCY"));
			assertFalse(script.contains("CREATE TABLE ORDERS"));
			assertFalse(script.contains("CREATE TABLE ORDER_ITEM"));
		}
		finally
		{
			DbExplorerSettings.setAutoGeneratePKName(oldGenProp);
		}
	}

}