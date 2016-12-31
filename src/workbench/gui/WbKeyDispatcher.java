/*
 * WbFocusManager.java
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
package workbench.gui;

import java.awt.Component;
import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;

import javax.swing.FocusManager;
import javax.swing.KeyStroke;

import workbench.WbManager;

import workbench.gui.actions.WbAction;
import workbench.gui.components.WbTable;
import workbench.gui.sql.EditorPanel;

/**
 * A KeyEventDispatcher which handle Ctrl-Tab and Ctrl-Shift-Tab to navigate in tabbed pane.
 *
 * This is necessary to allow Ctrl-Tab to switch tabs in the MainWindow. If no KeyEventDispatcher
 * is installed, Ctrl-Tab will jump inside the current tabt to the next focusable component,
 * even if an action is registered with the Ctrl-Tab keystroke.
 * <br>
 * WbFocusManager can grab two actions (see grabActions()). If the shortcuts
 * for those actions are pressed, the action are processed in dispatchKeyEvent()
 * and the focus manager is signalled to no longer process the key event.
 *
 * @author Thomas Kellerer
 * @see MainWindow#windowActivated(java.awt.event.WindowEvent)
 * @see MainWindow#windowDeactivated(java.awt.event.WindowEvent)
 * @see WbManager#initUI()
 */
public class WbKeyDispatcher
	implements KeyEventDispatcher
{
	private WbAction nextTab;
	private WbAction prevTab;
  private boolean enabled;

	private static class LazyInstanceHolder
	{
		protected static final WbKeyDispatcher instance = new WbKeyDispatcher();
	}

	public static WbKeyDispatcher getInstance()
	{
		return LazyInstanceHolder.instance;
	}

	private WbKeyDispatcher()
	{
    super();
	}

	/**
	 * Define the two actions to be grabbed by this focus manager.
	 * <br>
	 * As these actions are window specific, the window that wants to
	 * override the focus traversal keys, needs to regiters these actions
	 * in the windowActivated event.
	 * <br>
	 * To make sure, the actions are executed for the correct window, the window
	 * must de-register the actions in the windowDeactivated event by passing <tt>null</tt>
	 * for the two parameters
	 *
	 * @param next the action for "Next tab", may be null
	 * @param prev the action for "previous tab", may be null
	 */
	public void grabActions(WbAction next, WbAction prev)
	{
		synchronized (LazyInstanceHolder.instance)
		{
			nextTab = next;
			prevTab = prev;
      enabled = nextTab != null && prevTab != null;
		}
	}

  @Override
  public boolean dispatchKeyEvent(KeyEvent evt)
  {
    // intercept the Window Alt-Key handling when the Alt-Key is used
    // to prevent rectangular selections in the editor activating the menu
    if (evt.getKeyCode() == KeyEvent.VK_ALT && evt.getID() == KeyEvent.KEY_RELEASED)
    {
      FocusManager mgr = FocusManager.getCurrentManager();
      Component owner = mgr.getFocusOwner();
      // Allow Alt-Click in the editor and result sets
      if ( (owner instanceof EditorPanel) || (owner instanceof WbTable))
      {
        evt.consume();
        return true;
      }
    }

    if (!enabled) return false;

    KeyStroke key = KeyStroke.getKeyStrokeForEvent(evt);
    boolean processed = false;

    synchronized (LazyInstanceHolder.instance)
    {
      if (nextTab.getAccelerator().equals(key))
      {
        evt.consume();
        nextTab.actionPerformed(null);
        processed = true;
      }
      else if (prevTab.getAccelerator().equals(key))
      {
        evt.consume();
        prevTab.actionPerformed(null);
        processed = true;
      }
    }
    return processed;
  }


}
