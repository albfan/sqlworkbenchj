/*
 * SqlFormatterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.formatter;

import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.resource.Settings;

/**
 *
 * @author support@sql-workbench.net
 */
public class SqlFormatterTest
	extends TestCase
{
	public SqlFormatterTest(String testName)
	{
		super(testName);
	}

	public void setUp()
		throws Exception
	{
		TestUtil util = new TestUtil(this.getName());
		util.prepareEnvironment();
	}

	public void testWbConfirm()
	{
		try
		{
			String sql = "wbconfirm 'my message'";
			SqlFormatter f = new SqlFormatter(sql, 100);
			CharSequence formatted = f.getFormattedSql();
			String expected = "WbConfirm 'my message'";
			assertEquals("WbConfirm not formatted correctly", expected, formatted);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testAliasForSubselect()
	{
		try
		{
			String sql = "select a,b, (select a,b from t2) col4 from t1";
			SqlFormatter f = new SqlFormatter(sql, 100);
			CharSequence formatted = f.getFormattedSql();
			String expected = "SELECT a,\n" + "       b,\n" + "       (SELECT a, b FROM t2) col4\n" + "FROM t1";
			assertEquals("SELECT in VALUES not formatted", expected, formatted);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testAsInFrom()
	{
		try
		{
			String sql = "select t1.a, t2.b from bla as t1, t2";
			SqlFormatter f = new SqlFormatter(sql, 100);
			CharSequence formatted = f.getFormattedSql();
			String expected = "SELECT t1.a,\n" + "       t2.b\n" + "FROM bla AS t1,\n" + "     t2";
//			Thread.yield();
//			System.out.println("sql=" + formatted);
			assertEquals("SELECT in VALUES not formatted", expected, formatted);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testInsertWithSubselect()
	{
		try
		{
			String sql = "insert into tble (a,b) values ( (select max(x) from y), 'bla')";
			String expected = "INSERT INTO tble\n" + "(\n" + "  a,\n" + "  b\n" + ")  \n" + "VALUES\n" + "(\n" + "   (SELECT MAX(x) FROM y),\n" + "  'bla'\n" + ")";
			SqlFormatter f = new SqlFormatter(sql, 100);
			CharSequence formatted = f.getFormattedSql();
//			System.out.println("*** got ***");
//			System.out.println(formatted);
//			System.out.println("*** expected ***");
//			System.out.println(expected);
//			System.out.println("**************");
			assertEquals("SELECT in VALUES not formatted", expected, formatted);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void testLowerCaseFunctions()
	{
		try
		{
			String sql = "select col1, MAX(col2) from theTable group by col1;";
			String expected = "SELECT col1,\n       max(col2)\nFROM theTable\nGROUP BY col1;";
			SqlFormatter f = new SqlFormatter(sql, 100);
			f.setUseLowerCaseFunctions(true);
			CharSequence formatted = f.getFormattedSql();
//			System.out.println("*** got ***");
//			System.out.println(formatted);
//			System.out.println("*** expected ***");
//			System.out.println(expected);
//			System.out.println("**************");
			assertEquals("SELECT in VALUES not formatted", expected, formatted);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void testCase()
	{
		try
		{
			String sql = "SELECT col1 as bla, case when x = 1 then 2 else 3 end AS y FROM table";
			String expected = "SELECT col1 AS bla,\n       CASE\n         WHEN x=1 THEN 2\n         ELSE 3\n       END AS y\nFROM TABLE";
			SqlFormatter f = new SqlFormatter(sql, 100);
			CharSequence formatted = f.getFormattedSql();
			assertEquals("CASE alias not formatted", expected, formatted);

			sql = "SELECT case when x = 1 then 2 else 3 end AS y FROM table";
			expected = "SELECT CASE\n         WHEN x=1 THEN 2\n         ELSE 3\n       END AS y\nFROM TABLE";
			f = new SqlFormatter(sql, 100);
			formatted = f.getFormattedSql();
			assertEquals("CASE alias not formatted", expected, formatted);

			sql = "SELECT a,b,c from table order by b,case when a=1 then 2 when a=2 then 1 else 3 end";
			expected = "SELECT a,\n" + "       b,\n" + "       c\n" + "FROM TABLE\n" + "ORDER BY b,\n" + "         CASE \n" + "           WHEN a=1 THEN 2\n" + "           WHEN a=2 THEN 1\n" + "           ELSE 3\n" + "         END";
			f = new SqlFormatter(sql, 100);
			formatted = f.getFormattedSql();
//			System.out.println("=================");
//			System.out.println(formatted);
//			System.out.println("=================");
//			System.out.println(expected);
//			System.out.println("=================");
			assertEquals("CASE alias not formatted", expected, formatted);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testWhitespace()
	{
		try
		{
			String sql = "alter table epg_value add constraint fk_value_attr foreign key (id_attribute) references attribute(id);";
			String expected = "ALTER TABLE epg_value ADD CONSTRAINT fk_value_attr FOREIGN KEY (id_attribute) REFERENCES attribute(id);";
			SqlFormatter f = new SqlFormatter(sql, 100);
			CharSequence formatted = f.getFormattedSql();
			assertEquals("ALTER TABLE not correctly formatted", expected, formatted.toString().trim());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void testColumnThreshold()
		throws Exception
	{
		try
		{
			String sql = "SELECT a,b,c from mytable";
			SqlFormatter f = new SqlFormatter(sql, 100);
			f.setMaxColumnsPerSelect(5);
			CharSequence formatted = f.getFormattedSql();
			String expected = "SELECT a, b, c\nFROM mytable";

			sql = "SELECT a,b,c,d,e,f,g,h,i from mytable";
			f = new SqlFormatter(sql, 100);
			f.setMaxColumnsPerSelect(5);
			formatted = f.getFormattedSql();
			expected = "SELECT a, b, c, d, e,\n       f, g, h, i\nFROM mytable";
			assertEquals(expected, formatted);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testBracketIdentifier()
		throws Exception
	{
		try
		{
			String sql = "SELECT a,b,[MyCol] from mytable";
			SqlFormatter f = new SqlFormatter(sql, 100);
			CharSequence formatted = f.getFormattedSql();
			String expected = "SELECT a,\n       b,\n       [MyCol]\nFROM mytable";
			assertEquals(expected, formatted);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testDecode()
	{
		try
		{
			String sql = "SELECT DECODE((MOD(INPUT-4,12)+1),1,'RAT',2,'OX',3,'TIGER',4,'RABBIT',5,'DRAGON',6,'SNAKE',7,'HORSE',8,'SHEEP/GOAT',9,'MONKEY',10,'ROOSTER',11,'DOG',12,'PIG')  YR FROM DUAL";

			String expected = "SELECT DECODE((MOD(INPUT-4,12)+1),\n" + "             1,'RAT',\n" + "             2,'OX',\n" + "             3,'TIGER',\n" + "             4,'RABBIT',\n" + "             5,'DRAGON',\n" + "             6,'SNAKE',\n" + "             7,'HORSE',\n" + "             8,'SHEEP/GOAT',\n" + "             9,'MONKEY',\n" + "             10,'ROOSTER',\n" + "             11,'DOG',\n" + "             12,'PIG'\n" + "       )  YR\n" + "FROM DUAL";

			SqlFormatter f = new SqlFormatter(sql, 100);
			CharSequence formatted = f.getFormattedSql();

//			System.out.println(StringUtil.escapeUnicode(expected));
//			System.out.println(StringUtil.escapeUnicode(formatted));
//			System.out.println(formatted);
			assertEquals("Complex DECODE not formatted correctly", expected, formatted);


			sql = "select decode(col1, 'a', 1, 'b', 2, 'c', 3, 99) from dual";
			f = new SqlFormatter(sql, 100);
			formatted = f.getFormattedSql();
			expected = "SELECT decode(col1,\n" + "              'a', 1,\n" + "              'b', 2,\n" + "              'c', 3,\n" + "              99\n" + "       ) \n" + "FROM dual";

//			System.out.println(StringUtil.escapeUnicode(expected));
//			System.out.println(StringUtil.escapeUnicode(formatted));
			assertEquals("DECODE not formatted correctly", expected, formatted);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testQuotedIdentifier()
		throws Exception
	{
		try
		{
			String sql = "SELECT a,b,\"c d\" from mytable";
			SqlFormatter f = new SqlFormatter(sql, 100);
			CharSequence formatted = f.getFormattedSql();
			String expected = "SELECT a,\n       b,\n       \"c d\"\nFROM mytable";
			assertEquals(expected, formatted);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testGetFormattedSql()
		throws Exception
	{
		try
		{
			String sql = "--comment\nselect * from blub;";
			Settings.getInstance().setInternalEditorLineEnding(Settings.UNIX_LINE_TERMINATOR_PROP_VALUE);

			SqlFormatter f = new SqlFormatter(sql, 100);
			String nl = f.getLineEnding();
			CharSequence formatted = f.getFormattedSql();
//			System.out.println(formatted);
			String expected = "--comment\nSELECT *\nFROM blub;";
			assertEquals("Not correctly formatted", expected, formatted);

			sql = "select x from y union all select y from x";
			f = new SqlFormatter(sql, 100);
			formatted = f.getFormattedSql();
			expected = "SELECT x\nFROM y\nUNION ALL\nSELECT y\nFROM x";
			assertEquals(expected, formatted);

			sql = "select x,y from y order by x\n--trailing comment";
			f = new SqlFormatter(sql, 100);
			formatted = f.getFormattedSql();
			expected = "SELECT x,\n       y\nFROM y\nORDER BY x\n--trailing comment";
			assertEquals(expected, formatted.toString().trim());

			sql = "select x,y,z from y where a = 1 and b = 2";
			f = new SqlFormatter(sql, 100);
			formatted = f.getFormattedSql();
			expected = "SELECT x,\n       y,\n       z\nFROM y\nWHERE a = 1\nAND   b = 2";
			assertEquals(expected, formatted);

			sql = "select x,y,z from y where a = 1 and b = (select min(x) from y)";
			f = new SqlFormatter(sql, 100);
			formatted = f.getFormattedSql();
			expected = "SELECT x,\n       y,\n       z\nFROM y\nWHERE a = 1\nAND   b = (SELECT MIN(x) FROM y)";
			assertEquals(expected, formatted);

			sql = "select x,y,z from y where a = 1 and b = (select min(x) from y)";
			f = new SqlFormatter(sql, 10);
			formatted = f.getFormattedSql();
//			System.out.println(formatted);
			expected = "SELECT x,\n       y,\n       z\nFROM y\nWHERE a = 1\nAND   b = (SELECT MIN(x)\n           FROM y)";
//			System.out.println(expected);
			assertEquals(expected, formatted);

			sql = "UPDATE customer " + "   SET duplicate_flag = CASE (SELECT COUNT(*) FROM customer c2 WHERE c2.f_name = customer.f_name AND c2.s_name = customer.s_name GROUP BY f_name,s_name)  \n" + "                           WHEN 1 THEN 0  " + "                           ELSE 1  " + "                        END";
			expected = "UPDATE customer\n" + "   SET duplicate_flag = CASE (SELECT COUNT(*)\n" + "                              FROM customer c2\n" + "                              WHERE c2.f_name = customer.f_name\n" + "                              AND   c2.s_name = customer.s_name\n" + "                              GROUP BY f_name,\n" + "                                       s_name)\n" + "                          WHEN 1 THEN 0\n" + "                          ELSE 1\n" + "                        END";
			f = new SqlFormatter(sql, 10);

			formatted = f.getFormattedSql();
//			System.out.println(StringUtil.escapeUnicode(expected, CharacterRange.RANGE_NONE));
//			System.out.println("-------");
//			System.out.println(formatted);
//			System.out.println(StringUtil.escapeUnicode(formatted));
			assertEquals(expected, formatted.toString().trim());

			sql = "SELECT ber.nachname AS ber_nachname, \n" + "       ber.nummer AS ber_nummer \n" + "FROM table a WHERE (x in (select bla,bla,alkj,aldk,alkjd,dlaj,alkjdaf from blub 1, blub2, blub3 where x=1 and y=2 and z=3 and a=b and c=d) or y = 5)" + " and a *= b and b = c (+)";
			f = new SqlFormatter(sql, 10);
			formatted = f.getFormattedSql();
			expected = "SELECT ber.nachname AS ber_nachname,\n" + "       ber.nummer AS ber_nummer\n" + "FROM TABLE a\n" + "WHERE (x IN (SELECT bla,\n" + "                    bla,\n" + "                    alkj,\n" + "                    aldk,\n" + "                    alkjd,\n" + "                    dlaj,\n" + "                    alkjdaf\n" + "             FROM blub 1,\n" + "                  blub2,\n" + "                  blub3\n" + "             WHERE x = 1\n" + "             AND   y = 2\n" + "             AND   z = 3\n" + "             AND   a = b\n" + "             AND   c = d) OR y = 5)\n" + "AND   a *= b\n" + "AND   b = c (+)";
//			System.out.println(formatted);
//			System.out.println("---------");
//			System.out.println(expected);
			assertEquals(expected, formatted.toString().trim());

			sql = "update x set (a,b) = (select x,y from k);";
			f = new SqlFormatter(sql, 50);
			formatted = f.getFormattedSql();
			expected = "UPDATE x\n   SET (a,b) = (SELECT x, y FROM k);";
			assertEquals(expected, formatted.toString().trim());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}