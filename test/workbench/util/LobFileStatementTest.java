/*
 * LobFileStatementTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.File;
import junit.framework.*;
import workbench.TestUtil;
import workbench.WbTestCase;

/**
 *
 * @author support@sql-workbench.net
 */
public class LobFileStatementTest 
	extends WbTestCase
{
	private TestUtil util;

	public LobFileStatementTest(String testName)
	{
		super(testName);
		util = getTestUtil();
	}

	public void setUp()
	{
		util.emptyBaseDirectory();
	}

	public void testGetParameterCount()
	{
		File f = new File(util.getBaseDir(), "test.data");
		try
		{
			// LobFileStatement checks for the presence of the file!
			f.createNewFile();
			String sql = "update bla set col = {$blobfile=" + f.getAbsolutePath() + "} where x = 1";
			LobFileStatement stmt = new LobFileStatement(sql);
			assertEquals("Wrong parameter count", 1, stmt.getParameterCount());
			LobFileParameter[] parms = stmt.getParameters();
			assertEquals("Wrong parameter type", true, parms[0].isBinary());

			f = new File(util.getBaseDir(), "some file.data");
			f.createNewFile();

			sql = "update bla set col = {$clobfile='" + f.getAbsolutePath() + "' encoding=utf8} where x = 1";
			stmt = new LobFileStatement(sql);
			assertEquals("Wrong parameter count", 1, stmt.getParameterCount());
			parms = stmt.getParameters();
			assertEquals("Wrong parameter type", false, parms[0].isBinary());
			assertEquals("Wrong encoding", "utf8", parms[0].getEncoding());

			File target = new File(parms[0].getFilename());
			assertEquals("Wrong filename parsed", "some file.data", f.getName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail("could not parse statement");
		}
		finally
		{
			f.delete();
		}
	}


	public void testGetPreparedSql()
	{
		File f = new File(util.getBaseDir(), "test.data");
		try
		{
			// LobFileStatement checks for the presence of the file!
			f.createNewFile();

			String sql = "update bla set col = {$blobfile=" + f.getAbsolutePath() + "} where x = 1";
			LobFileStatement stmt = new LobFileStatement(sql);
			String newSql = stmt.getPreparedSql();
			assertEquals("Wrong SQL generated", "update bla set col =  ?  where x = 1", newSql);

			sql = "update bla set col = {$clobfile='" +  f.getAbsolutePath() + "'} where x = 1";
			stmt = new LobFileStatement(sql);
			assertEquals("Wrong SQL generated", "update bla set col =  ?  where x = 1", stmt.getPreparedSql());

			sql = "update bla set col = {$clobfile='" +  f.getAbsolutePath() + "' encoding='UTF-8'} where x = 1";
			stmt = new LobFileStatement(sql);
			assertEquals("Wrong SQL generated", "update bla set col =  ?  where x = 1", stmt.getPreparedSql());

		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail("could not parse statement");
		}
		finally
		{
			f.delete();
		}
	}


}
