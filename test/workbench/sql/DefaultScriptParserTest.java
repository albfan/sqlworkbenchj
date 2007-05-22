/*
 * DefaultScriptParserTest.java
 * JUnit based test
 *
 * Created on 18. Mai 2007, 11:03
 */

package workbench.sql;

import junit.framework.TestCase;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.StringUtil;

/**
 *
 * @author thomas
 */
public class DefaultScriptParserTest extends TestCase
{
	
	public DefaultScriptParserTest(String testName)
	{
		super(testName);
	}

  public void testGetNextCommand()
  {
    String sql = "-- test select\nselect * from dual;\nupdate bla set x = 5;";
    DefaultScriptParser parser = new DefaultScriptParser(sql);

		String cmd = null;
		while ((cmd = parser.getNextCommand()) != null)
		{
			System.out.println("cmd=" + cmd);
		}
		
  }
	
}
