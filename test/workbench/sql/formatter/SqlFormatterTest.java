/*
 * SqlFormatterTest.java
 * JUnit based test
 *
 * Created on August 16, 2006, 9:44 PM
 */

package workbench.sql.formatter;

import junit.framework.*;
import workbench.resource.Settings;
import workbench.util.CharacterRange;
import workbench.util.StringUtil;

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

	public void testGetFormattedSql() throws Exception
	{
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		try
		{
			String sql = "--comment\nselect * from blub;";
			SqlFormatter f = new SqlFormatter(sql,100);
			String formatted = f.getFormattedSql();
			String expected = "--comment" + nl + "SELECT *" + nl + "FROM blub;";
			assertEquals("Not correctly formatted", expected, formatted);
			
			sql = "select x from y union all select y from x";
			f = new SqlFormatter(sql,100);
			formatted = f.getFormattedSql();
			expected = "SELECT x" + nl + "FROM y" + nl + "UNION ALL" + nl + "SELECT y" + nl + "FROM x";
//			System.out.println("Formatted: " + StringUtil.escapeUnicode(formatted, CharacterRange.RANGE_NONE));
//			System.out.println("expected: " + StringUtil.escapeUnicode(expected, CharacterRange.RANGE_NONE));
			assertEquals(expected, formatted);

			sql = "select x,y,z from y where a = 1 and b = 2";
			f = new SqlFormatter(sql,100);
			formatted = f.getFormattedSql();
			expected = "SELECT x," + nl + "       y," + nl + "       z" + nl + "FROM y" + nl + "WHERE a = 1" + nl + "AND   b = 2";
			assertEquals(expected, formatted);
			
			sql = "select x,y,z from y where a = 1 and b = (select min(x) from y)";
			f = new SqlFormatter(sql,100);
			formatted = f.getFormattedSql();
			expected = "SELECT x," + nl + "       y," + nl + "       z" + nl + "FROM y" + nl + "WHERE a = 1" + nl + "AND   b = (SELECT MIN(x) FROM y)";
			assertEquals(expected, formatted);
			
			sql = "select x,y,z from y where a = 1 and b = (select min(x) from y)";
			f = new SqlFormatter(sql,10);
			formatted = f.getFormattedSql();
			System.out.println(formatted);
			expected = "SELECT x," + nl + "       y," + nl + "       z" + nl + "FROM y" + nl + "WHERE a = 1" + nl + "AND   b = (SELECT MIN(x) " + nl + "           FROM y)";
			System.out.println(expected);
			assertEquals(expected, formatted);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	
}
