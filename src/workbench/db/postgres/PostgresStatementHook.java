/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.db.postgres;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.WbConnection;

import workbench.sql.StatementHook;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;

import workbench.util.StringUtil;

/**
 * A StatementHook to retrieve Postgres notifications.
 *
 * @author Thomas Kellerer
 */
public class PostgresStatementHook
  implements StatementHook
{
  // The access to the PGConnection and PGNotification methods can only be done through reflection
  // as the driver's classes are not available through the default classloader
  private Method getNotifications;
  private Method getName;
  private Method getPID;
  private Method getParameter;

  private volatile static boolean available = true;

  public PostgresStatementHook(WbConnection connection)
  {
    initialize(connection);
  }

  @Override
  public String preExec(StatementRunner runner, String sql)
  {
    return sql;
  }

  @Override
  public void postExec(StatementRunner runner, String sql, StatementRunnerResult result)
  {
    if (available)
    {
      List<String> messages = getMessages(runner.getConnection());
      for (String msg : messages)
      {
        result.addMessage(msg);
      }
    }
  }

  @Override
  public boolean isPending()
  {
    return false;
  }

  @Override
  public boolean displayResults()
  {
    return true;
  }

  @Override
  public boolean fetchResults()
  {
    return true;
  }

  @Override
  public void close(WbConnection conn)
  {
    // nothing to do
  }

  private List<String> getMessages(WbConnection conn)
  {
    if (!available || conn == null) return Collections.emptyList();

    List<String> result = new ArrayList<>(1);
    try
    {
      Object notifications = getNotifications.invoke(conn.getSqlConnection(), (Object[])null);

      if (notifications != null && notifications.getClass().isArray())
      {
        int length = Array.getLength(notifications);
        for (int i=0; i < length; i++)
        {
          Object notification = Array.get(notifications, i);
          String msg = getMessage(notification);
          if (msg != null)
          {
            result.add(msg);
          }
        }
      }
    }
    catch (Throwable th)
    {
      LogMgr.logError("PostgresStatementHook.getMessages()","Could not retrieve messages", th);
      setUnavailable();
    }

    return result;
  }

  private String getMessage(Object notification)
    throws Exception
  {
    if (notification == null) return null;
    if (!available) return null;

    if (getName == null)
    {
      initGetters(notification);
    }

    if (!available) return null;

    String name = (String)getName.invoke(notification, (Object[])null);
    Object pid = getPID.invoke(notification, (Object[])null);
    String payload = (String)getParameter.invoke(notification, (Object[])null);
    String payloadMsg = "";
    if (StringUtil.isNonEmpty(payload))
    {
      payloadMsg = " " + ResourceMgr.getFormattedString("MsgPgNotificationPayload", payload);
    }
    return ResourceMgr.getFormattedString("MsgPgNotificationBase", name, payloadMsg, pid.toString());
  }

  private synchronized void initialize(WbConnection conn)
  {
    if (!available) return;
    if (conn.getUrl().startsWith("jdbc:pgsql"))
    {
      // the PG/NG driver does not support the notification classes
      setUnavailable();
      return;
    }

    try
    {
      getNotifications = conn.getSqlConnection().getClass().getMethod("getNotifications", (Class[])null);
    }
    catch (Throwable t)
    {
      LogMgr.logError("PostgresStatementHook.initialize()", "Could not obtain getNotifications() method", t);
      getNotifications = null;
      setUnavailable();
    }
  }

  private synchronized void initGetters(Object notification)
  {
    try
    {
      getName = notification.getClass().getMethod("getName", (Class[])null);
      getParameter = notification.getClass().getMethod("getParameter", (Class[])null);
      getPID = notification.getClass().getMethod("getPID", (Class[])null);
    }
    catch (Throwable th)
    {
      LogMgr.logError("PostgresStatementHook.initGetters()", "Could not obtain methods from PGNotification interface", th);
      setUnavailable();
    }
  }

  private static synchronized void setUnavailable()
  {
    available = false;
  }
}
