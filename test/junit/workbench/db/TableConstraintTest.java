/*
 * TableConstraintTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import static org.junit.Assert.*;
import org.junit.Test;


/**
 *
 * @author Thomas Kellerer
 */
public class TableConstraintTest
{

	@Test
	public void testEquals()
	{
		TableConstraint c1 = new TableConstraint("POSITIVE_NR", "(NR > 0)");
		TableConstraint c2 = new TableConstraint("POSITIVE_NR", "(NR > 0)");
		assertTrue(c1.equals(c2));

		c1 = new TableConstraint("POSITIVE_NR", "(NR > 1)");
		c2 = new TableConstraint("POSITIVE_NR", "(NR > 0)");
		assertFalse(c1.equals(c2));

		c1 = new TableConstraint("SYS_1234", "(NR > 0)");
		c1.setIsSystemName(true);
		c2 = new TableConstraint("SYS_4321", "(NR > 0)");
		c2.setIsSystemName(true);
		assertTrue(c1.equals(c2));

		c1 = new TableConstraint("SYS_1234", "(NR > 0)");
		c1.setIsSystemName(true);
		c2 = new TableConstraint("POSITIVE_NR", "(NR > 0)");
		assertFalse(c1.equals(c2));

		assertTrue(c1.expressionIsEqual(c2));
	}
}
