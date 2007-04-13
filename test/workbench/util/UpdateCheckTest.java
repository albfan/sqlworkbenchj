/*
 * UpdateCheckTest.java
 * JUnit based test
 *
 * Created on April 13, 2007, 11:57 PM
 */

package workbench.util;

import java.util.Date;
import junit.framework.TestCase;

/**
 *
 * @author support@sql-workbench.net
 */
public class UpdateCheckTest extends TestCase
{
	
	public UpdateCheckTest(String testName)
	{
		super(testName);
	}
	
	public void testNeedCheck()
	{
		try
		{
			UpdateCheck check = new UpdateCheck();
			int interval = 7;
			Date last = new Date(2007, 3, 10);
			Date now = new Date(2007, 3, 10);
			boolean need = check.needCheck(interval, now, last);
			assertFalse(need);

			now = new Date(2007, 3, 17);
			need = check.needCheck(interval, now, last);
			assertFalse(need);

			now = new Date(2007, 3, 18);
			need = check.needCheck(interval, now, last);
			assertTrue(need);

			need = check.needCheck(interval, now, null);
			assertTrue(need);
			
			need = check.needCheck(interval, new Date(), null);
			assertTrue(need);
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
}
