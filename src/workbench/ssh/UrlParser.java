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

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class UrlParser
{
  // Stores JDBC URL prefixes that have more then elements in the "protocol" part of the URL
  private static final String LOCAL_IP = "127.0.0.1";

  private final String originalUrl;
  private int dbPort = -1;
  private String dbHostname;

  public UrlParser(String url)
  {
    this.originalUrl = url;

    // initialize remotePort and remoteServer
    getLocalUrl(LOCAL_IP, 0);
  }

  public String getLocalUrl(int localPort)
  {
    return getLocalUrl(LOCAL_IP, localPort);
  }

  public boolean isLocalURL()
  {
    return isLocalhost(dbHostname);
  }

  public String getDatabaseServer()
  {
    return dbHostname;
  }

  public boolean isDefaultPort()
  {
    return dbPort < 0;
  }

  public int getDatabasePort()
  {
    if (dbPort < 0)
    {
      return getDefaultPort();
    }
    return dbPort;
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
      dbHostname = hostAndPort.substring(serverStart.length(), colon);
      dbPort = StringUtil.getIntValue(port, -1);
    }
    else
    {
      dbHostname = hostAndPort.substring(serverStart.length());
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

    dbHostname = originalUrl.substring(hostStart + 1, firstColon);
    int secondColon = originalUrl.indexOf(':', firstColon + 1);
    if (secondColon > -1)
    {
      sidStart = secondColon;
      String port = originalUrl.substring(firstColon + 1, secondColon);
      dbPort = StringUtil.getIntValue(port, -1);
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

  private boolean isLocalhost(String host)
  {
    if (host == null) return false;
    return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
  }

}
