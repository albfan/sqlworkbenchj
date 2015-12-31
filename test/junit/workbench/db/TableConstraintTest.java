/*
 * TableConstraintTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
