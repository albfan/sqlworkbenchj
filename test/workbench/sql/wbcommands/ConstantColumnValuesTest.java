/*
 * StaticColumnValuesTest.java
 * JUnit based test
 *
 * Created on June 4, 2007, 3:46 PM
 */

package workbench.sql.wbcommands;

import java.util.Map;
import junit.framework.TestCase;
import workbench.db.ColumnIdentifier;

/**
 *
 * @author tkellerer
 */
public class ConstantColumnValuesTest extends TestCase
{
	
	public ConstantColumnValuesTest(String testName)
	{
		super(testName);
	}

  public void testGetStaticValues()
  {
    ConstantColumnValues parser = new ConstantColumnValues("test_run_id=42,title=\"hello, world\"");
    Map<ColumnIdentifier, String> result = parser.getStaticValues();
		for (Map.Entry<ColumnIdentifier, String> entry : result.entrySet())
		{
			String colValue = entry.getValue();
			if (entry.getKey().getColumnName().equalsIgnoreCase("test_run_id"))
			{
				assertEquals("42", colValue);
			}
			if (entry.getKey().getColumnName().equalsIgnoreCase("title"))
			{
				assertEquals("hello, world", colValue);
			}
		}
  }
	
}
