/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.wbcommands;

import java.io.File;
import java.util.List;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.DbDriver;

import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class CompactConnectionDefinition
{
	private File baseDir;

	public CompactConnectionDefinition(File dir)
	{
		this.baseDir = dir;
	}

	/**
	 * Parses a compact connection string in the format username/password@jdbc:....
	 *
	 * @param connectionString  the connection string to parse
	 * @param driverString      the driver string to parse {@link #parseDriver(java.lang.String) }
	 * @return a connection profile to be used
	 */
	public ConnectionProfile parseDefinition(String connectionString, String driverString)
	{
		if (StringUtil.isBlank(connectionString)) return null;

		DbDriver driver = parseDriver(driverString);
		int urlStart = connectionString.indexOf('@');
		String url = connectionString.substring(urlStart + 1);

		List<String> login = StringUtil.stringToList(connectionString.substring(0, urlStart), "/", false, true, false, false);
		String username = login.get(0);
		String pwd = login.get(1);
		ConnectionProfile result = new ConnectionProfile();
		result.setTemporaryProfile(true);
		result.setName("temp");
		result.setDriver(driver);
		result.setStoreExplorerSchema(false);
		result.setUrl(url);

		result.setPassword(pwd);
		result.setStorePassword(true);

		result.setUsername(username);
		result.setRollbackBeforeDisconnect(true);
		result.setReadOnly(false);
		result.reset();
		return result;
	}

	/**
	 * Parses a compact driver definition in the format classname@jarfile
	 *
	 * e.g. com.postgresql.Driver@c:/Drivers/Postgres/postgresql-9.3-1102.jdbc4.jar
	 * @param driverString
	 * @return
	 */
	public DbDriver parseDriver(String driverString)
	{
		if (StringUtil.isBlank(driverString))  return null;
		int pos = driverString.indexOf('@');

		String drvClass = null;
		String jarPath = null;
		if (pos == -1)
		{
			drvClass = driverString.trim();
		}
		else
		{
			drvClass = driverString.substring(0, pos).trim();
			String jarFile = driverString.substring(pos + 1);
			WbFile df = new WbFile(jarFile);
			if (df.isAbsolute() || baseDir == null)
			{
				jarPath = df.getFullPath();
			}
			else
			{
				df = new WbFile(baseDir, jarFile);
				jarPath = df.getFullPath();
			}
		}

		DbDriver drv = ConnectionMgr.getInstance().registerDriver(drvClass, jarPath);
		return drv;
	}
}
