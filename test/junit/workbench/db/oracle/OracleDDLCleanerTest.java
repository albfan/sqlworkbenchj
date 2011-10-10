/*
 * OracleDDLCleanerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleDDLCleanerTest
{

	public OracleDDLCleanerTest()
	{
	}

	@Test
	public void testCleanupQuotedIdentifiers()
	{
		String input = "SELECT \"PERSON\".\"FIRSTNAME\" FROM \"PERSON\"";
		String expected = "SELECT PERSON.FIRSTNAME FROM PERSON";
		String clean = OracleDDLCleaner.cleanupQuotedIdentifiers(input);
		assertEquals(expected, clean);

		input = "SELECT \"Person\".\"FIRSTNAME\" FROM \"Person\"";
		expected = "SELECT \"Person\".FIRSTNAME FROM \"Person\"";
		clean = OracleDDLCleaner.cleanupQuotedIdentifiers(input);
		assertEquals(expected, clean);

		input = "SELECT '\"' as quote FROM \"Person\"";
		clean = OracleDDLCleaner.cleanupQuotedIdentifiers(input);
		assertEquals(input, clean);

		input = "SELECT ' \"TEST\" ' as constant FROM \"Person\"";
		clean = OracleDDLCleaner.cleanupQuotedIdentifiers(input);
		assertEquals(input, clean);
	}

}
