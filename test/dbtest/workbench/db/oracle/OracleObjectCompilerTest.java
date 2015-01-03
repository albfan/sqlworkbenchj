/*
 * OracleObjectCompilerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.sql.DelimiterDefinition;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleObjectCompilerTest
	extends WbTestCase
{
	private static final String TEST_ID = "oracompiler";

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
		assertNotNull("Oracle not available", con);

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

		sql =
			"create table person (id integer primary key, first_name varchar(100), last_name varchar(100), check (id > 0));\n" +
			"create view v_person (id, full_name) as select id, first_name || ' ' || last_name from person;\n" +
			"create materialized view mv_person as select * from person;";
		TestUtil.executeScript(con, sql);
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

		ProcedureReader reader = con.getMetadata().getProcedureReader();
		List<ProcedureDefinition> procs = reader.getProcedureList(null, OracleTestUtil.SCHEMA_NAME, "%");
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

		TestUtil.executeScript(con, "create procedure nocando as begin null end;");
		ProcedureDefinition proc = reader.findProcedure(new ProcedureDefinition(null, OracleTestUtil.SCHEMA_NAME, "NOCANDO"));
		msg = compiler.compileObject(proc);
		assertNotNull(msg);
		assertTrue(msg.contains("PLS-00103"));
	}

	@Test
	public void testCompileView()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;
		OracleObjectCompiler compiler = new OracleObjectCompiler(con);
		TableIdentifier tbl = con.getMetadata().findObject(new TableIdentifier("V_PERSON"));
		assertTrue(OracleObjectCompiler.canCompile(tbl));
		String error = compiler.compileObject(tbl);
		assertNull(error);

		TableIdentifier mv = con.getMetadata().findObject(new TableIdentifier("MV_PERSON"));
		assertTrue(OracleObjectCompiler.canCompile(mv));
		error = compiler.compileObject(mv);
		assertNull(error);
	}
}
