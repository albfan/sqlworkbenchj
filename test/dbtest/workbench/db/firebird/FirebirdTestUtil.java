/*
 * FirebirdTestUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.firebird;

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
public class FirebirdTestUtil
{

	public static final String TEST_USER = "wbjunit";
	public static final String TEST_PWD = "wbjunit";


	/**
	 * Return a connection to a locally running Firebird database
	 */
	public static WbConnection getFirebirdConnection()
	{
		final String id = "WbJUnitFirebird";
		try
		{
			WbConnection con = ConnectionMgr.getInstance().findConnection(id);
			if (con != null) return con;

			ArgumentParser parser = new AppArguments();
			parser.parse("-url='jdbc:firebirdsql://localhost:3050/wbjunit' -autocommit=false -username=" + TEST_USER + " -password=" + TEST_PWD + " -driver=org.firebirdsql.jdbc.FBDriver");
			ConnectionProfile prof = BatchRunner.createCmdLineProfile(parser);
			prof.setName("WBJUnitFirebird");
			ConnectionMgr.getInstance().addProfile(prof);
			con = ConnectionMgr.getInstance().getConnection(prof, id);
			return con;
		}
		catch (Throwable th)
		{
			return null;
		}
	}

	public static void initTestCase()
		throws Exception
	{
		TestUtil util = new TestUtil("FirebirdTest");
		util.prepareEnvironment();
	}

	public static void cleanUpTestCase()
	{
		WbConnection con = getFirebirdConnection();
		dropAllObjects(con);
		ConnectionMgr.getInstance().disconnectAll();
	}

	public static void dropAllObjects(WbConnection con)
	{
		if (con == null) return;

		try
		{
			String[] types = { "TABLE", "VIEW", "SEQUENCE", "DOMAIN" };
			List<TableIdentifier> tables = con.getMetadata().getObjectList(null, types);
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
