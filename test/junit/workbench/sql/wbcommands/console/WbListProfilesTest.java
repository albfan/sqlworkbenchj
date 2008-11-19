/*
 * WbListProfilesTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands.console;

import junit.framework.TestCase;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.sql.StatementRunnerResult;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbListProfilesTest
	extends TestCase
{

	public WbListProfilesTest(String testName)
	{
		super(testName);
	}

	public void testNeedsConnection()
	{
		WbListProfiles list = new WbListProfiles();
		assertFalse(list.isConnectionRequired());
	}
	
	public void testExecute()
		throws Exception
	{
		String sql = "WbListProfiles";
		WbListProfiles list = new WbListProfiles();
		StatementRunnerResult result = list.execute(sql);
		assertTrue(result.isSuccess());

		ConnectionProfile prof = new ConnectionProfile("TestProfile", "dummy.class", "jdbc:test", "user", "pwd");
		ConnectionMgr.getInstance().clearProfiles();
		ConnectionMgr.getInstance().addProfile(prof);
		result = list.execute(sql);
		assertTrue(result.isSuccess());

		String msg = result.getMessageBuffer().toString();
		//System.out.println("****\n" + msg);
		String[] lines = msg.split(StringUtil.REGEX_CRLF);
		assertEquals(1, lines.length);
		//assertEquals("{Default group}/New profile", lines[0]);
		assertEquals("{Default group}/TestProfile", lines[0]);
	}
}
