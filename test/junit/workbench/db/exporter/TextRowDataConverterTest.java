/*
 * TextRowDataConverterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import workbench.WbTestCase;

import workbench.db.ColumnIdentifier;

import workbench.storage.BlobFormatterFactory;
import workbench.storage.BlobLiteralType;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;

import workbench.util.CollectionUtil;

import org.junit.Test;

import static org.junit.Assert.*;

import workbench.util.CharacterEscapeType;
import workbench.util.CharacterRange;

/**
 *
 * @author Thomas Kellerer
 */
public class TextRowDataConverterTest
	extends WbTestCase
{

	public TextRowDataConverterTest()
	{
		super("TextRowDataConverterTest");
	}

	@Test
	public void testDuplicateColumns()
		throws Exception
	{
		ColumnIdentifier id = new ColumnIdentifier("id", Types.INTEGER, true);
		id.setPosition(1);

		ColumnIdentifier name1 = new ColumnIdentifier("name", Types.VARCHAR);
		name1.setPosition(2);

		ColumnIdentifier name2 = new ColumnIdentifier("name", Types.VARCHAR);
		name2.setPosition(3);

		ResultInfo info = new ResultInfo(new ColumnIdentifier[] { id, name1, name2 });
		TextRowDataConverter converter = new TextRowDataConverter();
		converter.setResultInfo(info);
		converter.setDelimiter(";");
		converter.setNullString("<NULL>");
		converter.setColumnsToExport(CollectionUtil.arrayList(id, name2));
		RowData data = new RowData(info);
		data.setValue(0, new Integer(42));
		data.setValue(1, "name_1");
		data.setValue(2, "name_2");

		StringBuilder line = converter.convertRowData(data, 0);
		assertEquals("Wrong data", "42;name_2", line.toString().trim());
	}

	@Test
	public void testNullString()
		throws Exception
	{
		String[] cols = new String[] { "char_col", "int_col"};
		int[] types = new int[] { Types.VARCHAR, Types.INTEGER};
		int[] sizes = new int[] { 10, 10 };

		ResultInfo info = new ResultInfo(cols, types, sizes);
		TextRowDataConverter converter = new TextRowDataConverter();
		converter.setResultInfo(info);
		converter.setDelimiter(";");
		converter.setNullString("<NULL>");
		RowData data = new RowData(info);
		data.setValue(0, null);
		data.setValue(1, new Integer(42));

		StringBuilder line = converter.convertRowData(data, 0);
		assertEquals("Wrong data", "<NULL>;42", line.toString().trim());

		converter.setNullString("[null]");
		data.setValue(0, "foobar");
		data.setValue(1, null);

		line = converter.convertRowData(data, 0);
		assertEquals("Wrong data", "foobar;[null]", line.toString().trim());

		converter.setDelimiter(";");
		converter.setEscapeRange(CharacterRange.RANGE_7BIT);
		converter.setEscapeType(CharacterEscapeType.pgHex);
		converter.setQuoteCharacter("'");
		converter.setNullString("\\foobar");
		line = converter.convertRowData(data, 0);
		assertEquals("foobar;\\foobar", line.toString().trim());

		converter.setNullString("\\NULL");
		data.setValue(0, "foo;bar");
		line = converter.convertRowData(data, 0);
		assertEquals("'foo;bar';\\NULL", line.toString().trim());
	}

	@Test
	public void testConvert()
		throws Exception
	{
		String[] cols = new String[] { "char_col", "int_col", "date_col", "ts_col", "t_col"};
		int[] types = new int[] { Types.VARCHAR, Types.INTEGER, Types.DATE, Types.TIMESTAMP, Types.TIME };
		int[] sizes = new int[] { 10, 10, 10, 10, 10 };

		ResultInfo info = new ResultInfo(cols, types, sizes);
		TextRowDataConverter converter = new TextRowDataConverter();
		converter.setDefaultTimestampFormat("yyyy-MM-dd HH:mm:ss");
		converter.setDefaultDateFormat("yyyy-MM-dd");
		converter.setDefaultTimeFormat("HH:mm:ss");
		converter.setWriteHeader(true);
		converter.setResultInfo(info);
		converter.setDelimiter(";");
		StringBuilder header = converter.getStart();
		assertNotNull(header);
		assertEquals("Wrong header", "char_col;int_col;date_col;ts_col;t_col", header.toString().trim());

		RowData data = new RowData(info);
		data.setValue(0, "data1");
		data.setValue(1, new Integer(42));
		Calendar c = Calendar.getInstance();
		c.set(2006, 9, 26, 17, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		java.util.Date d = c.getTime();
		data.setValue(2, c.getTime());
		java.sql.Timestamp ts = new java.sql.Timestamp(d.getTime());
		data.setValue(3, ts);

		c = Calendar.getInstance();
		c.set(1970, 0, 1, 23, 42, 24);
		c.set(Calendar.SECOND, 24);
		c.set(Calendar.MILLISECOND, 0);
		data.setValue(4, new java.sql.Time(c.getTime().getTime()));

		StringBuilder line = converter.convertRowData(data, 0);
		assertEquals("Wrong columns exporter", "data1;42;2006-10-26;2006-10-26 17:00:00;23:42:24", line.toString().trim());

		List<ColumnIdentifier> columns = new ArrayList<>();
		columns.add(info.getColumn(0));
		columns.add(info.getColumn(1));
		converter.setColumnsToExport(columns);
		line = converter.convertRowData(data, 0);
		assertNotNull("Data not converted", line);
		assertEquals("Wrong columns exporter", "data1;42", line.toString().trim());
	}

	@Test
	public void testBlobEncoding()
		throws Exception
	{
		String[] cols = new String[] { "id", "blob_col"};
		int[] types = new int[] { Types.INTEGER, Types.BLOB };
		int[] sizes = new int[] { 10, 10 };

		ResultInfo info = new ResultInfo(cols, types, sizes);
		TextRowDataConverter converter = new TextRowDataConverter();
		converter.setDefaultTimestampFormat("yyyy-MM-dd HH:mm:ss");
		converter.setDefaultDateFormat("yyyy-MM-dd");
		converter.setWriteHeader(true);
		converter.setBlobFormatter(BlobFormatterFactory.createInstance(BlobLiteralType.base64));
		converter.setResultInfo(info);
		converter.setDelimiter(";");
		converter.setWriteBlobToFile(false);
		StringBuilder header = converter.getStart();
		assertNotNull(header);
		assertEquals("Wrong header", "id;blob_col", header.toString().trim());

		RowData data = new RowData(info);
		data.setValue(0, new Integer(42));
		data.setValue(1, new byte[] {1,2,3,4} );

		StringBuilder line = converter.convertRowData(data, 0);
		assertEquals("42;AQIDBA==", line.toString().trim());

		converter.setBlobFormatter(BlobFormatterFactory.createAnsiFormatter());
		line = converter.convertRowData(data, 0);
		assertEquals("42;0x01020304", line.toString().trim());

		converter.setBlobFormatter(BlobFormatterFactory.createInstance(BlobLiteralType.octal));
		line = converter.convertRowData(data, 0);
		assertEquals("42;\\001\\002\\003\\004", line.toString().trim());
	}

}
