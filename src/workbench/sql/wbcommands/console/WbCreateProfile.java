/*
 * WbStoreProfile.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.DbDriver;

import workbench.sql.BatchRunner;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.wbcommands.ConnectionDescriptor;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.StringUtil;


/**
 *
 * @author Thomas Kellerer
 */
public class WbCreateProfile
	extends SqlCommand
{
	public static final String VERB = "WbCreateProfile";
	public static final String ARG_DRV_NAME = "driverName";

	public WbCreateProfile()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(WbStoreProfile.ARG_PROFILE_NAME);
		cmdLine.addArgument(WbStoreProfile.ARG_SAVE_PASSWORD, ArgumentType.BoolArgument);
		cmdLine.addArgument(AppArguments.ARG_CONN_AUTOCOMMIT);
		cmdLine.addArgument(AppArguments.ARG_CONN_DRIVER_CLASS);
		cmdLine.addArgument(AppArguments.ARG_CONN_DRIVER);
		cmdLine.addArgument(AppArguments.ARG_CONN_JAR);
		cmdLine.addArgument(AppArguments.ARG_CONN_PWD);
		cmdLine.addArgument(AppArguments.ARG_CONN_URL);
		cmdLine.addArgument(AppArguments.ARG_CONN_USER);
		cmdLine.addArgument(AppArguments.ARG_CONN_SSH_HOST);
		cmdLine.addArgument(AppArguments.ARG_CONN_SSH_KEYFILE, ArgumentType.Filename);
		cmdLine.addArgument(AppArguments.ARG_CONN_SSH_PWD);
		cmdLine.addArgument(AppArguments.ARG_CONN_SSH_USER);
		cmdLine.addArgument(AppArguments.ARG_CONN_SSH_DB_HOST);
		cmdLine.addArgument(AppArguments.ARG_CONN_SSH_DB_PORT);
		cmdLine.addArgument("user");
		cmdLine.addArgument(AppArguments.ARG_CONN_FETCHSIZE);
		cmdLine.addArgument(AppArguments.ARG_CONN_EMPTYNULL);
		cmdLine.addArgument(AppArguments.ARG_ALT_DELIMITER);
		cmdLine.addArgument(AppArguments.ARG_CONN_SEPARATE);
		cmdLine.addArgument(AppArguments.ARG_CONN_TRIM_CHAR);
		cmdLine.addArgument(AppArguments.ARG_CONN_REMOVE_COMMENTS);
		cmdLine.addArgument(AppArguments.ARG_CONN_CHECK_OPEN_TRANS);
		cmdLine.addArgument(AppArguments.ARG_READ_ONLY);
		cmdLine.addArgument(AppArguments.ARG_CONN_PROPS);
		cmdLine.addArgument(AppArguments.ARG_CONN_EMPTYNULL);
		cmdLine.addArgument(ARG_DRV_NAME);
		cmdLine.addArgument(AppArguments.ARG_PROFILE_GROUP);
		cmdLine.addArgument(AppArguments.ARG_CONN_DESCRIPTOR);
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult();

		cmdLine.parse(getCommandLine(sql));
		String name = cmdLine.getValue(WbStoreProfile.ARG_PROFILE_NAME);
		if (StringUtil.isBlank(name))
		{
			result.addErrorMessageByKey("ErrNoProfile");
			return result;
		}

		ConnectionProfile profile = BatchRunner.createCmdLineProfile(cmdLine, false);

    DbDriver drv = getDriverFromCommandline();
    profile.setDriver(drv);

    profile.setName(name);
		profile.setTemporaryProfile(false);
		profile.setStorePassword(cmdLine.getBoolean(WbStoreProfile.ARG_SAVE_PASSWORD, true));
		profile.setStoreExplorerSchema(false);

		String group = cmdLine.getValue(AppArguments.ARG_PROFILE_GROUP);
		if (group != null)
		{
			profile.setGroup(group);
		}

		ConnectionMgr.getInstance().addProfile(profile);
		ConnectionMgr.getInstance().saveProfiles();
		ConnectionMgr.getInstance().saveDrivers();

		result.addMessageByKey("MsgProfileAdded", profile.getKey().toString());
		return result;
	}

  private DbDriver getDriverFromCommandline()
  {
    DbDriver drv = null;
    String url = cmdLine.getValue(AppArguments.ARG_CONN_URL);
    String driverclass = cmdLine.getValue(AppArguments.ARG_CONN_DRIVER);
    if (driverclass == null)
    {
      driverclass = cmdLine.getValue(AppArguments.ARG_CONN_DRIVER_CLASS);
    }

    boolean driverFound = false;
    String jar = cmdLine.getValue(AppArguments.ARG_CONN_JAR);

    String drvName = cmdLine.getValue(ARG_DRV_NAME);

    if (drvName != null)
    {
      List<DbDriver> drivers = ConnectionMgr.getInstance().getDrivers();
      for (DbDriver dbDriver : drivers)
      {
        if (dbDriver.getName().equalsIgnoreCase(drvName))
        {
          drv = dbDriver;
          driverFound = true;
          break;
        }
      }
    }
    else if (driverclass != null)
    {
      // this might return a driver template without a valid classpath
      drv = ConnectionMgr.getInstance().findDriverByClass(driverclass);

      if (drv != null)
      {
        driverFound = true;
        // if the jar file is not valid, replace it with the one provided by the user
        if (!drv.canReadLibrary() && jar != null)
        {
          drv.setLibrary(jar);
        }
      }
    }
    else
    {
      driverclass = ConnectionDescriptor.findDriverClassFromUrl(url);
      drv = ConnectionMgr.getInstance().findDriver(driverclass);
      if (drv != null)
      {
        if (!drv.canReadLibrary() && jar != null)
        {
          drv.setLibrary(jar);
        }
      }
    }

    if (!driverFound && driverclass != null)
    {
      drv = new DbDriver(driverclass);
      if (drvName != null)
      {
        drv.setName(drvName);
      }
      drv.setLibrary(jar);
      ConnectionMgr.getInstance().getDrivers().add(drv);
    }

    return drv;
  }

	@Override
	public boolean isWbCommand()
	{
		return true;
	}

  @Override
  protected boolean isConnectionRequired()
  {
    return false;
  }

  @Override
  public String getVerb()
  {
    return VERB;
  }

}
