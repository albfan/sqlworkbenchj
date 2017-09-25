/*
 * PanelTitleSetter.java
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
package workbench.gui.sql;

import java.awt.Container;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import workbench.interfaces.MainPanel;
import workbench.resource.GuiSettings;

import workbench.util.NumberStringCache;

/**
 *
 * @author Thomas Kellerer
 */
public class PanelTitleSetter
{
  public static void updateTitle(MainPanel client)
  {
    JPanel p = (JPanel)client;
    Container parent = p.getParent();
    if (parent instanceof JTabbedPane)
    {
      JTabbedPane tab = (JTabbedPane)parent;
      int index = tab.indexOfComponent(p);
      if (index > -1)
      {
        client.setTabTitle(tab, index);
      }
    }
  }

  public static void setTabTitle(final JTabbedPane tab, MainPanel panel, int index, String plainTitle)
  {
    if (index < 0) return;

    String title = plainTitle;
    if (panel.isLocked())
    {
      title = "<html><i>" + title + "</i> ";
      if (GuiSettings.getShowTabIndex())
      {
        if (index < 9)
        {
          title += "<u>";
        }
        title += NumberStringCache.getNumberString(index + 1);
        if (index < 9)
        {
          title += "</u>";
        }
      }
      title += "</html>";
      tab.setTitleAt(index, title);
    }
    else
    {
      if (GuiSettings.getShowTabIndex())
      {
        title += " " + NumberStringCache.getNumberString(index + 1);
      }
      tab.setTitleAt(index, title);

      if (index < 9 && GuiSettings.getShowTabIndex())
      {
        char c = NumberStringCache.getNumberString(index + 1).charAt(0);
        int pos = plainTitle.length() + 1;
        tab.setMnemonicAt(index, c);
        // The Mnemonic index has to be set explicitely otherwise
        // the display would be wrong if the tab title contains
        // the mnemonic character
        tab.setDisplayedMnemonicIndexAt(index, pos);
      }
    }
  }
}
