/*
 * ToolDefinitionTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.File;
import workbench.TestUtil;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author tkellerer
 */
public class ToolDefinitionTest
{

	@Test
	public void testGetExecutable()
		throws Exception
	{
		TestUtil util = new TestUtil("ToolDefinition");
		File f = File.createTempFile("my test", ".exec", new File(util.getBaseDir()));

		WbFile t = new WbFile(f);
		ToolDefinition tool = new ToolDefinition("\"" + t.getFullPath() + "\" /h", "MyTool");
		WbFile exe = tool.getExecutable();
		assertEquals(t.getFullPath(), exe.getFullPath());
	}

}
