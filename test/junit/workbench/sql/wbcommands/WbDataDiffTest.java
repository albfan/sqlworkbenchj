/*
 * WbDataDiffTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.Reader;
import java.sql.SQLException;
import java.sql.Statement;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.WbConnection;
import workbench.sql.ScriptParser;
import workbench.sql.StatementRunner;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.WbFile;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbDataDiffTest
	extends WbTestCase
{
	private WbConnection source;
	private WbConnection target;
	private TestUtil util;

	public WbDataDiffTest(String testName)
	{
		super(testName);
		util = new TestUtil("dataDiffTest");
	}

	public void testIsConnectionRequired()
	{
		WbDataDiff diff = new WbDataDiff();
		assertFalse(diff.isConnectionRequired());
	}

	private void setupConnections()
		throws Exception
	{
		this.source = util.getConnection("dataDiffSource");
		this.target = util.getConnection("dataDiffTarget");
	}

	public void testExecute()
		throws Exception
	{
		String script = "drop all objects;\n" +
			"create table person (person_id integer primary key, firstname varchar(100), lastname varchar(100));\n" +
			"create table address (address_id integer primary key, street varchar(50), city varchar(100), phone varchar(50), email varchar(50));\n" +
			"create table person_address (person_id integer, address_id integer, primary key (person_id, address_id)); \n" +
			"alter table person_address add constraint fk_adr foreign key (address_id) references address (address_id);\n" +
			"alter table person_address add constraint fk_per foreign key (person_id) references person (person_id);\n";

		setupConnections();

		Statement srcStmt;
		Statement targetStmt;

		try
		{
			util.prepareEnvironment();
			TestUtil.executeScript(source, script);
			TestUtil.executeScript(target, script);

			srcStmt = source.createStatement();
			insertData(srcStmt);
			source.commit();

			targetStmt = target.createStatement();
			insertData(targetStmt);

			// Delete rows so that the diff needs to create INSERT statements

			// as person_id and address are always equal in the test data I don't need to specify both
			targetStmt.executeUpdate("DELETE FROM person_address WHERE person_id in (10,14)");
			targetStmt.executeUpdate("DELETE FROM address WHERE address_id in (10, 14)");
			targetStmt.executeUpdate("DELETE FROM person WHERE person_id in (10, 14)");

			// Change some rows so that the diff needs to create UPDATE statements
			targetStmt.executeUpdate("UPDATE person SET firstname = 'Wrong' WHERE person_id in (17,2)");
			targetStmt.executeUpdate("UPDATE address SET city = 'Wrong' WHERE address_id in (17,2)");


			// Insert some rows so that the diff needs to create DELETE statements
			targetStmt.executeUpdate("insert into person (person_id, firstname, lastname) values " +
				"(300, 'doomed', 'doomed')");
			targetStmt.executeUpdate("insert into person (person_id, firstname, lastname) values " +
				"(301, 'doomed', 'doomed')");

			targetStmt.executeUpdate("insert into address (address_id, street, city, phone, email) values " +
				" (300, 'tobedelete', 'none', 'none', 'none')");

			targetStmt.executeUpdate("insert into address (address_id, street, city, phone, email) values " +
				" (301, 'tobedelete', 'none', 'none', 'none')");

			targetStmt.executeUpdate("insert into person_address (address_id, person_id) values (300,300)");
			targetStmt.executeUpdate("insert into person_address (address_id, person_id) values (301,301)");
			target.commit();

			util.emptyBaseDirectory();

			StatementRunner runner = new StatementRunner();
			runner.setBaseDir(util.getBaseDir());
			String sql = "WbDataDiff -referenceProfile=dataDiffSource -targetProfile=dataDiffTarget -includeDelete=true -checkDependencies=true -file=sync.sql -encoding=UTF8";
			runner.runStatement(sql);

			WbFile main = new WbFile(util.getBaseDir(), "sync.sql");
			assertTrue(main.exists());

			Reader r = EncodingUtil.createReader(main, "UTF-8");
			String sync = FileUtil.readCharacters(r);
			ScriptParser parser = new ScriptParser();
			parser.setScript(sync);
			assertEquals(10, parser.getSize());

			String[] expectedFiles = new String[]
			{
				"address_$delete.sql",
				"address_$insert.sql",
				"address_$update.sql",
				"person_$delete.sql",
				"person_$insert.sql",
				"person_$update.sql",
				"person_address_$delete.sql",
				"person_address_$insert.sql"
			};

			for (String fname : expectedFiles)
			{
				WbFile f = new WbFile(util.getBaseDir(), fname);
				assertTrue(f.exists());
				if (!f.delete())
				{
					fail("Could not delete " + f.getFullPath());
				}
			}

			if (!main.delete())
			{
				fail("Could not delete " + main.getFullPath());
			}


			TestUtil.executeScript(source, "update person set lastname = '<name>' where person_id = 10;commit;");
			
			sql = "WbDataDiff -type=xml -referenceProfile=dataDiffSource -targetProfile=dataDiffTarget -includeDelete=true -checkDependencies=true -file=sync.txt -encoding=UTF8";
			runner.runStatement(sql);

			main = new WbFile(util.getBaseDir(), "sync.txt");
			assertTrue(main.exists());

			expectedFiles = new String[]
			{
				"address_$delete.xml",
				"address_$insert.xml",
				"address_$update.xml",
				"person_$delete.xml",
				"person_$insert.xml",
				"person_$update.xml",
				"person_address_$delete.xml",
				"person_address_$insert.xml"
			};

			for (String fname : expectedFiles)
			{
				WbFile f = new WbFile(util.getBaseDir(), fname);
				assertTrue(f.exists());
//				if (!f.delete())
//				{
//					fail("Could not delete " + f.getFullPath());
//				}
			}

//			if (!main.delete())
//			{
//				fail("Could not delete " + main.getFullPath());
//			}

		}
		finally
		{
			source.disconnect();
			target.disconnect();
		}
	}

	private void insertData(Statement stmt)
		throws SQLException
	{
		int rowCount = 20;
		for (int i=0; i < rowCount; i++)
		{
			stmt.executeUpdate("insert into person (person_id, firstname, lastname) values (" + i + ", 'first" + i + "', 'last" + i + "')");
		}
		for (int i=0; i < rowCount; i++)
		{
			stmt.executeUpdate("insert into address (address_id, street, city, phone, email) values (" + i + ", 'street" + i + "', 'city" + i + "', 'phone" + i + "', 'email"+i + "')");
		}
		for (int i=0; i < rowCount; i++)
		{
			stmt.executeUpdate("insert into person_address (address_id, person_id) values (" +i + ", " + i + ")");
		}
	}
}
