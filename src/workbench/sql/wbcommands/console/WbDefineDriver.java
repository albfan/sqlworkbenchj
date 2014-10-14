/*
 * WbDefineDriver.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands.console;

import java.sql.SQLException;
import java.util.List;

import workbench.AppArguments;
import workbench.resource.ResourceMgr;

import workbench.db.ConnectionMgr;
import workbench.db.DbDriver;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class WbDefineDriver
	extends SqlCommand
{
	public static final String VERB = "WbDefineDriver";
	public static final String ARG_DRV_NAME = "name";

	public WbDefineDriver()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(ARG_DRV_NAME);
		cmdLine.addArgument(AppArguments.ARG_CONN_DRIVER_CLASS);
		cmdLine.addArgument(AppArguments.ARG_CONN_JAR);
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	protected boolean isConnectionRequired()
	{
		return false;
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult();

		String arguments = getCommandLine(sql);
		cmdLine.parse(arguments);
		if (!cmdLine.hasArguments())
		{
			result.addMessageByKey("ErrDefDrv");
			result.setFailure();
			return result;
		}

		String name = cmdLine.getValue(ARG_DRV_NAME);
		String drvClass = cmdLine.getValue(AppArguments.ARG_CONN_DRIVER_CLASS);
		String jarFile =  cmdLine.getValue(AppArguments.ARG_CONN_JAR);

		if (StringUtil.isBlank(name))
		{
			result.addMessageByKey("ErrDefDrvNoName");
			result.addMessageByKey("ErrDefDrv");
			result.setFailure();
			return result;
		}

		name = name.trim();

		if (!isNameUnique(name))
		{
			result.addMessageByKey("ErrDrvNameNotUnique");
			result.addMessageByKey("ErrDefDrv");
			result.setFailure();
			return result;
		}

		if (StringUtil.isBlank(drvClass))
		{
			result.addMessageByKey("ErrDefDrvNoClass");
			result.addMessageByKey("ErrDefDrv");
			result.setFailure();
			return result;
		}

		DbDriver drv = new DbDriver();
		drv.setLibrary(jarFile);
		drv.setName(name);
		drv.setDriverClass(drvClass);

		List<String> libs = drv.getLibraryList();
		for (String file : libs)
		{
			WbFile f = new WbFile(file);
			if (!f.exists())
			{
				String msg = ResourceMgr.getFormattedString("ErrFileNotFound", f.getFullPath());
				result.addMessage(msg);
				result.setFailure();
			}
		}
		if (!result.isSuccess())
		{
			return result;
		}

		ConnectionMgr.getInstance().getDrivers().add(drv);
		ConnectionMgr.getInstance().saveDrivers();
		result.addMessage(ResourceMgr.getFormattedString("MsgDriverAdded", name));
		result.setSuccess();
		return result;
	}

	private boolean isNameUnique(String name)
	{
		List<DbDriver> drivers = ConnectionMgr.getInstance().getDrivers();
		for (DbDriver drv : drivers)
		{
			if (drv.getName().equalsIgnoreCase(name))
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

}
