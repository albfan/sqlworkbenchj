/*
 * RegexModifierTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
