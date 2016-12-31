/*
 * ImportFileHandlerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.db.importer;

import org.junit.Test;
import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.util.ZipOutputFactory;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ImportFileHandlerTest
	extends WbTestCase
{

	public ImportFileHandlerTest()
	{
		super("ImportFileHandlerTest");
	}

	@Test
	public void testHandler()
	{
		try
		{
			TestUtil util = new TestUtil("outputFactoryTest");
			File importFile  = new File(util.getBaseDir(), "datafile.txt");

			File archive = new File(util.getBaseDir(), "archive.zip");
			ZipOutputFactory zout = new ZipOutputFactory(archive);
			PrintWriter out = new PrintWriter(zout.createWriter(importFile, "UTF-8"));

			String firstline = "nr\tfirstname\tlastname";
			out.println(firstline);
			out.print(Integer.toString(1));
			out.print('\t');
			out.println("First\t\"Last");
			out.println("name\"");
			out.close();

			zout.done();

			ImportFileHandler handler = new ImportFileHandler();
			handler.setMainFile(archive, "UTF-8");
			assertEquals("ZIP Archive not recognized", true, handler.isZip());

			BufferedReader br = handler.getMainFileReader();
			String line = br.readLine();

			assertEquals("Wrong data read", firstline, line);

			handler.done();
			handler.setMainFile(archive, "UTF-8");
			handler.done();
			if (!archive.delete())
			{
				fail("Could not delete archive!");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
