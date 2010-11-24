/*
 * OracleObjectCompilerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import java.util.List;
import workbench.db.ProcedureDefinition;
import workbench.TestUtil;
import workbench.db.WbConnection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.WbTestCase;
import workbench.sql.DelimiterDefinition;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleObjectCompilerTest
	extends WbTestCase
{
	private static final String TEST_ID = "orametadata";

	public OracleObjectCompilerTest()
	{
		super(TEST_ID);
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		String sql = "CREATE OR REPLACE FUNCTION aaa_get_answer \n " +
			"RETURN INTEGER \n" +
			"IS \n" +
			"BEGIN\n" +
			"   return 42;\n" +
			"END;\n" +
			"/";
		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER);

		String declaration =
			"CREATE OR REPLACE PACKAGE emp_mgmt AS  \n" +
			"   FUNCTION create_dept(department_id NUMBER, location_id NUMBER)  \n" +
			"      RETURN NUMBER;  \n" +
			"   FUNCTION hire (last_name VARCHAR2, job_id VARCHAR2, manager_id NUMBER, salary NUMBER, commission_pct NUMBER, department_id NUMBER) RETURN NUMBER;\n " +
			"   PROCEDURE remove_emp(employee_id NUMBER);  \n" +
			"END emp_mgmt; \n" +
			"/";
		TestUtil.executeScript(con, declaration, DelimiterDefinition.DEFAULT_ORA_DELIMITER);
		
		String body =
			"CREATE OR REPLACE PACKAGE BODY emp_mgmt AS  \n" +
			" \n" +
			"  FUNCTION hire (last_name VARCHAR2, job_id VARCHAR2, manager_id NUMBER, salary NUMBER, commission_pct NUMBER, department_id NUMBER)  \n" +
			"     RETURN NUMBER  \n" +
			"  IS  \n" +
			"    new_empno NUMBER;  \n" +
			"  BEGIN  \n" +
			"     RETURN(1);  \n" +
			"  END;  \n" +
			" \n" +
			"  FUNCTION create_dept(department_id NUMBER, location_id NUMBER)  \n" +
			"     RETURN NUMBER IS  \n" +
			"        new_deptno NUMBER;  \n" +
			"     BEGIN  \n" +
			"        RETURN(2);  \n" +
			"     END;  \n" +
			"      \n" +
			"  PROCEDURE remove_emp (employee_id NUMBER) IS  \n" +
			"     BEGIN  \n" +
			"       NULL; \n" +
			"     END;  \n" +
			"END emp_mgmt; \n" +
			"/";
		TestUtil.executeScript(con, body, DelimiterDefinition.DEFAULT_ORA_DELIMITER);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testCompileProcedure()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		List<ProcedureDefinition> procs = con.getMetadata().getProcedureReader().getProcedureList(null, OracleTestUtil.SCHEMA_NAME, "%");
		assertEquals(4, procs.size());

		ProcedureDefinition get = procs.get(0);
		assertEquals("AAA_GET_ANSWER", get.getProcedureName());

		assertTrue(OracleObjectCompiler.canCompile(get));

		OracleObjectCompiler compiler = new OracleObjectCompiler(con);
		String sql = compiler.createCompileStatement(get);
		assertEquals("ALTER FUNCTION WBJUNIT.AAA_GET_ANSWER COMPILE", sql);
		String msg = compiler.compileObject(get);
		assertNull(msg);

		ProcedureDefinition pkgFunc = procs.get(1);
		sql = compiler.createCompileStatement(pkgFunc);
		msg = compiler.compileObject(pkgFunc);
		assertNull(msg);
	}
}
