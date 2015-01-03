/*
 * XmlRowDataConverterTest.java
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
package workbench.db.exporter;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.storage.ResultInfo;
import workbench.storage.RowData;

import workbench.util.StringUtil;
import workbench.util.ValueConverter;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class XmlRowDataConverterTest
	extends WbTestCase
{

	public XmlRowDataConverterTest()
	{
		super("XmlRowDataConverterTest");
	}

	@Test
	public void testConvert()
		throws Exception
	{
		String[] cols = new String[] { "char_col", "int_col", "date_col", "ts_col"};
		int[] types = new int[] { Types.VARCHAR, Types.INTEGER, Types.DATE, Types.TIMESTAMP };
		int[] sizes = new int[] { 10, 10, 10, 10 };

		ResultInfo info = new ResultInfo(cols, types, sizes);
		XmlRowDataConverter converter = new XmlRowDataConverter();
		converter.setDefaultTimestampFormat("yyyy-MM-dd HH:mm:ss");
		converter.setDefaultDateFormat("yyyy-MM-dd");
		converter.setWriteHeader(true);
		converter.setResultInfo(info);

		String generatingSql = "SELECT * FROM some_table WHERE x < 1000";
		converter.setGeneratingSql(generatingSql);

		StringBuilder header = converter.getStart();
		assertNotNull(header);

		RowData data = new RowData(info);
		data.setValue(0, "char_column_data");
		data.setValue(1, new Integer(42));
		ValueConverter valueConverter = new ValueConverter("yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss");
		Date d1 = (Date)valueConverter.convertValue("2008-07-23", Types.DATE);
		data.setValue(2, d1);
		Timestamp ts1 = (Timestamp)valueConverter.convertValue("2008-07-23 13:42:01", Types.TIMESTAMP);
		data.setValue(3, ts1);

		StringBuilder converted = converter.convertRowData(data, 0);
		assertNotNull(converted);
		String xml = converted.toString();

		String colValue = TestUtil.getXPathValue(xml, "/row-data[@row-num='1']/column-data[@index='0']/text()");
		assertEquals(data.getValue(0), colValue);

		colValue = TestUtil.getXPathValue(xml, "/row-data[@row-num='1']/column-data[@index='1']/text()");
		assertEquals(data.getValue(1).toString(), colValue);

		colValue = TestUtil.getXPathValue(xml, "/row-data[@row-num='1']/column-data[@index='2']/text()");
		assertEquals("2008-07-23", colValue);

		colValue = TestUtil.getXPathValue(xml, "/row-data[@row-num='1']/column-data[@index='2']/@longValue");
		long l = Long.valueOf(colValue);
		Date d2 = new Date(l);
		assertEquals(d1, d2);

		colValue = TestUtil.getXPathValue(xml, "/row-data[@row-num='1']/column-data[@index='3']/text()");
		assertEquals("2008-07-23 13:42:01", colValue);

		colValue = TestUtil.getXPathValue(xml, "/row-data[@row-num='1']/column-data[@index='3']/@longValue");
		l = Long.valueOf(colValue);
		Timestamp ts2 = new Timestamp(l);
		assertEquals("2008-07-23 13:42:01.000", StringUtil.getIsoTimestampFormatter().format(ts2));
		assertEquals(ts1, ts2);

		String head = converter.getStart().toString();
		head += converter.getEnd(1).toString();

		String sql = TestUtil.getXPathValue(head, "/wb-export/meta-data/generating-sql");
		assertEquals(generatingSql, sql.trim());
	}

}

