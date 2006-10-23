/*
 * TableCreatorTest.java
 * JUnit based test
 *
 * Created on October 13, 2006, 1:21 PM
 */

package workbench.db;

import junit.framework.TestCase;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import workbench.TestUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class TableCreatorTest extends TestCase
{
	private TestUtil util;
	
	public TableCreatorTest(String testName)
	{
		super(testName);
		try
		{
			util = new TestUtil();
			util.prepareEnvironment();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void setUp()
		throws Exception
	{
		super.setUp();
		util.emptyBaseDirectory();
	}
	
	public void testCreateTable() 
		throws Exception
	{
		
		try
		{
			WbConnection con = util.getConnection();
			Statement stmt = con.createStatement();
			stmt.executeUpdate("CREATE TABLE create_test (zzz integer, bbb integer, aaa integer, ccc integer)");
			TableIdentifier oldTable = new TableIdentifier("create_test");
			TableIdentifier newTable = new TableIdentifier("new_table");
			
			ColumnIdentifier[] cols  = con.getMetadata().getColumnIdentifiers(oldTable);
			ColumnIdentifier c = cols[0];
			cols[0] = cols[2];
			cols[2] = c;
			
			TableCreator creator = new TableCreator(con, newTable, cols);
			creator.createTable();
			
			cols = con.getMetadata().getColumnIdentifiers(newTable);
			assertEquals(4, cols.length);
			
			assertEquals("ZZZ", cols[0].getColumnName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
	
}
