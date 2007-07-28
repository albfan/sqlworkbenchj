/*
 * SourceTableArgumentTest.java
 * JUnit based test
 *
 * Created on July 4, 2007, 1:08 PM
 */

package workbench.sql.wbcommands;

import java.sql.Statement;
import java.util.List;
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.util.SqlUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class SourceTableArgumentTest
  extends TestCase
{

  public SourceTableArgumentTest(String testName)
  {
    super(testName);
  }

  public void testGetTables()
  {
    WbConnection con = null;
    Statement stmt = null;
    try
    {
      TestUtil util = new TestUtil("args");
      con = util.getConnection();
      stmt = con.createStatement();
      stmt.executeUpdate("create table arg_test (nr integer, data varchar(100))");
      con.commit();

      SourceTableArgument parser = new SourceTableArgument(null, con);
      List<TableIdentifier> tables = parser.getTables();
      assertEquals("Wrong number of table", 0, tables.size());

      parser = new SourceTableArgument(" ", con);
      tables = parser.getTables();
      assertEquals("Wrong number of table", 0, tables.size());
			
      parser = new SourceTableArgument("first_table, second_table, myschema.third_table", con);
      tables = parser.getTables();
      assertEquals("Wrong number of table", 3, tables.size());
      assertEquals("Wrong table retrieved", true, tables.get(0).getTableName().equalsIgnoreCase("first_table"));
      assertEquals("Wrong table retrieved", true, tables.get(1).getTableName().equalsIgnoreCase("second_table"));
      assertEquals("Wrong table retrieved", true, tables.get(2).getTableName().equalsIgnoreCase("third_table"));
      assertEquals("Wrong table retrieved", true, tables.get(2).getSchema().equalsIgnoreCase("myschema"));

      parser = new SourceTableArgument("*", con);
      tables = parser.getTables();
      assertEquals("Wrong number of table", 1, tables.size());
      assertEquals("Wrong table retrieved", true, tables.get(0).getTableName().equalsIgnoreCase("arg_test"));
    }
    catch (Exception e)
    {
      e.printStackTrace();
      fail(e.getMessage());
    }
    finally
    {
      SqlUtil.closeStatement(stmt);
      ConnectionMgr.getInstance().disconnectAll();
    }
  }
}
