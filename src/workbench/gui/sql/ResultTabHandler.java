/*
 * ResultTabHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import workbench.gui.actions.CloseAllResultsAction;
import workbench.gui.actions.CloseResultTabAction;
import workbench.gui.actions.SqlPanelReloadAction;
import workbench.gui.actions.RenameTabAction;
import workbench.gui.actions.ShowSourceQueryAction;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
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

	public void mousePressed(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
	}

	public void mouseEntered(MouseEvent e)
	{
	}

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
		menu.add(new CloseAllResultsAction(client));
		return menu;
	}

	public Component getComponent()
	{
		return resultTab;
	}

	public void setCurrentTabTitle(String newName)
	{
		int index = this.resultTab.getSelectedIndex();
		if (StringUtil.isBlank(newName))
		{
			newName = ResourceMgr.getString("LblTabResult");
		}
		resultTab.setTitleAt(index, newName);
	}

	public String getCurrentTabTitle()
	{
		int index = this.resultTab.getSelectedIndex();
		return resultTab.getTitleAt(index);
	}

	public boolean canRenameTab()
	{
		return true;
	}

	public void addTabChangeListener(ChangeListener l)
	{
		this.resultTab.addChangeListener(l);
	}
}
