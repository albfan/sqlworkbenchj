/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.db;

import workbench.sql.ErrorDescriptor;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class RegexErrorPositionReaderTest
{

	public RegexErrorPositionReaderTest()
	{
	}

	@Test
	public void testPostgres()
	{
		RegexErrorPositionReader reader = new RegexErrorPositionReader("(?i)position:\\s+[0-9]+");
		reader.setNumbersAreOneBased(true);
		String msg =
			"ERROR: column \"foobar\" of relation \"person\" does not exist\n" +
			"  Position: 22";
		String sql =
			"update person \n" +
			"  set foobar = 5\n" +
			"where id > 0";
		ErrorDescriptor error = reader.getErrorPosition(sql, msg);
		assertNotNull(error);
		assertEquals(21, error.getErrorPosition());
	}

	@Test
	public void testFirebird()
	{
		RegexErrorPositionReader reader = new RegexErrorPositionReader("(?i)\\sline\\s[0-9]+", "(?i)\\scolumn\\s[0-9]+");
		reader.setNumbersAreOneBased(true);
		String msg =
			"GDS Exception. 335544569. Dynamic SQL Error\n" +
			"SQL error code = -206\n" +
			"Column unknown\n" +
			"X\n" +
			"At line 2, column 7";

		String sql =
			"update person \n" +
			"  set x = 5 \n" +
			"where id > 0";
		ErrorDescriptor error = reader.getErrorPosition(sql, msg);
		assertNotNull(error);
		assertEquals(1, error.getErrorLine());
		assertEquals(6, error.getErrorColumn());

		assertEquals(sql.indexOf("x = 5"), error.getErrorPosition());
	}

	@Test
	public void testMaxDB()
	{
		RegexErrorPositionReader reader = new RegexErrorPositionReader("(?i)\\(at [0-9]+\\)\\:");
		reader.setNumbersAreOneBased(false);
		String msg = "[-5016] (at 22): Unknown column: x";

		String sql =
			"update person \n" +
			"  set x = 5 \n" +
			"where id > 0";

		ErrorDescriptor error = reader.getErrorPosition(sql, msg);
		assertNotNull(error);
		assertEquals(22, error.getErrorPosition());
	}

	@Test
	public void testHana()
	{
//		RegexErrorPositionReader reader = new RegexErrorPositionReader("(?i)\\sline\\s[0-9]+", "(?i)\\scol\\s[0-9]+");
		RegexErrorPositionReader reader = new RegexErrorPositionReader("(?i)\\(at pos [0-9]+\\)");
		reader.setNumbersAreOneBased(false);
		String msg = "SAP DBTech JDBC: [257] (at 63): sql syntax error: incorrect syntax near \"1\": line 3 col 45 (at pos 63)";

		String sql =
      "select *\n" +
      "from foo\n" +
      "where order_date >= current_date - interval '1' day\n" +
      "and active = 1;";

		ErrorDescriptor error = reader.getErrorPosition(sql, msg);
		assertNotNull(error);
//    assertEquals(3, error.getErrorLine());
//    assertEquals(45, error.getErrorColumn());
    System.out.println(error.getErrorPosition());
	}
}
