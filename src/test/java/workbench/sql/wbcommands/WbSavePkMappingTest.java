/*
 * WbSavePkMappingTest.java
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
package workbench.sql.wbcommands;

import java.io.BufferedReader;
import java.util.List;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.sql.StatementRunnerResult;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.storage.PkMapping;
import workbench.util.WbFile;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbSavePkMappingTest
	extends WbTestCase
{

	public WbSavePkMappingTest()
	{
		super("WbSavePkMappingTest");
	}

	@Test
	public void testExecute()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbFile f = new WbFile(util.getBaseDir(), "pkmapping.properties");
		PkMapping.getInstance().clear();
		PkMapping.getInstance().addMapping("person", "id");
		PkMapping.getInstance().addMapping("v_person", "id");

		WbSavePkMapping save = new WbSavePkMapping();

		StatementRunnerResult result = save.execute(save.getVerb() + "-file=" + f.getFullPath());
		assertNotNull(result);
		assertTrue(result.isSuccess());

		BufferedReader in = EncodingUtil.createBufferedReader(f, "ISO-8859-1");
		String lines = FileUtil.readCharacters(in);
		assertTrue(lines.contains("# Primary key mapping for SQL Workbench/J"));
		assertTrue(lines.contains("person=id"));
		assertTrue(lines.contains("v_person=id"));
	}
}
