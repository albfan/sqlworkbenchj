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
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.TestUtil;
import workbench.resource.ResourceMgr;

/**
 *
 * @author support@sql-workbench.net
 */
public class LobFileStatementTest extends TestCase
{
	private TestUtil util;

	public LobFileStatementTest(String testName)
	{
		super(testName);
		try
		{
			util = new TestUtil();
			util.prepareBaseDir();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
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
