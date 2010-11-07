/*
 * H2DomainReaderTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.h2database;

import java.util.List;
import org.junit.AfterClass;
import workbench.db.TableIdentifier;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.db.DomainIdentifier;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class H2DomainReaderTest
	extends WbTestCase
{

	public H2DomainReaderTest()
	{
		super("H2DomainReaderTest");
	}

	@AfterClass
	public static void tearDown()
	{
		ConnectionMgr.getInstance().disconnectAll();
	}
	
	@Test
	public void testGetDomainList()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();

		String script = "CREATE DOMAIN positive_integer AS integer NOT NULL CHECK (value > 0);";
		TestUtil.executeScript(con, script);

		List<TableIdentifier> objects = con.getMetadata().getObjectList(null, new String[] { "DOMAIN" });
		assertNotNull(objects);
		assertEquals(1, objects.size());

		H2DomainReader reader = new H2DomainReader();
		List<DomainIdentifier> domains = reader.getDomainList(con, null, null);
		assertNotNull(domains);
		assertEquals(1, domains.size());

		DomainIdentifier domain = domains.get(0);
		assertEquals("(VALUE > 0)", domain.getCheckConstraint());
		assertEquals("INTEGER", domain.getDataType());
		assertFalse(domain.isNullable());
		String sql = domain.getSource(con).toString();
		assertEquals("CREATE DOMAIN POSITIVE_INTEGER AS INTEGER\n   CHECK NOT NULL (VALUE > 0);", sql.trim());
	}

}
