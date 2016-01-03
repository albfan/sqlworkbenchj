/*
 * WbListProfilesTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.sql.wbcommands.console;

import workbench.WbTestCase;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;

import workbench.sql.StatementRunnerResult;

import workbench.util.StringUtil;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbListProfilesTest
	extends WbTestCase
{

	public WbListProfilesTest()
	{
		super("WbListProfilesTest");
	}

	@Test
	public void testNeedsConnection()
	{
		WbListProfiles list = new WbListProfiles();
		assertFalse(list.isConnectionRequired());
	}

	@Test
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

		String msg = result.getMessages().toString();
//		System.out.println("****\n" + msg);
		String[] lines = msg.split(StringUtil.REGEX_CRLF);
		assertEquals(2, lines.length);
		assertEquals("Default group", lines[0]);
		assertEquals("  TestProfile, Username=user, URL=jdbc:test", lines[1]);
	}
}
