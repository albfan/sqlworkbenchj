/*
 * OracleSchemaDiffTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.WbConnection;
import workbench.db.diff.SchemaDiff;
import workbench.util.FileUtil;
import static org.junit.Assert.*;


/**
 *
 * @author Thomas Kellerer
 */
public class PostgresSchemaDiffTest
	extends WbTestCase
{

	private static final String REFERENCE_SCHEMA = "refschema";
	private static final String TARGET_SCHEMA = "targetschema";

	public PostgresSchemaDiffTest()
	{
		super("PostgresSchemaDiffTest");
	}

	@Before
	public void beforeTest()
		throws Exception
	{
		PostgresTestUtil.initTestCase(REFERENCE_SCHEMA);

	}

	@After
	public void afterTest()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase(REFERENCE_SCHEMA);
		PostgresTestUtil.cleanUpTestCase(TARGET_SCHEMA);
	}

	@Test
	public void testCheckConstraints()
		throws Exception
	{
		WbConnection conn = PostgresTestUtil.getPostgresConnection();
		if (conn == null)
		{
			return;
		}

		String schema =
			"CREATE SCHEMA " + TARGET_SCHEMA + ";\n" +
			"COMMIT;\n";

		String sql = "CREATE TABLE XXXX.foo \n" +
			"( \n" +
			"  id integer not null, \n" +
			"  weekday_start integer not null, \n" +
			"  weekday integer, \n" +
			"  is_active integer not null, \n" +
			"  CONSTRAINT CHK_WEEKDAY CHECK (WEEKDAY IS NULL OR (weekday IN (1,2,3,4,5,6,7))), \n "+
			"  CONSTRAINT CHK_WEEKDAY_START CHECK (weekday_start IN (1,2,3,4,5,6,7)), \n" +
      "  CONSTRAINT CKC_IS_ACTIVE CHECK (IS_ACTIVE IN (0,1)), \n" +
			"  CONSTRAINT pk_foo PRIMARY KEY (id) \n" +
			");\n" +
			"commit;\n";

		String script = schema + sql.replace("XXXX", REFERENCE_SCHEMA) +
			sql.replace("XXXX", TARGET_SCHEMA);

		TestUtil.executeScript(conn, script);

		SchemaDiff diff = new SchemaDiff(conn, conn);
		diff.setIncludeViews(true);
		diff.setSchemas(REFERENCE_SCHEMA, TARGET_SCHEMA);

		diff.setIncludeViews(false);
		diff.setCompareConstraintsByName(true);
		diff.setIncludeTableConstraints(true);
		TestUtil util = getTestUtil();
		File outfile = new File(util.getBaseDir(), "pg_check_diff.xml");
		Writer out = new FileWriter(outfile);
		diff.writeXml(out);
		FileUtil.closeQuietely(out);
		assertTrue(outfile.exists());
		assertTrue(outfile.length() > 0);
		Reader in = new FileReader(outfile);
		String xml = FileUtil.readCharacters(in);
		assertNotNull(xml);

		String value = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table)");
		assertEquals("0", value);

		value = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table[@name='foo']/table-constraints)");
		assertEquals("0", value);
	}

}
