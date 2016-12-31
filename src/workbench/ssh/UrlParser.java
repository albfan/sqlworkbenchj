/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.ssh;

import java.util.Set;

import workbench.db.DbMetadata;
import workbench.db.JdbcUtils;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class UrlParser
{
  private static Set<String> rewriteSupport = CollectionUtil.caseInsensitiveSet(
    DbMetadata.DBID_PG,
    DbMetadata.DBID_ORA,
    DbMetadata.DBID_MS,
    DbMetadata.DBID_MYSQL,
    DbMetadata.DBID_FIREBIRD,
    DbMetadata.DBID_DB2_LUW,
    DbMetadata.DBID_VERTICA);

  // Stores JDBC URL prefixes that have more then elements in the "protocol" part of the URL
  private static final String LOCAL_IP = "127.0.0.1";

  private final String originalUrl;
  private int remotePort = -1;
  private String remoteServer;

  public UrlParser(String url)
  {
    this.originalUrl = url;

    // initialize remotePort and remoteServer
    getLocalUrl(LOCAL_IP, 0);
  }

  public static boolean canRewriteURL(String url)
  {
    if (url == null) return false;
    String dbId = JdbcUtils.getDbIdFromUrl(url);
    return rewriteSupport.contains(dbId);
  }

  public String getLocalUrl(int localPort)
  {
    return getLocalUrl(LOCAL_IP, localPort);
  }

  public String getDatabaseServer()
  {
    return remoteServer;
  }

  public boolean isDefaultPort()
  {
    return remotePort < 0;
  }

  public int getDatabasePort()
  {
    if (remotePort < 0)
    {
      return getDefaultPort();
    }
    return remotePort;
  }

  private String getLocalUrl(String localHost, int localPort)
  {
    if (originalUrl == null) return null;
    String lower = originalUrl.toLowerCase();
    if (lower.startsWith("jdbc:oracle:"))
    {
      if (lower.indexOf("@(description") > -1)
      {
        return rewriteOracleConnectionDescriptor(originalUrl, localHost, localPort);
      }
      if (lower.indexOf("//@") > -1)
      {
        return rewriteUrl(originalUrl, localHost, localPort, "//@", "/");
      }
      return rewriteOldOracle(originalUrl, localHost, localPort);
    }

    if (lower.startsWith("jdbc:jtds:sqlserver:"))
    {
      return rewriteUrl(originalUrl, localHost, localPort, "//", ";");
    }

    if (lower.startsWith("jdbc:sqlserver:"))
    {
      return rewriteUrl(originalUrl, localHost, localPort, "//", ";");
    }

    return rewriteUrl(originalUrl, localHost, localPort, "//", "/");
  }

  /**
   * rewrite a "standard" URL of the format jdbc:productname://server:host
   *
   */
  private String rewriteUrl(String originalUrl, String host, int localPort, String serverStart, String afterPort)
  {
    int hostStart = originalUrl.indexOf(serverStart);
    if (hostStart < 0) return originalUrl;
    int hostEnd = originalUrl.indexOf(afterPort, hostStart + serverStart.length());
    if (hostEnd < 0)
    {
      hostEnd = originalUrl.length();
    }

    // a port was specified. Extract it for later use
    String hostAndPort = originalUrl.substring(hostStart, hostEnd);
    int colon = hostAndPort.indexOf(':');
    if (colon > 0)
    {
      String port = hostAndPort.substring(colon + 1);
      remoteServer = hostAndPort.substring(serverStart.length(), colon);
      remotePort = StringUtil.getIntValue(port, -1);
    }
    else
    {
      remoteServer = hostAndPort.substring(serverStart.length());
    }

    String newUrl = originalUrl.substring(0, hostStart + serverStart.length());
    newUrl += host + ":" + localPort;
    newUrl += originalUrl.substring(hostEnd);

    return newUrl;
  }

  private String rewriteOldOracle(String originalUrl, String host, int localPort)
  {
    int hostStart = originalUrl.indexOf('@');
    int firstColon = originalUrl.indexOf(':', hostStart + 1);
    int sidStart = firstColon;

    if (firstColon < 0)
    {
      // invalid URL, do nothing
      return originalUrl;
    }

    remoteServer = originalUrl.substring(hostStart + 1, firstColon);
    int secondColon = originalUrl.indexOf(':', firstColon + 1);
    if (secondColon > -1)
    {
      sidStart = secondColon;
      String port = originalUrl.substring(firstColon + 1, secondColon);
      remotePort = StringUtil.getIntValue(port, -1);
    }

    return originalUrl.substring(0, hostStart + 1) + host + ":" + localPort + originalUrl.substring(sidStart);
  }

  private String rewriteOracleConnectionDescriptor(String originalUrl, String localHost, int localPort)
  {
    return originalUrl;
  }

  private int getDefaultPort()
  {
    if (originalUrl == null) return -1;
    String lower = originalUrl.toLowerCase();
    if (lower.startsWith("jdbc:postgresql:"))
    {
      return 5432;
    }
    if (lower.startsWith("jdbc:mysql:"))
    {
      return 3306;
    }
    if (lower.startsWith("jdbc:oracle:"))
    {
      return 1521;
    }
    if (lower.startsWith("jdbc:sqlserver:") || lower.startsWith("jdbc:jtds:sqlserver:"))
    {
      return 1433;
    }
    return -1;

  }
}
