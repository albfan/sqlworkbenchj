/*
 * SqlFormatterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.formatter;

import java.util.List;
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.resource.Settings;
import workbench.util.CollectionBuilder;

/**
 *
 * @author support@sql-workbench.net
 */
public class SqlFormatterTest
	extends TestCase
{
	public SqlFormatterTest(String testName)
	{
		super(testName);
	}

	public void setUp()
		throws Exception
	{
		TestUtil util = new TestUtil(this.getName());
		util.prepareEnvironment();
	}

	public void testSubSelect()
		throws Exception
	{
		String sql = "SELECT state, SUM(numorders) as numorders, SUM(pop) as pop \n" +
             "FROM ((SELECT o.state, COUNT(*) as numorders, 0 as pop \n" +
             "FROM orders o \n" +
             "GROUP BY o.state)) \n";
		String expected = "SELECT state,\n" +
             "       SUM(numorders) AS numorders,\n" +
             "       SUM(pop) AS pop\n" +
             "FROM ((SELECT o.state,\n" +
             "              COUNT(*) AS numorders,\n" +
             "              0 AS pop\n" +
             "       FROM orders o\n" +
             "       GROUP BY o.state))";
		SqlFormatter f = new SqlFormatter(sql);
		String formatted = f.getFormattedSql().toString();
//		System.out.println("+++++++++++++++++++ result: \n" + formatted + "\n********** expected:\n" + expected + "\n-------------------");
		assertEquals(expected, formatted);
		
		sql = "SELECT state, SUM(numorders) as numorders, SUM(pop) as pop \n" +
             "FROM ((SELECT o.state, COUNT(*) as numorders, 0 as pop \n" +
             "FROM orders o \n" +
             "GROUP BY o.state) \n" +
             "UNION ALL \n" +
             "(SELECT state, 0 as numorders, SUM(pop) as pop \n" +
             "FROM zipcensus \n" +
             "GROUP BY state)) summary \n" +
             "GROUP BY state \n" +
             "ORDER BY 2 DESC";
		f = new SqlFormatter(sql);
		formatted = f.getFormattedSql().toString();
		expected = "SELECT state,\n" +
             "       SUM(numorders) AS numorders,\n" +
             "       SUM(pop) AS pop\n" +
             "FROM ((SELECT o.state,\n" +
             "              COUNT(*) AS numorders,\n" +
             "              0 AS pop\n" +
             "       FROM orders o\n" +
             "       GROUP BY o.state)\n" +
             "       UNION ALL\n" +
             "       (SELECT state,\n" +
             "              0 AS numorders,\n" +
             "              SUM(pop) AS pop\n" +
             "       FROM zipcensus\n" +
             "       GROUP BY state)) summary\n" +
             "GROUP BY state\n" +
             "ORDER BY 2 DESC";
//		System.out.println("+++++++++++++++++++ result: \n" + formatted + "\n********** expected:\n" + expected + "\n-------------------");
		assertEquals(expected, formatted);
	}

	public void testUnion()
		throws Exception
	{
		String sql =
			"SELECT e.ManagerID, e.EmployeeID, e.Title, edh.DepartmentID,  \n" +
             "        0 AS Level \n" +
             "    FROM HumanResources.Employee AS e \n" +
             "    INNER JOIN HumanResources.EmployeeDepartmentHistory AS edh \n" +
             "        ON e.EmployeeID = edh.EmployeeID AND edh.EndDate IS NULL \n" +
             "    WHERE ManagerID IS NULL \n" +
             "    UNION ALL \n" +
             "    SELECT e.ManagerID, e.EmployeeID, e.Title, edh.DepartmentID, \n" +
             "        Level + 1 \n" +
             "    FROM HumanResources.Employee AS e \n" +
             "    INNER JOIN HumanResources.EmployeeDepartmentHistory AS edh \n" +
             "        ON e.EmployeeID = edh.EmployeeID AND edh.EndDate IS NULL \n" +
             "    INNER JOIN DirectReports AS d \n" +
             "        ON e.ManagerID = d.EmployeeID";
		SqlFormatter f = new SqlFormatter(sql);
		String formatted = f.getFormattedSql().toString();
		String expected = "SELECT e.ManagerID,\n" +
             "       e.EmployeeID,\n" +
             "       e.Title,\n" +
             "       edh.DepartmentID,\n" +
             "       0 AS LEVEL\n" +
             "FROM HumanResources.Employee AS e \n" +
             "     INNER JOIN HumanResources.EmployeeDepartmentHistory AS edh ON e.EmployeeID = edh.EmployeeID AND edh.EndDate IS NULL\n" +
             "WHERE ManagerID IS NULL\n" +
             "UNION ALL\n" +
             "SELECT e.ManagerID,\n" +
             "       e.EmployeeID,\n" +
             "       e.Title,\n" +
             "       edh.DepartmentID,\n" +
             "       LEVEL+1\n" +
             "FROM HumanResources.Employee AS e \n" +
             "     INNER JOIN HumanResources.EmployeeDepartmentHistory AS edh ON e.EmployeeID = edh.EmployeeID AND edh.EndDate IS NULL \n" +
             "     INNER JOIN DirectReports AS d ON e.ManagerID = d.EmployeeID";
//		System.out.println("+++++++++++++++++++ result: \n" + formatted + "\n********** expected:\n" + expected + "\n-------------------");
		assertEquals(expected, formatted);

		sql = "(SELECT o.state, COUNT(*) as numorders, 0 as pop \n" +
             "FROM orders o \n" +
             "GROUP BY o.state) \n" +
             "UNION ALL \n" +
             "(SELECT state, 0 as numorders, SUM(pop) as pop \n" +
             "FROM zipcensus \n" +
             "GROUP BY state)";
		f = new SqlFormatter(sql);
		formatted = f.getFormattedSql().toString();
		expected = "(SELECT o.state,\n" +
             "       COUNT(*) AS numorders,\n" +
             "       0 AS pop\n" +
             "FROM orders o\n" +
             "GROUP BY o.state)\n" +
             "UNION ALL\n" +
             "(SELECT state,\n" +
             "       0 AS numorders,\n" +
             "       SUM(pop) AS pop\n" +
             "FROM zipcensus\n" +
             "GROUP BY state)";
//		System.out.println("+++++++++++++++++++ result: \n" + formatted + "\n********** expected:\n" + expected + "\n-------------------");
		assertEquals(expected, formatted);

	}

	public void testKeywordsAsFunction()
		throws Exception
	{
		String sql = "SELECT right(name,5) FROM person";
		SqlFormatter f = new SqlFormatter(sql);
		f.setUseLowerCaseFunctions(true);
		f.setDBFunctions(CollectionBuilder.hashSet("RIGHT", "LEFT"));
		String formatted = f.getFormattedSql().toString();
		System.out.println("*******\n" + formatted + "\n**********");
		String expected = "SELECT right(name,5)\nFROM person";
		assertEquals(expected, formatted);
	}
	
	public void testWbVars()
		throws Exception
	{
		String sql = "SELECT * FROM mytable WHERE id in ($[somestuff])";
		SqlFormatter f = new SqlFormatter(sql);
		String formatted = f.getFormattedSql().toString();
//		System.out.println("*******\n" + formatted + "\n**********");
		String expected = "SELECT *\nFROM mytable\nWHERE id IN ($[somestuff])";
		assertEquals(expected, formatted);
		
		sql = "SELECT * FROM mytable WHERE id in ($[&somestuff])";
		f = new SqlFormatter(sql);
		formatted = f.getFormattedSql().toString();
		expected = "SELECT *\nFROM mytable\nWHERE id IN ($[&somestuff])";
		assertEquals(expected, formatted);
	}
	
	public void testCTE()
		throws Exception
	{
		String sql = "WITH RECURSIVE DirectReports (ManagerID, EmployeeID, Title, DeptID, Level) \n" +
             "AS \n" +
             "( \n" +
             "-- Anchor member definition \n" +
             "    SELECT e.ManagerID, e.EmployeeID, e.Title, edh.DepartmentID,  \n" +
             "        0 AS Level \n" +
             "    FROM HumanResources.Employee AS e \n" +
             "    INNER JOIN HumanResources.EmployeeDepartmentHistory AS edh \n" +
             "        ON e.EmployeeID = edh.EmployeeID AND edh.EndDate IS NULL \n" +
             "    WHERE ManagerID IS NULL \n" +
             "    UNION ALL \n" +
             "-- Recursive member definition \n" +
             "    SELECT e.ManagerID, e.EmployeeID, e.Title, edh.DepartmentID, \n" +
             "        Level + 1 \n" +
             "    FROM HumanResources.Employee AS e \n" +
             "    INNER JOIN HumanResources.EmployeeDepartmentHistory AS edh \n" +
             "        ON e.EmployeeID = edh.EmployeeID AND edh.EndDate IS NULL \n" +
             "    INNER JOIN DirectReports AS d \n" +
             "        ON e.ManagerID = d.EmployeeID \n" +
             ") \n" +
             "-- Statement that executes the CTE \n" +
             "SELECT ManagerID, EmployeeID, Title, Level \n" +
             "FROM DirectReports \n" +
             "INNER JOIN HumanResources.Department AS dp \n" +
             "    ON DirectReports.DeptID = dp.DepartmentID \n" +
             "WHERE dp.GroupName = N'Research and Development' OR Level = 0";

		SqlFormatter f = new SqlFormatter(sql);
		String formatted = f.getFormattedSql().toString();
		String expected =
						"WITH RECURSIVE DirectReports (ManagerID, EmployeeID, Title, DeptID, LEVEL) \n" +
						"AS\n" +
						"(\n" +
						"  -- Anchor member definition \n" +
						"  SELECT e.ManagerID,\n" +
						"         e.EmployeeID,\n" +
						"         e.Title,\n" +
						"         edh.DepartmentID,\n" +
						"         0 AS LEVEL\n" +
						"  FROM HumanResources.Employee AS e \n" +
						"       INNER JOIN HumanResources.EmployeeDepartmentHistory AS edh ON e.EmployeeID = edh.EmployeeID AND edh.EndDate IS NULL\n" +
						"  WHERE ManagerID IS NULL\n" +
						"  UNION ALL\n" +
						"  -- Recursive member definition \n" +
						"  SELECT e.ManagerID,\n" +
						"         e.EmployeeID,\n" +
						"         e.Title,\n" +
						"         edh.DepartmentID,\n" +
						"         LEVEL+1\n" +
						"  FROM HumanResources.Employee AS e \n" +
						"       INNER JOIN HumanResources.EmployeeDepartmentHistory AS edh ON e.EmployeeID = edh.EmployeeID AND edh.EndDate IS NULL \n" +
						"       INNER JOIN DirectReports AS d ON e.ManagerID = d.EmployeeID\n" +
						")\n" +
						"-- Statement that executes the CTE \n" +
						"SELECT ManagerID,\n" +
						"       EmployeeID,\n" +
						"       Title,\n" +
						"       LEVEL\n" +
						"FROM DirectReports \n" +
						"     INNER JOIN HumanResources.Department AS dp ON DirectReports.DeptID = dp.DepartmentID\n" +
						"WHERE dp.GroupName = N 'Research and Development'\n" +
						"OR    LEVEL = 0";

//		System.out.println("+++++++++++++++++++\n" + formatted + "\n**********\n" + expected + "\n-------------------");
		assertEquals(expected, formatted);
		sql = "with tmp as\n" +
			"(SELECT *\n" +
			"FROM users\n" +
			") select tmp.*,nvl((select 1 from td_cdma_ip where tmp.src_ip between\n"+
			"ip_fromip and ip_endip),0) isNew\n"+
			"from tmp ";

		expected = "WITH tmpAS\n" +
							"(\n" +
							"  SELECT * FROM users\n" +
							")\n" +
							"SELECT tmp. *,\n" +
							"       nvl((SELECT 1 FROM td_cdma_ip WHERE tmp.src_ip BETWEEN ip_fromip AND ip_endip),0) isNew\n" +
							"FROM tmp";
		f = new SqlFormatter(sql);
		formatted = f.getFormattedSql().toString();
//		System.out.println("++++++++++++++++++\n" + formatted + "\n**********\n" + expected + "\n-------------------");
		assertEquals(expected, formatted);


		// Make sure a WITH in a different statement is not mistaken for a CTE
		sql = "CREATE VIEW vfoo \n" +
					"AS \n" +
					"SELECT id, name FROM foo WHERE id BETWEEN 1 AND 10000 \n" +
					"WITH CHECK OPTION";
		f = new SqlFormatter(sql);
		formatted = f.getFormattedSql().toString();

		expected = "CREATE VIEW vfoo \n" +
							"AS\n" +
							"SELECT id,\n" +
							"       name\n" +
							"FROM foo\n" +
							"WHERE id BETWEEN 1\n" +
							"AND   10000 WITH CHECK OPTION";
//		System.out.println("++++++++++++++++++\n" + formatted + "\n**********\n" + expected + "\n-------------------");
		assertEquals(expected, formatted);
	}

	public void testCTAS()
		throws Exception
	{
		String sql = "CREATE table cust as select * from customers where rownum <= 1000";
		String expected =
				"CREATE TABLE cust \n"+
				"AS\n"+
				"SELECT *\n" +
				"FROM customers\n" +
				"WHERE rownum <= 1000";
		SqlFormatter f = new SqlFormatter(sql);
		String formatted = f.getFormattedSql().toString();
//			System.out.println("**************\n" + formatted + "\n**********\n" + expected + "\n*************");
		assertEquals(expected, formatted);
	}

	public void testUnknown()
		throws Exception
	{
		String sql =
					"SELECT e.ename AS employee, \n" +
					"       CASE row_number() over (PARTITION BY d.deptno ORDER BY e.empno) \n" +
					"         WHEN 1 THEN d.dname \n" +
					"         ELSE NULL \n" +
					"       END AS department \n" +
					"FROM emp e\n" +
					"     INNER JOIN dept d ON (e.deptno = d.deptno) \n" +
					"ORDER BY d.deptno, \n" +
					"         e.empno";

		String expected =
			"SELECT e.ename AS employee,\n" +
			"       CASE row_number() OVER (PARTITION BY d.deptno ORDER BY e.empno)\n" +
			"         WHEN 1 THEN d.dname\n" +
			"         ELSE NULL\n" +
			"       END AS department\n" +
			"FROM emp e \n" +
			"     INNER JOIN dept d ON (e.deptno = d.deptno)\n" +
			"ORDER BY d.deptno,\n" +
			"         e.empno";

		SqlFormatter f = new SqlFormatter(sql);
		String formatted = f.getFormattedSql().toString();
//			System.out.println("**************\n" + formatted + "\n**********\n" + expected);
		assertEquals(expected, formatted);
	}

	public void testLowerCaseKeywords()
		throws Exception
	{
		try
		{
			String sql = "SELECT foo FROM bar";
			String expected =
				"select foo\n" +
				"from bar";
			Settings.getInstance().setFormatterUpperCaseKeywords(false);
			SqlFormatter f = new SqlFormatter(sql);
			String formatted = f.getFormattedSql().toString();
//			System.out.println("**************\n" + formatted + "\n**********\n" + expected);
			assertEquals(expected, formatted);
		}
		finally
		{
			Settings.getInstance().setFormatterUpperCaseKeywords(true);
		}
	}
	public void testFormatInsert()
		throws Exception
	{
		try
		{
			Settings.getInstance().setFormatterMaxColumnsInInsert(3);
			String sql = "insert into x ( col1,col2,col3) values (1,2,3)";
			String expected = "INSERT INTO x\n  (col1, col2, col3) \nVALUES\n  (1, 2, 3)";
			SqlFormatter f = new SqlFormatter(sql, 100);
			String formatted = (String) f.getFormattedSql();
//			System.out.println("*********\n" + formatted + "\n---\n" + expected + "\n************");
			assertEquals(expected, formatted);

			Settings.getInstance().setFormatterMaxColumnsInInsert(3);
			sql = "insert into x ( col1,col2,col3,col4,col5) values (1,2,3,4,5)";
			expected = "INSERT INTO x\n  (col1, col2, col3,\n   col4, col5) \nVALUES\n  (1, 2, 3,\n   4, 5)";
			f = new SqlFormatter(sql, 100);
			formatted = (String) f.getFormattedSql();
//			System.out.println("*********\n" + formatted + "\n---\n" + expected + "\n************");
			assertEquals(expected, formatted);
		}
		finally
		{
			Settings.getInstance().setFormatterMaxColumnsInInsert(1);
		}
	}

	public void testFormatUpdate()
		throws Exception
	{
		try
		{
			Settings.getInstance().setFormatterMaxColumnsInUpdate(3);
			String sql = "update mytable set col1=5,col2=6,col3=4";
			String expected = "UPDATE mytable\n   SET col1 = 5, col2 = 6, col3 = 4";
			SqlFormatter f = new SqlFormatter(sql, 100);
			String formatted = (String) f.getFormattedSql();
//			System.out.println("*********\n" + formatted + "\n---\n" + expected + "\n************");
			assertEquals(expected, formatted);
			sql = "update mytable set col1=1,col2=2,col3=3,col4=4,col5=5";
			expected = "UPDATE mytable\n   SET col1 = 1, col2 = 2, col3 = 3,\n       col4 = 4, col5 = 5";
			f = new SqlFormatter(sql, 100);
			formatted = (String) f.getFormattedSql();
//			System.out.println("*********\n" + formatted + "\n--- expected\n" + expected + "\n************");
			assertEquals(expected, formatted);
		}
		finally
		{
			Settings.getInstance().setFormatterMaxColumnsInUpdate(1);
		}
	}

	public void testFormatUnicode()
		throws Exception
	{
		String sql = "insert into x(ss2,ss3,ss2) values('\u32A5\u0416','dsaffds',234)";
		String expected = "INSERT INTO x\n(\n  ss2,\n  ss3,\n  ss2\n) \nVALUES\n(\n  '\u32A5\u0416',\n  'dsaffds',\n  234\n)";
		SqlFormatter f = new SqlFormatter(sql, 100);
		String formatted = (String) f.getFormattedSql();
		assertEquals(expected, formatted);
	}

	public void testCreateTable()
		throws Exception
	{
		String sql = null;
		SqlFormatter f = null;
		String formatted = null;
		List<String> lines = null;

		sql = "create table person (id1 integer not null, id2 integer not null, id3 integer not null, firstname varchar(50), lastname varchar(50), primary key (id1, id2), foreign key (id3) references othertable(id));";
		f = new SqlFormatter(sql, 100);
		formatted = f.getFormattedSql().toString();
		lines = TestUtil.getLines(formatted);
		assertEquals("  id1           INTEGER NOT NULL,", lines.get(2));
		assertEquals("  PRIMARY KEY (id1,id2),", lines.get(7));
		assertEquals("  FOREIGN KEY (id3) REFERENCES othertable (id)", lines.get(8));

		sql = "create table person (somecol integer primary key, firstname varchar(50), lastname varchar(50));";
		f = new SqlFormatter(sql, 100);
		formatted = f.getFormattedSql().toString();
		lines = TestUtil.getLines(formatted);
		assertEquals("  somecol     INTEGER PRIMARY KEY,", lines.get(2));

		sql = "create table person (id1 integer not null, id2 integer not null, firstname varchar(50), lastname varchar(50), primary key (id1, id2));";
		f = new SqlFormatter(sql, 100);
		formatted = f.getFormattedSql().toString();
		lines = TestUtil.getLines(formatted);
		assertEquals("  id1           INTEGER NOT NULL,", lines.get(2));
		assertEquals("  PRIMARY KEY (id1,id2)", lines.get(6));
	}

	public void testFileParam()
		throws Exception
	{
		String sql = "wbexport -file=\"c:\\Documents and Settings\\test.txt\" -type=text";
		SqlFormatter f = new SqlFormatter(sql, 100);
		String formatted = f.getFormattedSql().toString();
		assertTrue(formatted.indexOf("\"c:\\Documents and Settings\\test.txt\"") > 0);
	}

	public void testWbConfirm()
		throws Exception
	{
		String sql = "wbconfirm 'my message'";
		SqlFormatter f = new SqlFormatter(sql, 100);
		CharSequence formatted = f.getFormattedSql();
		String expected = "WbConfirm 'my message'";
		assertEquals("WbConfirm not formatted correctly", expected, formatted);
	}

	public void testAliasForSubselect()
		throws Exception
	{
		String sql = "select a,b, (select a,b from t2) col4 from t1";
		SqlFormatter f = new SqlFormatter(sql, 100);
		CharSequence formatted = f.getFormattedSql();
		String expected = "SELECT a,\n" + "       b,\n" + "       (SELECT a, b FROM t2) col4\n" + "FROM t1";
		assertEquals("SELECT in VALUES not formatted", expected, formatted);
	}

	public void testAsInFrom()
		throws Exception
	{
		String sql = "select t1.a, t2.b from bla as t1, t2";
		SqlFormatter f = new SqlFormatter(sql, 100);
		CharSequence formatted = f.getFormattedSql();
		String expected = "SELECT t1.a,\n" + "       t2.b\n" + "FROM bla AS t1,\n" + "     t2";
		assertEquals("SELECT in VALUES not formatted", expected, formatted);
	}

	public void testInsertWithSubselect()
		throws Exception
	{
		String sql = "insert into tble (a,b) values ( (select max(x) from y), 'bla')";
		String expected = "INSERT INTO tble\n" + "(\n" + "  a,\n" + "  b\n" + ") \n" + "VALUES\n" + "(\n" + "   (SELECT MAX(x) FROM y),\n" + "  'bla'\n" + ")";
		SqlFormatter f = new SqlFormatter(sql, 100);
		CharSequence formatted = f.getFormattedSql();
//			System.out.println("**************\n" + formatted + "\n**********\n" + expected);
		assertEquals("SELECT in VALUES not formatted", expected, formatted);
	}

	public void testLowerCaseFunctions()
		throws Exception
	{
		String sql = "select col1, MAX(col2) from theTable group by col1;";
		String expected = "SELECT col1,\n       max(col2)\nFROM theTable\nGROUP BY col1;";
		SqlFormatter f = new SqlFormatter(sql, 100);
		f.setUseLowerCaseFunctions(true);
		CharSequence formatted = f.getFormattedSql();
//			System.out.println("**************\n" + formatted + "\n**********\n" + expected);
		assertEquals("SELECT in VALUES not formatted", expected, formatted);
	}

	public void testCase()
		throws Exception
	{
		String sql = "SELECT col1 as bla, case when x = 1 then 2 else 3 end AS y FROM person";
		String expected =
			"SELECT col1 AS bla,\n" +
			"       CASE\n" +
			"         WHEN x = 1 THEN 2\n" +
			"         ELSE 3\n" +
			"       END AS y\n" +
			"FROM person";
		SqlFormatter f = new SqlFormatter(sql, 100);
		CharSequence formatted = f.getFormattedSql();
		assertEquals("CASE alias not formatted", expected, formatted);

		sql = "SELECT case when x = 1 then 2 else 3 end AS y FROM person";
		expected =
			"SELECT CASE\n" +
			"         WHEN x = 1 THEN 2\n" +
			"         ELSE 3\n" +
			"       END AS y\n" +
			"FROM person";
		f = new SqlFormatter(sql, 100);
		formatted = f.getFormattedSql();
//			System.out.println("**************\n" + formatted + "\n**********\n" + expected);
		assertEquals("CASE alias not formatted", expected, formatted);

		sql = "SELECT a,b,c from table order by b,case when a=1 then 2 when a=2 then 1 else 3 end";
		expected =
			"SELECT a,\n" +
			"       b,\n" +
			"       c\n" +
			"FROM TABLE\n" +
			"ORDER BY b,\n" +
			"         CASE\n" +
			"           WHEN a = 1 THEN 2\n" +
			"           WHEN a = 2 THEN 1\n" +
			"           ELSE 3\n" +
			"         END";
		f = new SqlFormatter(sql, 100);
		formatted = f.getFormattedSql();
//			System.out.println("**************\n" + formatted + "\n**********\n" + expected);
		assertEquals("CASE alias not formatted", expected, formatted);
	}

	public void testWhitespace()
	{
		try
		{
			String sql = "alter table epg_value add constraint fk_value_attr foreign key (id_attribute) references attribute(id);";
			String expected = "ALTER TABLE epg_value ADD CONSTRAINT fk_value_attr FOREIGN KEY (id_attribute) REFERENCES attribute(id);";
			SqlFormatter f = new SqlFormatter(sql, 100);
			CharSequence formatted = f.getFormattedSql();
			assertEquals("ALTER TABLE not correctly formatted", expected, formatted.toString().trim());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void testColumnThreshold()
		throws Exception
	{
		try
		{
			String sql = "SELECT a,b,c from mytable";
			Settings.getInstance().setFormatterMaxColumnsInSelect(5);
			SqlFormatter f = new SqlFormatter(sql, 100);
			CharSequence formatted = f.getFormattedSql();
			String expected = "SELECT a, b, c\nFROM mytable";

			sql = "SELECT a,b,c,d,e,f,g,h,i from mytable";
			f = new SqlFormatter(sql, 100);
			formatted = f.getFormattedSql();
			expected = "SELECT a, b, c, d, e,\n       f, g, h, i\nFROM mytable";
			assertEquals(expected, formatted);
		}
		finally
		{
			Settings.getInstance().setFormatterMaxColumnsInSelect(1);
		}
	}

	public void testBracketIdentifier()
		throws Exception
	{
		try
		{
			String sql = "SELECT a,b,[MyCol] from mytable";
			SqlFormatter f = new SqlFormatter(sql, 100);
			Settings.getInstance().setFormatterMaxColumnsInSelect(1);
			CharSequence formatted = f.getFormattedSql();
			String expected = "SELECT a,\n       b,\n       [MyCol]\nFROM mytable";
//			System.out.println("*********\n" + formatted + "\n---\n" + expected + "\n************");
			assertEquals(expected, formatted);
		}
		finally
		{
			Settings.getInstance().setFormatterMaxColumnsInSelect(1);
		}
	}

	public void testDecode()
	{
		try
		{
			String sql = "SELECT DECODE((MOD(INPUT-4,12)+1),1,'RAT',2,'OX',3,'TIGER',4,'RABBIT',5,'DRAGON',6,'SNAKE',7,'HORSE',8,'SHEEP/GOAT',9,'MONKEY',10,'ROOSTER',11,'DOG',12,'PIG')  YR FROM DUAL";

			String expected = "SELECT DECODE((MOD(INPUT-4,12)+1),\n" + "             1,'RAT',\n" + "             2,'OX',\n" + "             3,'TIGER',\n" + "             4,'RABBIT',\n" + "             5,'DRAGON',\n" + "             6,'SNAKE',\n" + "             7,'HORSE',\n" + "             8,'SHEEP/GOAT',\n" + "             9,'MONKEY',\n" + "             10,'ROOSTER',\n" + "             11,'DOG',\n" + "             12,'PIG'\n" + "       )  YR\n" + "FROM DUAL";

			SqlFormatter f = new SqlFormatter(sql, 100);
			CharSequence formatted = f.getFormattedSql();

//			System.out.println("**************\n" + formatted + "\n**********\n" + expected);
			assertEquals("Complex DECODE not formatted correctly", expected, formatted);


			sql = "select decode(col1, 'a', 1, 'b', 2, 'c', 3, 99) from dual";
			f = new SqlFormatter(sql, 100);
			formatted = f.getFormattedSql();
			expected = "SELECT decode(col1,\n" + "              'a', 1,\n" + "              'b', 2,\n" + "              'c', 3,\n" + "              99\n" + "       ) \n" + "FROM dual";

//			System.out.println("**************\n" + formatted + "\n**********\n" + expected);
			assertEquals("DECODE not formatted correctly", expected, formatted);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testQuotedIdentifier()
		throws Exception
	{
		try
		{
			String sql = "SELECT a,b,\"c d\" from mytable";
			SqlFormatter f = new SqlFormatter(sql, 100);
			CharSequence formatted = f.getFormattedSql();
			String expected = "SELECT a,\n       b,\n       \"c d\"\nFROM mytable";
			assertEquals(expected, formatted);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testGetFormattedSql()
		throws Exception
	{
		try
		{
			String sql = "--comment\nselect * from blub;";
			Settings.getInstance().setInternalEditorLineEnding(Settings.UNIX_LINE_TERMINATOR_PROP_VALUE);

			SqlFormatter f = new SqlFormatter(sql, 100);

			CharSequence formatted = f.getFormattedSql();
//			System.out.println("**************\n" + formatted + "\n**********\n" + expected);
			String expected = "--comment\nSELECT *\nFROM blub;";
			assertEquals("Not correctly formatted", expected, formatted);

			sql = "select x from y union all select y from x";
			f = new SqlFormatter(sql, 100);
			formatted = f.getFormattedSql();
			expected = "SELECT x\nFROM y\nUNION ALL\nSELECT y\nFROM x";
			assertEquals(expected, formatted);

			sql = "select x,y from y order by x\n--trailing comment";
			f = new SqlFormatter(sql, 100);
			formatted = f.getFormattedSql();
			expected = "SELECT x,\n       y\nFROM y\nORDER BY x\n--trailing comment";
			assertEquals(expected, formatted.toString().trim());

			sql = "select x,y,z from y where a = 1 and b = 2";
			f = new SqlFormatter(sql, 100);
			formatted = f.getFormattedSql();
			expected = "SELECT x,\n       y,\n       z\nFROM y\nWHERE a = 1\nAND   b = 2";
			assertEquals(expected, formatted);

			sql = "select x,y,z from y where a = 1 and b = (select min(x) from y)";
			f = new SqlFormatter(sql, 100);
			formatted = f.getFormattedSql();
			expected = "SELECT x,\n       y,\n       z\nFROM y\nWHERE a = 1\nAND   b = (SELECT MIN(x) FROM y)";
			assertEquals(expected, formatted);

			sql = "select x,y,z from y where a = 1 and b = (select min(x) from y)";
			f = new SqlFormatter(sql, 10);
			formatted = f.getFormattedSql();
			expected = "SELECT x,\n       y,\n       z\nFROM y\nWHERE a = 1\nAND   b = (SELECT MIN(x)\n           FROM y)";
			assertEquals(expected, formatted);

			sql = "UPDATE customer " + "   SET duplicate_flag = CASE (SELECT COUNT(*) FROM customer c2 WHERE c2.f_name = customer.f_name AND c2.s_name = customer.s_name GROUP BY f_name,s_name)  \n" + "                           WHEN 1 THEN 0  " + "                           ELSE 1  " + "                        END";
			expected =
				"UPDATE customer\n" +
				"   SET duplicate_flag = CASE (SELECT COUNT(*)\n" +
				"                              FROM customer c2\n" +
				"                              WHERE c2.f_name = customer.f_name\n" +
				"                              AND   c2.s_name = customer.s_name\n" +
				"                              GROUP BY f_name,\n" +
				"                                       s_name)\n" +
				"                          WHEN 1 THEN 0\n" +
				"                          ELSE 1\n" +
				"                        END";
			f = new SqlFormatter(sql, 10);

			formatted = f.getFormattedSql();
//			System.out.println("**************\n" + formatted + "\n**********\n" + expected);
			assertEquals(expected, formatted.toString().trim());

			sql = "SELECT ber.nachname AS ber_nachname, \n" + "       ber.nummer AS ber_nummer \n" + "FROM table a WHERE (x in (select bla,bla,alkj,aldk,alkjd,dlaj,alkjdaf from blub 1, blub2, blub3 where x=1 and y=2 and z=3 and a=b and c=d) or y = 5)" + " and a *= b and b = c (+)";
			f = new SqlFormatter(sql, 10);
			formatted = f.getFormattedSql();
			expected = "SELECT ber.nachname AS ber_nachname,\n" + "       ber.nummer AS ber_nummer\n" + "FROM TABLE a\n" + "WHERE (x IN (SELECT bla,\n" + "                    bla,\n" + "                    alkj,\n" + "                    aldk,\n" + "                    alkjd,\n" + "                    dlaj,\n" + "                    alkjdaf\n" + "             FROM blub 1,\n" + "                  blub2,\n" + "                  blub3\n" + "             WHERE x = 1\n" + "             AND   y = 2\n" + "             AND   z = 3\n" + "             AND   a = b\n" + "             AND   c = d) OR y = 5)\n" + "AND   a *= b\n" + "AND   b = c (+)";
//			System.out.println("**************\n" + formatted + "\n**********\n" + expected);
			assertEquals(expected, formatted.toString().trim());

			sql = "update x set (a,b) = (select x,y from k);";
			f = new SqlFormatter(sql, 50);
			formatted = f.getFormattedSql();
			expected = "UPDATE x\n   SET (a,b) = (SELECT x, y FROM k);";
			assertEquals(expected, formatted.toString().trim());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
