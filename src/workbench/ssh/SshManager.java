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

import java.util.HashMap;
import java.util.Map;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SshManager
{
  private final Object lock = new Object();
  private Map<String, Entry> activeSessions = new HashMap<>();

  public PortForwarder getForwarder(String remoteHost, String user, String pwd)
  {
    String key = makeKey(remoteHost, user);
    PortForwarder forwarder = null;
    synchronized (lock)
    {
      Entry e = activeSessions.get(key);
      if (e == null)
      {
        e = new Entry(new PortForwarder(remoteHost, user, pwd));
        forwarder = e.fwd;
        e.usageCount = 1;
        activeSessions.put(key, e);
      }
      else
      {
        forwarder = e.fwd;
        e.usageCount ++;
      }
    }
    return forwarder;
  }

  public void decrementUsage(String remoteHost, String user)
  {
    synchronized (lock)
    {
      String key = makeKey(remoteHost, user);
      Entry e = activeSessions.get(key);
      if (e != null)
      {
        e.usageCount --;
        if (e.usageCount == 0)
        {
          e.fwd.close();
          activeSessions.remove(key);
        }
      }
    }
  }

  public void disconnect(String remoteHost, String user)
  {
    synchronized (lock)
    {
      String key = makeKey(remoteHost, user);
      Entry e = activeSessions.get(key);
      if (e != null)
      {
        e.fwd.close();
        activeSessions.remove(key);
      }
    }
  }

  public void disconnectAll()
  {
    synchronized (lock)
    {
      for (Entry e : activeSessions.values())
      {
        e.fwd.close();
      }
      activeSessions.clear();
    }
  }

  private String makeKey(String host, String user)
  {
    return StringUtil.coalesce(user, "<noname>") + "@" + host;
  }

  private static class Entry
  {
    final PortForwarder fwd;
    int usageCount;

    Entry(PortForwarder fwd)
    {
      this.fwd = fwd;
    }
  }
}

