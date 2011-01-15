/*
 * DbmsOutputTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import workbench.util.StringUtil;
import java.sql.Statement;
import workbench.util.SqlUtil;
import org.junit.AfterClass;
import org.junit.Test;
import workbench.WbTestCase;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class DbmsOutputTest
	extends WbTestCase
{
	public DbmsOutputTest()
	{
		super("DbmsOutputTest");
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testOutput()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		Statement stmt = null;
		try
		{
			stmt = con.createStatement();
			DbmsOutput output = new DbmsOutput(con.getSqlConnection());
			output.enable(-1);
			stmt.execute("begin\n dbms_output.put_line('Hello, World'); end;");
			String out = output.getResult();
			assertEquals("Hello, World", out.trim());

			output.disable();
			stmt.execute("begin\n dbms_output.put_line('Hello, World'); end;");
			out = output.getResult();
			assertTrue(StringUtil.isEmptyString(out));

			output.close();
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
	}
}
