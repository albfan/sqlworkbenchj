/*
 * WbFocusManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import java.awt.event.KeyEvent;
import javax.swing.DefaultFocusManager;
import javax.swing.KeyStroke;
import workbench.gui.actions.WbAction;

/**
 * A focus manager which can grab focus traversal keys before they
 * are processed by the actual compontents. This is necessary to
 * allow Ctrl-Tab to switch tabs in the MainWindow. If no FocusManager
 * is installed, Ctrl-Tab will jump inside the current tabt to the next
 * focusable component, even if an action is registered with the Ctrl-Tab
 * keystroke.
 * <br>
 * WbFocusManager can grab two actions (see grabActions()). If the shortcuts
 * for those actions are pressed, the action is called directly, otherwise the
 * event is handed to the DefaultFocusManager.
 * <br>
 * Currently only two (named) actions are supported, but this can easily
 * be changed to support a variable number of actions by storing them
 * in a Map.
 * @author Thomas Kellerer
 */
public class WbFocusManager
	extends DefaultFocusManager
{
	private WbAction nextTab;
	private WbAction prevTab;

	protected static class LazyInstanceHolder
	{
		protected static final WbFocusManager instance = new WbFocusManager();
	}
	
	public static WbFocusManager getInstance()
	{
		return LazyInstanceHolder.instance;
	}

	private WbFocusManager()
	{
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
		}
	}

	/**
	 * Process the key event for focus traversal. If the keyStroke identified
	 * by the passed event maps to one of the actions registered with grabActions()
	 * the action is executed, and the event is marked as consumed.
	 * <br>
	 * The focusedComponent
	 * @param focusedComponent
	 * @param anEvent
	 */
	public void processKeyEvent(Component focusedComponent, KeyEvent anEvent)
	{
		KeyStroke key = KeyStroke.getKeyStrokeForEvent(anEvent);

		synchronized (LazyInstanceHolder.instance)
		{
			if (nextTab != null && nextTab.getAccelerator().equals(key))
			{
				anEvent.consume();
				nextTab.actionPerformed(null);
				return;
			}
			else if (prevTab != null && prevTab.getAccelerator().equals(key))
			{
				anEvent.consume();
				prevTab.actionPerformed(null);
				return;
			}
		}
		// the call to super may not be synchronized, otherwise I have seen
		// deadlocks when tabbing through the controls of a dialog
		super.processKeyEvent(focusedComponent, anEvent);
	}
}
