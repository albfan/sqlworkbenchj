/*
 * PackageParserTest.java
 * JUnit based test
 *
 * Created on May 17, 2006, 10:13 PM
 */

package workbench.db.oracle;

import junit.framework.TestCase;
import junit.framework.*;
import java.io.IOException;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.sql.formatter.Token;

/**
 *
 * @author support@sql-workbench.net
 */
public class PackageParserTest extends TestCase
{
	String decl = "CREATE OR REPLACE PACKAGE emp_actions AS  -- spec \n" + 
             "   TYPE EmpRecTyp IS RECORD (emp_id INT, salary REAL); \n" + 
             "   CURSOR desc_salary RETURN EmpRecTyp \n" + 
             "   PROCEDURE hire_employee ( \n" + 
             "      ename  VARCHAR2, \n" + 
             "      job    VARCHAR2, \n" + 
             "      mgr    NUMBER, \n" + 
             "      sal    NUMBER, \n" + 
             "      comm   NUMBER, \n" + 
             "      deptno NUMBER) \n" + 
             "   PROCEDURE fire_employee (emp_id NUMBER); \n" + 
             "END emp_actions;";
	
  String body = "CREATE OR REPLACE PACKAGE BODY emp_actions AS  -- body \n" + 
             "   CURSOR desc_salary RETURN EmpRecTyp IS \n" + 
             "      SELECT empno, sal FROM emp ORDER BY sal DESC; \n" + 
             "   PROCEDURE hire_employee ( \n" + 
             "      ename  VARCHAR2, \n" + 
             "      job    VARCHAR2, \n" + 
             "      mgr    NUMBER, \n" + 
             "      sal    NUMBER, \n" + 
             "      comm   NUMBER, \n" + 
             "      deptno NUMBER) IS \n" + 
             "   BEGIN \n" + 
             "      INSERT INTO emp VALUES (empno_seq.NEXTVAL, ename, job, \n" + 
             "         mgr, SYSDATE, sal, comm, deptno) \n" + 
             "   END hire_employee; \n" + 
             " \n" + 
             "   PROCEDURE fire_employee (emp_id NUMBER) IS \n" + 
             "   BEGIN \n" + 
             "      DELETE FROM emp WHERE empno = emp_id; \n" + 
             "   END fire_employee; \n" +
             "END emp_actions;";

	public PackageParserTest(String testName)
	{
		super(testName);
	}

	protected void setUp() throws Exception
	{
	}

	protected void tearDown() throws Exception
	{
	}
	
	public void testParser()
	{
		String script = decl + "\n/\n/" + body;
		OraclePackageParser parser = new OraclePackageParser(script);
		String parsedBody = parser.getPackageBody();
		String parsedDecl = parser.getPackageDeclaration();
		//System.out.println("decl=" + parsedDecl);
		assertEquals(body, parsedBody);
		assertEquals(decl, parsedDecl);
	}
}
