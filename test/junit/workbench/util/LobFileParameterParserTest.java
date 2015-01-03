/*
 * LobFileParameterParserTest.java
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

import workbench.WbTestCase;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class LobFileParameterParserTest
	extends WbTestCase
{

	public LobFileParameterParserTest()
	{
		super("LobFileParameterParserTest");
	}

	@Test
	public void testGetParameters()
		throws Exception
	{
		String sql = "update bla set col = {$blobfile=c:/temp/test.data} where x=1";

		LobFileParameterParser p = new LobFileParameterParser(sql);
		LobFileParameter[] parms = p.getParameters();
		assertNotNull(parms);
		assertEquals("File not recognized", 1, parms.length);
		assertEquals("Wrong filename", "c:/temp/test.data", parms[0].getFilename());

		sql = "update bla set col = {$clobfile=c:/temp/test.data encoding=UTF8} where x=1";
		p = new LobFileParameterParser(sql);
		parms = p.getParameters();
		assertNotNull(parms);
		assertEquals("File not recognized", 1, parms.length);
		assertEquals("Wrong filename", "c:/temp/test.data", parms[0].getFilename());
		assertEquals("Wrong encoding", "UTF8", parms[0].getEncoding());

		sql = "update bla set col = {$clobfile='c:/my data/test.data' encoding='UTF-8'} where x=1";
		p = new LobFileParameterParser(sql);
		parms = p.getParameters();
		assertNotNull(parms);
		assertEquals("File not recognized", 1, parms.length);
		assertEquals("Wrong filename", "c:/my data/test.data", parms[0].getFilename());
		assertEquals("Wrong encoding", "UTF-8", parms[0].getEncoding());

		sql = "{$blobfile=c:/temp/test.data}";
		p = new LobFileParameterParser(sql);
		parms = p.getParameters();
		assertNotNull(parms);
		assertEquals("File not recognized", 1, parms.length);
		assertEquals("Wrong filename returned", "c:/temp/test.data", parms[0].getFilename());
	}

}
