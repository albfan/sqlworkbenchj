/*
 * CaseInsensitiveComparatorTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.tools;

import workbench.util.CaseInsensitiveComparator;
import java.util.Comparator;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Thomas Kellerer
 */
public class CaseInsensitiveComparatorTest
{

	@Test
	public void testComparator()
	{
		Comparator<String> c = new CaseInsensitiveComparator();
		int i = c.compare("Test", "TEST");
		assertEquals(0, i);

		i = c.compare("TEST", "test");
		assertEquals(0, i);

		i = c.compare("test", "test");
		assertEquals(0, i);

		i = c.compare("test", "tesd");
		assertEquals(false, (i == 0));
	}
}
