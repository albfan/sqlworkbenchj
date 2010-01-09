/*
 * DataExporterTest.java
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

import java.util.List;
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.gui.dialogs.export.TextOptions;
import workbench.util.CharacterRange;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class DataExporterTest
	extends TestCase
{

	public DataExporterTest(String testName)
	{
		super(testName);
	}

	public void testExporQueryResult()
		throws Exception
	{
		TestUtil util = new TestUtil("DataExporterTest");
		util.emptyBaseDirectory();
		try
		{
			WbConnection con = util.getConnection();

			String script =
				"CREATE TABLE person (nr integer, firstname varchar(20), lastname varchar(20));\n" +
				"INSERT INTO person (nr, firstname, lastname) VALUES (1, 'Arthur', 'Dent');\n" +
				"INSERT INTO person (nr, firstname, lastname) VALUES (2, 'Zaphod', 'Beeblebrox');\n" +
				"INSERT INTO person (nr, firstname, lastname) VALUES (3, 'Ford', 'Prefect');\n" +
				"INSERT INTO person (nr, firstname, lastname) VALUES (4, 'Tricia', 'McMillian');\n" +
				"commit;\n";

			TestUtil.executeScript(con, script);

			DataExporter exporter = new DataExporter(con);
			exporter.setOutputType(ExportType.TEXT);
			exporter.setTextOptions(getOptions());

			WbFile exportFile = new WbFile(util.getBaseDir(), "query_export.txt");
			exporter.addQueryJob("SELECT * FROM person ORDER BY nr;", exportFile);

			long rowCount = exporter.startExport();
			assertEquals(4, rowCount);
			List<String> lines = TestUtil.readLines(exportFile);
			assertEquals(5, lines.size());
			assertEquals("NR\tFIRSTNAME\tLASTNAME", lines.get(0));
			assertEquals("1\tArthur\tDent", lines.get(1));
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	private TextOptions getOptions()
	{
		return new TextOptions()
		{
			public String getTextDelimiter() { return "\t"; }
			public boolean getExportHeaders() { return true;	}
			public String getTextQuoteChar() { return "\""; }
			public boolean getQuoteAlways() { return false; }
			public CharacterRange getEscapeRange() { return CharacterRange.RANGE_NONE; }
			public String getLineEnding() { return "\n"; }
			public String getDecimalSymbol() { return "."; }

			public void setExportHeaders(boolean flag) { }
			public void setTextDelimiter(String delim) { }
			public void setTextQuoteChar(String quote) { }
			public void setQuoteAlways(boolean flag) { }
			public void setEscapeRange(CharacterRange range) { }
			public void setLineEnding(String ending) { }
			public void setDecimalSymbol(String decimal) { }
		};
	}

	public void testExportWhere()
		throws Exception
	{
		TestUtil util = new TestUtil("DataExporterTest");
		util.emptyBaseDirectory();
		try
		{
			WbConnection con = util.getConnection();

			String script =
				"CREATE TABLE person (nr integer, firstname varchar(20), lastname varchar(20));\n" +
				"INSERT INTO person (nr, firstname, lastname) VALUES (1, 'Arthur', 'Dent');\n" +
				"INSERT INTO person (nr, firstname, lastname) VALUES (2, 'Zaphod', 'Beeblebrox');\n" +
				"INSERT INTO person (nr, firstname, lastname) VALUES (3, 'Ford', 'Prefect');\n" +
				"INSERT INTO person (nr, firstname, lastname) VALUES (4, 'Tricia', 'McMillian');\n" +
				"commit;\n";

			TestUtil.executeScript(con, script);

			DataExporter exporter = new DataExporter(con);
			exporter.setOutputType(ExportType.TEXT);
			exporter.setTextOptions(getOptions());

			WbFile exportFile = new WbFile(util.getBaseDir(), "query_export.txt");
			//exporter.addQueryJob("SELECT * FROM person ORDER BY nr;", exportFile);
			TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("PERSON"));
			exporter.addTableExportJob(exportFile, tbl, "WHERE nr < 3");

			long rowCount = exporter.startExport();
			assertEquals(2, rowCount);
			List<String> lines = TestUtil.readLines(exportFile);
			assertEquals(3, lines.size());
			assertEquals("NR\tFIRSTNAME\tLASTNAME", lines.get(0));
			assertEquals("1\tArthur\tDent", lines.get(1));
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
}
