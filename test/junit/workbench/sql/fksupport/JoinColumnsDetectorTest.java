/*
 * JoinColumnsDetectorTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.fksupport;

import org.junit.AfterClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.resource.Settings;
import workbench.util.TableAlias;
import static org.junit.Assert.*;

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
		throws Exception
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
			"create table address (adr_id integer primary key, address varchar(50), person_id integer, person_tenant_id integer, foreign key (person_id, person_tenant_id) references person(per_id, tenant_id));\n" +
			"create table address_history (ahi_id integer primary key, old_address varchar(50), address_id integer, foreign key (address_id) references address(adr_id));\n" +
			"commit;"
		);

		TableAlias person = new TableAlias("person p");
		TableAlias address = new TableAlias("address a");
		TableAlias history = new TableAlias("address_history ah");
		JoinColumnsDetector detector = new JoinColumnsDetector(conn, person, address);
		Settings.getInstance().setAutoCompletionPasteCase("lower");
		String join = detector.getJoinCondition();
		assertEquals("p.tenant_id = a.person_tenant_id AND p.per_id = a.person_id", join.trim());
		
		detector = new JoinColumnsDetector(conn, address, history);
		join = detector.getJoinCondition();
		assertEquals("a.adr_id = ah.address_id", join.trim());
	}
}
