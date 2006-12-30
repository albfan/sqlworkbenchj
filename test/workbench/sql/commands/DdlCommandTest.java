/*
 * DdlCommandTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;

import junit.framework.TestCase;
import junit.framework.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.db.WbConnection;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class DdlCommandTest 
	extends TestCase
{
	
	public DdlCommandTest(String testName)
	{
		super(testName);
	}

	public void testGetTypeAndObject() throws Exception
	{
		try
		{
			// detection of the type is already tested for SqlUtil.getCreateType()
			// so we only need to test getName();
			String sql = "-- test\ncreate or \t replace\n\nprocedure bla";
			String name = DdlCommand.CREATE.getObjectName(sql);
			assertEquals("bla", name);
			
			sql = "-- test\ncreate \n\ntrigger test_trg for mytable";
			name = DdlCommand.CREATE.getObjectName(sql);
			assertEquals("test_trg", name);
			
			sql = "-- test\ncreate function \n\n myfunc\n as something";
			name = DdlCommand.CREATE.getObjectName(sql);
			assertEquals("myfunc", name);

			sql = "-- test\ncreate or replace package \n\n some_package \t\t\n as something";
			name = DdlCommand.CREATE.getObjectName(sql);
			assertEquals("some_package", name);
			
			sql = "-- test\ncreate package body \n\n some_body \t\t\n as something";
			name = DdlCommand.CREATE.getObjectName(sql);
			assertEquals("some_body", name);
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	
}
