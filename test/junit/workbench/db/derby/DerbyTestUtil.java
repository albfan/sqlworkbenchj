/*
 * PostgresTestHelper
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2012, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.derby;

import java.util.Properties;
import workbench.AppArguments;
import workbench.TestUtil;
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
