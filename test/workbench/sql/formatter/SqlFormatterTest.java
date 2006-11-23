/*
 * SqlFormatterTest.java
 * JUnit based test
 *
 * Created on August 16, 2006, 9:44 PM
 */

package workbench.sql.formatter;

import junit.framework.*;
import workbench.TestUtil;
import workbench.resource.Settings;

/**
 *
 * @author support@sql-workbench.net
 */
public class SqlFormatterTest extends TestCase
{

	public SqlFormatterTest(String testName)
	{
		super(testName);
	}

	public void setUp()
		throws Exception
	{
		TestUtil util = new TestUtil();
		util.prepareEnvironment();
	}

	public void testWhitespace()
	{
		try
		{
			String sql = "alter table epg_value add constraint fk_value_attr foreign key (id_attribute) references attribute(id);";
			String expected = "ALTER TABLE epg_value ADD CONSTRAINT fk_value_attr FOREIGN KEY (id_attribute) REFERENCES attribute(id);";
			SqlFormatter f = new SqlFormatter(sql, 100);
			String formatted = f.getFormattedSql();
			assertEquals("ALTER TABLE not correctly formatted", expected, formatted.trim());
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
			String formatted = f.getFormattedSql();
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
			String formatted = f.getFormattedSql();
			String expected = "SELECT a,\n       b,\n       [MyCol]\nFROM mytable";
			assertEquals(expected, formatted);
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
			String formatted = f.getFormattedSql();
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

			SqlFormatter f = new SqlFormatter(sql,100);
			String nl = f.getLineEnding();
			String formatted = f.getFormattedSql();
//			System.out.println(formatted);
			String expected = "--comment\nSELECT *\nFROM blub;";
			assertEquals("Not correctly formatted", expected, formatted);

			sql = "select x from y union all select y from x";
			f = new SqlFormatter(sql,100);
			formatted = f.getFormattedSql();
			expected = "SELECT x\nFROM y\nUNION ALL\nSELECT y\nFROM x";
			assertEquals(expected, formatted);

			sql = "select x,y from y order by x\n--trailing comment";
			f = new SqlFormatter(sql,100);
			formatted = f.getFormattedSql();
			expected = "SELECT x,\n       y\nFROM y\nORDER BY x\n--trailing comment";
			assertEquals(expected, formatted.trim());
			
			sql = "select x,y,z from y where a = 1 and b = 2";
			f = new SqlFormatter(sql,100);
			formatted = f.getFormattedSql();
			expected = "SELECT x,\n       y,\n       z\nFROM y\nWHERE a = 1\nAND   b = 2";
			assertEquals(expected, formatted);

			sql = "select x,y,z from y where a = 1 and b = (select min(x) from y)";
			f = new SqlFormatter(sql,100);
			formatted = f.getFormattedSql();
			expected = "SELECT x,\n       y,\n       z\nFROM y\nWHERE a = 1\nAND   b = (SELECT MIN(x) FROM y)";
			assertEquals(expected, formatted);

			sql = "select x,y,z from y where a = 1 and b = (select min(x) from y)";
			f = new SqlFormatter(sql,10);
			formatted = f.getFormattedSql();
//			System.out.println(formatted);
			expected = "SELECT x,\n       y,\n       z\nFROM y\nWHERE a = 1\nAND   b = (SELECT MIN(x)\n           FROM y)";
//			System.out.println(expected);
			assertEquals(expected, formatted);

			sql = "UPDATE customer " +
             "   SET duplicate_flag = CASE (SELECT COUNT(*) FROM customer c2 WHERE c2.f_name = customer.f_name AND c2.s_name = customer.s_name GROUP BY f_name,s_name)  \n" +
             "                           WHEN 1 THEN 0  " +
             "                           ELSE 1  " +
             "                        END";
			expected = "UPDATE customer\n" +
             "   SET duplicate_flag = CASE (SELECT COUNT(*)\n" +
             "                              FROM customer c2\n" +
             "                              WHERE c2.f_name = customer.f_name\n" +
             "                              AND   c2.s_name = customer.s_name\n" +
             "                              GROUP BY f_name,\n" +
             "                                       s_name)\n" +
             "                          WHEN 1 THEN 0\n" +
             "                          ELSE 1\n" +
             "                        END";
			f = new SqlFormatter(sql,10);

			formatted = f.getFormattedSql();
//			System.out.println(StringUtil.escapeUnicode(expected, CharacterRange.RANGE_NONE));
//			System.out.println("-------");
//			System.out.println(formatted);
//			System.out.println(StringUtil.escapeUnicode(formatted));
			assertEquals(expected, formatted.trim());

			sql = "SELECT ber.nachname AS ber_nachname, \n" +
             "       ber.nummer AS ber_nummer \n" +
             "FROM table a WHERE (x in (select bla,bla,alkj,aldk,alkjd,dlaj,alkjdaf from blub 1, blub2, blub3 where x=1 and y=2 and z=3 and a=b and c=d) or y = 5)" +
						 " and a *= b and b = c (+)";
			f = new SqlFormatter(sql,10);
			formatted = f.getFormattedSql();
			expected = "SELECT ber.nachname AS ber_nachname,\n" +
             "       ber.nummer AS ber_nummer\n" +
             "FROM TABLE a\n" +
             "WHERE (x IN (SELECT bla,\n" +
             "                    bla,\n" +
             "                    alkj,\n" +
             "                    aldk,\n" +
             "                    alkjd,\n" +
             "                    dlaj,\n" +
             "                    alkjdaf\n" +
             "             FROM blub 1,\n" +
             "                  blub2,\n" +
             "                  blub3\n" +
             "             WHERE x = 1\n" +
             "             AND   y = 2\n" +
             "             AND   z = 3\n" +
             "             AND   a = b\n" +
             "             AND   c = d) OR y = 5)\n" +
             "AND   a *= b\n" +
             "AND   b = c (+)";
//			System.out.println(formatted);
//			System.out.println("---------");
//			System.out.println(expected);
			assertEquals(expected, formatted.trim());
			
			sql = "update x set (a,b) = (select x,y from k);";
			f = new SqlFormatter(sql,50);
			formatted = f.getFormattedSql();
			expected = "UPDATE x\n   SET (a,b) = (SELECT x, y FROM k);";
			assertEquals(expected, formatted.trim());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


}
