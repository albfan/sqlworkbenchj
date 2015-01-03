/*
 * Db2TestUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.ibm;

import java.sql.Statement;
import java.util.List;

import workbench.AppArguments;
import workbench.TestUtil;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.GenericObjectDropper;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.sql.BatchRunner;

import workbench.util.ArgumentParser;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2TestUtil
{
	private static boolean isAvailable = true;

	public static String getSchemaName()
	{
		return getProperty("wbjunit.db2.schema", "wbjunit").toUpperCase();
	}

	private static String getProperty(String key, String defaultValue)
	{
		String value= System.getProperty(key, null);
		if (value == null || value.equals("${" + key + "}"))
		{
			return defaultValue;
		}
		return value;
	}

	/**
	 * Return a connection to a locally running DB2 database.
	 *
	 * The connection information is obtained from the following system properties:
	 * <ul>
	 * <li>Database name: wbjunit.db2.testdb (default: tkdb)</li>
	 * <li>Database user: wbjunit.db2.user (default: thomas)</li>
	 * <li>Password: wbjunit.db2.password (default: welcome)</li>
	 * <li>Schema: wbjunit.db2.schema (default: wbjunit)</li>
	 * </ul>
	 * The build script (build.xml) will set those system properties from a
	 * file called <tt>db2.test.properties</tt> if it is present.
	 *
	 * @return null if DB2 is not available
	 */
	public static WbConnection getDb2Connection()
	{
		final String id = "WBJUnitDB2";
		if (!isAvailable) return null;
		try
		{
			WbConnection con = ConnectionMgr.getInstance().findConnection(id);
			if (con != null) return con;

			String dbname = getProperty("wbjunit.db2.testdb", "tkdb");
			String username = getProperty("wbjunit.db2.user", "thomas");
			String pwd = getProperty("wbjunit.db2.password", "welcome");
			String port = getProperty("wbjunit.db2.port", "50001");
			String host = getProperty("wbjunit.db2.host", "db2wbtest");

			ArgumentParser parser = new AppArguments();
			parser.parse("-url='jdbc:db2://" + host + ":" + port + "/" + dbname + "' -username=" + username + " -password=" + pwd + " -driver=com.ibm.db2.jcc.DB2Driver");
			ConnectionProfile prof = BatchRunner.createCmdLineProfile(parser);
			prof.setName("WBJUnitDB2");
			prof.addConnectionProperty("retrieveMessagesFromServerOnGetMessage", "true");
			ConnectionMgr.getInstance().addProfile(prof);
			con = ConnectionMgr.getInstance().getConnection(prof, id);
			return con;
		}
		catch (Throwable th)
		{
			th.printStackTrace();
			isAvailable = false;
			return null;
		}
	}

	public static void initTestCase()
		throws Exception
	{
		String schema = getSchemaName();
		TestUtil util = new TestUtil(schema);
		util.prepareEnvironment();

		if (!isAvailable) return;

		WbConnection con = getDb2Connection();
		if (con == null)
		{
			isAvailable = false;
			return;
		}

		Statement stmt = null;
		try
		{
			stmt = con.createStatement();

			stmt.execute("set schema " + schema);
			dropAllObjects(con);
			con.commit();
		}
		catch (Exception e)
		{
			con.rollbackSilently();
		}
	}

	public static void cleanUpTestCase()
	{
		if (!isAvailable) return;
		WbConnection con = getDb2Connection();
		dropAllObjects(con);
		ConnectionMgr.getInstance().disconnectAll();
	}

	public static void dropAllObjects(WbConnection con)
	{
		if (con == null) return;

		try
		{
			List<TableIdentifier> tables = con.getMetadata().getObjectList(getSchemaName(), null);
			GenericObjectDropper dropper = new GenericObjectDropper();
			dropper.setConnection(con);
			dropper.setObjects(tables);
			dropper.setCascade(true);
			dropper.dropObjects();
			con.commit();
			con.getObjectCache().clear();
		}
		catch (Exception e)
		{
			con.rollbackSilently();
		}
	}

}
