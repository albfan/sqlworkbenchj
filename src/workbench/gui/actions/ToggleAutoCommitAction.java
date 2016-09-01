/*
 * ToggleAutoCommitAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ImageIcon;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import workbench.resource.ResourceMgr;

import workbench.db.WbConnection;
import workbench.gui.components.WbToolbarButton;

/**
 * An action to toggle the auto commit attribute of the
 * given {@link workbench.db.WbConnection}
 *
 * @author Thomas Kellerer
 */
public class ToggleAutoCommitAction
  extends CheckBoxAction
  implements PropertyChangeListener
{
  private WbConnection connection;
  private JToggleButton toggleButton;

  public ToggleAutoCommitAction()
  {
    super("MnuTxtToggleAutoCommit", null);
    this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
  }

  public void setConnection(WbConnection conn)
  {
    if (this.connection != null)
    {
      this.connection.removeChangeListener(this);
    }
    this.connection = conn;
    if (this.connection != null)
    {
      this.connection.addChangeListener(this);
    }
    this.checkState();
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    if (this.connection != null && this.isEnabled())
    {
      this.connection.toggleAutoCommit();
      checkState();
    }
  }

  private void checkState()
  {
    if (this.connection != null)
    {
      this.setEnabled(true);
      this.setSwitchedOn(this.connection.getAutoCommit());
      if (this.toggleButton != null)
      {
        this.toggleButton.setSelected(isSwitchedOn());
      }
    }
    else
    {
      this.setEnabled(false);
    }
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt)
  {
    if (evt.getSource() == this.connection && WbConnection.PROP_AUTOCOMMIT.equals(evt.getPropertyName()))
    {
      this.checkState();
    }
  }

  @Override
  public void dispose()
  {
    super.dispose();
    if (this.connection != null)
    {
      this.connection.removeChangeListener(this);
    }
    if (this.toggleButton != null)
    {
      toggleButton.setAction(null);
      toggleButton.setIcon(null);
      toggleButton = null;
    }
  }

  @Override
  public void addToToolbar(JToolBar aToolbar)
  {
    if (this.toggleButton == null) this.createButton();
    aToolbar.add(this.toggleButton);
  }

  private JToggleButton createButton()
  {
    this.toggleButton = new JToggleButton(this);
    this.toggleButton.setMargin(WbToolbarButton.MARGIN);
    ImageIcon icon = getToolbarIcon();
    if (icon != null)
    {
      this.toggleButton.setIcon(icon);
      this.toggleButton.setText(null);
    }
    this.toggleButton.setSelected(isSwitchedOn());
    return this.toggleButton;
  }

}
