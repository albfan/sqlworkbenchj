/*
 * DbObjectSourcePanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JPanel;
import workbench.db.WbConnection;
import workbench.gui.MainWindow;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.RunStatement;
import workbench.gui.components.DropDownButton;
import workbench.gui.components.WbToolbar;
import workbench.gui.sql.EditorPanel;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.Reloadable;
import workbench.interfaces.RunnableStatement;
import workbench.log.LogMgr;
import workbench.util.StringUtil;

/**
 * @author thomas
 */
public class DbObjectSourcePanel
	extends JPanel
	implements ActionListener
{
	private EditorPanel sourceEditor;
	private ReloadAction reloadSource;
	private RunStatement recreateObject;
	private DropDownButton editButton;
	private EditorTabSelectMenu selectTabMenu;
	private MainWindow parentWindow;
	
	public DbObjectSourcePanel(MainWindow parent, Reloadable reloader)
	{
		parentWindow = parent;
		if (reloader != null)
		{
			reloadSource = new ReloadAction(reloader);
			reloadSource.setEnabled(false);
		}
//		if (runner != null)
//		{
//			recreateObject = new RunStatement(runner);
//			recreateObject.setEnabled(false);
//		}
		
		this.sourceEditor = EditorPanel.createSqlEditor();
		this.sourceEditor.showFindOnPopupMenu();
		this.sourceEditor.setEditable(false);
		this.setLayout(new BorderLayout());
		WbToolbar toolbar = new WbToolbar();
//		toolbar.addDefaultBorder();
		
		if (recreateObject != null) toolbar.add(recreateObject);
		if (reloadSource != null) toolbar.add(reloadSource);
		
		this.add(this.sourceEditor, BorderLayout.CENTER);
		if (recreateObject != null || reloadSource != null)
		{
			this.add(toolbar, BorderLayout.NORTH);
		}
		editButton = new DropDownButton("Edit in");
		selectTabMenu = new EditorTabSelectMenu(this, "Edit in", "LblEditInTab", parent);
		editButton.setDropDownMenu(selectTabMenu.getPopupMenu());
		toolbar.add(editButton);
	}

	public void actionPerformed(ActionEvent e)
	{
		String command = e.getActionCommand();

		if (command.startsWith("panel-") && this.parentWindow != null)
		{
			try
			{
				final int panelIndex = Integer.parseInt(command.substring(6));
				// Allow the selection change to finish so that
				// we have the correct table name in the instance variables
				EventQueue.invokeLater(new Runnable()
				{
					public void run()
					{
						editText(panelIndex);
					}
				});
			}
			catch (Exception ex)
			{
				LogMgr.logError("TableListPanel().actionPerformed()", "Error when accessing editor tab", ex);
			}
		}
	}
		
	private void editText(final int panelIndex)
	{
		final SqlPanel panel;

		if (panelIndex == -1)
		{
			panel = (SqlPanel)this.parentWindow.addTab(true, true);
		}
		else
		{
		 panel = (SqlPanel)this.parentWindow.getSqlPanel(panelIndex);
		}

		String sql = this.getText();
		if (sql != null)
		{
			panel.setStatementText(sql);

			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					parentWindow.requestFocus();
					if (panelIndex > -1) parentWindow.selectTab(panelIndex);
					panel.selectEditor();
				}
			});
		}
	}
	
	public void setText(final String sql)
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				sourceEditor.setText(sql);
				boolean hasText = !StringUtil.isEmptyString(sql);
				if (reloadSource != null) reloadSource.setEnabled(hasText);
				if (recreateObject != null) recreateObject.setEnabled(hasText);
			}
		});
	}
	
	public String getText()
	{
		return sourceEditor.getText();
	}
	
	public void setCaretPosition(int pos)
	{
		sourceEditor.setCaretPosition(pos);
	}
	
	public void scrollToCaret()
	{
		sourceEditor.scrollToCaret();
	}
	
	public void setDatabaseConnection(WbConnection con)
	{
		sourceEditor.setDatabaseConnection(con);
	}
	
	public void setEditable(boolean flag)
	{
		sourceEditor.setEditable(flag);
	}
}
