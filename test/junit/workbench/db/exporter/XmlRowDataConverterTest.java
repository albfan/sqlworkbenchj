/*
 * XmlRowDataConverterTest.java
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
public class XmlRowDataConverterTest
	extends TestCase
{

	public XmlRowDataConverterTest(String testName)
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
		XmlRowDataConverter converter = new XmlRowDataConverter();
		converter.setDefaultTimestampFormat("yyyy-MM-dd HH:mm:ss");
		converter.setDefaultDateFormat("yyyy-MM-dd");
		converter.setWriteHeader(true);
		converter.setResultInfo(info);
		StrBuffer header = converter.getStart();
		assertNotNull(header);
		
		RowData data = new RowData(info);
		data.setValue(0, "char_column_data");
		data.setValue(1, new Integer(42));
		ValueConverter valueConverter = new ValueConverter("yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss");
		data.setValue(2, valueConverter.convertValue("2008-07-23", Types.DATE));
		data.setValue(3, valueConverter.convertValue("2008-07-23 13:42:01", Types.TIMESTAMP));

		StrBuffer converted = converter.convertRowData(data, 0);
		assertNotNull(converted);
		String xml = converted.toString();
//		System.out.println(xml);
		String colValue = TestUtil.getXPathValue(xml, "/row-data[@row-num='1']/column-data[@index='0']/text()");
		assertEquals(data.getValue(0), colValue);
		
		colValue = TestUtil.getXPathValue(xml, "/row-data[@row-num='1']/column-data[@index='1']/text()");
		assertEquals(data.getValue(1).toString(), colValue);
		
		colValue = TestUtil.getXPathValue(xml, "/row-data[@row-num='1']/column-data[@index='2']/text()");
		assertEquals("2008-07-23", colValue);

		colValue = TestUtil.getXPathValue(xml, "/row-data[@row-num='1']/column-data[@index='2']/@longValue");
		assertEquals("1216764000000", colValue);
		
		colValue = TestUtil.getXPathValue(xml, "/row-data[@row-num='1']/column-data[@index='3']/text()");
		assertEquals("2008-07-23 13:42:01", colValue);
		
		colValue = TestUtil.getXPathValue(xml, "/row-data[@row-num='1']/column-data[@index='3']/@longValue");
		assertEquals("1216813321000", colValue);
	}
	
}

