/*
 * ConstraintNameTesterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.sqltemplates;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import workbench.WbTestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class ConstraintNameTesterTest
	extends WbTestCase
{
	public ConstraintNameTesterTest()
	{
		super("ConstraintNameTesterTest");
	}

	@Before
	public void setUp()
	{
	}

	@Test
	public void testOracle()
	{
		ConstraintNameTester tester = new ConstraintNameTester("oracle");
		assertTrue(tester.isSystemConstraintName("SYS_C0013077"));
		assertFalse(tester.isSystemConstraintName("SYS_C00130779")); // system generated names are exactly 12 characters long
		assertFalse(tester.isSystemConstraintName("PK_SOME_TABLE"));
		assertFalse(tester.isSystemConstraintName("SYS_MY_PK"));
	}

	@Test
	public void testH2()
	{
		ConstraintNameTester tester = new ConstraintNameTester("h2");
		assertTrue(tester.isSystemConstraintName("CONSTRAINT_FOOBAR"));
	}

	@Test
	public void testMicrosoft()
	{
		ConstraintNameTester tester = new ConstraintNameTester("microsoft_sql_server");
		assertTrue(tester.isSystemConstraintName("FK__child__base_id__70099B30"));
		assertTrue(tester.isSystemConstraintName("CK__check_test__id__2E3BD7D3"));
		assertTrue(tester.isSystemConstraintName("PK__child__3213D0856E2152BE"));
		assertFalse(tester.isSystemConstraintName("PK_child__100"));
		assertFalse(tester.isSystemConstraintName("fk_child_base"));
	}


}
