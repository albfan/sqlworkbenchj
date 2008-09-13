/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
