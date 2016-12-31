/*
 * HtmlRowDataConverterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.db.exporter;

import java.sql.Types;

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
public class HtmlRowDataConverterTest
	extends WbTestCase
{

	public HtmlRowDataConverterTest()
	{
		super("HtmlRowDataConverterTest");
	}

	@Test
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

		StringBuilder header = converter.getStart();
		assertNotNull(header);
		StringBuilder end = converter.getEnd(1);
		assertNotNull(end);

		RowData data = new RowData(info);
		data.setValue(0, "char_column_data");
		data.setValue(1, new Integer(42));
		ValueConverter valueConverter = new ValueConverter("yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss");
		data.setValue(2, valueConverter.convertValue("2008-07-23", Types.DATE));
		data.setValue(3, valueConverter.convertValue("2008-07-23 13:42:01", Types.TIMESTAMP));

		StringBuilder converted = converter.convertRowData(data, 0);
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
