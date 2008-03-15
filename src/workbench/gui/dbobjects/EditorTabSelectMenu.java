/*
 * EditorTabSelectMenu.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import workbench.db.TableIdentifier;
import workbench.gui.MainWindow;
import workbench.gui.components.WbMenu;
import workbench.gui.components.WbMenuItem;
import workbench.interfaces.FilenameChangeListener;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.NumberStringCache;

/**
 * @author support@sql-workbench.net
 */
public class EditorTabSelectMenu
	extends WbMenu
	implements FilenameChangeListener, ChangeListener
{
	private MainWindow parentWindow;
	private ActionListener target;
	private String regularTooltip;
	private String newTabTooltip;
	public static final String PANEL_CMD_PREFIX = "panel_";
	private TableIdentifier table;
	
	public EditorTabSelectMenu(ActionListener l, String label, String tooltipKeyNewTab, String tooltipKeyTab, MainWindow parent)
	{
		super(label);
		parentWindow = parent;
		target = l;
		newTabTooltip = ResourceMgr.getDescription(tooltipKeyNewTab, true);
		regularTooltip = ResourceMgr.getDescription(tooltipKeyTab, true);
		if (parentWindow != null)
		{
			updateMenu();
			parentWindow.addFilenameChangeListener(this);
			parentWindow.addIndexChangeListener(this);
		}
	}
	
	public void setTable(TableIdentifier tbl)
	{
		this.table = tbl;
	}
	
	public TableIdentifier getTable()
	{
		return table;
	}
	
	protected synchronized void updateMenu()
	{
		if (parentWindow == null) return;
		
		String[] panels = this.parentWindow.getPanelLabels();
		if (panels == null) return;
		
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
		
		Font boldFont = show.getFont();
		if (boldFont != null) boldFont = boldFont.deriveFont(Font.BOLD);

		addSeparator();

		for (int i=0; i < panels.length; i++)
		{
			JMenuItem item = new WbMenuItem(panels[i]);
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

	public void stateChanged(ChangeEvent e)
	{
		// Updating the menu needs to be done "later" because
		// the ChangeEvent is also triggered when a tab has been
		// removed (thus implicitely changing the index)
		// but the changeEvent occurs before the actual
		// panel is removed from the control.
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				updateMenu();
			}
		});
	}
}
