/*
 * OracleConstraintReaderTest.java
 * JUnit based test
 *
 * Created on March 21, 2007, 12:54 PM
 */

package workbench.db.oracle;

import junit.framework.TestCase;

/**
 *
 * @author tkellerer
 */
public class OracleConstraintReaderTest extends TestCase
{
	
	public OracleConstraintReaderTest(String testName)
	{
		super(testName);
	}
	
	protected void setUp() throws Exception
	{
		super.setUp();
	}
	
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}
	
	public void testIsDefaultNNConstraint()
	{
		OracleConstraintReader instance = new OracleConstraintReader();
		String definition = "\"MY_COL\" IS NOT NULL";
		boolean result = instance.isDefaultNNConstraint(definition);
		assertEquals("Default NN not recognized", true, result);
		
		definition = "\"MY_COL\" IS NOT NULL OR COL2 IS NOT NULL";
		result = instance.isDefaultNNConstraint(definition);
		assertEquals("Default NN not recognized", false, result);
		
	}
	
}
