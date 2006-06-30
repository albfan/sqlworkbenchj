/*
 * WbExportTest.java
 * JUnit based test
 *
 * Created on June 26, 2006, 7:15 PM
 */

package workbench.sql.wbcommands;

import junit.framework.*;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import workbench.WbManager;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.sql.ScriptCommandDefinition;
import workbench.sql.ScriptParser;
import workbench.sql.StatementRunnerResult;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbExportTest extends TestCase
{
	private String dbName;
	private String basedir;
	private final int rowcount = 10;
	private WbExport exportCmd = new WbExport();
	private WbConnection connection;
	
	public WbExportTest(String testName)
	{
		super(testName);
		try
		{
			File tempdir = new File(System.getProperty("java.io.tmpdir"));
			File dir = new File(tempdir, "wbtest");
			dir.mkdir();
			basedir = dir.getAbsolutePath();
			File db = new File(basedir, "wbexporttest");
			dbName = db.getAbsolutePath();
			WbManager.getInstance().prepareForTest(basedir);
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	protected void setUp() throws Exception
	{
		this.connection = prepareDatabase();
		this.exportCmd.setConnection(this.connection);
	}

	protected void tearDown() throws Exception
	{
		this.connection.disconnect();
	}

	public void testTextExport() throws Exception
	{
		try
		{
			File exportFile = new File(this.basedir, "export.txt");
			StatementRunnerResult result = exportCmd.execute(this.connection, "wbexport -file='" + exportFile.getAbsolutePath() + "' -type=text -header=true -sourcetable=junit_test -writeoracleloader=true");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);
			
			assertEquals("Export file not created", true, exportFile.exists());
			// WbExport creates an empty line at the end plus the header line
			// we end up with rowcount + 2 lines in the export file
			assertEquals("Wrong number of lines", rowcount + 1, FileUtil.countLines(exportFile));
			
			File ctl = new File(this.basedir, "export.ctl");
			assertEquals("Control file not created", true, ctl.exists());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testSqlExport() throws Exception
	{
		try
		{
			File exportFile = new File(this.basedir, "export.sql");
			StatementRunnerResult result = exportCmd.execute(this.connection, "wbexport -file='" + exportFile.getAbsolutePath() + "' -type=sql -sourcetable=junit_test -table=other_table");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);
			
			assertEquals("Export file not created", true, exportFile.exists());
			
			ScriptParser p = new ScriptParser(1024*1024);
			p.setFile(exportFile);
			List l = p.getCommands();
			assertEquals("Wrong number of statements", rowcount + 1, l.size());
			String sql = p.getCommand(0);
			String verb = SqlUtil.getSqlVerb(sql);
			assertEquals("Not an insert file", "INSERT", verb);
			String table = SqlUtil.getInsertTable(sql);
			assertNotNull("No insert table found", table);
			assertEquals("Wrong target table", "OTHER_TABLE", table.toUpperCase());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testXmlExport() throws Exception
	{
		try
		{
			StatementRunnerResult result = exportCmd.execute(this.connection, "wbexport -outputdir='" + basedir + "' -type=xml -sourcetable=*");
			assertEquals("Export failed: " + result.getMessageBuffer().toString(), result.isSuccess(), true);

			File dir = new File(basedir);
			
			File[] files = dir.listFiles();
			int xmlFiles = 0;
			for (int i = 0; i < files.length; i++)
			{
				if (files[i].getAbsolutePath().endsWith(".xml")) xmlFiles ++;
			}
			assertEquals("Not all tables exported", 2, xmlFiles);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private WbConnection prepareDatabase()
		throws SQLException, ClassNotFoundException
	{
		File dir = new File(basedir);
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++)
		{
			files[i].delete();
		}
		Class.forName("org.hsqldb.jdbcDriver");
		String url = "jdbc:hsqldb:" + dbName + ";shutdown=true";
		Connection con = DriverManager.getConnection(url, "sa", "");
		Statement stmt = con.createStatement();
		stmt.executeUpdate("CREATE TABLE junit_test (nr integer primary key, firstname varchar(100), lastname varchar(100))");
		PreparedStatement pstmt = con.prepareStatement("insert into junit_test (nr, firstname, lastname) values (?,?,?)");
		for (int i=0; i < rowcount; i ++)
		{
			pstmt.setInt(1, i);
			pstmt.setString(2, "FirstName" + i);
			pstmt.setString(3, "LastName" + i);
			pstmt.executeUpdate();
		}
		con.commit();
		
		stmt.executeUpdate("CREATE TABLE person (nr integer primary key, firstname varchar(100), lastname varchar(100))");
		pstmt = con.prepareStatement("insert into person (nr, firstname, lastname) values (?,?,?)");
		for (int i=0; i < rowcount; i ++)
		{
			pstmt.setInt(1, i);
			pstmt.setString(2, "FirstName" + i);
			pstmt.setString(3, "LastName" + i);
			pstmt.executeUpdate();
		}
		con.commit();
		pstmt.close();
		stmt.close();
		
		WbConnection wb = new WbConnection(con);
		
		// Create a dummy profiles as the profile
		// is used all around the Wb sources
		ConnectionProfile prof = new ConnectionProfile();
		prof.setAutocommit(false);
		prof.setEmptyStringIsNull(true);
		prof.setIgnoreDropErrors(true);
		prof.setIncludeNullInInsert(true);
		prof.setName("JunitTest");
		prof.setDriverName("HSQLDB");
		prof.setDriverclass("org.hsqldb.jdbcDriver");
		prof.setUrl(url);
		prof.setPassword("");
		wb.setProfile(prof);
		return wb;
	}	
}
