/*
 * VersionNumberTest.java
 * JUnit based test
 *
 * Created on April 13, 2007, 10:41 PM
 */

package workbench.util;

import junit.framework.TestCase;

/**
 *
 * @author support@sql-workbench.net
 */
public class VersionNumberTest extends TestCase
{
	
	public VersionNumberTest(String testName)
	{
		super(testName);
	}
	
	public void testVersion()
	{
		VersionNumber one = new VersionNumber("94");
		assertEquals(one.getMajorVersion(), 94);
		assertEquals(one.getMinorVersion(), -1);
		
		VersionNumber two = new VersionNumber("94.2");
		assertEquals(two.getMajorVersion(), 94);
		assertEquals(two.getMinorVersion(), 2);
		
		assertTrue(two.isNewerThan(one));
		
		VersionNumber na = new VersionNumber(null);
		assertFalse(na.isNewerThan(two));
	}
	
}
