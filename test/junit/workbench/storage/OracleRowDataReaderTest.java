/*
 * OracleRowDataReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.storage;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleRowDataReaderTest
{

	public OracleRowDataReaderTest()
	{
	}

	@Test
	public void testCleanupTSValue()
	{
		assertEquals("2015-01-26 11:42:46.07", OracleRowDataReader.removeTimezone("2015-01-26 11:42:46.07"));
		assertEquals("2015-01-26 11:42:46.078", OracleRowDataReader.removeTimezone("2015-01-26 11:42:46.078"));
		assertEquals("2015-01-26 11:42:46.0789", OracleRowDataReader.removeTimezone("2015-01-26 11:42:46.0789"));
		assertEquals("2015-01-26 11:42:46.07899", OracleRowDataReader.removeTimezone("2015-01-26 11:42:46.07899"));
		assertEquals("2015-01-26 11:42:46.07899", OracleRowDataReader.removeTimezone("2015-01-26 11:42:46.07899   "));
		assertEquals("2015-01-26 11:42:46.078999", OracleRowDataReader.removeTimezone("2015-01-26 11:42:46.078999"));
		assertEquals("2015-01-26 11:42:46.078999", OracleRowDataReader.removeTimezone("2015-01-26 11:42:46.078999       "));
		assertEquals("2015-01-26 11:42:46.0", OracleRowDataReader.removeTimezone("2015-01-26 11:42:46.0"));
		assertEquals("2015-01-26 11:42:46.894119", OracleRowDataReader.removeTimezone("2015-01-26 11:42:46.894119 Europe/Berlin"));
		assertEquals("2015-01-26 11:42", OracleRowDataReader.removeTimezone("2015-01-26 11:42"));
		assertEquals("2016-07-26 12:15:16.0", OracleRowDataReader.removeTimezone("2016-07-26 12:15:16.0 UTC"));
	}

}
