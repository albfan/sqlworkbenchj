/*
 * RegExComparatorTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.storage.filter;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class RegExComparatorTest
{

	public RegExComparatorTest()
	{
	}

	@Test
	public void testEvaluate()
	{
		RegExComparator comp = new RegExComparator();
		assertTrue(comp.needsValue());
		assertTrue(comp.supportsType(String.class));
		assertTrue(comp.supportsIgnoreCase());
		assertFalse(comp.comparesEquality());
		
		assertTrue(comp.evaluate("[a-z]+", "Arthur", true));
		assertFalse(comp.evaluate("^[a-z]+", "Arthur", false));
		
		assertTrue(comp.validateInput("^TEST"));
		assertFalse(comp.validateInput("(bla"));
	}
}
