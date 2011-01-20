/*
 * WbListVarsTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.wbcommands;

import org.junit.Before;
import org.junit.AfterClass;
import org.junit.Test;
import workbench.WbTestCase;
import workbench.sql.StatementRunnerResult;
import workbench.sql.VariablePool;
import workbench.storage.DataStore;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbListVarsTest
	extends WbTestCase
{

	public WbListVarsTest()
	{
		super("WbListVarsTest");
	}

	@AfterClass
	public static void tearDown()
	{
		VariablePool.getInstance().clear();
	}

	@Before
	public void setup()
	{
		VariablePool.getInstance().clear();
	}

	@Test
	public void testExecute()
		throws Exception
	{
		VariablePool.getInstance().setParameterValue("myvar", "42");
		VariablePool.getInstance().setParameterValue("another", "Arthur");
		WbListVars list = new WbListVars();
		assertFalse(list.isConnectionRequired());
		StatementRunnerResult result = list.execute(list.getVerb());
		assertNotNull(result);
		assertTrue(result.hasDataStores());
		assertEquals(1, result.getDataStores().size());
		DataStore ds = result.getDataStores().get(0);
		assertEquals(2, ds.getRowCount());

		// The result is sorted alphabetically
		assertEquals("another", ds.getValueAsString(0, 0));
		assertEquals("Arthur", ds.getValueAsString(0, 1));
		assertEquals("myvar", ds.getValueAsString(1, 0));
		assertEquals("42", ds.getValueAsString(1, 1));
	}
	
}
