/*
 * TextRowDataConverterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
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
import workbench.db.ColumnIdentifier;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.util.StrBuffer;

/**
 *
 * @author support@sql-workbench.net
 */
public class TextRowDataConverterTest extends junit.framework.TestCase
{
	
	public TextRowDataConverterTest(String testName)
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
		TextRowDataConverter converter = new TextRowDataConverter();
		converter.setDefaultTimestampFormat("yyyy-MM-dd HH:mm:ss");
		converter.setDefaultDateFormat("yyyy-MM-dd");
		converter.setWriteHeader(true);
		converter.setResultInfo(info);
		converter.setDelimiter(";");
		StrBuffer header = converter.getStart();
		assertNotNull(header);
		assertEquals("Wrong header", "char_col;int_col;date_col;ts_col", header.toString().trim());

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

		StrBuffer line = converter.convertRowData(data, 0);
		assertEquals("Wrong columns exporter", "data1;42;2006-10-26;2006-10-26 17:00:00", line.toString().trim());

		List<ColumnIdentifier> columns = new ArrayList<ColumnIdentifier>();
		columns.add(info.getColumn(0));
		columns.add(info.getColumn(1));
		converter.setColumnsToExport(columns);
		line = converter.convertRowData(data, 0);
		assertNotNull("Data not converted", line);
		assertEquals("Wrong columns exporter", "data1;42", line.toString().trim());


	}
}
