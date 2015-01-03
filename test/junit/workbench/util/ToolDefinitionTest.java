/*
 * ToolDefinitionTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
		ToolDefinition tool = new ToolDefinition(t.getFullPath(), "/h", "MyTool");
		WbFile exe = tool.getExecutable();
		assertEquals(t.getFullPath(), exe.getFullPath());
	}

}
