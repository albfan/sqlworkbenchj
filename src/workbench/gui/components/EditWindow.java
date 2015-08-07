/*
 * EditWindow.java
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
package workbench.gui.components;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.EscAction;
import workbench.gui.sql.EditorPanel;
import workbench.interfaces.Restoreable;
import workbench.interfaces.TextContainer;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *
 * @author Thomas Kellerer
 */
public class EditWindow
	extends JDialog
	implements ActionListener, WindowListener
{
	private TextContainer textContainer;
	private JComponent editor;
	private Restoreable componentSettings;
	private JButton okButton = new WbButton(ResourceMgr.getString("LblOK"));
	private JButton cancelButton = new WbButton(ResourceMgr.getString("LblCancel"));
	private boolean isCancelled = true;
	private String settingsId;

	public EditWindow(Frame owner, String title, String text)
	{
		this(owner, title, text, "workbench.data.edit.window");
	}

	public EditWindow(Frame owner, String title, String text, boolean createSqlEditor, boolean showCloseButtonOnly)
	{
		this(owner, title, text, "workbench.data.edit.window", createSqlEditor, true, showCloseButtonOnly);
	}

	public EditWindow(Frame owner, String title, String text, String id)
	{
		this(owner, title, text, id, false);
	}

	public EditWindow(Frame owner, String title, String text, String id, boolean createSqlEditor)
	{
		this(owner, title, text, id, createSqlEditor, true, false);
	}

	public EditWindow(Frame owner, String title, String text, String id, boolean createSqlEditor, boolean modal)
	{
		this(owner, title, text, id, createSqlEditor, modal, false);
	}

	public EditWindow(final Frame owner, final String title, final String text, final String id, final boolean createSqlEditor, final boolean modal, final boolean showCloseButtonOnly)
	{
		super(owner, title, modal);
		init(text, id, createSqlEditor, showCloseButtonOnly);
		WbSwingUtilities.center(this, owner);
	}

	public EditWindow(final Dialog owner, final String title, final String text, final String id, final boolean createSqlEditor)
	{
		super(owner, title, true);
		init(text, id, createSqlEditor, true);
		WbSwingUtilities.center(this, WbManager.getInstance().getCurrentWindow());
	}

	public EditWindow(final Dialog owner, final String title, final String text, final boolean createSqlEditor, final boolean showCloseButtonOnly)
	{
		super(owner, title, true);
		init(text, "workbench.data.edit.window", createSqlEditor, showCloseButtonOnly);
		WbSwingUtilities.center(this, WbManager.getInstance().getCurrentWindow());
	}

	public void setReadOnly()
	{
		this.textContainer.setEditable(false);
	}

	private void init(String text, String id, boolean createSqlEditor, boolean showCloseButtonOnly)
	{
		this.settingsId = id;
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.getContentPane().setLayout(new BorderLayout());
		if (createSqlEditor)
		{
			EditorPanel panel = EditorPanel.createSqlEditor();
			panel.showFindOnPopupMenu();
			panel.showFormatSql();
			this.editor = panel;
			this.textContainer = panel;
		}
		else
		{
			if (Settings.getInstance().getUsePlainEditorForData())
			{
				PlainEditor ed = new PlainEditor();
				this.componentSettings = ed;
				this.textContainer = ed;
				this.editor = ed;
			}
			else
			{
				EditorPanel panel = EditorPanel.createTextEditor();
				panel.showFindOnPopupMenu();
				this.editor = panel;
				this.textContainer = panel;
			}
		}

		this.getContentPane().add(editor, BorderLayout.CENTER);
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		if (!showCloseButtonOnly)
		{
			buttonPanel.add(this.okButton);
		}
		else
		{
			this.cancelButton.setText(ResourceMgr.getString("LblClose"));
		}
		buttonPanel.add(this.cancelButton);
		this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		this.textContainer.setText(text);
		this.editor.setMinimumSize(new Dimension(100,100));
		this.editor.setPreferredSize(new Dimension(300,200));
		this.textContainer.setCaretPosition(0);

		this.okButton.addActionListener(this);
		this.cancelButton.addActionListener(this);

		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.setDefaultComponent(editor);
		pol.addComponent(editor);
		pol.addComponent(this.okButton);
		pol.addComponent(this.cancelButton);
		this.setFocusTraversalPolicy(pol);
		this.setFocusCycleRoot(false);

		// creating the action will add it to the input map of the dialog
		// which will enable the key
		new EscAction(this, this);

		if (!Settings.getInstance().restoreWindowSize(this, settingsId))
		{
			this.setSize(500,400);
		}

		this.addWindowListener(this);
	}

	public void setInfoText(String text)
	{
		if (this.editor instanceof PlainEditor)
		{
			((PlainEditor)editor).setInfoText(text);
		}
	}

	public void hideCancelButton()
	{
		this.cancelButton.removeActionListener(this);
		this.cancelButton.setVisible(false);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.okButton)
		{
			this.isCancelled = false;
		}
		else if (e.getSource() == this.cancelButton)
		{
			this.isCancelled = true;
		}
		closeWindow();
	}

	private void closeWindow()
	{
		setVisible(false);
		dispose();
	}
	public boolean isCancelled()
	{
		return this.isCancelled;
	}

	public String getText()
	{
		return this.textContainer.getText();
	}

	@Override
	public void windowActivated(java.awt.event.WindowEvent e)
	{
	}

	@Override
	public void windowClosed(java.awt.event.WindowEvent e)
	{
		Settings.getInstance().storeWindowSize(this, this.settingsId);
		if (componentSettings != null) componentSettings.saveSettings();
	}

	@Override
	public void windowClosing(java.awt.event.WindowEvent e)
	{
	}

	@Override
	public void windowDeactivated(java.awt.event.WindowEvent e)
	{
	}

	@Override
	public void windowDeiconified(java.awt.event.WindowEvent e)
	{
	}

	@Override
	public void windowIconified(java.awt.event.WindowEvent e)
	{
	}

	@Override
	public void windowOpened(java.awt.event.WindowEvent e)
	{
		validate();
		editor.validate();
		editor.requestFocusInWindow();
		WbSwingUtilities.repaintLater(this);
	}

}
