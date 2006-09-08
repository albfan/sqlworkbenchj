/*
 * WbManagerTest.java
 * JUnit based test
 *
 * Created on August 28, 2006, 7:10 PM
 */

package workbench;

import java.io.File;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.Statement;
import junit.framework.TestCase;
import workbench.db.WbConnection;
import workbench.util.EncodingUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbManagerTest extends TestCase
{
	
	public WbManagerTest(String testName)
	{
		super(testName);
	}

	private static final String UMLAUTS = "\u00f6\u00e4\u00fc";

	private String createScript(String basedir)
	{
		File f = new File(basedir, "batch_script.sql");
		PrintWriter w = null;
		try
		{
			w = new PrintWriter(EncodingUtil.createWriter(f, "UTF8", false));
			w.println("create table batch_test (nr integer, name varchar(100));\n");
			w.println("insert into batch_test (nr, name) values (1, '" + UMLAUTS + "');\n");
			w.println("commit;\n");
			w.close();
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return f.getAbsolutePath();
	}
	
	public void testBatchMode()
	{
		try
		{
			TestUtil util = new TestUtil();
			util.prepareBaseDir();
			System.setProperty("workbench.system.doexit", "false");
			String script = createScript(util.getBaseDir());
			String args[] = { "-embedded", 
												"-nosettings",
												"-configdir=" + util.getBaseDir(),
												"-url='jdbc:hsqldb:" + util.getDbName() + ";shutdown=true'",
												"-user=sa",
												"-driver=org.hsqldb.jdbcDriver",
												"-script='" + script + "'",
												"-encoding=UTF8"
												};
			WbManager.main(args);
			WbConnection con = util.getConnection();
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select nr, name from batch_test");
			if (rs.next())
			{
				int nr = rs.getInt(1);
				String name = rs.getString(2);
				assertEquals("Wrong id retrieved", 1, nr);
				assertEquals("Wronb name retrieved", UMLAUTS, name);
			}
			rs.close();
			stmt.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


}
