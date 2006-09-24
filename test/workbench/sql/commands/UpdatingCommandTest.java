/*
 * UpdatingCommandTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.Statement;
import junit.framework.*;
import workbench.TestUtil;
import workbench.db.WbConnection;
import workbench.sql.DefaultStatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.util.EncodingUtil;
import workbench.util.SqlUtil;

/**
 * @author support@sql-workbench.net
 */
public class UpdatingCommandTest 
	extends TestCase
{
	private TestUtil util;
	private WbConnection connection;
	private DefaultStatementRunner runner;
	
	public UpdatingCommandTest(String testName)
	{
		super(testName);
		try
		{
			util = new TestUtil();
			util.prepareEnvironment();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void setUp()
		throws Exception
	{
		super.setUp();
		util.emptyBaseDirectory();
		runner = util.createConnectedStatementRunner();
		connection = runner.getConnection();
	}

	public void tearDown()
		throws Exception
	{
		connection.disconnect();
		super.tearDown();
	}

	public void testInsertBlob()
	{
		try
		{
			Statement stmt = this.connection.createStatement();
			stmt.executeUpdate("CREATE MEMORY TABLE blob_test(nr integer, blob_data BINARY)");
			stmt.close();
			
			final byte[] blobData = new byte[] { 1,2,3,4,5,6 };
			File blobFile = new File(util.getBaseDir(), "blob_data.data");
			OutputStream out = new FileOutputStream(blobFile);
			out.write(blobData);
			out.close();
			
			String sql = "-- read blob from file\ninsert into blob_test(nr, blob_data)\nvalues\n(1,{$blobfile='" + blobFile.getName() + "'})";
			runner.runStatement(sql, -1, -1);
			StatementRunnerResult result = runner.getResult();
			if (!result.isSuccess()) System.out.println(result.getMessageBuffer().toString());
			assertEquals("Insert not executed", true, result.isSuccess());
			
			stmt = this.connection.createStatement();
			ResultSet rs = stmt.executeQuery("select nr, blob_data from blob_test");
			if (rs.next())
			{
				ResultInfo info = new ResultInfo(rs.getMetaData(), this.connection);
				RowData data = new RowData(2);
				data.read(rs, info);
				
				Object value = data.getValue(0);
				int nr = ((Integer)value).intValue();
				assertEquals("Wrong id inserted", 1, nr);
				
				value = data.getValue(1);
				assertTrue(value instanceof byte[]);
				
				byte[] blob = (byte[])value;
				assertEquals("Wrong blob size retrieved", blobData.length, blob.length);
				
				for (int i = 0; i < blob.length; i++)
				{
					assertEquals("Wrong blob contents", blobData[i], blob[i]);
				}
			}
			else
			{
				fail("No data in table");
			}
			SqlUtil.closeAll(rs, stmt);
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testInsertClob()
	{
		try
		{
			Statement stmt = this.connection.createStatement();
			stmt.executeUpdate("CREATE MEMORY TABLE clob_test(nr integer, clob_data LONGVARCHAR)");
			stmt.close();
			
			final String clobData = "Clob data to be inserted";
			File clobFile = new File(util.getBaseDir(), "clob_data.data");
			Writer w = EncodingUtil.createWriter(clobFile, "UTF8", false);
			w.write(clobData);
			w.close();
			
			String sql = "-- read clob from file\ninsert into clob_test(nr, clob_data)\nvalues\n(1,{$clobfile='" + clobFile.getName() + "' encoding='UTF-8'})";
			runner.runStatement(sql, -1, -1);
			StatementRunnerResult result = runner.getResult();
			if (!result.isSuccess()) System.out.println(result.getMessageBuffer().toString());
			assertEquals("Insert not executed", true, result.isSuccess());
			
			stmt = this.connection.createStatement();
			ResultSet rs = stmt.executeQuery("select nr, clob_data from clob_test");
			if (rs.next())
			{
				int nr = rs.getInt(1);
				assertEquals("Wrong id inserted", 1, nr);
				
				String value = rs.getString(2);
				assertEquals("Wrong clob inserted", clobData, value);
			}
			else
			{
				fail("No data in table");
			}
			SqlUtil.closeAll(rs, stmt);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	
}
