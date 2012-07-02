/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.oracle;

import org.junit.Test;
import workbench.WbTestCase;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleStatementHookTest
	extends WbTestCase
{
	public OracleStatementHookTest()
	{
	}

	/**
	 * Test of injectHint method, of class OracleStatementHook.
	 */
	@Test
	public void testInjectHint()
	{
		String sql = "select * from foo";
		OracleStatementHook hook = new OracleStatementHook();
		String expResult = "select /*+ gather_plan_statistics */ * from foo";
		String result = hook.injectHint(sql);
		assertEquals(expResult, result);

		sql = "select /*+qb_name(foo) */ * from foo";
		expResult = "select /*+ gather_plan_statistics qb_name(foo) */ * from foo";
		result = hook.injectHint(sql);
		assertEquals(expResult, result);

		sql = "select /*+ qb_name(foo) */ * from foo";
		expResult = "select /*+ gather_plan_statistics  qb_name(foo) */ * from foo";
		result = hook.injectHint(sql);
		assertEquals(expResult, result);

		sql = "select /* do stuff */ * from foo";
		expResult = "select /*+ gather_plan_statistics */ /* do stuff */ * from foo";
		result = hook.injectHint(sql);
		assertEquals(expResult, result);
	}

}
