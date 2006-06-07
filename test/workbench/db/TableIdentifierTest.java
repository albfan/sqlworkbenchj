/*
 * TableIdentifierTest.java
 * JUnit based test
 *
 * Created on 16. Mai 2006, 10:59
 */

package workbench.db;

import java.sql.Connection;
import java.sql.DriverManager;
import junit.framework.*;

/**
 *
 * @author <a href="mailto:thomas.kellerer@mgm-tp.com">Thomas Kellerer</a>
 */
public class TableIdentifierTest extends TestCase
{
	
	public TableIdentifierTest(String testName)
	{
		super(testName);
	}

	protected void setUp() throws Exception
	{
	}

	protected void tearDown() throws Exception
	{
	}

	public void testIdentifier()
	{
		String sql = "BDB_IE.dbo.tblBDBMMPGroup";
		TableIdentifier tbl = new TableIdentifier(sql);
		assertEquals("BDB_IE", tbl.getCatalog());
		assertEquals("dbo", tbl.getSchema());
		assertEquals("BDB_IE.dbo.tblBDBMMPGroup", tbl.getTableExpression());
		
	}
	
}
