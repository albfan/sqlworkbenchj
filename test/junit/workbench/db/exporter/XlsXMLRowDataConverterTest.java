/*
 * XlsXMLRowDataConverterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.util.StrBuffer;
import workbench.util.ValueConverter;

/**
 *
 * @author support@sql-workbench.net
 */
public class XlsXMLRowDataConverterTest
	extends TestCase
{

	public XlsXMLRowDataConverterTest(String testName)
	{
		super(testName);
	}

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
		converter.setEncoding("UTF-8");
		StrBuffer header = converter.getStart();
		assertNotNull(header);
		StrBuffer footer = converter.getEnd(1);
		assertNotNull(footer);

		RowData data = new RowData(info);
		data.setValue(0, "char_column_data");
		data.setValue(1, new Integer(42));
		ValueConverter valueConverter = new ValueConverter("yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss");
		data.setValue(2, valueConverter.convertValue("2008-07-23", Types.DATE));
		data.setValue(3, valueConverter.convertValue("2008-07-23 13:42:01", Types.TIMESTAMP));

		StrBuffer converted = converter.convertRowData(data, 0);
		assertNotNull(converted);
		String row = converted.toString();

		String xml = header.toString() + row + footer.toString();
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
}
