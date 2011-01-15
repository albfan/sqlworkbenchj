/*
 * WbLoadPkMappingTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Map;
import org.junit.AfterClass;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.storage.PkMapping;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class WbLoadPkMappingTest
	extends WbTestCase
{

	public WbLoadPkMappingTest()
	{
		super("WbLoadPkMappingTest");
	}

	@AfterClass
	public static void tearDown()
	{
		PkMapping.getInstance().clear();
	}

	@Test
	public void testExecute()
		throws Exception
	{
		TestUtil util = getTestUtil();
		StatementRunner runner;

		try
		{
			util.prepareEnvironment();
			runner = util.createConnectedStatementRunner();

			File f = new File(util.getBaseDir(), "pkmapping.def");
			PrintWriter w = new PrintWriter(new FileWriter(f));
			w.println("junitpk=id,name");
			w.close();

			String sql = "-- load mapping from a file \n     " + WbLoadPkMapping.FORMATTED_VERB + "\n -file='" + f.getAbsolutePath() + "'";
			SqlCommand command = runner.getCommandToUse(sql);
			assertTrue(command instanceof WbLoadPkMapping);
			runner.runStatement(sql);
			StatementRunnerResult result = runner.getResult();
			assertEquals("Loading not successful", true, result.isSuccess());

			Map mapping = PkMapping.getInstance().getMapping();
			String cols = (String)mapping.get("junitpk");
			assertEquals("Wrong pk mapping stored", "id,name", cols);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

}
