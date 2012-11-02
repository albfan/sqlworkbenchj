/*
 * UpdateAnalyzerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.completion;

import java.util.List;

import workbench.WbTestCase;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class UpdateAnalyzerTest
	extends WbTestCase
{
	public UpdateAnalyzerTest()
	{
		super("UpdateAnalyzerTest");
	}

	@Test
	public void testGetColumns()
	{
		String sql = "update foo set col_1 = x, col_3 = y, col_5 = 'foobar'";

		int pos = sql.indexOf("x,") - 2;
		UpdateAnalyzer check = new UpdateAnalyzer(null, sql, pos);
		List<UpdateAnalyzer.ColumnInfo> cols = check.getColumns();
		System.out.println(cols);
		assertEquals(3, cols.size());
		UpdateAnalyzer.ColumnInfo col = cols.get(1);
		System.out.println(sql.substring(col.valueStartPos, col.valueEndPos));
	}
}
