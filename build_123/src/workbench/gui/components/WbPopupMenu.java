/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer.
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
package workbench.gui.components;

import java.awt.Component;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import workbench.gui.actions.WbAction;

/**
 *
 * @author Thomas Kellerer
 */
public class WbPopupMenu
  extends JPopupMenu
{

  public WbPopupMenu()
  {
  }

  public WbPopupMenu(String label)
  {
    super(label);
  }

  @Override
  public void removeAll()
  {
    int count = getComponentCount();
    for (int i = 0; i < count; i++)
    {
      Component comp = getComponent(i);
      if (comp instanceof JMenuItem)
      {
        JMenuItem item = (JMenuItem)comp;
        if (item instanceof WbMenuItem)
        {
          ((WbMenuItem)item).dispose();
        }
        else
        {
          Action action = item.getAction();
          if (action instanceof WbAction)
          {
            ((WbAction)action).dispose();
          }
        }
      }
    }
    super.removeAll();
  }

}
