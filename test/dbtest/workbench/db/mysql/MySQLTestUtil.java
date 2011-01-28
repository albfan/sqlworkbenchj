/*
 * MySQLTestUtil.java
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.mysql;

import workbench.AppArguments;
import workbench.TestUtil;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.sql.BatchRunner;
import workbench.util.ArgumentParser;

/**
 *
 * @author Thomas Kellerer
 */
public class MySQLTestUtil
{

	public static final String TEST_USER = "wbjunit";
	public static final String TEST_PWD = "wbjunit";
	public static final String PROFILE_NAME = "WBJUnitMySQL";
	public static final String DB_NAME = "wbjunit";

	/**
	 * Return a connection to a locally running MySQL database
	 */
	public static WbConnection getMySQLConnection()
	{
		try
		{
			WbConnection con = ConnectionMgr.getInstance().findConnection(PROFILE_NAME);
			if (con != null) return con;

			ArgumentParser parser = new AppArguments();
			parser.parse("-url='jdbc:mysql://localhost/wbjunit' -username=" + TEST_USER + " -password=" + TEST_PWD + " -driver=com.mysql.jdbc.Driver");
			ConnectionProfile prof = BatchRunner.createCmdLineProfile(parser);
			prof.setName(PROFILE_NAME);
			ConnectionMgr.getInstance().addProfile(prof);
			con = ConnectionMgr.getInstance().getConnection(prof, PROFILE_NAME);
			return con;
		}
		catch (Throwable th)
		{
			th.printStackTrace();
			return null;
		}
	}


	public static void initTestcase(String name)
		throws Exception
	{
		TestUtil util = new TestUtil(name);
		util.prepareEnvironment();
	}
}
