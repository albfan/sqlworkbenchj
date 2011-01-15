/*
 * CommandMapperTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.resource.Settings;
import static org.junit.Assert.*;
import org.junit.Test;
import workbench.WbTestCase;

/**
 * @author Thomas Kellerer
 */
public class CommandMapperTest
	extends WbTestCase
{

	public CommandMapperTest()
	{
		super();
		prepare();
	}

	@Test
	public void testSelectIntoPattern()
		throws Exception
	{
		String pgPattern = Settings.getInstance().getProperty("workbench.db.postgresql.selectinto.pattern", null);
		assertNotNull(pgPattern);

		Pattern pg = Pattern.compile(pgPattern, Pattern.CASE_INSENSITIVE);
		String sql = "select * from table";
		Matcher m = pg.matcher(sql);
		assertFalse(m.find());

		sql = "wbselectblob blob_column into c:/temp/pic.jpg from mytable";
		m = pg.matcher(sql);
		assertFalse(m.find());

		sql = "select col1, col2, col3 INTO new_table FROM existing_table";
		m = pg.matcher(sql);
		assertTrue(m.find());

		String informixPattern = Settings.getInstance().getProperty("workbench.db.informix_dynamic_server.selectinto.pattern", null);
		assertNotNull(informixPattern);

		Pattern ifx = Pattern.compile(informixPattern, Pattern.CASE_INSENSITIVE);
		String ifxsql = "select * from table";
		m = ifx.matcher(ifxsql);
		assertFalse(m.find());

		ifxsql = "wbselectblob blob_column into c:/temp/pic.jpg from mytable";
		m = ifx.matcher(ifxsql);
		assertFalse(m.find());

		ifxsql = "select col1, col2, col3 FROM existing_table INTO new_table";
		m = ifx.matcher(ifxsql);
		assertTrue(m.find());

	}
}
