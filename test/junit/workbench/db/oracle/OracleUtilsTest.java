/*
 * OracleUtilsTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.db.oracle;

import org.junit.Test;

import static org.junit.Assert.*;


/**
 *
 * @author Thomas Kellerer
 */
public class OracleUtilsTest
{

	public OracleUtilsTest()
	{
	}

	@Test
	public void testTrimSQLPlusLineContinuation()
	{
		String input =
			"exec some_procedure(42,-\n" +
			"                    24);";
		String expResult =
			"exec some_procedure(42,\n" +
			"                    24);\n";
		String result = OracleUtils.trimSQLPlusLineContinuation(input);
		assertEquals(expResult, result);
	}

	@Test
	public void testIs12102()
	{
		String version =
			"Oracle Database 12c Enterprise Edition Release 12.1.0.1.0 - 64bit Production\n" +
			"With the Partitioning, OLAP, Advanced Analytics and Real Application Testing options";

		boolean is12102 = OracleUtils.is12_1_0_2(version);
		assertFalse(is12102);

		version =
			"Oracle Database 12c Enterprise Edition Release 12.1.0.2.0 - 64bit Production\n" +
			"With the Partitioning, OLAP, Advanced Analytics and Real Application Testing options";
		is12102 = OracleUtils.is12_1_0_2(version);
		assertTrue(is12102);

		version =
			"Oracle Database 12c Enterprise Edition Release 12.2.0.1.0 - 64bit Production\n" +
			"With the Partitioning, OLAP, Advanced Analytics and Real Application Testing options";
		is12102 = OracleUtils.is12_1_0_2(version);
		assertTrue(is12102);

	}
}
