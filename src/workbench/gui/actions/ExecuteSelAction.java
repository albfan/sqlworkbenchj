/*
 * ExecuteSelAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.KeyStroke;

import workbench.interfaces.TextSelectionListener;
import workbench.resource.GuiSettings;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.sql.SqlPanel;

/**
 * Run the selected text as a script in the current SQL Panel
 *
 * @see workbench.gui.sql.SqlPanel#runSelectedStatement()
 * @author  Thomas Kellerer
 */
public class ExecuteSelAction
	extends WbAction
	implements TextSelectionListener, PropertyChangeListener
{
	private SqlPanel target;
	private boolean isEnabled;
	private boolean checkSelection;

	public ExecuteSelAction(SqlPanel aPanel)
	{
		super();
		this.target = aPanel;
		this.initMenuDefinition("MnuTxtExecuteSel", KeyStroke.getKeyStroke(KeyEvent.VK_E, PlatformShortcuts.getDefaultModifier()));
		this.setIcon("execute_sel");
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setAlternateAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));
		this.setEnabled(false);
		if (GuiSettings.getExecuteOnlySelected())
		{
			checkSelection = true;
			target.getEditor().addSelectionListener(this);
			checkSelection();
		}
		Settings.getInstance().addPropertyChangeListener(this, GuiSettings.PROPERTY_EXEC_SEL_ONLY);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		if (isEnabled())
		{
			target.runSelectedStatement();
		}
	}

	@Override
	public void setEnabled(boolean flag)
	{
		super.setEnabled(flag);
		isEnabled = flag;
		checkSelection();
	}

	@Override
	public void selectionChanged(int newStart, int newEnd)
	{
		if (isEnabled)
		{
			super.setEnabled(newStart < newEnd);
		}
	}

	public void checkSelection()
	{
		if (checkSelection && isEnabled)
		{
			if (target == null) return;
			if (target.getEditor() == null) return;

			int start = target.getEditor().getSelectionStart();
			int end = target.getEditor().getSelectionEnd();
			super.setEnabled(start < end);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (target == null) return;
		if (target.getEditor() == null) return;

		if (GuiSettings.PROPERTY_EXEC_SEL_ONLY.equals(evt.getPropertyName()))
		{
			boolean wasChecking = checkSelection;
			checkSelection = GuiSettings.getExecuteOnlySelected();
			if (wasChecking)
			{
				super.setEnabled(isEnabled);
				target.getEditor().removeSelectionListener(this);
			}
			else
			{
				target.getEditor().addSelectionListener(this);
				checkSelection();
			}
		}
	}
}
