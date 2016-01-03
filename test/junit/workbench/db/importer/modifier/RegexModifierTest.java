/*
 * RegexModifierTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.db.importer.modifier;

import static org.junit.Assert.*;
import org.junit.Test;
import workbench.db.ColumnIdentifier;

/**
 *
 * @author Thomas Kellerer
 */
public class RegexModifierTest
{
	public RegexModifierTest()
	{
	}

	@Test
	public void testModifyValue()
	{
		RegexModifier modifier = new RegexModifier();

		ColumnIdentifier fname = new ColumnIdentifier("fname");
		ColumnIdentifier lname = new ColumnIdentifier("lname");
		modifier.addDefinition(fname, "bronx", "brox");
		modifier.addDefinition(lname, "\\\"", "\\'");

		String modified = modifier.modifyValue(fname, "Zaphod Beeblebronx");
		assertEquals("Zaphod Beeblebrox", modified);

		modified = modifier.modifyValue(lname, "Zaphod Beeblebronx");
		assertEquals("Zaphod Beeblebronx", modified);

		modified = modifier.modifyValue(lname, "Test\" value");
	}
}
