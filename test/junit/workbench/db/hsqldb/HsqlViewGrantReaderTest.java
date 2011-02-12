/*
 * HsqlViewGrantReaderTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.hsqldb;

import workbench.resource.Settings;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.TableGrant;
import workbench.db.TableIdentifier;
import workbench.db.ViewGrantReader;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class HsqlViewGrantReaderTest
	extends WbTestCase
{

	public HsqlViewGrantReaderTest()
	{
		super("HsqlViewGrantReaderTest");
	}

	@Test
	public void testGetViewGrantSql()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getHSQLConnection("viewgranttest");
		con.getMetadata().clearIgnoredSchemas();
		TestUtil.executeScript(con,
			"create user someone password 'welcome';\n" +
			"create table person (id integer, name varchar(100));\n" +
			"create view v_person as select * from person;\n" +
			"grant select on v_person to someone;\n" +
			"commit;"
			);

		TableIdentifier view = new TableIdentifier("PUBLIC", "V_PERSON");
		view.setType("VIEW");
		assertEquals("V_PERSON", view.getTableName());
		ViewGrantReader reader = ViewGrantReader.createViewGrantReader(con);
		Collection<TableGrant> grants = reader.getViewGrants(con, view);
		int grantCount = 0;
		for (TableGrant grant : grants)
		{
			if ("SOMEONE".equals(grant.getGrantee()))
			{
				assertEquals("SELECT", grant.getPrivilege());
				grantCount ++;
			}
		}
		assertEquals(1, grantCount);
	}

}