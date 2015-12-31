/*
 * XsltTransformerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import java.util.List;
import java.io.File;
import workbench.WbTestCase;
import workbench.TestUtil;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class XsltTransformerTest
	extends WbTestCase
{

	public XsltTransformerTest()
	{
		super("XsltTransformerTest");
	}

	@Test
	public void testTransform()
		throws Exception
	{
		TestUtil util = getTestUtil();
		try
		{
			File input = util.copyResourceFile(this, "person.xml");
			File xslt = util.copyResourceFile(this, "wbexport2text.xslt");

			File output = new File(util.getBaseDir(), "data.txt");
			String inputFileName = input.getAbsolutePath();
			String outputFileName = output.getAbsolutePath();
			String xslFileName = xslt.getAbsolutePath();

			XsltTransformer transformer = new XsltTransformer();
			transformer.transform(inputFileName, outputFileName, xslFileName);
			assertTrue(output.exists());
			List<String> lines = StringUtil.readLines(output);
			assertNotNull(lines);
			assertEquals(6, lines.size());
		}
		finally
		{
			util.emptyBaseDirectory();
		}
	}

}
