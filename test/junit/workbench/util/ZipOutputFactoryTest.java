/*
 * ZipOutputFactoryTest.java
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
import java.io.PrintWriter;

import workbench.TestUtil;
import workbench.WbTestCase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ZipOutputFactoryTest
	extends WbTestCase
{

	@Test
	public void testOuputFactory()
		throws Exception
	{
		TestUtil util = getTestUtil();
		File importFile  = new File(util.getBaseDir(), "datafile.txt");

		File archive = new File(util.getBaseDir(), "archive.zip");
		ZipOutputFactory zout = new ZipOutputFactory(archive);
		PrintWriter out = new PrintWriter(zout.createWriter(importFile, "UTF-8"));

		out.println("nr\tfirstname\tlastname");
		out.print(Integer.toString(1));
		out.print('\t');
		out.println("First\t\"Last");
		out.println("name\"");
		out.close();

		zout.done();

		if (!archive.delete())
		{
			fail("Could not delete archive");
		}
	}
}
