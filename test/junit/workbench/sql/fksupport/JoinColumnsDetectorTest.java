/*
 * JoinColumnsDetectorTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.sql.fksupport;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.resource.Settings;

import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;

import workbench.util.TableAlias;

import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static workbench.resource.GeneratedIdentifierCase.*;

/**
 *
 * @author Thomas Kellerer
 */
public class JoinColumnsDetectorTest
	extends WbTestCase
{

	public JoinColumnsDetectorTest()
	{
		super("JoinColumnsDetectorTest");
	}

	@AfterClass
	public static void tearDownClass()
	{
		ConnectionMgr.getInstance().disconnectAll();
	}

	@Test
	public void testGetJoinSQL()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection conn = util.getConnection();
		TestUtil.executeScript(conn,
			"create table person (per_id integer not null, tenant_id integer not null, person_name varchar(10), primary key (per_id, tenant_id));\n" +
			"create table address_type (type_id integer primary key, type_name varchar(50));\n" +
			"create table address \n" +
			"( \n" +
			"   adr_id integer primary key, \n" +
			"   address varchar(50), \n " +
			"   person_id integer, \n "+
			"   person_tenant_id integer, \n" +
			"   adr_type_id integer, \n" +
			"   foreign key (person_id, person_tenant_id) \n" +
			"      references person(per_id, tenant_id), \n" +
			"  foreign key (adr_type_id) references address_type (type_id) \n" +
			");\n" +
			"create table address_history " +
			 "( \n " +
			"    ahi_id integer primary key, \n  " +
			"    old_address varchar(50), \n " +
			"    address_id integer, \n " +
			"    foreign key (address_id) references address(adr_id)\n" +
			");\n" +
			"commit;"
		);

		TableAlias person = new TableAlias("person p");
		TableAlias address = new TableAlias("address a");
		TableAlias history = new TableAlias("address_history ah");
		TableAlias adt = new TableAlias("address_type adt");
		JoinColumnsDetector detector = new JoinColumnsDetector(conn, person, address);
		Settings.getInstance().setAutoCompletionPasteCase(lower);
		String join = detector.getJoinCondition();
		assertEquals("p.tenant_id = a.person_tenant_id AND p.per_id = a.person_id", join.trim());

		detector = new JoinColumnsDetector(conn, address, history);
		join = detector.getJoinCondition();
		assertEquals("a.adr_id = ah.address_id", join.trim());

		detector = new JoinColumnsDetector(conn, address, adt);
		join = detector.getJoinCondition();
		assertEquals("adt.type_id = a.adr_type_id", join.trim());
	}
}
