/*
 * XsltTransformerTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
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
			util.copyResourceFile(this, "person.xml");
			File input = new File(util.getBaseDir(), "person.xml");
			util.copyResourceFile(this, "wbexport2text.xslt");
			File xslt = new File(util.getBaseDir(), "wbexport2text.xslt");

			File output = new File(util.getBaseDir(), "data.txt");
			String inputFileName = input.getAbsolutePath();
			String outputFileName = output.getAbsolutePath();
			String xslFileName = xslt.getAbsolutePath();

			XsltTransformer transformer = new XsltTransformer();
			transformer.transform(inputFileName, outputFileName, xslFileName);
			assertTrue(output.exists());
			List<String> lines = TestUtil.readLines(output);
			assertNotNull(lines);
			assertEquals(6, lines.size());
		}
		finally
		{
			util.emptyBaseDirectory();
		}
	}

}
