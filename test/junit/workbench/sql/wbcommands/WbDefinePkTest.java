/*
 * WbDefinePkTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.util.Map;
import workbench.TestUtil;
import workbench.sql.StatementRunner;
import workbench.sql.SqlCommand;
import workbench.storage.PkMapping;
import static org.junit.Assert.*;
import org.junit.Test;
import workbench.WbTestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class WbDefinePkTest
	extends WbTestCase
{

	public WbDefinePkTest()
	{
		super("WbDefinePkTest");
	}

	@Test
	public void testExecute()
		throws Exception
	{
		TestUtil util = new TestUtil(getClass().getName() + "_testExecute");
		util.prepareEnvironment();
		StatementRunner runner = util.createConnectedStatementRunner();

		String sql = "--define a new PK for a view\nwbdefinepk junitpk=id,name";
		SqlCommand command = runner.getCommandToUse(sql);
		assertTrue(command instanceof WbDefinePk);
		runner.runStatement(sql);

		Map mapping = PkMapping.getInstance().getMapping();
		String cols = (String) mapping.get("junitpk");
		assertEquals("Wrong pk mapping stored", "id,name", cols);

	}
}
