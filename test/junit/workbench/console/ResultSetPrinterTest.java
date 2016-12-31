/*
 * ResultSetPrinterTest.java
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
package workbench.console;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.Statement;

import workbench.TestUtil;

import workbench.db.WbConnection;

import workbench.sql.StatementRunnerResult;

import workbench.util.StringUtil;

import junit.framework.TestCase;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class ResultSetPrinterTest
	extends TestCase
{
	public ResultSetPrinterTest()
	{
		super("ResultSetPrinterTest");
	}

	@Test
	public void testConsumeResult()
		throws Exception
	{
		TestUtil util = new TestUtil("ResultSetPrinter");
		WbConnection con = null;
		try
		{
			con = util.getConnection("PrinterTest");
			Statement stmt = con.createStatement();
			stmt.executeUpdate("create table person (nr integer, firstname varchar(20), lastname varchar(20))");

			int rowCount = 2;
			for (int i = 0; i < rowCount; i++)
			{
				stmt.executeUpdate("insert into person values (" + i + ", 'firstname', 'lastname')");
			}
			stmt.executeUpdate("insert into person values (42, 'first\nname', 'lastname\nlines')");
			con.commit();
			rowCount ++;

			ResultSet rs = stmt.executeQuery("select * from person order by nr");

			ByteArrayOutputStream ba = new ByteArrayOutputStream(500);
			PrintStream ps = new PrintStream(ba);
			ResultSetPrinter printer = new ResultSetPrinter(ps);
			StatementRunnerResult result = new StatementRunnerResult();
			result.addResultSet(rs);
			printer.consumeResult(result);
			ps.close();
			rs.close();

			String out = ba.toString();
//			System.out.println(out);
			String[] lines = out.split(StringUtil.LINE_TERMINATOR);

			// expected is one line per row in the database (rowCount)
			// plus two lines heading
			// plus one additional line for the multi-line values
			// plus one line with the number of rows retrieved
			// so we wind up with rowCount + 4
			assertEquals(rowCount + 5, lines.length);

			assertEquals("NR         | FIRSTNAME            | LASTNAME            ", lines[0]);
			assertEquals("-----------+----------------------+---------------------", lines[1]);
			assertEquals("         0 | firstname            | lastname            ", lines[2]);
			assertEquals("         1 | firstname            | lastname            ", lines[3]);
			assertEquals("        42 | first                | lastname            ", lines[4]);
			assertEquals("           : name                 : lines", lines[5]);
			assertEquals("(3 Rows)", lines[7]);

			rs = stmt.executeQuery("select * from person order by nr");
			ba.reset();
			ps = new PrintStream(ba);
			printer = new ResultSetPrinter(ps);
			printer.setFormatColumns(false);
			result = new StatementRunnerResult();
			result.setShowRowCount(false);
			result.addResultSet(rs);
			printer.consumeResult(result);
			ps.close();
			rs.close();
			out = ba.toString();
//			System.out.println(out);
			lines = out.split(StringUtil.LINE_TERMINATOR);
			assertEquals(rowCount + 1, lines.length);
			assertEquals("NR | FIRSTNAME | LASTNAME", lines[0]);
			assertEquals("0 | firstname | lastname", lines[1]);
			assertEquals("1 | firstname | lastname", lines[2]);
			assertEquals("42 | first\\nname | lastname\\nlines", lines[3]);

			rs = stmt.executeQuery("select * from person order by nr");
			ba.reset();
			ps = new PrintStream(ba);
			printer = new ResultSetPrinter(ps);
			printer.setPrintRowsAsLine(false);
			result = new StatementRunnerResult();
			result.addResultSet(rs);
			printer.consumeResult(result);
			ps.close();
			rs.close();
			out = ba.toString();
//			System.out.println(out);
			lines = out.split(StringUtil.LINE_TERMINATOR);
			assertEquals(16, lines.length);
			assertEquals("---- [Row 1] -------------------------------", lines[0]);
			assertEquals("NR        : 0", lines[1]);
			assertEquals("FIRSTNAME : firstname", lines[2]);
			assertEquals("LASTNAME  : lastname", lines[3]);
			assertEquals("---- [Row 2] -------------------------------", lines[4]);
			assertEquals("NR        : 1", lines[5]);
			assertEquals("FIRSTNAME : firstname", lines[6]);
			assertEquals("LASTNAME  : lastname", lines[7]);
			assertEquals("---- [Row 3] -------------------------------", lines[8]);
			assertEquals("NR        : 42", lines[9]);
			assertEquals("FIRSTNAME : first", lines[10]);
			assertEquals("            name", lines[11]);
			assertEquals("LASTNAME  : lastname", lines[12]);
			assertEquals("            lines", lines[13]);
		}
		finally
		{
			con.disconnect();
		}
	}
}
