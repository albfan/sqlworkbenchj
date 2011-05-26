/*
 * SqlFormatterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.formatter;

import java.util.List;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.resource.Settings;
import workbench.util.CollectionUtil;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlFormatterTest
	extends WbTestCase
{
	public SqlFormatterTest()
	{
		super("SqlFormatterTest");
	}


	@Test
	public void testMySQLWhiteSpaceBug()
		throws Exception
	{
		String sql = "INSERT INTO test (id, wert) VALUES ( uuid(), 1)";
		String expected = "INSERT INTO test\n  (id, wert)\nVALUES\n  (uuid(), 1)";

		SqlFormatter f = new SqlFormatter(sql, 100);
		int cols = Settings.getInstance().getFormatterMaxColumnsInInsert();
		try
		{
			Settings.getInstance().setFormatterMaxColumnsInInsert(10);
			String formatted = f.getFormattedSql().toString();
			System.out.println("**************\n" + formatted + "\n----------------------\n" + expected + "\n************************");
			assertEquals("SELECT in VALUES not formatted", expected, formatted);
		}
		finally
		{
			Settings.getInstance().setFormatterMaxColumnsInInsert(cols);
		}
	}

	@Test
	public void testTrailingSemicolon()
		throws Exception
	{
		String sql = "select * from test";
		SqlFormatter f = new SqlFormatter(sql);
		String formatted = f.getFormattedSql();
		String expected =
			"SELECT *\n" +
			"FROM test";
		assertEquals(expected, formatted);

		sql = "select * from test;";
		f = new SqlFormatter(sql);
		formatted = f.getFormattedSql();
		expected =
			"SELECT *\n" +
			"FROM test;";
		assertEquals(expected, formatted);

		f = new SqlFormatter("select");
		assertEquals("SELECT", 	f.getFormattedSql());
	}

	@Test
	public void testDateLiteral()
		throws Exception
	{
		String sql = "select * from my_table where birthday = date'1950-05-06'";
		SqlFormatter f = new SqlFormatter(sql);
		String formatted = f.getFormattedSql();
		String expected =
			"SELECT *\n" +
			"FROM my_table\n" +
			"WHERE birthday = DATE '1950-05-06'";
		assertEquals(expected, formatted);

		sql = "insert into some_table (id, created_at) values (1, timestamp      '2011-12-13 01:02:03')";
		f = new SqlFormatter(sql);
		formatted = f.getFormattedSql();
		expected =
			"INSERT INTO some_table\n" +
			"(\n"+
			"  id,\n"+
			"  created_at\n"+
			")\n"+
			"VALUES\n"+
			"(\n"+
			"  1,\n"+
			"  TIMESTAMP '2011-12-13 01:02:03'\n"+
			")";
		//System.out.println("*****\n" + formatted + "\n---------\n" + expected);
		assertEquals(expected, formatted);
	}

	@Test
	public void testOldStyleJoin()
		throws Exception
	{
		String sql = "select * from table1, table2 where table1.col1 = table2.col1 and table1.col3 in (1,2,3,4,5)";
		SqlFormatter f = new SqlFormatter(sql);
		f.setCommaAfterLineBreak(false);
		String expected =
				"SELECT *\n"+
				"FROM table1,\n" +
				"     table2\n"+
				"WHERE table1.col1 = table2.col1\n"+
				"AND   table1.col3 IN (1,2,3,4,5)";
		String formatted = f.getFormattedSql().trim();
		assertEquals(expected, formatted);
		f = new SqlFormatter(sql);
		f.setCommaAfterLineBreak(true);
		formatted = f.getFormattedSql().trim();
		expected =
				"SELECT *\n"+
				"FROM table1\n" +
				"     ,table2\n"+
				"WHERE table1.col1 = table2.col1\n"+
				"AND   table1.col3 IN (1,2,3,4,5)";
		assertEquals(expected, formatted);
		f = new SqlFormatter(sql);
		f.setCommaAfterLineBreak(true);
		f.setAddSpaceAfterLineBreakComma(true);
		formatted = f.getFormattedSql().trim();
		expected =
				"SELECT *\n"+
				"FROM table1\n" +
				"     , table2\n"+
				"WHERE table1.col1 = table2.col1\n"+
				"AND   table1.col3 IN (1,2,3,4,5)";
		assertEquals(expected, formatted);
	}

	@Test
	public void testInListWithJoin()
		throws Exception
	{
		String sql = "select * from table1 join table2 on table1.col1 = table2.col1 and table1.col3 in (1,2,3,4,5)";
		SqlFormatter f = new SqlFormatter(sql);
		String expected =
			"SELECT *\n" +
			"FROM table1 \n"+
			"  JOIN table2 ON table1.col1 = table2.col1 AND table1.col3 IN (1,2,3,4,5)";
		String formatted = f.getFormattedSql().trim();
		assertEquals(expected, formatted);
	}

	@Test
	public void testCaseWithComma()
		throws Exception
	{
		String sql = "select case when a is null then b else c end as some_col, other_col from foo";
		SqlFormatter f = new SqlFormatter(sql);
		f.setCommaAfterLineBreak(false);
		String expected =
			"SELECT CASE\n" +
			"         WHEN a IS NULL THEN b\n" +
			"         ELSE c\n" +
			"       END AS some_col,\n" +
			"       other_col\n" +
			"FROM foo";
		String formatted = f.getFormattedSql();
		assertEquals(expected, formatted);
		f = new SqlFormatter(sql);
		f.setCommaAfterLineBreak(true);
		formatted = f.getFormattedSql();
		expected =
			"SELECT CASE\n" +
			"         WHEN a IS NULL THEN b\n" +
			"         ELSE c\n" +
			"       END AS some_col\n" +
			"       ,other_col\n" +
			"FROM foo";
		assertEquals(expected, formatted);
	}

	@Test
	public void testCommaAtStart()
		throws Exception
	{
		int cols = Settings.getInstance().getFormatterMaxColumnsInSelect();
		try
		{
			String sql = "select col1, col2, col3, col4, col5 from some_table;";
			SqlFormatter f = new SqlFormatter(sql);
			f.setCommaAfterLineBreak(true);
			Settings.getInstance().setFormatterMaxColumnsInSelect(1);
			String formatted = f.getFormattedSql();
			String expected =
				"SELECT col1\n" +
				"       ,col2\n" +
				"       ,col3\n" +
				"       ,col4\n" +
				"       ,col5\n" +
				"FROM some_table;";
			assertEquals(expected, formatted);

			f = new SqlFormatter(sql);
			f.setCommaAfterLineBreak(true);
			f.setAddSpaceAfterLineBreakComma(true);
			Settings.getInstance().setFormatterMaxColumnsInSelect(1);
			formatted = f.getFormattedSql();
			expected =
				"SELECT col1\n" +
				"       , col2\n" +
				"       , col3\n" +
				"       , col4\n" +
				"       , col5\n" +
				"FROM some_table;";
			assertEquals(expected, formatted);

			Settings.getInstance().setFormatterMaxColumnsInSelect(3);
			f = new SqlFormatter(sql);
			f.setCommaAfterLineBreak(true);
			formatted = f.getFormattedSql();
			expected =
				"SELECT col1, col2, col3\n" +
				"       ,col4, col5\n" +
				"FROM some_table;";
			assertEquals(expected, formatted);
		}
		finally
		{
			Settings.getInstance().setFormatterMaxColumnsInSelect(cols);
		}
	}

	@Test
	public void testNestedSubselect()
		throws Exception
	{
		String sql = "select id,  \n" +
             "       (select sum(damage)  \n" +
             "        from (select damage \n" +
             "              from fact_eventplayerdamage f2 \n" +
             "              where f2.damage >= f.damage \n" +
             "              order by damage asc \n" +
             "              limit 5) t \n" +
             "       ) \n" +
             "from fact_eventplayerdamage f \n";

		SqlFormatter f = new SqlFormatter(sql);
		String formatted = f.getFormattedSql();
		String expected = "SELECT id,\n" +
             "       (SELECT SUM(damage)\n" +
             "        FROM (SELECT damage\n" +
             "              FROM fact_eventplayerdamage f2\n" +
             "              WHERE f2.damage >= f.damage\n" +
             "              ORDER BY damage ASC LIMIT 5) t)\n" +
             "FROM fact_eventplayerdamage f";
//		System.out.println("+++++++++++++++++++ result: \n" + formatted + "\n********** expected:\n" + expected + "\n-------------------");
		assertEquals(expected, formatted);

	}

	@Test
	public void testQuotes()
		throws Exception
	{
		String sql = "select ' test '''||firstname||''' test' from person";
		SqlFormatter f = new SqlFormatter(sql);
		String formatted = f.getFormattedSql();
		String expected = "SELECT ' test ''' ||firstname|| ''' test'\nFROM person";
//		System.out.println("+++++++++++++++++++ result: \n" + formatted + "\n********** expected:\n" + expected + "\n-------------------");
		assertEquals(expected, formatted);
	}

	@Test
	public void testUpdate()
		throws Exception
	{
		String sql = "update tableA set completed_Date =  ( select min(disconnect_Date) from tableB ) ";
		SqlFormatter f = new SqlFormatter(sql);
		String formatted = f.getFormattedSql();
		String expected = "UPDATE tableA\n" +
							"   SET completed_Date = (SELECT MIN(disconnect_Date) FROM tableB)";

//		System.out.println("+++++++++++++++++++ result: \n" + formatted + "\n********** expected:\n" + expected + "\n-------------------");
		assertEquals(expected, formatted);

		sql = "update tableA set completed_Date =  ( select id from tableB ) ";
		f = new SqlFormatter(sql);
		formatted = f.getFormattedSql();
		expected = "UPDATE tableA\n" +
							"   SET completed_Date = (SELECT id FROM tableB)";
		assertEquals(expected, formatted);
//		System.out.println("+++++++++++++++++++ result: \n" + formatted + "\n********** expected:\n" + expected + "\n-------------------");

	}

	@Test
	public void testAsOf()
		throws Exception
	{
		String sql = "select x1 as ofx from the_table;";
		SqlFormatter f = new SqlFormatter(sql);
		String formatted = f.getFormattedSql();
		String expected = "SELECT x1 AS ofx\n" +
			"FROM the_table;";
//		System.out.println("+++++++++++++++++++ result: \n" + formatted + "\n********** expected:\n" + expected + "\n-------------------");
		assertEquals(expected, formatted);
	}

	@Test
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
		String formatted = f.getFormattedSql();
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
		formatted = f.getFormattedSql();
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

	@Test
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
		String formatted = f.getFormattedSql();
		String expected = "SELECT e.ManagerID,\n" +
             "       e.EmployeeID,\n" +
             "       e.Title,\n" +
             "       edh.DepartmentID,\n" +
             "       0 AS LEVEL\n" +
             "FROM HumanResources.Employee AS e \n" +
             "  INNER JOIN HumanResources.EmployeeDepartmentHistory AS edh ON e.EmployeeID = edh.EmployeeID AND edh.EndDate IS NULL\n" +
             "WHERE ManagerID IS NULL\n" +
             "UNION ALL\n" +
             "SELECT e.ManagerID,\n" +
             "       e.EmployeeID,\n" +
             "       e.Title,\n" +
             "       edh.DepartmentID,\n" +
             "       LEVEL+1\n" +
             "FROM HumanResources.Employee AS e \n" +
             "  INNER JOIN HumanResources.EmployeeDepartmentHistory AS edh ON e.EmployeeID = edh.EmployeeID AND edh.EndDate IS NULL \n" +
             "  INNER JOIN DirectReports AS d ON e.ManagerID = d.EmployeeID";
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
		formatted = f.getFormattedSql();
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

	@Test
	public void testKeywordsAsFunction()
		throws Exception
	{
		String sql = "SELECT right(name,5) FROM person";
		SqlFormatter f = new SqlFormatter(sql);
		f.setUseLowerCaseFunctions(true);
		f.addDBFunctions(CollectionUtil.treeSet("RIGHT", "LEFT"));
		String formatted = f.getFormattedSql();
//		System.out.println("*******\n" + formatted + "\n**********");
		String expected = "SELECT right(name,5)\nFROM person";
		assertEquals(expected, formatted);
	}

	@Test
	public void testWbVars()
		throws Exception
	{
		String sql = "SELECT * FROM mytable WHERE id in ($[somestuff])";
		SqlFormatter f = new SqlFormatter(sql);
		String formatted = f.getFormattedSql();
		String expected = "SELECT *\nFROM mytable\nWHERE id IN ($[somestuff])";
//		System.out.println("*******\n" + formatted + "\n**********");
		assertEquals(expected, formatted);

		sql = "SELECT * FROM mytable WHERE id in ($[&somestuff])";
		f = new SqlFormatter(sql);
		formatted = f.getFormattedSql();
		expected = "SELECT *\nFROM mytable\nWHERE id IN ($[&somestuff])";
		assertEquals(expected, formatted);

		sql = "SELECT * FROM mytable where id=$[var_id]";
		f = new SqlFormatter(sql);
		formatted = f.getFormattedSql();
		//System.out.println("*******\n" + formatted + "\n**********");
		expected = "SELECT *\nFROM mytable\nWHERE id = $[var_id]";
		assertEquals(expected, formatted);
	}

	@Test
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
		String formatted = f.getFormattedSql();
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
						"    INNER JOIN HumanResources.EmployeeDepartmentHistory AS edh ON e.EmployeeID = edh.EmployeeID AND edh.EndDate IS NULL\n" +
						"  WHERE ManagerID IS NULL\n" +
						"  UNION ALL\n" +
						"  -- Recursive member definition \n" +
						"  SELECT e.ManagerID,\n" +
						"         e.EmployeeID,\n" +
						"         e.Title,\n" +
						"         edh.DepartmentID,\n" +
						"         LEVEL+1\n" +
						"  FROM HumanResources.Employee AS e \n" +
						"    INNER JOIN HumanResources.EmployeeDepartmentHistory AS edh ON e.EmployeeID = edh.EmployeeID AND edh.EndDate IS NULL \n" +
						"    INNER JOIN DirectReports AS d ON e.ManagerID = d.EmployeeID\n" +
						")\n" +
						"-- Statement that executes the CTE \n" +
						"SELECT ManagerID,\n" +
						"       EmployeeID,\n" +
						"       Title,\n" +
						"       LEVEL\n" +
						"FROM DirectReports \n" +
						"  INNER JOIN HumanResources.Department AS dp ON DirectReports.DeptID = dp.DepartmentID\n" +
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

		expected = "WITH tmp AS\n" +
							"(\n" +
							"  SELECT * FROM users\n" +
							")\n" +
							"SELECT tmp. *,\n" +
							"       nvl((SELECT 1 FROM td_cdma_ip WHERE tmp.src_ip BETWEEN ip_fromip AND ip_endip),0) isNew\n" +
							"FROM tmp";
		f = new SqlFormatter(sql);
		f.addDBFunctions(CollectionUtil.caseInsensitiveSet("nvl"));
		formatted = f.getFormattedSql();
//		System.out.println("++++++++++++++++++\n" + formatted + "\n**********\n" + expected + "\n-------------------");
		assertEquals(expected, formatted);


		// Make sure a WITH in a different statement is not mistaken for a CTE
		sql = "CREATE VIEW vfoo \n" +
					"AS \n" +
					"SELECT id, name FROM foo WHERE id BETWEEN 1 AND 10000 \n" +
					"WITH CHECK OPTION";
		f = new SqlFormatter(sql);
		formatted = f.getFormattedSql();

		expected = "CREATE VIEW vfoo \n" +
							"AS\n" +
							"SELECT id,\n" +
							"       name\n" +
							"FROM foo\n" +
							"WHERE id BETWEEN 1\n" +
							"AND   10000 WITH CHECK OPTION";
//		System.out.println("++++++++++++++++++\n" + formatted + "\n**********\n" + expected + "\n-------------------");
		assertEquals(expected, formatted);

		// Test multiple CTEs in a single statement
		sql = "with first_cte (col1, col2) AS " +
			"( select col1, col2 from table_1), " +
			"second_cte (col1, col2) as " +
			"( select col4, col5 from table_2), third_cte as (select 1,2 from dual)" +
			"select * from first_cte f join second_cte s on (f.col1 = s.col2)";
		f = new SqlFormatter(sql);
		formatted = f.getFormattedSql();
		expected = "WITH first_cte (col1, col2) AS\n" +
             "(\n" +
             "  SELECT col1, col2 FROM table_1\n" +
             "),\n" +
             "second_cte (col1, col2) AS\n" +
             "(\n" +
             "  SELECT col4, col5 FROM table_2\n" +
             "),\n" +
             "third_cte AS\n" +
             "(\n" +
             "  SELECT 1, 2 FROM dual\n" +
             ")\n" +
             "SELECT *\n" +
             "FROM first_cte f \n" +
             "  JOIN second_cte s ON (f.col1 = s.col2)";
//		System.out.println("++++++++++++++++++\n" + formatted + "\n**********\n" + expected + "\n-------------------");
		assertEquals(expected, formatted);

		sql = "WITH temp1 (c1,t1,t2) AS  \n" +
             "( \n" +
             "   VALUES (1,2,3)  \n" +
             ") \n" +
             "SELECT * \n" +
             "FROM temp1";
	  f = new SqlFormatter(sql);
		formatted = f.getFormattedSql();
		expected = "WITH temp1 (c1, t1, t2) AS\n" +
             "(\n" +
             "  VALUES ( 1, 2, 3 )\n" +
             ")\n" +
             "SELECT *\n" +
             "FROM temp1";
//		System.out.println("++++++++++++++++++\n" + formatted + "\n**********\n" + expected + "\n-------------------");
		assertEquals(expected, formatted);
	}

	@Test
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
		String formatted = f.getFormattedSql();
//		System.out.println("**************\n" + formatted + "\n------------------\n" + expected + "\n*************");
		assertEquals(expected, formatted);
	}

	@Test
	public void testRownumber()
		throws Exception
	{
		String sql = "select row_number() over (order by id) from table";
		SqlFormatter f = new SqlFormatter(sql);
		String formatted = f.getFormattedSql();
		String expected = "SELECT row_number() OVER (ORDER BY id)\nFROM TABLE";
		assertEquals(expected, formatted);
	}

	@Test
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
			"  INNER JOIN dept d ON (e.deptno = d.deptno)\n" +
			"ORDER BY d.deptno,\n" +
			"         e.empno";

		SqlFormatter f = new SqlFormatter(sql);
		f.addDBFunctions(CollectionUtil.caseInsensitiveSet("nvl"));
		String formatted = f.getFormattedSql();
//			System.out.println("**************\n" + formatted + "\n**********\n" + expected);
		assertEquals(expected, formatted);
	}

	@Test
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
			String formatted = f.getFormattedSql();
//			System.out.println("**************\n" + formatted + "\n**********\n" + expected);
			assertEquals(expected, formatted);
		}
		finally
		{
			Settings.getInstance().setFormatterUpperCaseKeywords(true);
		}
	}

	@Test
	public void testFormatMultiValueInsert()
		throws Exception
	{
		try
		{
			String sql = "insert into my_table (col1, col2, col3) values (1,2,3), (4,5,6), (7,8,9)";
			SqlFormatter f = new SqlFormatter(sql);
			String formatted = f.getFormattedSql();
			String expected = "INSERT INTO my_table\n" +
             "(\n" +
             "  col1,\n" +
             "  col2,\n" +
             "  col3\n" +
             ")\n" +
             "VALUES\n" +
             "(\n" +
             "  1,\n" +
             "  2,\n" +
             "  3\n" +
             "),\n" +
             "(\n" +
             "  4,\n" +
             "  5,\n" +
             "  6\n" +
             "),\n" +
             "(\n" +
             "  7,\n" +
             "  8,\n" +
             "  9\n" +
             ")";
//				System.out.println("******************\n" + formatted + "\n-------------------------\n" + expected + "\n************************");
			assertEquals(expected, formatted);
			Settings.getInstance().setFormatterMaxColumnsInInsert(10);
			f = new SqlFormatter(sql);
			formatted = f.getFormattedSql();
			expected = "INSERT INTO my_table\n" +
             "  (col1, col2, col3) \n" +
             "VALUES \n" +
             "  (1, 2, 3),\n" +
             "  (4, 5, 6),\n" +
             "  (7, 8, 9)";
//				System.out.println("******************\n" + formatted + "\n-------------------------\n" + expected + "\n************************");
		}
		finally
		{
			Settings.getInstance().setFormatterMaxColumnsInInsert(1);
		}

	}

	@Test
	public void testFormatInsert()
		throws Exception
	{
		try
		{
			Settings.getInstance().setFormatterMaxColumnsInInsert(3);
			String sql = "insert into x ( col1,col2,col3) values (1,2,3)";
			String expected = "INSERT INTO x\n  (col1, col2, col3)\nVALUES\n  (1, 2, 3)";
			SqlFormatter f = new SqlFormatter(sql, 100);
			String formatted = f.getFormattedSql();
//			System.out.println("*********\n" + formatted + "\n---\n" + expected + "\n************");
			assertEquals(expected, formatted);

			Settings.getInstance().setFormatterMaxColumnsInInsert(3);
			sql = "insert into x ( col1,col2,col3,col4,col5) values (1,2,3,4,5)";
			expected = "INSERT INTO x\n  (col1, col2, col3,\n   col4, col5)\nVALUES\n  (1, 2, 3,\n   4, 5)";
			f = new SqlFormatter(sql, 100);
			formatted = f.getFormattedSql();
//			System.out.println("*********\n" + formatted + "\n---\n" + expected + "\n************");
			assertEquals(expected, formatted);

		}
		finally
		{
			Settings.getInstance().setFormatterMaxColumnsInInsert(1);
		}
	}

	@Test
	public void testFormatUpdate()
		throws Exception
	{
		try
		{
			Settings.getInstance().setFormatterMaxColumnsInUpdate(3);
			String sql = "update mytable set col1=5,col2=6,col3=4";
			String expected = "UPDATE mytable\n   SET col1 = 5, col2 = 6, col3 = 4";
			SqlFormatter f = new SqlFormatter(sql, 100);
			String formatted = f.getFormattedSql();
//			System.out.println("*********\n" + formatted + "\n---\n" + expected + "\n************");
			assertEquals(expected, formatted);
			sql = "update mytable set col1=1,col2=2,col3=3,col4=4,col5=5";
			expected = "UPDATE mytable\n   SET col1 = 1, col2 = 2, col3 = 3,\n       col4 = 4, col5 = 5";
			f = new SqlFormatter(sql, 100);
			formatted = f.getFormattedSql();
//			System.out.println("*********\n" + formatted + "\n--- expected\n" + expected + "\n************");
			assertEquals(expected, formatted);
		}
		finally
		{
			Settings.getInstance().setFormatterMaxColumnsInUpdate(1);
		}
	}

	@Test
	public void testFormatUnicode()
		throws Exception
	{
		String sql = "insert into x(ss2,ss3,ss2) values('\u32A5\u0416','dsaffds',234)";
		String expected = "INSERT INTO x\n(\n  ss2,\n  ss3,\n  ss2\n)\nVALUES\n(\n  '\u32A5\u0416',\n  'dsaffds',\n  234\n)";
		SqlFormatter f = new SqlFormatter(sql, 100);
		String formatted = f.getFormattedSql();
		assertEquals(expected, formatted);
	}

	@Test
	public void testCreateTable()
		throws Exception
	{
		String sql = null;
		SqlFormatter f = null;
		String formatted = null;
		List<String> lines = null;

		sql = "create table person (id1 integer not null, id2 integer not null, id3 integer not null, firstname varchar(50), lastname varchar(50), primary key (id1, id2), foreign key (id3) references othertable(id));";
		f = new SqlFormatter(sql, 100);
		formatted = f.getFormattedSql();
		lines = TestUtil.getLines(formatted);
//		System.out.println("***\n" + formatted + "\n***");
		assertEquals("  id1         INTEGER NOT NULL,", lines.get(2));
		assertEquals("  PRIMARY KEY (id1,id2),", lines.get(7));
		assertEquals("  FOREIGN KEY (id3) REFERENCES othertable (id)", lines.get(8));

		sql = "create table person (somecol integer primary key, firstname varchar(50), lastname varchar(50));";
		f = new SqlFormatter(sql, 100);
		formatted = f.getFormattedSql();
		lines = TestUtil.getLines(formatted);
		assertEquals("  somecol     INTEGER PRIMARY KEY,", lines.get(2));

		sql = "create table person (id1 integer not null, id2 integer not null, firstname varchar(50), lastname varchar(50), primary key (id1, id2));";
		f = new SqlFormatter(sql, 100);
		formatted = f.getFormattedSql();
		lines = TestUtil.getLines(formatted);
		assertEquals("  id1         INTEGER NOT NULL,", lines.get(2));
		assertEquals("  PRIMARY KEY (id1,id2)", lines.get(6));

		sql = "create table person (id1 integer not null, constraint xyz exclude (id1 with =))";
		f = new SqlFormatter(sql);
		formatted = f.getFormattedSql();
		String expected =
				"CREATE TABLE person \n"+
				"(\n"+
				"  id1   INTEGER NOT NULL,\n"+
				"  CONSTRAINT xyz EXCLUDE (id1 WITH = )\n"+
				")";
		assertEquals(expected, formatted.trim());

		sql = "create table person (id1 integer not null primary key, some_data varchar (100), constraint xyz exclude (some_data with =))";
		f = new SqlFormatter(sql);
		formatted = f.getFormattedSql();
//		System.out.println("++++\n" + formatted + "\n-----");
		expected =
				"CREATE TABLE person \n"+
				"(\n"+
				"  id1         INTEGER NOT NULL PRIMARY KEY,\n"+
				"  some_data   VARCHAR(100),\n"+
				"  CONSTRAINT xyz EXCLUDE (some_data WITH = )\n"+
				")";
		assertEquals(expected, formatted.trim());

	}

	@Test
	public void testFileParam()
		throws Exception
	{
		String sql = "wbexport -file=\"c:\\Documents and Settings\\test.txt\" -type=text";
		SqlFormatter f = new SqlFormatter(sql, 100);
		String formatted = f.getFormattedSql();
		assertTrue(formatted.indexOf("\"c:\\Documents and Settings\\test.txt\"") > 0);
	}

	@Test
	public void testWbConfirm()
		throws Exception
	{
		String sql = "wbconfirm 'my message'";
		SqlFormatter f = new SqlFormatter(sql, 100);
		CharSequence formatted = f.getFormattedSql();
		String expected = "WbConfirm 'my message'";
		assertEquals("WbConfirm not formatted correctly", expected, formatted);
	}

	@Test
	public void testAliasForSubselect()
		throws Exception
	{
		String sql = "select a,b, (select a,b from t2) col4 from t1";
		SqlFormatter f = new SqlFormatter(sql, 100);
		CharSequence formatted = f.getFormattedSql();
		String expected = "SELECT a,\n" + "       b,\n" + "       (SELECT a, b FROM t2) col4\n" + "FROM t1";
		assertEquals("SELECT in VALUES not formatted", expected, formatted);
	}

	@Test
	public void testAsInFrom()
		throws Exception
	{
		String sql = "select t1.a, t2.b from bla as t1, t2";
		SqlFormatter f = new SqlFormatter(sql, 100);
		CharSequence formatted = f.getFormattedSql();
		String expected = "SELECT t1.a,\n" + "       t2.b\n" + "FROM bla AS t1,\n" + "     t2";
		assertEquals("SELECT in VALUES not formatted", expected, formatted);
	}

	@Test
	public void testInsertWithSubselect()
		throws Exception
	{
		String sql = "insert into tble (a,b) values ( (select max(x) from y), 'bla')";
		String expected = "INSERT INTO tble\n" + "(\n" + "  a,\n" + "  b\n" + ")\n" + "VALUES\n" + "(\n" + "  (SELECT MAX(x) FROM y),\n" + "  'bla'\n" + ")";
		SqlFormatter f = new SqlFormatter(sql, 100);
		CharSequence formatted = f.getFormattedSql();
//		System.out.println("**************\n" + formatted + "\n----------------------\n" + expected + "\n************************");
		assertEquals("SELECT in VALUES not formatted", expected, formatted);
	}

	@Test
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

	@Test
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

	@Test
	public void testWhitespace()
	{
		try
		{
			String sql = "alter table epg_value add constraint fk_value_attr foreign key (id_attribute) references attribute(id);";
			String expected = "ALTER TABLE epg_value ADD CONSTRAINT fk_value_attr FOREIGN KEY (id_attribute) REFERENCES attribute (id);";
			SqlFormatter f = new SqlFormatter(sql, 100);
			CharSequence formatted = f.getFormattedSql();
			assertEquals("ALTER TABLE not correctly formatted", expected, formatted.toString().trim());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Test
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

	@Test
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

	@Test
	public void testDecode()
		throws Exception
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

	@Test
	public void testQuotedIdentifier()
		throws Exception
	{
		String sql = "SELECT a,b,\"c d\" from mytable";
		SqlFormatter f = new SqlFormatter(sql, 100);
		CharSequence formatted = f.getFormattedSql();
		String expected = "SELECT a,\n       b,\n       \"c d\"\nFROM mytable";
		assertEquals(expected, formatted);
	}

	@Test
	public void testInListCommas()
		throws Exception
	{
		String sql = "select a from b where c in (1,2,3);";
		SqlFormatter f = new SqlFormatter(sql, 100);
		f.setAddSpaceAfterCommInList(true);
		CharSequence formatted = f.getFormattedSql();
		String expected = "SELECT a\nFROM b\nWHERE c IN (1, 2, 3);";
//		System.out.println("*****************\n" + formatted + "\n*************\n" + expected + "\n------------------");
		assertEquals(expected, formatted);

		sql = "select * from table1 join table2 on table1.col1 = table2.col1 and table1.col3 in (1,2,3,4,5)";
		f = new SqlFormatter(sql);
		f.setAddSpaceAfterCommInList(true);
		expected =
			"SELECT *\n" +
			"FROM table1 \n"+
			"  JOIN table2 ON table1.col1 = table2.col1 AND table1.col3 IN (1, 2, 3, 4, 5)";
		formatted = f.getFormattedSql().trim();
		assertEquals(expected, formatted);
	}

	@Test
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
