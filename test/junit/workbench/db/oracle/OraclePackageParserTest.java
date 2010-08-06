/*
 * OraclePackageParserTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import java.sql.DatabaseMetaData;
import workbench.WbTestCase;
import workbench.db.ProcedureDefinition;
import workbench.util.CollectionUtil;

/**
 * @author Thomas Kellerer
 */
public class OraclePackageParserTest
	extends WbTestCase
{
	String decl = "CREATE   OR   REPLACE PACKAGE emp_actions AS  -- spec \n" +
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
             "   PROCEDURE fire_employee (emp_id NUMBER, fire_date DATE); \n" +
             "END emp_actions;";

  String body = "CREATE \nOR\t    REPLACE PACKAGE BODY emp_actions AS  -- body \n" +
             "   CURSOR desc_salary RETURN EmpRecTyp IS \n" +
             "      SELECT empno, sal FROM emp ORDER BY sal DESC; \n" +
						 "   /** Procedure hire_employee **/ \n" +
             "   PROCEDURE hire_employee( \n" +
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
						 "   /** Procedure fire_employee **/ \n" +
             "   PROCEDURE fire_employee(emp_id NUMBER) IS \n" +
             "   BEGIN \n" +
             "      DELETE FROM emp WHERE empno = emp_id; \n" +
             "   END fire_employee; \n" +
             " \n" +
             " \n" +
             "   PROCEDURE fire_employee(emp_id NUMBER, fire_date DATE) IS \n" +
             "   BEGIN \n" +
             "      DELETE FROM emp WHERE empno = emp_id; \n" +
             "   END fire_employee; \n" +
             "END emp_actions;";

	public OraclePackageParserTest(String testName)
	{
		super(testName);
	}

	public void testParser()
	{
		String script = decl + "\n/\n/" + body;
		OraclePackageParser parser = new OraclePackageParser(script);
		String parsedBody = parser.getPackageBody();
		String parsedDecl = parser.getPackageDeclaration();
		assertEquals(body, parsedBody);
		assertEquals(decl, parsedDecl);
	}

	public void testFindProc()
	{
		String script = decl + "\n/\n/" + body;
		int pos = script.indexOf("   PROCEDURE hire_employee(") + 3;
		ProcedureDefinition proc = new ProcedureDefinition("HIRE_EMPLOYEE", DatabaseMetaData.procedureNoResult);
		int procPos = OraclePackageParser.findProcedurePosition(script, proc, CollectionUtil.arrayList("emp_id"));
		assertEquals(pos, procPos);

		pos = script.indexOf("PROCEDURE fire_employee(emp_id NUMBER, fire_date DATE)");
		proc = new ProcedureDefinition("fire_employee", DatabaseMetaData.procedureNoResult);
		procPos = OraclePackageParser.findProcedurePosition(script, proc, CollectionUtil.arrayList("emp_id", "fire_date"));
		assertEquals(pos, procPos);

		pos = script.indexOf("PROCEDURE fire_employee(emp_id NUMBER)");
		proc = new ProcedureDefinition("fire_employee", DatabaseMetaData.procedureNoResult);
		procPos = OraclePackageParser.findProcedurePosition(script, proc, CollectionUtil.arrayList("emp_id"));
		assertEquals(pos, procPos);
	}

	public void testfindProcInHeader()
	{
		String script = decl + "\n/\n";
		int pos = script.indexOf("PROCEDURE hire_employee (");
		ProcedureDefinition proc = new ProcedureDefinition("HIRE_EMPLOYEE", DatabaseMetaData.procedureNoResult);
		int procPos = OraclePackageParser.findProcedurePosition(script, proc, CollectionUtil.arrayList("emp_id"));
		assertEquals(pos, procPos);

		pos = script.indexOf("PROCEDURE fire_employee (emp_id NUMBER);");
		proc = new ProcedureDefinition("FIRE_EMPLOYEE", DatabaseMetaData.procedureNoResult);
		procPos = OraclePackageParser.findProcedurePosition(script, proc, CollectionUtil.arrayList("emp_id"));
		assertEquals(pos, procPos);

		pos = script.indexOf("PROCEDURE fire_employee (emp_id NUMBER, fire_date DATE); ");
		proc = new ProcedureDefinition("FIRE_EMPLOYEE", DatabaseMetaData.procedureNoResult);
		procPos = OraclePackageParser.findProcedurePosition(script, proc, CollectionUtil.arrayList("EMP_ID", "FIRE_DATE"));
		assertEquals(pos, procPos);

	}
}
