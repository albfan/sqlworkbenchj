/*
 *  TableCopyTest.java
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.wbcommands;

import java.util.Map;
import workbench.util.ArgumentParser;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class TableCopyTest
{

	@Test
	public void testParseMapping()
	{
		TableCopy copy = new TableCopy();
		String cmdline = "-columns='Time/\"Time\", Intrvl/\"Intrvl\"'";

		ArgumentParser parser = new ArgumentParser();
		parser.addArgument(WbCopy.PARAM_COLUMNS);
		parser.parse(cmdline);

		Map<String, String> map = copy.parseMapping(parser);
		for (Map.Entry<String, String> entry : map.entrySet())
		{
			assertEquals("\"" + entry.getKey() + "\"", entry.getValue());
		}

	}
}
