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
package workbench.gui.toolbar;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import workbench.resource.IconMgr;

import workbench.gui.actions.WbAction;
import workbench.gui.components.DividerBorder;

/**
 *
 * @author Thomas Kellerer
 */
public class ActionRenderer
  extends DefaultListCellRenderer
{
  private Border separator = new DividerBorder(DividerBorder.HORIZONTAL_MIDDLE);
  public ActionRenderer()
  {
    super();
    setIconTextGap(10);
    setHorizontalTextPosition(SwingConstants.RIGHT);
  }

  @Override
  public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
  {
    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    if (value instanceof String)
    {
      setText(" ");
      setBorder(separator);
      setIcon(null);
    }
    else if (value instanceof WbAction)
    {
      WbAction action = (WbAction)value;
      setText(action.getMenuLabel());
      String iconKey = action.getIconKey();
      if (iconKey != null)
      {
        ImageIcon icon = IconMgr.getInstance().getLabelIcon(iconKey);
        setIcon(icon);
      }
      setToolTipText(action.getToolTipText());
    }
    return this;
  }

}
