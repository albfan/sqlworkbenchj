/*
 * ImportFileListerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ImportFileListerTest
	extends WbTestCase
{

	@Test
	public void testMultipleFiles()
		throws Exception
	{
		TestUtil util = getTestUtil("ImportFileLister");
		TestUtil.writeFile(util.getFile("file_one.txt"), "foobar");
		TestUtil.writeFile(util.getFile("afile.txt"), "foobar");
		TestUtil.writeFile(util.getFile("zfile.txt"), "foobar");

		List<String> names = CollectionUtil.arrayList("zfile.txt", "file_one.txt", "afile.txt");

		ImportFileLister lister = new ImportFileLister(null, new File(util.getBaseDir()), names);
		List<WbFile> files = lister.getFiles();
		assertNotNull(files);
		assertEquals(3, files.size());

		assertEquals("zfile.txt", files.get(0).getName());
		assertEquals("file_one.txt", files.get(1).getName());
		assertEquals("afile.txt", files.get(2).getName());
	}

	@Test
	public void testIgnoreFiles()
		throws Exception
	{
		TestUtil util = getTestUtil("ImportFileLister");
		for (int i=0; i < 5; i++)
		{
			FileOutputStream out = new FileOutputStream(new File(util.getBaseDir(), "dbo.table-" + i + ".zip"));
			out.write(42);
			out.close();
		}
		ImportFileLister lister = new ImportFileLister(null, new File(util.getBaseDir()), "zip");
		List<WbFile> files = lister.getFiles();
		assertNotNull(files);
		assertEquals(5, files.size());

		List<String> toIgnore = StringUtil.stringToList("dbo.table-2.zip", ",");
		lister.ignoreFiles(toIgnore);

		files = lister.getFiles();
		assertEquals(4, files.size());
	}

}
