/*
 * TextRowDataConverterTest.java
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.junit.Test;
import workbench.WbTestCase;
import workbench.db.ColumnIdentifier;
import workbench.storage.BlobFormatterFactory;
import workbench.storage.BlobLiteralType;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.util.StrBuffer;
import static org.junit.Assert.*;

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
		StrBuffer header = converter.getStart();
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

		StrBuffer line = converter.convertRowData(data, 0);
		assertEquals("Wrong columns exporter", "data1;42;2006-10-26;2006-10-26 17:00:00;23:42:24", line.toString().trim());

		List<ColumnIdentifier> columns = new ArrayList<ColumnIdentifier>();
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
		StrBuffer header = converter.getStart();
		assertNotNull(header);
		assertEquals("Wrong header", "id;blob_col", header.toString().trim());

		RowData data = new RowData(info);
		data.setValue(0, new Integer(42));
		data.setValue(1, new byte[] {1,2,3,4} );

		StrBuffer line = converter.convertRowData(data, 0);
		assertEquals("42;AQIDBA==", line.toString().trim());

		converter.setBlobFormatter(BlobFormatterFactory.createAnsiFormatter());
		line = converter.convertRowData(data, 0);
		assertEquals("42;0x01020304", line.toString().trim());

		converter.setBlobFormatter(BlobFormatterFactory.createInstance(BlobLiteralType.octal));
		line = converter.convertRowData(data, 0);
		assertEquals("42;\\001\\002\\003\\004", line.toString().trim());
	}

}
