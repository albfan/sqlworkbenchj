/*
 * WbDefinePkTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.util.Map;
import junit.framework.*;
import workbench.TestUtil;
import workbench.sql.StatementRunner;
import workbench.sql.SqlCommand;
import workbench.storage.PkMapping;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbDefinePkTest extends TestCase
{
	
	public WbDefinePkTest(String testName)
	{
		super(testName);
	}

	public void testExecute() throws Exception
	{
		StatementRunner runner;
		
		try
		{
			TestUtil util = new TestUtil(getClass().getName()+"_testExecute");
			util.prepareEnvironment();
			runner = util.createConnectedStatementRunner();
			
			String sql = "--define a new PK for a view\nwbdefinepk junitpk=id,name";
			SqlCommand command = runner.getCommandToUse(sql);
			assertTrue(command instanceof WbDefinePk);
			runner.runStatement(sql, -1, -1);

			Map mapping = PkMapping.getInstance().getMapping();
			String cols = (String)mapping.get("junitpk");
			assertEquals("Wrong pk mapping stored", "id,name", cols);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
}
