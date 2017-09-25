/*
 * EventNotifier.java
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
package workbench.util;

import java.util.ArrayList;
import java.util.List;

import workbench.interfaces.EventDisplay;

/**
 *
 * @author Thomas Kellerer
 */
public class EventNotifier
{
  private List<EventDisplay> displayClients = new ArrayList<>(1);
  private NotifierEvent lastEvent = null;
  private static EventNotifier instance = new EventNotifier();

  private EventNotifier()
  {
  }

  public static EventNotifier getInstance()
  {
    return instance;
  }

  public synchronized void addEventDisplay(EventDisplay d)
  {
    displayClients.add(d);
    if (this.lastEvent != null)
    {
      d.showAlert(lastEvent);
    }
  }

  public synchronized void removeEventDisplay(EventDisplay d)
  {
    displayClients.remove(d);
  }

  public synchronized void displayNotification(NotifierEvent e)
  {
    this.lastEvent = e;
    for (EventDisplay d : displayClients)
    {
      d.showAlert(e);
    }
  }

  public synchronized void removeNotification()
  {
    for (EventDisplay d : displayClients)
    {
      d.removeAlert();
    }
  }

}
