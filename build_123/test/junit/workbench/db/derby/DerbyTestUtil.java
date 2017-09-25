/*
 * DerbyTestUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.db.derby;

import java.util.Properties;

import workbench.AppArguments;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;

import workbench.sql.BatchRunner;

import workbench.util.ArgumentParser;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class DerbyTestUtil
{

	public static final String PROFILE_NAME = "WBJUnitDerby";

	/**
	 * Return a connection to an embedded Derby engine
	 */
	public static WbConnection getDerbyConnection(String basedir)
	{
		try
		{
			WbConnection con = ConnectionMgr.getInstance().findConnection(PROFILE_NAME);
			if (con != null) return con;

			ArgumentParser parser = new AppArguments();
			parser.parse("-url='jdbc:derby:memory:" + PROFILE_NAME + ";create=true' -driver=org.apache.derby.jdbc.EmbeddedDriver");
			ConnectionProfile prof = BatchRunner.createCmdLineProfile(parser);
			prof.setName(PROFILE_NAME);
			WbFile dir = new WbFile(basedir);
			System.setProperty("derby.system.home", dir.getFullPath());
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

	public static void clearProperties()
	{
		Properties props = System.getProperties();
		props.remove("derby.system.home");
	}
}
