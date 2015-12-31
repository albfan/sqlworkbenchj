/*
 * XlsXMLRowDataConverterTest.java
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

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.storage.ResultInfo;
import workbench.storage.RowData;

import workbench.util.ValueConverter;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class XlsXMLRowDataConverterTest
	extends WbTestCase
{

	public XlsXMLRowDataConverterTest()
	{
		super("XlsXMLRowDataConverterTest");
	}

	@Test
	public void testConvert()
		throws Exception
	{
		String[] cols = new String[]
		{
			"char_col", "int_col", "date_col", "ts_col"
		};
		int[] types = new int[]
		{
			Types.VARCHAR, Types.INTEGER, Types.DATE, Types.TIMESTAMP
		};
		int[] sizes = new int[]
		{
			10, 10, 10, 10
		};

		ResultInfo info = new ResultInfo(cols, types, sizes);
		XlsXMLRowDataConverter converter = new XlsXMLRowDataConverter();
		converter.setDefaultTimestampFormat("yyyy-MM-dd HH:mm:ss");
		converter.setDefaultDateFormat("yyyy-MM-dd");
		converter.setWriteHeader(true);
		converter.setResultInfo(info);
		String generatingSql = "SELECT * FROM some_table WHERE some_id < 1000";
		converter.setGeneratingSql(generatingSql);

		converter.setEncoding("UTF-8");
		StringBuilder header = converter.getStart();
		assertNotNull(header);
		StringBuilder footer = converter.getEnd(1);
		assertNotNull(footer);

		RowData data = new RowData(info);
		data.setValue(0, "char_column_data");
		data.setValue(1, new Integer(42));
		ValueConverter valueConverter = new ValueConverter("yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss");
		data.setValue(2, valueConverter.convertValue("2008-07-23", Types.DATE));
		data.setValue(3, valueConverter.convertValue("2008-07-23 13:42:01", Types.TIMESTAMP));

		StringBuilder converted = converter.convertRowData(data, 0);
		assertNotNull(converted);
		String row = converted.toString();

		String xml = header.toString() + row + footer.toString();
//		TestUtil.writeFile(new File("c:/temp/xls.xml"), xml);
//		System.out.println(xml);
//		System.out.println("-----------------------------------------");

		Map<String, String> nsMap = new HashMap<String, String>();
		nsMap.put("mso", "urn:schemas-microsoft-com:office:spreadsheet");

		// Check header row
		String colValue = TestUtil.getXPathValue(xml, "/mso:Workbook/mso:Worksheet/mso:Table/mso:Row[1]/mso:Cell[1]/mso:Data/text()", nsMap);
		assertEquals("char_col", colValue);

		colValue = TestUtil.getXPathValue(xml, "/mso:Workbook/mso:Worksheet/mso:Table/mso:Row[1]/mso:Cell[2]/mso:Data/text()", nsMap);
		assertEquals("int_col", colValue);

		colValue = TestUtil.getXPathValue(xml, "/mso:Workbook/mso:Worksheet/mso:Table/mso:Row[1]/mso:Cell[3]/mso:Data/text()", nsMap);
		assertEquals("date_col", colValue);

		colValue = TestUtil.getXPathValue(xml, "/mso:Workbook/mso:Worksheet/mso:Table/mso:Row[1]/mso:Cell[4]/mso:Data/text()", nsMap);
		assertEquals("ts_col", colValue);

		// Check data values
		colValue = TestUtil.getXPathValue(xml, "/mso:Workbook/mso:Worksheet/mso:Table/mso:Row[2]/mso:Cell[1]/mso:Data/text()", nsMap);
		assertEquals(data.getValue(0), colValue);

		colValue = TestUtil.getXPathValue(xml, "/mso:Workbook/mso:Worksheet/mso:Table/mso:Row[2]/mso:Cell[2]/mso:Data/text()", nsMap);
		assertEquals(data.getValue(1).toString(), colValue);

		colValue = TestUtil.getXPathValue(xml, "/mso:Workbook/mso:Worksheet/mso:Table/mso:Row[2]/mso:Cell[3]/mso:Data/text()", nsMap);
		assertEquals("2008-07-23T00:00:00", colValue);

		colValue = TestUtil.getXPathValue(xml, "/mso:Workbook/mso:Worksheet/mso:Table/mso:Row[2]/mso:Cell[4]/mso:Data/text()", nsMap);
		assertEquals("2008-07-23T13:42:01", colValue);
	}

	@Test
	public void testNullString()
		throws Exception
	{
		String[] cols = new String[] { "char_col", "int_col"};
		int[] types = new int[] { Types.VARCHAR, Types.INTEGER};
		int[] sizes = new int[] { 10, 10 };

		ResultInfo info = new ResultInfo(cols, types, sizes);
		XlsXMLRowDataConverter converter = new XlsXMLRowDataConverter();
		converter.setResultInfo(info);
		converter.setNullString("[NULL]");
		RowData data = new RowData(info);
		data.setValue(0, null);
		data.setValue(1, new Integer(42));

		converter.setEncoding("UTF-8");
		StringBuilder header = converter.getStart();
		assertNotNull(header);
		StringBuilder footer = converter.getEnd(1);
		assertNotNull(footer);

		StringBuilder converted = converter.convertRowData(data, 0);
		assertNotNull(converted);
		String row = converted.toString();
		String xml = header.toString() + row + footer.toString();

//		System.out.println(xml);
//		System.out.println("-----------------------------------------");

		Map<String, String> nsMap = new HashMap<String, String>();
		nsMap.put("mso", "urn:schemas-microsoft-com:office:spreadsheet");

		// Check data values
		String colValue = TestUtil.getXPathValue(xml, "/mso:Workbook/mso:Worksheet/mso:Table/mso:Row[2]/mso:Cell[1]/mso:Data/text()", nsMap);
		assertEquals("[NULL]", colValue);

		colValue = TestUtil.getXPathValue(xml, "/mso:Workbook/mso:Worksheet/mso:Table/mso:Row[2]/mso:Cell[2]/mso:Data/text()", nsMap);
		assertEquals("42", colValue);

	}
}
