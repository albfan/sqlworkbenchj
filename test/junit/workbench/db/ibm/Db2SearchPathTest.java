/*
 * Db2SearchPathTest.java
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
package workbench.db.ibm;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2SearchPathTest
{
	public Db2SearchPathTest()
	{
	}

	@Test
	public void testParseResult()
	{
		Db2SearchPath reader = new Db2SearchPath();
		List<String> entries = CollectionUtil.arrayList("*LBL");
		List<String> result = reader.parseResult(entries);
		assertTrue(result.isEmpty());
		entries = CollectionUtil.arrayList("one, two");
		result = reader.parseResult(entries);
		assertEquals(2, result.size());
	}
}
