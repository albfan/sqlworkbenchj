/*
 * ToolDefinitionTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.File;
import junit.framework.TestCase;
import workbench.TestUtil;

/**
 *
 * @author tkellerer
 */
public class ToolDefinitionTest
	extends TestCase
{

	public ToolDefinitionTest(String testName)
	{
		super(testName);
	}

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
