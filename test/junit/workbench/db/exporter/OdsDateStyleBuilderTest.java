/*
 * OdsDateStyleBuilderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
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
