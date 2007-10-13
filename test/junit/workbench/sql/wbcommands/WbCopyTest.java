/*
 * WbCopyTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.ResultSet;
import java.sql.Statement;
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.sql.DefaultStatementRunner;
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

	public void testCopy() throws Exception
	{
		try
		{
			TestUtil util = new TestUtil("WbCopyTest_testExecute");
			util.prepareEnvironment();
			
			DefaultStatementRunner runner = util.createConnectedStatementRunner();
			WbConnection con = runner.getConnection();
			
			Statement stmt = con.createStatement();
			
			stmt.executeUpdate("create table source_data (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");
			
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (1,'Dent', 'Arthur', '01')");
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (2,'Beeblebrox', 'Zaphod','0202')");
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (3,'Moviestar', 'Mary', '030303')");
			stmt.executeUpdate("insert into source_data (nr, lastname, firstname, binary_data) values (4,'Perfect', 'Ford', '04040404')");

			con.commit();
			
			String sql = "--copy source_data and create target\nwbcopy -sourceTable=source_data -targettable=target_data -createTarget=true";
			runner.runStatement(sql, -1, -1);
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
			
			// Allow WbCopy to find the PK columns automatically.
			//stmt.executeUpdate("alter table target_data add primary key (nr)");
			//con.commit();

			sql = "--update target table\nwbcopy -sourceTable=source_data -targettable=target_data -mode=update";
			runner.runStatement(sql, -1, -1);
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
			TestUtil util = new TestUtil("WbCopyTest_testExecute");
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
				"-columns=nr/tnr, lastname/tlastname, firstname/tfirstname";
			
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
			
			WbConnection con = util.getConnection("schemaCopySource");
			WbConnection target = util.getConnection("schemaCopyTarget");

			WbCopy copyCmd = new WbCopy();
			copyCmd.setConnection(con);
			
			Statement stmt = con.createStatement();
			
			stmt.executeUpdate("create table person (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");
			stmt.executeUpdate("create table address (person_id integer, address_details varchar(100))");
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
			
			con.commit();

			Statement tstmt = target.createStatement();
			tstmt.executeUpdate("create table person (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");
			tstmt.executeUpdate("create table address (person_id integer, address_details varchar(100))");
			tstmt.executeUpdate("alter table address add foreign key (person_id) references person(nr)");
			target.commit();
			
			String sql = "wbcopy -sourceTable=some_data,address,person -checkDependencies=true -sourceProfile='schemaCopySource' -targetProfile='schemaCopyTarget'";
			
			StatementRunnerResult result = copyCmd.execute(sql);
			assertEquals(result.getMessageBuffer().toString(), true, result.isSuccess());
			
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

	public void testCopySchemaCreateTable()
	{
		try
		{
			TestUtil util = new TestUtil("WbCopyTest");
			util.prepareEnvironment();
			
			WbConnection con = util.getConnection("schemaCopyCreateSource");
			WbConnection target = util.getConnection("schemaCopyCreateTarget");

			WbCopy copyCmd = new WbCopy();
			copyCmd.setConnection(con);
			
			Statement stmt = con.createStatement();
			
			stmt.executeUpdate("create table person (nr integer not null primary key, lastname varchar(50), firstname varchar(50), binary_data blob)");
			stmt.executeUpdate("create table address (person_id integer, address_details varchar(100))");
			
			stmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (1,'Dent', 'Arthur', '01')");
			stmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (2,'Beeblebrox', 'Zaphod','0202')");
			stmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (3,'Moviestar', 'Mary', '030303')");
			stmt.executeUpdate("insert into person (nr, lastname, firstname, binary_data) values (4,'Perfect', 'Ford', '04040404')");

			stmt.executeUpdate("insert into address (person_id, address_details) values (1, 'Arlington')");
			stmt.executeUpdate("insert into address (person_id, address_details) values (2, 'Heart of Gold')");
			stmt.executeUpdate("insert into address (person_id, address_details) values (3, 'Sleepy by Lane')");
			stmt.executeUpdate("insert into address (person_id, address_details) values (4, 'Betelgeuse')");
			
			con.commit();

			String sql = "wbcopy -createTarget=true -sourceTable=person,address -sourceProfile='schemaCopyCreateSource' -targetProfile='schemaCopyCreateTarget'";
			
			StatementRunnerResult result = copyCmd.execute(sql);
			assertEquals(result.getMessageBuffer().toString(), true, result.isSuccess());
			
			Statement tstmt = target.createStatement();
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
