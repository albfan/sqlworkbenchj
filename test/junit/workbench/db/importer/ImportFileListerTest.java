/*
 * ImportFileListerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.db.importer;

import org.junit.Test;
import workbench.WbTestCase;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import workbench.TestUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ImportFileListerTest
	extends WbTestCase
{

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
