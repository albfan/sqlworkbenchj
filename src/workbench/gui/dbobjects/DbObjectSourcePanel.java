/*
 * DbObjectSourcePanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
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
import workbench.db.SourceStatementsHelp;
import workbench.db.WbConnection;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ReloadAction;
import workbench.gui.components.DropDownButton;
import workbench.gui.components.FocusIndicator;
import workbench.gui.components.WbToolbar;
import workbench.gui.sql.EditorPanel;
import workbench.gui.sql.PanelContentSender;
import workbench.interfaces.Reloadable;
import workbench.interfaces.Resettable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 * @author Thomas Kellerer
 */
public class DbObjectSourcePanel
	extends JPanel
	implements ActionListener, Resettable
{
	protected EditorPanel sourceEditor;
	protected ReloadAction reloadSource;
	protected DropDownButton editButton;
	private EditorTabSelectMenu selectTabMenu;
	private MainWindow parentWindow;
	private boolean initialized;

	public DbObjectSourcePanel(MainWindow window, Reloadable reloader)
	{
		super();
		parentWindow = window;
		if (reloader != null)
		{
			reloadSource = new ReloadAction(reloader);
			reloadSource.setEnabled(false);
		}
	}

	private void initGui()
	{
		if (initialized) return;

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				_initGui();
			}
		});
	}

	private void _initGui()
	{
		if (initialized) return;

		this.sourceEditor = EditorPanel.createSqlEditor();
		this.sourceEditor.showFindOnPopupMenu();
		this.sourceEditor.setEditable(false);
		this.setLayout(new BorderLayout());
		WbToolbar toolbar = new WbToolbar();

		if (reloadSource != null)
		{
			toolbar.add(reloadSource);
			reloadSource.addToInputMap(sourceEditor);
		}

		this.add(this.sourceEditor, BorderLayout.CENTER);
		if (reloadSource != null)
		{
			this.add(toolbar, BorderLayout.NORTH);
		}
		if (parentWindow != null)
		{
			editButton = new DropDownButton(ResourceMgr.getString("LblEditScriptSource"));
			selectTabMenu = new EditorTabSelectMenu(this, ResourceMgr.getString("LblEditScriptSource"), "LblEditInNewTab", "LblEditInTab", parentWindow);
			editButton.setDropDownMenu(selectTabMenu.getPopupMenu());
			toolbar.add(editButton);
		}
		if (Settings.getInstance().showFocusInDbExplorer())
		{
			new FocusIndicator(sourceEditor, sourceEditor);
		}
		initialized = true;
	}

	public void actionPerformed(ActionEvent e)
	{
		String command = e.getActionCommand();

		if (command.startsWith(EditorTabSelectMenu.PANEL_CMD_PREFIX) && this.parentWindow != null)
		{
			try
			{
				final int panelIndex = Integer.parseInt(command.substring(EditorTabSelectMenu.PANEL_CMD_PREFIX.length()));

				// Allow the selection change to finish so that
				// we have the correct table name in the instance variables
				EventQueue.invokeLater(new Runnable()
				{
					public void run()
					{
						editText(panelIndex, false);
					}
				});
			}
			catch (Exception ex)
			{
				LogMgr.logError("TableListPanel().actionPerformed()", "Error when accessing editor tab", ex);
			}
		}
	}

	private void editText(final int panelIndex, final boolean appendText)
	{
		if (this.parentWindow != null)
		{
			PanelContentSender sender = new PanelContentSender(this.parentWindow);
			sender.sendContent(getText(), panelIndex, appendText);
		}
	}

	public void setPlainText(final String sql)
	{
		initGui();
		setEditorText(sql, false);
	}

	/**
	 * Set the SQL source. If the text contains an error message
	 * indicating that the source is not available (as returned by DbMetadata
	 * if the SQL Queries have not been configured for e.g. stored procedures)
	 * syntax highlighting will be disabled.
	 */
	public void setText(final String sql)
	{
		initGui();
		boolean hasText = !StringUtil.isEmptyString(sql);
		if (reloadSource != null) reloadSource.setEnabled(hasText);

		if (editButton != null) editButton.setEnabled(hasText);
		if (hasText && sql.startsWith(SourceStatementsHelp.VIEW_ERROR_START) ||
				sql.startsWith(SourceStatementsHelp.PROC_ERROR_START) ||
				sql.startsWith(ResourceMgr.getString("MsgSynonymSourceNotImplemented"))
			 )
		{
			setEditorText(sql, false);
		}
		else
		{
			setEditorText(sql, true);
		}
	}

	private void setEditorText(final String text, final boolean enableHighlight)
	{
		if (enableHighlight)
		{
			sourceEditor.enableSqlHighlight();
		}
		else
		{
			sourceEditor.disableSqlHighlight();
		}

		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				sourceEditor.setText(text == null ? "" : text);
				sourceEditor.invalidate();
				sourceEditor.doLayout();
			}
		});

	}

	public String getText()
	{
		if (sourceEditor == null) return null;
		return sourceEditor.getText();
	}

	public void requestFocus()
	{
		if (sourceEditor != null)
		{
			this.sourceEditor.requestFocus();
		}
	}

	public boolean requestFocusInWindow()
	{
		if (sourceEditor != null)
		{
			return this.sourceEditor.requestFocusInWindow();
		}
		return false;
	}

	public void setCaretPosition(int pos, boolean selectLine)
	{
		initGui();
		sourceEditor.setCaretPosition(pos);
		if (selectLine)
		{
			int line = sourceEditor.getCaretLine();
			int length = sourceEditor.getLineLength(line);
			int lineStart = sourceEditor.getLineStartOffset(line);
			if (lineStart > 0 && length > 0)
			{
				sourceEditor.select(lineStart, lineStart + length);
			}
		}
	}

	public void scrollToCaret()
	{
		if (sourceEditor != null)
		{
			sourceEditor.scrollToCaret();
		}
	}

	public void setDatabaseConnection(WbConnection con)
	{
		initGui();
		sourceEditor.setDatabaseConnection(con);
	}

	public void setEditable(boolean flag)
	{
		if (sourceEditor != null)
		{
			sourceEditor.setEditable(flag);
		}
	}

	public void reset()
	{
		if (sourceEditor != null)
		{
			this.setText("");
		}
	}

}
