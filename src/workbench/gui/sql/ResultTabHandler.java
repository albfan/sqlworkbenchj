/*
 * ResultTabHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.gui.sql;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;

import workbench.resource.ResourceMgr;

import workbench.gui.actions.AutomaticReloadAction;
import workbench.gui.actions.CancelAutoReloadAction;
import workbench.gui.actions.CloseAllResultsAction;
import workbench.gui.actions.CloseEmptyResultsAction;
import workbench.gui.actions.CloseOtherResultsAction;
import workbench.gui.actions.CloseResultTabAction;
import workbench.gui.actions.DetachResultTabAction;
import workbench.gui.actions.RenameTabAction;
import workbench.gui.actions.ShowSourceQueryAction;
import workbench.gui.actions.SqlPanelReloadAction;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ResultTabHandler
	implements MouseListener, RenameableTab
{
	private JTabbedPane resultTab;
	private SqlPanel client;

	public ResultTabHandler(JTabbedPane tab, SqlPanel sqlPanel)
	{
		resultTab = tab;
		resultTab.addMouseListener(this);
		client = sqlPanel;
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		if (e.getSource() != this.resultTab) return;

		int index = resultTab.getSelectedIndex();
		boolean isResultTab = (index != resultTab.getTabCount() - 1);

		if (!isResultTab) return;

		if (e.getButton() == MouseEvent.BUTTON3)
		{
			JPopupMenu menu = createPopup();
			menu.show(resultTab, e.getX(), e.getY());
		}
		else if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2)
		{
			ShowSourceQueryAction action = new ShowSourceQueryAction(client);
			action.showQuery();
		}
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
	}

	private JPopupMenu createPopup()
	{
		JPopupMenu menu = new JPopupMenu();
		menu.add(new RenameTabAction(this));
		menu.add(new ShowSourceQueryAction(client));
		menu.add(new SqlPanelReloadAction(client));
		menu.addSeparator();
		menu.add(new CloseResultTabAction(client));
		menu.add(new CloseOtherResultsAction(client));
		menu.add(new CloseEmptyResultsAction(client));
		menu.add(new CloseAllResultsAction(client));
		menu.addSeparator();
    menu.add(new AutomaticReloadAction(client));
    menu.add(new CancelAutoReloadAction(client));
    menu.addSeparator();
		menu.add(new DetachResultTabAction(client));
		return menu;
	}

	@Override
	public Component getComponent()
	{
		return resultTab;
	}

	@Override
	public void setCurrentTabTitle(String newName)
	{
		int index = this.resultTab.getSelectedIndex();
		if (StringUtil.isBlank(newName))
		{
			newName = ResourceMgr.getString("LblTabResult");
		}
		resultTab.setTitleAt(index, newName);
	}

	@Override
	public String getCurrentTabTitle()
	{
		int index = this.resultTab.getSelectedIndex();
		return resultTab.getTitleAt(index);
	}

	@Override
	public boolean canRenameTab()
	{
		return true;
	}

	@Override
	public void addTabChangeListener(ChangeListener l)
	{
		this.resultTab.addChangeListener(l);
	}
}
