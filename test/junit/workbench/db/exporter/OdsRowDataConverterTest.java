/*
 * OdsRowDataConverterTest.java
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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.util.FileUtil;
import workbench.util.ValueConverter;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class OdsRowDataConverterTest
	extends TestCase
{

	public OdsRowDataConverterTest(String testName)
	{
		super(testName);
	}

	public void testColumnToName()
	{
		OdsRowDataConverter converter = new OdsRowDataConverter();
		assertEquals("A", converter.columnToName(1));
		assertEquals("Z", converter.columnToName(26));
		assertEquals("AA", converter.columnToName(27));
		assertEquals("AP", converter.columnToName(42));
		assertEquals("SR", converter.columnToName(512));
		assertEquals("SR", converter.columnToName(512));
		assertEquals("ZZ", converter.columnToName(702));
		assertEquals("AAA", converter.columnToName(703));
		assertEquals("AKI", converter.columnToName(971));
		assertEquals("AMJ", converter.columnToName(1024));
	}
	
	public void testConvert()
		throws Exception
	{
		TestUtil util = new TestUtil("OdsExportTest");
		util.prepareEnvironment();

		String[] cols = new String[] { "char_col", "int_col", "date_col", "ts_col"};
		int[] types = new int[] { Types.VARCHAR, Types.INTEGER, Types.DATE, Types.TIMESTAMP };
		int[] sizes = new int[] { 10, 10, 10, 10 };

		ResultInfo info = new ResultInfo(cols, types, sizes);
		OdsRowDataConverter converter = new OdsRowDataConverter();
		WbFile output = new WbFile(util.getBaseDir(), "testData.ods");
		converter.setOutputFile(output);
		converter.setDefaultTimestampFormat("yyyy-MM-dd HH:mm:ss");
		converter.setDefaultDateFormat("yyyy-MM-dd");
		converter.setWriteHeader(true);
		converter.setResultInfo(info);

		// Start writing
		converter.getStart();
		
		RowData data = new RowData(info);
		data.setValue(0, "char_column_data");
		data.setValue(1, new Integer(42));
		ValueConverter valueConverter = new ValueConverter("yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss");
		data.setValue(2, valueConverter.convertValue("2008-07-23", Types.DATE));
		data.setValue(3, valueConverter.convertValue("2008-07-23 13:42:01", Types.TIMESTAMP));

		converter.convertRowData(data, 1);
		
		// Finish writing, make sure the archive is closed properly
		converter.getEnd(1);

		assertTrue(output.exists());
		
		ZipFile archive = new ZipFile(output);
		ZipEntry entry = archive.getEntry("content.xml");
		InputStream in = archive.getInputStream(entry);
		InputStreamReader reader = new InputStreamReader(in, "UTF-8");
		String content = FileUtil.readCharacters(reader);
		archive.close();
//		System.out.println(content);

		Map<String, String> nsMap = new HashMap<String, String>();
		nsMap.put("office", "urn:oasis:names:tc:opendocument:xmlns:office:1.0");
		nsMap.put("text", "urn:oasis:names:tc:opendocument:xmlns:text:1.0");
		nsMap.put("table", "urn:oasis:names:tc:opendocument:xmlns:table:1.0");

		String colValue = TestUtil.getXPathValue(content, 
			"/office:document-content/office:body/office:spreadsheet/table:table[1]/table:table-row[1]/table:table-cell[1]/text:p/text()",
			nsMap);
		assertEquals(data.getValue(0), colValue);
		
		colValue = TestUtil.getXPathValue(content,
			"/office:document-content/office:body/office:spreadsheet/table:table[1]/table:table-row[1]/table:table-cell[2]/text:p/text()",
			nsMap);
		assertEquals(data.getValue(1).toString(), colValue);		
		
		colValue = TestUtil.getXPathValue(content,
			"/office:document-content/office:body/office:spreadsheet/table:table[1]/table:table-row[1]/table:table-cell[3]/text:p/text()",
			nsMap);
		assertEquals("2008-07-23", colValue);		

		colValue = TestUtil.getXPathValue(content,
			"/office:document-content/office:body/office:spreadsheet/table:table[1]/table:table-row[1]/table:table-cell[3]/@office:date-value",
			nsMap);
		assertEquals("2008-07-23", colValue);		

		colValue = TestUtil.getXPathValue(content,
			"/office:document-content/office:body/office:spreadsheet/table:table[1]/table:table-row[1]/table:table-cell[4]/text:p/text()",
			nsMap);
		assertEquals("2008-07-23 13:42:01", colValue);		

		colValue = TestUtil.getXPathValue(content,
			"/office:document-content/office:body/office:spreadsheet/table:table[1]/table:table-row[1]/table:table-cell[4]/@office:date-value",
			nsMap);
		assertEquals("2008-07-23T13:42:01", colValue);		
		
	}
	
}
