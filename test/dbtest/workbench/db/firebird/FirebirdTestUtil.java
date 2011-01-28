/*
 * PostgresTestHelper
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
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
			parser.parse("-url='jdbc:firebirdsql://localhost:3050/wbjunit' -username=" + TEST_USER + " -password=" + TEST_PWD + " -driver=org.firebirdsql.jdbc.FBDriver");
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
			List<TableIdentifier> tables = con.getMetadata().getObjectList(null, null);
			GenericObjectDropper dropper = new GenericObjectDropper();
			dropper.setConnection(con);
			dropper.setObjects(tables);
			dropper.setCascade(true);
			dropper.dropObjects();
			con.commit();
		}
		catch (Exception e)
		{
			con.rollbackSilently();
		}
	}
}
