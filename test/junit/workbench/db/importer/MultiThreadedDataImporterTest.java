/*
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 *
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.interfaces.JobErrorHandler;
import workbench.util.MessageBuffer;
import workbench.util.SqlUtil;
import workbench.util.ValueConverter;

/**
 *
 * @author support@sql-workbench.net
 */
public class MultiThreadedDataImporterTest
	extends TestCase
{
	private int rowCount = 50;
	
	public MultiThreadedDataImporterTest(String testName)
	{
		super(testName);
	}

	public RowDataProducer getProducer()
	{
		return new RowDataProducer()
		{
			private boolean cancel = false;
			private RowDataReceiver receiver;

			public void cancel()
			{
				System.out.println("cancelling producer from " + Thread.currentThread().toString());
				cancel = true;
			}

			public String getLastRecord()
			{
				return null;
			}

			public MessageBuffer getMessages()
			{
				return null;
			}

			public boolean hasErrors()
			{
				return false;
			}

			public boolean hasWarnings()
			{
				return false;
			}

			public boolean isCancelled()
			{
				return cancel;
			}

			public void setAbortOnError(boolean flag)
			{

			}

			public void setErrorHandler(JobErrorHandler handler)
			{

			}

			public void setReceiver(RowDataReceiver target)
			{
				receiver = target;
			}

			public void setValueConverter(ValueConverter converter)
			{

			}

			public void start()
				throws Exception
			{
				try
				{
					Random r = new Random();
					for (int i=0; i < rowCount; i++)
					{
						Object[] row = new Object[2];
						row[0] = new Integer(i);
						row[1] = Integer.toString(i);

						// By sleeping the thread a little
						// I'm trying to provoke a situation where the
						// import queue is empty and the individual
						// worker threads have to wait for a new row
//						Thread.sleep(r.nextInt(50));
						if (cancel) break;
						receiver.processRow(row);
					}
					if (cancel) 
					{
						receiver.importCancelled();
					}
					else
					{
						receiver.importFinished();
					}
				}
				catch (Exception e)
				{
					System.out.println("exception in producer: " + e.getMessage());
					receiver.importCancelled();
				}
			}

			public void stop()
			{
				cancel = true;
			}
		};
	}

	public void testAbort()
	{
		ResultSet rs = null;
		Statement stmt = null;
		try
		{
			TestUtil util = new TestUtil("mt_textImport");

			File importFile  = new File(util.getBaseDir(), "text_import.txt");
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(importFile), "UTF-8"));
			out.println("nr\tfirstname\tlastname");
			out.println("1\tArthur\tDent");
			out.println("2\tFord\tPrefect");
			out.println("3\tZaphod\tBeeblebrox");
			out.close();

			WbConnection con = util.getConnection(new File(util.getBaseDir(), "import_data"), "mtTest", true);
			TestUtil.executeScript(con,
				"CREATE TABLE junit_test_pk " +
				"(nr integer primary key, firstname varchar(100), lastname varchar(100));\n" +
				"insert into junit_test_pk (nr, firstname, lastname) values (1, 'Mary', 'Moviestar');\n" +
				"insert into junit_test_pk (nr, firstname, lastname) values (2, 'Harry', 'Handsome');\n" +
				"commit;\n"
				);

			TextFileParser parser = new TextFileParser(importFile);
			parser.setContainsHeader(true);
			parser.setConnection(con);
			parser.setTableName("junit_test_pk");
			parser.setDelimiter("\t");
			parser.setAbortOnError(false);

			MultiThreadedDataImporter importer = new MultiThreadedDataImporter(1);

			importer.setConnection(con);
			importer.setContinueOnError(false);
			importer.setBatchSize(1);
			importer.setProducer(parser);
			importer.startImport();

			String msg = importer.getMessages().toString();
			System.out.println("*********\n" + msg + "\n************");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	public void _testImport()
		throws Exception
	{
		try
		{
			TestUtil util = new TestUtil(this.getName());
			util.emptyBaseDirectory();
			File db = new File(util.getBaseDir(), "importTest");

			WbConnection con = util.getConnection(db, "MultiThread", true);
			TestUtil.executeScript(con,
				"CREATE TABLE import_test (id integer, some_value varchar(20), check (id <> 19) );" +
				"commit;");

//			String cmdline = "-url=jdbc:postgresql://localhost/wbtest -driver=org.postgresql.Driver -username=thomas -password=welcome";
//			ArgumentParser parser = new ArgumentParser();
//			parser.addArgument("url");
//			parser.addArgument("username");
//			parser.addArgument("password");
//			parser.addArgument("driver");
//			parser.parse(cmdline);
//			ConnectionProfile prof = BatchRunner.createCmdLineProfile(parser);
//			WbConnection con = ConnectionMgr.getInstance().getConnection(prof, "multiTest");
//			TestUtil.executeScript(con,
//				"truncate TABLE import_test;" +
//				"commit;");

			rowCount = 100;

			MultiThreadedDataImporter importer = new MultiThreadedDataImporter(2);

			TableIdentifier tbl = new TableIdentifier("import_test");
			TableDefinition importTable = con.getMetadata().getTableDefinition(tbl);

			ColumnIdentifier[] cols = new ColumnIdentifier[importTable.getColumns().size()];
			for (int i=0; i < cols.length; i++)
			{
				cols[i] = importTable.getColumns().get(i);
			}
			importer.setConnection(con);
			importer.setContinueOnError(false);
			importer.setBatchSize(50);
			importer.setTargetTable(importTable.getTable(), cols);
			importer.setProducer(getProducer());

			long start = System.currentTimeMillis();
			importer.startImport();
			long end = System.currentTimeMillis();
			System.out.println("time: " + (end - start));

			String msg = importer.getMessages().toString();
			System.out.println("***********************\n" + msg + "\n***************************");
//			assertTrue(importer.isSuccess());

//			Statement stmt = con.createStatement();
//			ResultSet rs = stmt.executeQuery("select count(*) from import_test");
//			int count = 0;
//			if (rs.next()) count = rs.getInt(1);
//			SqlUtil.closeAll(rs, stmt);
//			assertEquals(rowCount, count);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
}
