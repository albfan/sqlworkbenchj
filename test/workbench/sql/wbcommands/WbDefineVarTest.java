/*
 * WbDefineVarTest.java
 * JUnit based test
 *
 * Created on 24. Juli 2006, 19:16
 */

package workbench.sql.wbcommands;

import junit.framework.*;
import workbench.TestUtil;
import workbench.sql.VariablePool;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbDefineVarTest extends TestCase
{
	
	public WbDefineVarTest(String testName)
	{
		super(testName);
		try
		{
			TestUtil util = new TestUtil();
			util.prepareEnvironment();
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	protected void setUp() throws Exception
	{
	}

	protected void tearDown() throws Exception
	{
	}

	public void testExecute() throws Exception
	{
		try
		{
			String sql = WbDefineVar.DEFINE_SHORT.getVerb() + " vari = 42";
			WbDefineVar.DEFINE_SHORT.execute(null,sql);
			String value = VariablePool.getInstance().getParameterValue("vari");
			assertEquals("Wrong variable value", "42", value);
			
			String replaced = VariablePool.getInstance().replaceAllParameters("select name from person where id = $[vari]");
			assertEquals("Wrong replacement", "select name from person where id = 42", replaced);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail("Error when defining variable");
		}
	}
	
}
