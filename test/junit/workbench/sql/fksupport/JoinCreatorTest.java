/*
 * JoinCreatorTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.fksupport;

import workbench.resource.Settings;
import org.junit.AfterClass;
import workbench.TestUtil;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import org.junit.Test;
import workbench.WbTestCase;
import workbench.util.TableAlias;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class JoinCreatorTest
	extends WbTestCase
{

	public JoinCreatorTest()
	{
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		ConnectionMgr.getInstance().disconnectAll();
	}

	@Test
	public void testJoinCreator()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection conn = util.getConnection();
		TestUtil.executeScript(conn,
			"create table person (per_id integer not null, tenant_id integer not null, person_name varchar(10), primary key (per_id, tenant_id));\n" +
			"create table address_type (type_id integer primary key, type_name varchar(50));\n" +
			"create table address (" +
			"   adr_id integer primary key, \n" +
			"   address varchar(50), \n" +
			"   person_id integer, \n" +
			"   person_tenant_id integer, \n" +
			"   adr_type_id integer, \n" +
			"   foreign key (person_id, person_tenant_id) references person(per_id, tenant_id), \n" +
			"   foreign key (adr_type_id) references address_type(type_id) \n" +
			");\n" +
			"commit;"
		);
		
		String sql = "select * from person p join address a join address_type adt on  ";
		int pos = sql.indexOf("address a") + "address a".length() + 1;
		Settings.getInstance().setAutoCompletionPasteCase("lower");
		JoinCreator creator = new JoinCreator(sql, pos, conn);
		
		TableAlias join = creator.getJoinTable();
		assertEquals("person", join.getObjectName());
		assertEquals("p", join.getNameToUse());
		TableAlias joined = creator.getJoinedTable();
		assertEquals("address", joined.getObjectName());
		assertEquals("a", joined.getNameToUse());
		String condition = creator.getJoinCondition();
		assertEquals("ON p.tenant_id = a.person_tenant_id AND p.per_id = a.person_id", condition);

		pos = sql.indexOf("address_type adt on") + "address_type adt on".length();
		creator.setCursorPosition(pos);
		condition = creator.getJoinCondition();
		assertEquals("a.adr_type_id = adt.type_id", condition);

		creator.setCursorPosition(pos + 1);
		condition = creator.getJoinCondition();
		assertEquals("a.adr_type_id = adt.type_id ", condition);

		// Test for sub-selects
		sql = "select * from person where id in (select person_id from address ad join address_type adt on )";
		pos = sql.indexOf("adt on") + "adt on".length() + 1;
		creator = new JoinCreator(sql, pos, conn);
		join = creator.getJoinTable();
		assertEquals("address", join.getObjectName());
		assertEquals("ad", join.getAlias());
		
		sql = "select person_id from address ad join address_type adt on ";
		pos = sql.length() - 1;
		creator = new JoinCreator(sql, pos, conn);
		join = creator.getJoinTable();
		assertEquals("address", join.getObjectName());
		assertEquals("ad", join.getAlias());
		assertEquals("ON ad.adr_type_id = adt.type_id", creator.getJoinCondition());

//		joined = creator.getJoinedTable();
//		assertEquals("address_type", joined.getObjectName());
//		assertEquals("adt", joined.getAlias());
		
//		sql = "select * from address a join person p on ";
//		pos = sql.length() -1;
//		creator = new JoinCreator(sql, pos, conn);
//		join = creator.getJoinTable();
//		System.out.println("jointable: " + join.getObjectName());
//		System.out.println("condition: " + creator.getJoinCondition());
	}
}
