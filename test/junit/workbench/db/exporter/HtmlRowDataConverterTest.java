/*
 * HtmlRowDataConverterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.sql.Types;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.util.StrBuffer;
import workbench.util.ValueConverter;

/**
 *
 * @author Thomas Kellerer
 */
public class HtmlRowDataConverterTest
	extends WbTestCase
{

	public HtmlRowDataConverterTest(String testName)
	{
		super(testName);
	}

	public void testConvert()
		throws Exception
	{
		String[] cols = new String[] { "char_col", "int_col", "date_col", "ts_col"};
		int[] types = new int[] { Types.VARCHAR, Types.INTEGER, Types.DATE, Types.TIMESTAMP };
		int[] sizes = new int[] { 10, 10, 10, 10 };

		ResultInfo info = new ResultInfo(cols, types, sizes);
		HtmlRowDataConverter converter = new HtmlRowDataConverter();
		converter.setDefaultTimestampFormat("yyyy-MM-dd HH:mm:ss");
		converter.setDefaultDateFormat("yyyy-MM-dd");
		converter.setWriteHeader(true);
		converter.setResultInfo(info);
		converter.setCreateFullPage(true);
		converter.setPageTitle("Unit Test");

		StrBuffer header = converter.getStart();
		assertNotNull(header);
		StrBuffer end = converter.getEnd(1);
		assertNotNull(end);

		RowData data = new RowData(info);
		data.setValue(0, "char_column_data");
		data.setValue(1, new Integer(42));
		ValueConverter valueConverter = new ValueConverter("yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss");
		data.setValue(2, valueConverter.convertValue("2008-07-23", Types.DATE));
		data.setValue(3, valueConverter.convertValue("2008-07-23 13:42:01", Types.TIMESTAMP));

		StrBuffer converted = converter.convertRowData(data, 0);
		assertNotNull(converted);
		String html = header + "\n" + converted.toString() + "\n" + end;
//		System.out.println(html);

		String title = TestUtil.getXPathValue(html, "/html/head/title");
		assertEquals("Unit Test", title);

		String colValue = TestUtil.getXPathValue(html, "/html/body/table/tr[2]/td[1]/text()");
		assertEquals(colValue, data.getValue(0));

		colValue = TestUtil.getXPathValue(html, "/html/body/table/tr[2]/td[2]/text()");
		assertEquals(colValue, data.getValue(1).toString());

		colValue = TestUtil.getXPathValue(html, "/html/body/table/tr[2]/td[3]/text()");
		assertEquals(colValue, "2008-07-23");

		colValue = TestUtil.getXPathValue(html, "/html/body/table/tr[2]/td[4]/text()");
		assertEquals(colValue, "2008-07-23 13:42:01");
	}
}
