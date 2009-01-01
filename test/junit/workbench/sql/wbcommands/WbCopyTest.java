/*
 * WbCopyTest.java
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

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.util.SqlUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbCopyTest
	extends TestCase
{

	public WbCopyTest(String testName)
	{
		super(testName);
	}

	public void testIsConnectionRequired()
	{
		WbCopy copy = new WbCopy();
		assertFalse(copy.isConnectionRequired());
	}
	
	public void testCopyWithSyncDelete() throws Exception
	{
		try
		{
			TestUtil util = new TestUtil("WbCopyTest_testExecute");
			util.prepareEnvironment();

			StatementRunner runner = util.createConnectedStatementRunner();
			WbConnection con = runner.getConnection();

			Statement stmt = con.createStatement();

			stmt.executeUpdate("create table source_data (nr integer not null primary key, lastname varchar(50), firstname varchar(50))");
			stmt.executeUpdate("create table target_data (nr integer not null primary key, lastname varchar(50), firstname varchar(50))");

			for (int i=0; i < 50; i++)
			{
				stmt.executeUpdate("insert into source_data (nr, lastname, firstname) values (" +  i + ",'Lastname" + i + "', 'Arthur" + i + ",')");
			}

			for (int i=0; i < 37; i++)
			{
				stmt.executeUpdate("insert into target_data (nr, lastname, firstname) values (" +  (i + 1000) + ",'Lastname" + i + "', 'Arthur" + i + ",')");
			}

			con.commit();

			String sql = "wbcopy -sourceTable=source_data " +
				           "       -targettable=target_data " +
									 "       -createTarget=false " +
									 "       -syncDelete=true " +
									 "       -batchSize=10";

			runner.runStatement(sql);
			StatementRunnerResult result = runner.getResult();
			assertEquals(result.getMessageBuffer().toString(), true, result.isSuccess());

			ResultSet rs = stmt.executeQuery("select count(*) from target_data");
			int count = -1;
			if (rs.next())
			{
				count = rs.getInt(1);
			}
			SqlUtil.closeResult(rs);
			assertEquals("Wrong rowcount", 50, count);

			rs = stmt.executeQuery("select count(*) from target_data where nr >= 1000");
			count = -1;
			if (rs.next())
			{
				count = rs.getInt(1);
			}
			SqlUtil.closeResult(rs);
			assertEquals("Rows not deleted", 0, count);
			ConnectionMgr.getInstance().removeProfile(con.getProfile());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	public void testCopy() throws Exception
	{
		try
		{
			TestUtil util = new TestUtil("WbCopyTest_testExecute");
			util.prepareEnvironment();

			StatementRunner runner = util.createConnectedStatementRunner();
			WbConnection con = runner.getConnection();

			Statement stmt = con.createStatement();

			stmt.executeUpdate("create table source_data (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");
			stmt.executeUpdate("create table target_data (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");

			stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (1,'Dent', 'Arthur', '01')");
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (2,'Beeblebrox', 'Zaphod','0202')");
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (3,'Moviestar', 'Mary', '030303')");
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (4,'Perfect', 'Ford', '04040404')");

			con.commit();

			String sql = "--copy source_data and create target\n" +
				"wbcopy -sourceTable=source_data " +
				"-targettable=target_data -createTarget=false";

			runner.runStatement(sql);
			StatementRunnerResult result = runner.getResult();
			assertEquals(result.getMessageBuffer().toString(), true, result.isSuccess());

			ResultSet rs = stmt.executeQuery("select count(*) from target_data");
			if (rs.next())
			{
				int count = rs.getInt(1);
				assertEquals("Incorrect number of rows copied", 4, count);
			}
			rs.close();
			rs = stmt.executeQuery("select lastname from target_data where nr = 3");
			if (rs.next())
			{
				String name = rs.getString(1);
				assertEquals("Incorrect value copied", "Moviestar", name);
			}
			else
			{
				fail("Record with nr = 3 not copied");
			}
			rs.close();
			rs = stmt.executeQuery("select nr, binary_data from target_data");
			while (rs.next())
			{
				int id = rs.getInt(1);
				Object blob = rs.getObject(2);
				assertNotNull("No blob data imported", blob);
				if (blob instanceof byte[])
				{
					byte[] retrievedData = (byte[])blob;
					assertEquals("Wrong blob size imported", id, retrievedData.length);
					assertEquals("Wrong content of blob data", id, retrievedData[0]);
				}
			}

			stmt.executeUpdate("update source_data set lastname = 'Prefect' where nr = 4");
			con.commit();

			sql = "--update target table\nwbcopy -sourceTable=source_data -targettable=target_data -mode=update";
			runner.runStatement(sql);
			result = runner.getResult();
			assertEquals("Copy not successful", true, result.isSuccess());

			rs = stmt.executeQuery("select lastname from target_data where nr = 4");
			if (rs.next())
			{
				String name = rs.getString(1);
				assertEquals("Incorrect value copied", "Prefect", name);
			}
			else
			{
				fail("Record with nr = 4 not copied");
			}
			ConnectionMgr.getInstance().removeProfile(con.getProfile());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	public void testCreateWithMap() throws Exception
	{
		try
		{
			TestUtil util = new TestUtil("CreateOrderedTest");
			util.prepareEnvironment();

			StatementRunner runner = util.createConnectedStatementRunner();
			WbConnection con = runner.getConnection();

			Statement stmt = con.createStatement();

			stmt.executeUpdate("create table source_data (nr integer not null primary key, lastname varchar(50), firstname varchar(50))");

			stmt.executeUpdate("insert into source_data (nr, lastname, firstname) values (1,'Dent', 'Arthur')");
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname) values (2,'Beeblebrox', 'Zaphod')");
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname) values (3,'Moviestar', 'Mary')");
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname) values (4,'Perfect', 'Ford')");

			con.commit();

			String sql = "wbcopy -sourceTable=source_data " +
									"-targetTable=target_data " +
									"-columns=lastname/nachname, firstname/vorname, nr/id "+
									"-createTarget=true";

			runner.runStatement(sql);
			StatementRunnerResult result = runner.getResult();
			assertEquals(result.getMessageBuffer().toString(), true, result.isSuccess());

			ResultSet rs = stmt.executeQuery("select count(*) from target_data");
			if (rs.next())
			{
				int count = rs.getInt(1);
				assertEquals("Incorrect number of rows copied", 4, count);
			}
			rs.close();

			// Make sure the order in the column mapping is preserved when creating the table
			List<ColumnIdentifier> columns = con.getMetadata().getTableColumns(new TableIdentifier("TARGET_DATA"));
			for (ColumnIdentifier col : columns)
			{
				if (col.getColumnName().equalsIgnoreCase("NACHNAME"))
				{
					assertEquals(1, col.getPosition());
				}
				else if (col.getColumnName().equalsIgnoreCase("VORNAME"))
				{
					assertEquals(2, col.getPosition());
				}
				else if (col.getColumnName().equalsIgnoreCase("ID"))
				{
					assertEquals(3, col.getPosition());
				}
				else
				{
					fail("Wrong column " + col.getColumnName() + " created");
				}
			}
			ConnectionMgr.getInstance().removeProfile(con.getProfile());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	public void testWithColumnMap()
	{
		try
		{
			TestUtil util = new TestUtil("CopyWithMapTest");
			util.prepareEnvironment();

			WbConnection con = util.getConnection("mappedCopyTest");

			WbCopy copyCmd = new WbCopy();
			copyCmd.setConnection(con);

			Statement stmt = con.createStatement();

			stmt.executeUpdate("create table source_data (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");

			stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (1,'Dent', 'Arthur', '01')");
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (2,'Beeblebrox', 'Zaphod','0202')");
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (3,'Moviestar', 'Mary', '030303')");
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (4,'Perfect', 'Ford', '04040404')");

			stmt.executeUpdate("create table target_data (tnr integer not null primary key, tlastname varchar(50), tfirstname varchar(50), tbinary_data blob)");
			stmt.executeUpdate("insert into target_data (tnr, tlastname, tfirstname) values (42,'Gaga', 'Radio')");

			con.commit();

			String sql = "wbcopy -sourceTable=source_data " +
				"-targetTable=target_data " +
				"-deleteTarget=true " +
				"-columns=lastname/tlastname, firstname/tfirstname, nr/tnr";

			StatementRunnerResult result = copyCmd.execute(sql);
			assertEquals("Copy not successful", true, result.isSuccess());

			ResultSet rs = stmt.executeQuery("select count(*) from target_data where tbinary_data is null");
			if (rs.next())
			{
				int count = rs.getInt(1);
				assertEquals("Incorrect number of rows copied", 4, count);
			}
			SqlUtil.closeResult(rs);

			rs = stmt.executeQuery("select tfirstname, tlastname from target_data where tnr = 3");
			if (rs.next())
			{
				String s = rs.getString(1);
				assertEquals("Incorrect firstname", "Mary", s);
				s = rs.getString(2);
				assertEquals("Incorrect firstname", "Moviestar", s);
			}
			else
			{
				fail("Nothing copied");
			}
			SqlUtil.closeResult(rs);
			ConnectionMgr.getInstance().removeProfile(con.getProfile());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	public void testQueryCopy()
	{
		try
		{
			TestUtil util = new TestUtil("WbCopyTest_testExecute");
			util.prepareEnvironment();

			WbConnection con = util.getConnection("queryCopyTest");

			WbCopy copyCmd = new WbCopy();
			copyCmd.setConnection(con);

			Statement stmt = con.createStatement();

			stmt.executeUpdate("create table source_data (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");

			stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (1,'Dent', 'Arthur', '01')");
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (2,'Beeblebrox', 'Zaphod','0202')");
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (3,'Moviestar', 'Mary', '030303')");
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (4,'Perfect', 'Ford', '04040404')");

			stmt.executeUpdate("create table target_data (tnr integer not null primary key, tlastname varchar(50), tfirstname varchar(50), tbinary_data blob)");

			con.commit();

			String sql = "wbcopy -sourceQuery='select firstname, nr, lastname from source_data where nr < 3' " +
				"-targetTable=target_data " +
				"-columns=tfirstname, tnr, tlastname";

			StatementRunnerResult result = copyCmd.execute(sql);
			assertEquals("Copy not successful", true, result.isSuccess());

			ResultSet rs = stmt.executeQuery("select count(*) from target_data where tbinary_data is null");
			if (rs.next())
			{
				int count = rs.getInt(1);
				assertEquals("Incorrect number of rows copied", 2, count);
			}
			SqlUtil.closeResult(rs);

			rs = stmt.executeQuery("select tfirstname, tlastname from target_data where tnr = 1");
			if (rs.next())
			{
				String s = rs.getString(1);
				assertEquals("Incorrect firstname", "Arthur", s);
				s = rs.getString(2);
				assertEquals("Incorrect firstname", "Dent", s);
			}
			else
			{
				fail("Nothing copied");
			}
			SqlUtil.closeResult(rs);
			ConnectionMgr.getInstance().removeProfile(con.getProfile());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	public void testQueryCopyNoPK()
	{
		try
		{
			TestUtil util = new TestUtil("WbCopyTest_testExecute");
			util.prepareEnvironment();

			WbConnection con = util.getConnection("queryCopyTest");

			WbCopy copyCmd = new WbCopy();
			copyCmd.setConnection(con);

			Statement stmt = con.createStatement();

			stmt.executeUpdate("create table source_data (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");

			stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (1,'Dent', 'Arthur', '01')");
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (2,'Beeblebrox', 'Zaphod','0202')");
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (3,'Moviestar', 'Mary', '030303')");
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (4,'Prefect', 'Ford', '04040404')");

			stmt.executeUpdate("create table target_data (tnr integer, tlastname varchar(50), tfirstname varchar(50), tbinary_data blob)");
			stmt.executeUpdate("insert into target_data (tnr, tlastname, tfirstname) values (1, 'Dend', 'Artur')");
			stmt.executeUpdate("insert into target_data (tnr, tlastname, tfirstname) values (2, 'Biblebrox', 'Zaphod')");

			con.commit();

			String sql = "wbcopy -sourceQuery='select firstname, nr, lastname from source_data' " +
				"-targetTable=target_data " +
				"-mode=update,insert " +
				"-keyColumns=tnr " +
				"-columns=tfirstname, tnr, tlastname";

			StatementRunnerResult result = copyCmd.execute(sql);
			if (!result.isSuccess())
			{
				String msg = result.getMessageBuffer().toString();
				System.out.println("***********");
				System.out.println(msg);
				System.out.println("***********");
			}
			
			assertEquals("Copy not successful", true, result.isSuccess());

			ResultSet rs = stmt.executeQuery("select tnr, tfirstname, tlastname from target_data");
			int count = 0;
			while (rs.next())
			{
				count ++;
				int id = rs.getInt(1);
				String fname = rs.getString(2);
				String lname = rs.getString(3);
				
				if (id == 1)
				{
					assertEquals("Incorrect firstname", "Arthur", fname);
					assertEquals("Incorrect firstname", "Dent", lname);
				}
				else if (id == 2)
				{
					assertEquals("Incorrect firstname", "Zaphod", fname);
					assertEquals("Incorrect firstname", "Beeblebrox", lname);
				}
				else if (id == 3)
				{
					assertEquals("Incorrect firstname", "Mary", fname);
					assertEquals("Incorrect firstname", "Moviestar", lname);
				}
				else if (id == 4)
				{
					assertEquals("Incorrect firstname", "Ford", fname);
					assertEquals("Incorrect firstname", "Prefect", lname);
				}
			}
			assertEquals(4, count);
			SqlUtil.closeResult(rs);
			ConnectionMgr.getInstance().removeProfile(con.getProfile());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
	
	public void testCopySchema()
	{
		try
		{
			TestUtil util = new TestUtil("WbCopyTest_testExecute");
			util.prepareEnvironment();

			WbConnection source = util.getConnection("schemaCopySource");
			WbConnection target = util.getConnection("schemaCopyTarget");

			WbCopy copyCmd = new WbCopy();
			copyCmd.setConnection(source);

			Statement stmt = source.createStatement();
			stmt.executeUpdate("CREATE SCHEMA copy_src");
			stmt.executeUpdate("SET SCHEMA copy_src");

			stmt.executeUpdate("create table person (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");
			stmt.executeUpdate("create table address (person_id integer not null primary key, address_details varchar(100))");
			stmt.executeUpdate("create table some_data (id integer, some_details varchar(100))");
			stmt.executeUpdate("alter table address add foreign key (person_id) references person(nr)");

			stmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (1,'Dent', 'Arthur', '01')");
			stmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (2,'Beeblebrox', 'Zaphod','0202')");
			stmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (3,'Moviestar', 'Mary', '030303')");
			stmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (4,'Perfect', 'Ford', '04040404')");

			stmt.executeUpdate("insert into address (person_id, address_details) values (1, 'Arlington')");
			stmt.executeUpdate("insert into address (person_id, address_details) values (2, 'Heart of Gold')");
			stmt.executeUpdate("insert into address (person_id, address_details) values (3, 'Sleepy by Lane')");
			stmt.executeUpdate("insert into address (person_id, address_details) values (4, 'Betelgeuse')");

			source.commit();

			Statement tstmt = target.createStatement();
			tstmt.executeUpdate("CREATE SCHEMA copy_target");
			tstmt.executeUpdate("SET SCHEMA copy_target");
			tstmt.executeUpdate("create table person (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");
			tstmt.executeUpdate("create table address (person_id integer not null primary key, address_details varchar(100))");
			tstmt.executeUpdate("alter table address add foreign key (person_id) references person(nr)");

			tstmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (1000, 'Tend', 'Ruhtra', null)");
			tstmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (1001, 'Tcefrep', 'Drof', null)");
			tstmt.executeUpdate("insert into address (person_id, address_details) values (1000, 'Notgnilra')");
			tstmt.executeUpdate("insert into address (person_id, address_details) values (1001, 'Esuegleteb')");
			target.commit();

			String sql = "WbCopy " +
				"-sourceTable=some_data,address,person " +
				"-mode=insert,update " +
				"-checkDependencies=true " +
				"-sourceProfile='schemaCopySource' " +
				"-targetProfile='schemaCopyTarget' " +
				"-syncDelete=true";

			StatementRunnerResult result = copyCmd.execute(sql);
			String msg = result.getMessageBuffer().toString();
			assertEquals(msg, true, result.isSuccess());

			ResultSet rs = tstmt.executeQuery("select nr, lastname, firstname from person");
			while (rs.next())
			{
				int nr = rs.getInt(1);
				String ln = rs.getString(2);
				String fn = rs.getString(3);
				if (nr == 1)
				{
					assertEquals("Incorrect data copied", "Dent", ln);
					assertEquals("Incorrect data copied", "Arthur", fn);
				}
				else if (nr == 2)
				{
					assertEquals("Incorrect data copied", "Beeblebrox", ln);
					assertEquals("Incorrect data copied", "Zaphod", fn);
				}
			}
			SqlUtil.closeResult(rs);

			rs = tstmt.executeQuery("select count(*) from address");
			if (rs.next())
			{
				assertEquals("Wrong number of rows copied to address table", 4, rs.getInt(1));
			}
			SqlUtil.closeResult(rs);


			// Now test deleting the target first
			sql = "WbCopy " +
				"-sourceTable=* " +
				"-mode=insert " +
				"-deleteTarget=true " +
				"-checkDependencies=true " +
				"-sourceProfile='schemaCopySource' " +
				"-targetProfile='schemaCopyTarget' ";

			result = copyCmd.execute(sql);
			msg = result.getMessageBuffer().toString();
			assertEquals(msg, true, result.isSuccess());
			
			rs = tstmt.executeQuery("select nr, lastname, firstname from person");
			while (rs.next())
			{
				int nr = rs.getInt(1);
				String ln = rs.getString(2);
				String fn = rs.getString(3);
				if (nr == 1)
				{
					assertEquals("Incorrect data copied", "Dent", ln);
					assertEquals("Incorrect data copied", "Arthur", fn);
				}
				else if (nr == 2)
				{
					assertEquals("Incorrect data copied", "Beeblebrox", ln);
					assertEquals("Incorrect data copied", "Zaphod", fn);
				}
			}
			SqlUtil.closeResult(rs);


			ConnectionMgr.getInstance().removeProfile(source.getProfile());
			ConnectionMgr.getInstance().removeProfile(target.getProfile());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
		int dummy = 5;
	}

	
	public void testCreateTarget()
	{
		try
		{
			TestUtil util = new TestUtil("WbCopyCreateTest");
			util.prepareEnvironment();

			WbConnection source = util.getConnection("copyCreateTestSource");
			WbConnection target = util.getHSQLConnection("copyCreateTestTarget");

			Statement tstmt = source.createStatement();

			tstmt.executeUpdate("create table person (nr integer not null primary key, \"Lastname\" varchar(50), firstname varchar(50))");
			tstmt.executeUpdate("insert into person (nr, \"Lastname\", firstname) values (1,'Dent', 'Arthur')");
			tstmt.executeUpdate("insert into person (nr, \"Lastname\", firstname) values (2,'Beeblebrox', 'Zaphod')");
			tstmt.executeUpdate("insert into person (nr, \"Lastname\", firstname) values (3,'Moviestar', 'Mary')");
			tstmt.executeUpdate("insert into person (nr, \"Lastname\", firstname) values (4,'Perfect', 'Ford')");
			source.commit();

			// First test a copy with a fully specified column mapping
			String sql = "wbcopy -createTarget=true " +
				"-sourceTable=person " +
				"-targetTable=participants " +
				"-columns='nr/person_id, firstname/firstname, \"Lastname\"/\"Lastname\"' " +
				"-sourceProfile='copyCreateTestSource' " +
				"-targetProfile='copyCreateTestTarget' ";

			WbCopy copyCmd = new WbCopy();
			StatementRunnerResult result = copyCmd.execute(sql);
			assertEquals(result.getMessageBuffer().toString(), true, result.isSuccess());

			Statement ttstmt = target.createStatement();
			ResultSet rs = ttstmt.executeQuery("select person_id, \"Lastname\", firstname from participants");
			while (rs.next())
			{
				int nr = rs.getInt(1);
				String ln = rs.getString(2);
				String fn = rs.getString(3);
				if (nr == 1)
				{
					assertEquals("Incorrect data copied", "Dent", ln);
					assertEquals("Incorrect data copied", "Arthur", fn);
				}
				else if (nr == 2)
				{
					assertEquals("Incorrect data copied", "Beeblebrox", ln);
					assertEquals("Incorrect data copied", "Zaphod", fn);
				}
			}
			SqlUtil.closeResult(rs);


			// Now test the table creation without columns
			sql = "wbcopy -createTarget=true " +
				"-dropTarget=true " +
				"-sourceTable=person " +
				"-targetTable=participants " +
				"-sourceProfile='copyCreateTestSource' " +
				"-targetProfile='copyCreateTestTarget' ";

			result = copyCmd.execute(sql);
			assertEquals(result.getMessageBuffer().toString(), true, result.isSuccess());

			rs = ttstmt.executeQuery("select nr, \"Lastname\", firstname from participants");
			while (rs.next())
			{
				int nr = rs.getInt(1);
				String ln = rs.getString(2);
				String fn = rs.getString(3);
				if (nr == 1)
				{
					assertEquals("Incorrect data copied", "Dent", ln);
					assertEquals("Incorrect data copied", "Arthur", fn);
				}
				else if (nr == 2)
				{
					assertEquals("Incorrect data copied", "Beeblebrox", ln);
					assertEquals("Incorrect data copied", "Zaphod", fn);
				}
			}
			SqlUtil.closeResult(rs);

			ConnectionMgr.getInstance().removeProfile(source.getProfile());
			ConnectionMgr.getInstance().removeProfile(target.getProfile());

		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}


	public void testCopySchemaCreateTable()
	{
		try
		{
			TestUtil util = new TestUtil("WbCopyTest");
			util.prepareEnvironment();

			WbConnection con = util.getConnection("schemaCopyCreateSource");
			WbConnection target = util.getHSQLConnection("schemaCopyCreateTarget");

			WbCopy copyCmd = new WbCopy();
			copyCmd.setConnection(con);

			Statement tstmt = con.createStatement();

			tstmt.executeUpdate("create table person (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");
			tstmt.executeUpdate("create table address (person_id integer, address_details varchar(100))");

			tstmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (1,'Dent', 'Arthur', '01')");
			tstmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (2,'Beeblebrox', 'Zaphod','0202')");
			tstmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (3,'Moviestar', 'Mary', '030303')");
			tstmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (4,'Perfect', 'Ford', '04040404')");

			tstmt.executeUpdate("insert into address (person_id, address_details) values (1, 'Arlington')");
			tstmt.executeUpdate("insert into address (person_id, address_details) values (2, 'Heart of Gold')");
			tstmt.executeUpdate("insert into address (person_id, address_details) values (3, 'Sleepy by Lane')");
			tstmt.executeUpdate("insert into address (person_id, address_details) values (4, 'Betelgeuse')");

			con.commit();

			String sql = "wbcopy -createTarget=true -sourceTable=person,address -sourceProfile='schemaCopyCreateSource' -targetProfile='schemaCopyCreateTarget'";

			StatementRunnerResult result = copyCmd.execute(sql);
			assertEquals(result.getMessageBuffer().toString(), true, result.isSuccess());

			Statement ttstmt = target.createStatement();
			ResultSet rs = ttstmt.executeQuery("select nr, lastname, firstname from person");
			while (rs.next())
			{
				int nr = rs.getInt(1);
				String ln = rs.getString(2);
				String fn = rs.getString(3);
				if (nr == 1)
				{
					assertEquals("Incorrect data copied", "Dent", ln);
					assertEquals("Incorrect data copied", "Arthur", fn);
				}
				else if (nr == 2)
				{
					assertEquals("Incorrect data copied", "Beeblebrox", ln);
					assertEquals("Incorrect data copied", "Zaphod", fn);
				}
			}
			SqlUtil.closeResult(rs);
			ConnectionMgr.getInstance().removeProfile(con.getProfile());
			ConnectionMgr.getInstance().removeProfile(target.getProfile());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

}
