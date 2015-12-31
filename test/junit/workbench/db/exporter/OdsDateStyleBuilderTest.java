/*
 * OdsDateStyleBuilderTest.java
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
package workbench.db.exporter;

import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OdsDateStyleBuilderTest
{

	public OdsDateStyleBuilderTest()
	{
	}

	@Test
	public void testGetXML()
	{
		OdsDateStyleBuilder builder = new OdsDateStyleBuilder("yyyy-MM-dd HH:mm:ss");
		String xml = builder.getXML("");
		String expected =
			"<number:year number:style=\"long\"/>\n" +
			"<number:text>-</number:text>\n" +
			"<number:month/>\n" +
			"<number:text>-</number:text>\n" +
			"<number:day/>\n" +
			"<number:text> </number:text>\n" +
			"<number:hours number:style=\"long\"/>\n" +
			"<number:text>:</number:text>\n" +
			"<number:minutes/>\n" +
			"<number:text>:</number:text>\n" +
			"<number:seconds number:style=\"long\"/>";
		assertEquals(expected, xml.trim());

		builder = new OdsDateStyleBuilder("dd.MM.yy HH:mm");
		xml = builder.getXML("");
		expected =
			"<number:day/>\n" +
			"<number:text>.</number:text>\n" +
			"<number:month/>\n" +
			"<number:text>.</number:text>\n" +
			"<number:year/>\n" +
			"<number:text> </number:text>\n" +
			"<number:hours number:style=\"long\"/>\n" +
			"<number:text>:</number:text>\n" +
			"<number:minutes/>";
//		System.out.println(xml.trim() + "\n-------\n" + expected);
		assertEquals(expected, xml.trim());

	}

}
