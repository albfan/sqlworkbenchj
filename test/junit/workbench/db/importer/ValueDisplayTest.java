/*
 * ValueDisplayTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ValueDisplayTest
{

	@Test
	public void testToString()
	{
		ValueDisplay value = new ValueDisplay((new Object[] { "one", "two", new Integer(42)} ));
		assertEquals("{[one],[two],[42]}", value.toString());
	}
}
