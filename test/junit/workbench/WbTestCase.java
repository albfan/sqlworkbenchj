/*
 * WbTestCase.java
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
package workbench;

import java.io.IOException;

/**
 * @author Thomas Kellerer
 */
public class WbTestCase
{
	private String name;
	private boolean prepared;

	public WbTestCase()
	{
		name = "WbTestCase";
		prepare();
	}

	public WbTestCase(String testName)
	{
		name = testName;
		prepare();
	}

	protected final void prepare()
	{
		System.setProperty("workbench.log.console", "false");
		System.setProperty("workbench.dbmetadata.debugmetasql", "true");
		getTestUtil();
	}

	protected synchronized TestUtil getTestUtil()
	{
		TestUtil util = new TestUtil(getName());
		if (prepared) return util;

		try
		{
			util.prepareEnvironment();
			prepared = true;
		}
		catch (IOException io)
		{
			io.printStackTrace();
		}
		return util;
	}

	protected TestUtil getTestUtil(String method)
	{
		return new TestUtil(getName() + "_" + "method");
	}

	public String getName()
	{
		return name;
	}
}
