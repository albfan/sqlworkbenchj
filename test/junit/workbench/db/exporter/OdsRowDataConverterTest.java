/*
 * OdsRowDataConverterTest.java
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

import org.junit.Test;
import workbench.WbTestCase;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Types;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import workbench.TestUtil;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.util.FileUtil;
import workbench.util.ValueConverter;
import workbench.util.WbFile;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OdsRowDataConverterTest
	extends WbTestCase
{

	public OdsRowDataConverterTest()
	{
		super("OdsRowDataConverterTest");
	}

	@Test
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

	@Test
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

		String generatingSql = "select * from my_table where some_id < 100";
		converter.setGeneratingSql(generatingSql);

		// Start writing
		converter.getStart();

		RowData row1 = new RowData(info);
		row1.setValue(0, "char_column_data");
		row1.setValue(1, new Integer(42));
		ValueConverter valueConverter = new ValueConverter("yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss");
		row1.setValue(2, valueConverter.convertValue("2008-07-23", Types.DATE));
		row1.setValue(3, valueConverter.convertValue("2008-07-23 13:42:01", Types.TIMESTAMP));

		converter.convertRowData(row1, 1);


		RowData row2 = new RowData(info);
		row2.setValue(0, "foobar");
		row2.setValue(1, null);
		row2.setValue(2, null);
		row2.setValue(3, null);

		converter.convertRowData(row2, 2);

		// Finish writing, make sure the archive is closed properly
		converter.getEnd(1);

		assertTrue(output.exists());

		ZipFile archive = new ZipFile(output);
		ZipEntry entry = archive.getEntry("content.xml");
		InputStream in = archive.getInputStream(entry);
		InputStreamReader reader = new InputStreamReader(in, "UTF-8");
		String content = FileUtil.readCharacters(reader);

		entry = archive.getEntry("meta.xml");
		in = archive.getInputStream(entry);
		reader = new InputStreamReader(in, "UTF-8");
		String meta = FileUtil.readCharacters(reader);

		archive.close();

		Map<String, String> contentNamespaces = TestUtil.getNameSpaces(content, "office:document-content");
		Map<String, String> metaNamespaces = TestUtil.getNameSpaces(meta, "office:document-meta");

		String colValue = TestUtil.getXPathValue(content,
			"/office:document-content/office:body/office:spreadsheet/table:table[1]/table:table-row[1]/table:table-cell[1]/text:p/text()",
			contentNamespaces);
		assertEquals(row1.getValue(0), colValue);

		colValue = TestUtil.getXPathValue(content,
			"/office:document-content/office:body/office:spreadsheet/table:table[1]/table:table-row[1]/table:table-cell[2]/text:p/text()",
			contentNamespaces);
		assertEquals(row1.getValue(1).toString(), colValue);

		colValue = TestUtil.getXPathValue(content,
			"/office:document-content/office:body/office:spreadsheet/table:table[1]/table:table-row[1]/table:table-cell[3]/text:p/text()",
			contentNamespaces);
		assertEquals("2008-07-23", colValue);

		colValue = TestUtil.getXPathValue(content,
			"/office:document-content/office:body/office:spreadsheet/table:table[1]/table:table-row[1]/table:table-cell[3]/@office:date-value",
			contentNamespaces);
		assertEquals("2008-07-23", colValue);

		colValue = TestUtil.getXPathValue(content,
			"/office:document-content/office:body/office:spreadsheet/table:table[1]/table:table-row[1]/table:table-cell[4]/text:p/text()",
			contentNamespaces);
		assertEquals("2008-07-23 13:42:01", colValue);

		colValue = TestUtil.getXPathValue(content,
			"/office:document-content/office:body/office:spreadsheet/table:table[1]/table:table-row[1]/table:table-cell[4]/@office:date-value",
			contentNamespaces);
		assertEquals("2008-07-23T13:42:01", colValue);

		String sql = TestUtil.getXPathValue(meta,"/office:document-meta/office:meta/dc:description",metaNamespaces);
		assertEquals(generatingSql, sql);
		String count = TestUtil.getXPathValue(content, "count(/office:document-content/office:automatic-styles/number:date-style)", contentNamespaces);
		assertEquals("2", count);

		String text = TestUtil.getXPathValue(content,"/office:document-content/office:automatic-styles/number:date-style[@style:name='N60']/number:year/@number:style", contentNamespaces);
		assertEquals("long", text);

		text = TestUtil.getXPathValue(content,"/office:document-content/office:automatic-styles/number:date-style[@style:name='N60']/number:text[1]/text()", contentNamespaces);
		assertEquals("-", text);

		text = TestUtil.getXPathValue(content,"/office:document-content/office:automatic-styles/number:date-style[@style:name='N50']/number:hours/@number:style", contentNamespaces);
		assertEquals("long", text);

		text = TestUtil.getXPathValue(content,"/office:document-content/office:automatic-styles/number:date-style[@style:name='N50']/number:text[4]/text()", contentNamespaces);
		assertEquals(":", text);

		String dateColStyle = TestUtil.getXPathValue(content,"/office:document-content/office:automatic-styles/style:style[@style:data-style-name='N60']/@style:name", contentNamespaces);
		assertEquals("ce2", dateColStyle);

		String tsColStyle = TestUtil.getXPathValue(content,"/office:document-content/office:automatic-styles/style:style[@style:data-style-name='N50']/@style:name", contentNamespaces);
		assertEquals("ce3", tsColStyle);

		String dateStyleUsed = TestUtil.getXPathValue(content, "/office:document-content/office:body/office:spreadsheet/table:table[1]/table:table-column[3]/@table:default-cell-style-name", contentNamespaces);
		assertEquals(dateColStyle, dateStyleUsed);

		String tsStyleUsed = TestUtil.getXPathValue(content, "/office:document-content/office:body/office:spreadsheet/table:table[1]/table:table-column[4]/@table:default-cell-style-name", contentNamespaces);
		assertEquals(tsColStyle, tsStyleUsed);
	}
}
