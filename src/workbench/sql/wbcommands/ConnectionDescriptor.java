/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
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

import workbench.AppArguments;
import workbench.resource.ResourceMgr;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.DbDriver;
import workbench.db.WbConnection;

import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class ConnectionDescriptor
{
	private File baseDir;
	private String jarfile;
	private int instance;
	private static int instanceCounter;

	public ConnectionDescriptor()
	{
		this(null);
	}

	public ConnectionDescriptor(String dirName)
	{
		baseDir = new File(StringUtil.isBlank(dirName) ? System.getProperty("user.dir") : dirName);
		instance = ++instanceCounter;
	}

	/**
	 * Parses a compact connection string in the format username=foo,pwd=bar,url=...,driver=com...,jar=
	 *
	 * @param connectionString    the connection string to parse
	 * @param currentConnection   the current connection, may be null
   *
	 * @return a connection profile to be used
	 */
	public ConnectionProfile parseDefinition(String connectionString, WbConnection currentConnection)
		throws InvalidConnectionDescriptor
	{
		if (StringUtil.isBlank(connectionString)) return null;

		List<String> elements = StringUtil.stringToList(connectionString, ",", true, true, false, false);
		String url = null;
		String user = null;
		String pwd = null;
		String driverClass = null;
    String driverName = null;
		jarfile = null;
    boolean useCurrentDriver = false;
    String autoCommit = null;

		for (String element : elements)
		{
			String lower = element.toLowerCase();
			if (lower.startsWith(AppArguments.ARG_CONN_USER + "=") || lower.startsWith("user="))
			{
				user = getValue(element);
			}
			if (lower.startsWith(AppArguments.ARG_CONN_PWD + "="))
			{
				pwd = getValue(element);
			}
			if (lower.startsWith(AppArguments.ARG_CONN_URL + "="))
			{
				url = getValue(element);
			}
			if (lower.startsWith(AppArguments.ARG_CONN_DRIVER + "=") || lower.startsWith(AppArguments.ARG_CONN_DRIVER_CLASS + "="))
			{
				driverClass = getValue(element);
			}
			if (lower.startsWith(AppArguments.ARG_CONN_DRIVER_NAME + "="))
			{
				driverName = getValue(element);
			}
			if (lower.startsWith(AppArguments.ARG_CONN_AUTOCOMMIT + "="))
			{
				autoCommit = getValue(element);
			}
			if (lower.startsWith("jar") || lower.startsWith(AppArguments.ARG_CONN_JAR + "="))
			{
				jarfile = getValue(element);
			}
		}

    if (url == null && currentConnection != null)
    {
      url = currentConnection.getUrl();
      useCurrentDriver = true;
    }
    else if (isSameDBMS(currentConnection, url))
    {
      useCurrentDriver = true;
    }

		if (StringUtil.isBlank(url))
		{
			throw new InvalidConnectionDescriptor("No JDBC URL specified in connection specification", ResourceMgr.getString("ErrConnectURLMissing"));
		}

    if (useCurrentDriver)
    {
      driverName = currentConnection.getProfile().getDriverName();
      driverClass = currentConnection.getProfile().getDriverclass();
    }
    else
    {
      if (StringUtil.isEmptyString(driverClass))
      {
        driverClass = findDriverClassFromUrl(url);
      }

      if (StringUtil.isEmptyString(driverClass) )
      {
        throw new InvalidConnectionDescriptor("No JDBC URL specified in connection specification", ResourceMgr.getFormattedString("ErrConnectDrvNotFound", url));
      }
    }

		DbDriver driver = null;
    if (StringUtil.isNonEmpty(driverName))
    {
      driver = ConnectionMgr.getInstance().findDriverByName(driverClass, driverName);
    }
    else
    {
      driver = getDriver(driverClass, jarfile);
    }

		ConnectionProfile result = new ConnectionProfile();
		result.setTemporaryProfile(true);
		result.setName("$temp-profile-"+instance);
		result.setDriver(driver);
		result.setStoreExplorerSchema(false);
		result.setUrl(url);
    if (autoCommit != null)
    {
      result.setAutocommit(StringUtil.stringToBool(autoCommit));
    }

		result.setPassword(pwd);
		result.setStorePassword(true);

		result.setUsername(user);
		result.setRollbackBeforeDisconnect(true);
		result.setReadOnly(false);
		result.reset();
		return result;
	}

	private String getValue(String parameter)
	{
		if (StringUtil.isEmptyString(parameter)) return null;
		int pos = parameter.indexOf('=');
		if (pos == -1) return null;
		return StringUtil.trimQuotes(parameter.substring(pos + 1).trim());
	}

	protected static String getUrlPrefix(String url)
	{
		if (StringUtil.isEmptyString(url)) return null;
		int pos = url.indexOf(':');
		if (pos == -1) return null;
		int pos2 = url.indexOf(':', pos + 1);
		if (pos2 == -1) return null;
		return url.substring(0, pos2 + 1);
	}

	public static String findDriverClassFromUrl(String url)
	{
		String prefix = getUrlPrefix(url);
		if (prefix == null) return null;

		List<DbDriver> templates = ConnectionMgr.getInstance().getDriverTemplates();

		for (DbDriver drv : templates)
		{
			String tempUrl = drv.getSampleUrl();
			if (tempUrl == null) continue;

			String pref = getUrlPrefix(tempUrl);
			if (prefix.equals(pref)) return drv.getDriverClass();
		}
		return null;
	}

	/**
	 * For testing purposes.
	 */
	public String getJarPath()
	{
		return getJarPath(this.jarfile);
	}

	private String getJarPath(String jarFile)
	{
		String jarPath = null;
		WbFile df = new WbFile(jarFile == null ? "" : jarFile);
		if (df.isAbsolute() || baseDir == null)
		{
			jarPath = df.getFullPath();
		}
		else
		{
			df = new WbFile(baseDir, jarFile);
			jarPath = df.getFullPath();
		}
		return jarPath;
	}

	private DbDriver getDriver(String className, String jarFile)
	{
		DbDriver drv = null;
		if (jarFile == null)
		{
			drv = ConnectionMgr.getInstance().findDriver(className);
		}
		else
		{
			String jarPath = getJarPath(jarFile);
			drv = ConnectionMgr.getInstance().registerDriver(className, jarPath);
		}
		return drv;
	}

  private boolean isSameDBMS(WbConnection connection, String url)
  {
    if (StringUtil.isEmptyString(url)) return false;
    if (connection == null) return false;
    String conPrefix = getUrlPrefix(connection.getUrl());
    return conPrefix.equals(getUrlPrefix(url));
  }
}
