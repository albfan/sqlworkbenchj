/*
 * EditorTabSelectMenu.java
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
package workbench.gui.dbobjects;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import workbench.interfaces.FilenameChangeListener;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.DependencyNode;

import workbench.gui.MainWindow;
import workbench.gui.components.WbMenu;
import workbench.gui.components.WbMenuItem;

import workbench.util.CollectionUtil;
import workbench.util.NumberStringCache;

/**
 * @author Thomas Kellerer
 */
public class EditorTabSelectMenu
	extends WbMenu
	implements FilenameChangeListener, ChangeListener
{
	public static final String CMD_CLIPBOARD = "clipboard";

	private MainWindow parentWindow;
	private ActionListener target;
	private String regularTooltip;
	private String newTabTooltip;
	public static final String PANEL_CMD_PREFIX = "panel_";
	private DependencyNode node;
	private boolean withClipboard;

	public EditorTabSelectMenu(ActionListener l, String label, String tooltipKeyNewTab, String tooltipKeyTab, MainWindow parent)
	{
		this(l,label, tooltipKeyNewTab, tooltipKeyTab, parent, false);
	}

	public EditorTabSelectMenu(ActionListener l, String label, String tooltipKeyNewTab, String tooltipKeyTab, MainWindow parent, boolean includeClipboard)
	{
		super(label);
		parentWindow = parent;
		target = l;
		newTabTooltip = ResourceMgr.getDescription(tooltipKeyNewTab, true);
		regularTooltip = ResourceMgr.getDescription(tooltipKeyTab, true);
		this.withClipboard = includeClipboard;
		if (parentWindow != null)
		{
			updateMenu();
			parentWindow.addFilenameChangeListener(this);
			parentWindow.addTabChangeListener(this);
		}
	}

	public void setDependencyNode(DependencyNode dep)
	{
		this.node = dep;
	}

	public DependencyNode getDependencyNode()
	{
		return node;
	}

	protected final synchronized void updateMenu()
	{
		if (parentWindow == null) return;

		List<String> panels = this.parentWindow.getPanelLabels();
		if (CollectionUtil.isEmpty(panels)) return;

		int count = this.getItemCount();
		// Make sure none of the items has an ActionListener attached
		for (int i=0; i < count; i++)
		{
			JMenuItem item = this.getItem(i);
			if (item != null && target != null)
			{
				item.removeActionListener(target);
			}
		}

		this.removeAll();

		int current = this.parentWindow.getCurrentPanelIndex();

		JMenuItem show = new WbMenuItem(ResourceMgr.getString("LblShowDataInNewTab"));
		show.setActionCommand(PANEL_CMD_PREFIX + "-1");
		show.setToolTipText(newTabTooltip);
		show.addActionListener(target);
		this.add(show);

		if (withClipboard)
		{
			JMenuItem clipboard = new WbMenuItem(ResourceMgr.getString("MnuTxtStmtClip"));
			clipboard.setToolTipText(ResourceMgr.getDescription("MnuTxtStmtClip", true));
			clipboard.setActionCommand(CMD_CLIPBOARD);
			clipboard.addActionListener(target);
			this.add(clipboard);
		}

		Font boldFont = show.getFont();
		if (boldFont != null) boldFont = boldFont.deriveFont(Font.BOLD);

		addSeparator();

		for (int i=0; i < panels.size(); i++)
		{
			if (panels.get(i) == null) continue;

			String menuText = panels.get(i);
			if (i < 9)
			{
				menuText += " &" + NumberStringCache.getNumberString(i+1);
			}
			else
			{
				menuText += NumberStringCache.getNumberString(i+1);
			}
			JMenuItem item = new WbMenuItem(menuText);

			item.setActionCommand(EditorTabSelectMenu.PANEL_CMD_PREFIX + NumberStringCache.getNumberString(i));
			if (i == current && boldFont != null)
			{
				item.setFont(boldFont);
			}

			// The tooltip is the same for all items
			item.setToolTipText(regularTooltip);
			item.addActionListener(target);
			this.add(item);
		}
	}

	/**
	 * This is a callback from the MainWindow if a tab has been
	 * renamed. As we are showing the tab names in the "Show table data"
	 * popup menu, we need to update the popup menu
	 */
	@Override
	public void fileNameChanged(Object sender, String newFilename)
	{
		try
		{
			updateMenu();
		}
		catch (Exception e)
		{
			LogMgr.logError("TableListPanel.fileNameChanged()", "Error when updating the popup menu", e);
		}
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		// Updating the menu needs to be done "later" because
		// the ChangeEvent is also triggered when a tab has been
		// removed (thus implicitely changing the index)
		// but the changeEvent occurs before the actual
		// panel is removed from the control.
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				updateMenu();
			}
		});
	}

	@Override
	public void dispose()
	{
		int count = this.getItemCount();
		// Make sure none of the items has an ActionListener attached
		for (int i = 0; i < count; i++)
		{
			JMenuItem item = this.getItem(i);
			if (item != null && target != null)
			{
				item.removeActionListener(target);
			}
			if (item instanceof WbMenuItem)
			{
				((WbMenuItem)item).dispose();
			}
		}
		super.dispose();
	}
}
