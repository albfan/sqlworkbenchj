/*
 * SqlUtilTest.java
 * JUnit based test
 *
 * Created on May 11, 2006, 9:27 PM
 */

package workbench.util;

import junit.framework.*;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.db.ColumnIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.sql.formatter.SqlFormatter;
import workbench.sql.formatter.Token;

/**
 *
 * @author support@sql-workbench.net
 */
public class SqlUtilTest 
	extends TestCase
{
	
	public SqlUtilTest(String testName)
	{
		super(testName);
	}

	protected void setUp() throws Exception
	{
	}

	protected void tearDown() throws Exception
	{
	}

	public void testGetSqlVerb()
	{
		String sql = "-- comment line1\nSELECT";
		String verb = SqlUtil.getSqlVerb(sql);
		assertEquals("SELECT", verb);
		
		sql = "-- comment line1\n-- second line\n\n /* bla */\nSELECT";
		verb = SqlUtil.getSqlVerb(sql);
		assertEquals("SELECT", verb);
		
			sql = "/* \n" + 
             "* $URL: svn+ssh://nichdexp.nichd.nih.gov/subversion/mtrac/trunk/db/00-release-1.0/01-trac-8-ddl.sql $ \n" + 
             "* $Revision: 1.1 $ \n" + 
             "* Created by Janek K. Claus. \n" + 
             "* $LastChangedBy: clausjan $ \n" + 
             "* $LastChangedDate: 2006-05-05 20:29:15 -0400 (Fri, 05 May 2006) $ \n" + 
             "*/ \n" + 
             "-- This is the initial creation script for the MTrac database. \n" + 
             "-- It assumes the database space and schema have been setup. \n" + 
             "-- Each table can be indivually recreated, if you add foreign keys, make sure that they will be deleted in all \n" + 
             "-- associated tables before dropping. \n" + 
             " \n" + 
             "-- ############################################# \n" + 
             "-- ##                                         ## \n" + 
             "-- ##              Organizations              ## \n" + 
             "-- ##                                         ## \n" + 
             "alter table participants drop constraint r_05;   -- make sure you recreate this foreign key after inserting data! \n";

			verb = SqlUtil.getSqlVerb(sql);
			assertEquals("ALTER", verb);
	}

	public void testGetTables()
	{
		String sql = "select *\nfrom\n-- list the tables here\ntable1 t1, table2 t2, table3 t3";
		List l = SqlUtil.getTables(sql, false);
		assertEquals(3, l.size());
		
		assertEquals("table1", (String)l.get(0));
		assertEquals("table2", (String)l.get(1));
		assertEquals("table3", (String)l.get(2));
		
		l = SqlUtil.getTables(sql, true);
		assertEquals(3, l.size());
		
		assertEquals("table1 t1", (String)l.get(0));
		assertEquals("table2 t2", (String)l.get(1));
		assertEquals("table3 t3", (String)l.get(2));		
		
		sql = "SELECT cr.dealid, \n" + 
					 "       cs.state, \n" + 
					 "       bq.* \n" + 
					 "FROM dbo.tblcreditrow cr  \n" + 
					 "INNER JOIN bdb_ie.dbo.tblbdbproduct p ON cr.partnumber = p.partnumber  \n" + 
					 "RIGHT OUTER JOIN tblbidquantity bq ON bq.partnumber LIKE p.mainpartnumber + '%'AND bq.bidid = cr.bidid  \n" + 
					 "INNER JOIN tblcredit c ON c.creditid = cr.creditid  \n" + 
					 "INNER JOIN tblcreditstate cs ON cs.creditstateid = c.creditstateid \n" + 
					 "WHERE c.arrivaldate >= '2006-04-01'";
		
		l = SqlUtil.getTables(sql, true);
		assertEquals(5, l.size());
		assertEquals("dbo.tblcreditrow cr", (String)l.get(0));
		assertEquals("bdb_ie.dbo.tblbdbproduct p", (String)l.get(1));
		assertEquals("tblbidquantity bq", (String)l.get(2));
		assertEquals("tblcredit c", (String)l.get(3));
		assertEquals("tblcreditstate cs", (String)l.get(4));
		
		l = SqlUtil.getTables(sql, false);
		assertEquals(5, l.size());
		assertEquals("dbo.tblcreditrow", (String)l.get(0));
		assertEquals("bdb_ie.dbo.tblbdbproduct", (String)l.get(1));
		assertEquals("tblbidquantity", (String)l.get(2));
		assertEquals("tblcredit", (String)l.get(3));
		assertEquals("tblcreditstate", (String)l.get(4));
		
	}

}
