/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
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
package workbench.gui.sql;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.Timer;

import workbench.log.LogMgr;
import workbench.resource.IconMgr;

import workbench.db.KeepAliveDaemon;

import workbench.gui.PanelReloader;
import workbench.gui.components.CompoundIcon;

import workbench.util.StringUtil;


/**
 *
 * @author Thomas Kellerer
 */
public class AutomaticRefreshMgr
  implements ActionListener
{
  private List<PanelEntry> panels = new ArrayList<>();

  public AutomaticRefreshMgr()
  {
  }

  public synchronized void removeRefresh(DwPanel panel)
  {
    if (panel == null) return;
    int id = panel.getId();
    int index = getIndexForId(id);
    removePanel(index);
  }

  public synchronized void clear()
  {
    for (PanelEntry entry : panels)
    {
      disposePanel(entry);
    }
    panels.clear();
  }

  public int getRefreshPeriod(DwPanel panel)
  {
    if (panel == null) return -1;
    int index = getIndexForId(panel.getId());
    if (index < 0) return -1;
    PanelEntry entry = panels.get(index);
    if (entry == null || entry.panel.get() == null || entry.timer == null) return -1;
    return entry.timer.getDelay();
  }

  public synchronized boolean isRegistered(DwPanel panel)
  {
    if (panel == null) return false;
    int index = getIndexForId(panel.getId());
    return index > -1;
  }

  public synchronized void addRefresh(PanelReloader loader, DwPanel panel, int milliSeconds)
  {
    if (panel == null) return;
    if (milliSeconds < 5) return;

    removeRefresh(panel);

    Timer timer = new Timer(milliSeconds, this);
    int id = panel.getId();
    timer.setActionCommand(Integer.toString(id));
    timer.setRepeats(true);
    timer.setCoalesce(true);
    PanelEntry entry = new PanelEntry(id);
    entry.panel = new WeakReference(panel);
    entry.timer = timer;
    entry.reloader = loader;
    panels.add(entry);
    timer.start();
    LogMgr.logDebug("AutomaticRefreshMgr.addRefresh()", "Registered panel: " + panel.getName() + ", id=" + id + ", interval=" + KeepAliveDaemon.getTimeDisplay(milliSeconds));
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    String cmd = e.getActionCommand();
    int id = StringUtil.getIntValue(cmd, -1);

    PanelEntry entry = findEntry(id);
    if (entry == null) return;

    if (entry.reloader == null) return;

    DwPanel panel = entry.panel.get();
    if (panel == null)
    {
      // the panel seems to be gone, so remove this entry
      LogMgr.logWarning("AutomaticRefreshMgr.actionPerformed()", "Panel with id=" + id + " is no longer valid");
      int index = getIndexForId(id);
      removePanel(index);
    }
    else
    {
      LogMgr.logDebug("AutomaticRefreshMgr.actionPerformed()", "Refreshing panel id=" + id);
      entry.reloader.startReloadPanel(panel);
    }
  }

  public Icon getTabIcon(Icon currentIcon, DwPanel panel)
  {
    ImageIcon refresh = IconMgr.getInstance().getLabelIcon("auto_refresh");
    int gap = (int)(refresh.getIconWidth() / 5);
    if (isRegistered(panel))
    {
      // the comparison between currentIcon and refresh works,
      // because IconMgr cashes the icons and always returns the same instance
      if (currentIcon == null || currentIcon == refresh)
      {
        return refresh;
      }

      if (currentIcon instanceof CompoundIcon)
      {
        CompoundIcon cicon = (CompoundIcon)currentIcon;
        if (cicon.contains(refresh))
        {
          // refresh icon already included, stick to the current compound icon
          return cicon;
        }
        else
        {
          // this can't really happen
          return new CompoundIcon(CompoundIcon.Axis.X_AXIS, gap, cicon, refresh);
        }
      }
      return new CompoundIcon(CompoundIcon.Axis.X_AXIS, gap, currentIcon, refresh);
    }
    else
    {
      // the comparison between currentIcon and refresh works,
      // because IconMgr cashes the icons and always returns the same instance
      if (currentIcon == null || currentIcon == refresh)
      {
        return null;
      }

      if (currentIcon instanceof CompoundIcon)
      {
        // currently there are only two possible icons for a result tab
        // and the refresh icon is always the last one, so it's safe
        // to return the first icon from the compound icon
        CompoundIcon cicon = (CompoundIcon)currentIcon;
        if (cicon.contains(refresh))
        {
          return cicon.getIcon(0);
        }
      }
    }
    return currentIcon;
  }

  public static int parseInterval(String interval)
  {
    if (StringUtil.isBlank(interval)) return 0;
    int seconds = StringUtil.getIntValue(interval, Integer.MIN_VALUE);
    if (seconds > 0)
    {
      // just a plain number --> the default is seconds
      seconds = seconds * 1000;
    }
    else
    {
      // not a plain number, assume a number with a unit, e.g. 5m or 30s
      seconds = (int)KeepAliveDaemon.parseTimeInterval(interval);
    }
    return seconds;
  }

  private synchronized void removePanel(int index)
  {
    if (index < 0 || index >= panels.size()) return;
    PanelEntry entry = panels.get(index);
    disposePanel(entry);
    panels.remove(index);
  }

  private void disposePanel(PanelEntry entry)
  {
    if (entry == null) return;
    if (entry.timer != null)
    {
      entry.timer.stop();
      entry.timer.removeActionListener(this);
    }
    entry.timer = null;
    entry.panel = null;
    LogMgr.logDebug("AutomaticRefreshMgr.disposePanel()", "Un-Registered panel with id:" + entry.panelId);
  }

  private PanelEntry findEntry(int id)
  {
    int index = getIndexForId(id);
    if (index < 0) return null;
    PanelEntry entry = panels.get(index);
    return entry;
  }

  private int getIndexForId(int id)
  {
    for (int i=0; i < panels.size(); i++)
    {
      PanelEntry entry = panels.get(i);
      if (entry == null) continue;
      if (entry.panelId == id) return i;
    }
    return -1;
  }

  private static class PanelEntry
  {
    WeakReference<DwPanel> panel;
    Timer timer;
    final int panelId;
    int numRepeats;
    PanelReloader reloader;
    PanelEntry(int id)
    {
      panelId = id;
    }
  }

}
