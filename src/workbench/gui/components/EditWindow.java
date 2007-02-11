/*
 * EditWindow.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import javax.swing.ActionMap;
import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
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
 * @author support@sql-workbench.net
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
	private String settingsId = null;
	
	public EditWindow(Frame owner, String title, String text)
	{
		this(owner, title, text, "workbench.data.edit.window");
	}
	
	public EditWindow(Frame owner, String title, String text, boolean createSqlEditor, boolean showCloseButtonOnly)
	{
		this(owner, title, text, "workbench.data.edit.window", createSqlEditor, true, showCloseButtonOnly);
	}
	
	public EditWindow(Frame owner, String title, String text, String settingsId)
	{
		this(owner, title, text, settingsId, false);
	}
	
	public EditWindow(Frame owner, String title, String text, String settingsId, boolean createSqlEditor)
	{
		this(owner, title, text, settingsId, createSqlEditor, true, false);
	}
	
	public EditWindow(Frame owner, String title, String text, String settingsId, boolean createSqlEditor, boolean modal)
	{
		this(owner, title, text, settingsId, createSqlEditor, modal, false);
	}
	
	public EditWindow(final Frame owner, final String title, final String text, final String settingsId, final boolean createSqlEditor, final boolean modal, final boolean showCloseButtonOnly)
	{
		super(owner, title, modal);
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				init(owner, text, settingsId, createSqlEditor, showCloseButtonOnly);
			}
		});
		
		// pack() needs to be called before center() !!!
		WbSwingUtilities.center(this, owner);
	}

	public EditWindow(final Dialog owner, final String title, final String text, final boolean createSqlEditor, final boolean showCloseButtonOnly)
	{
		super(owner, title, true);
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				init(owner, text, "workbench.data.edit.window", createSqlEditor, showCloseButtonOnly);
			}
		});
		WbSwingUtilities.center(this, WbManager.getInstance().getCurrentWindow());
	}
	
	public void setReadOnly()
	{
		this.textContainer.setEditable(false);
	}
	
	private void init(Window owner, String text, String settingsId, boolean createSqlEditor, boolean showCloseButtonOnly)
	{
		this.settingsId = settingsId;
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
				PlainEditor ed = new PlainEditor(this);
				ed.restoreSettings();
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

		InputMap im = new ComponentInputMap(this.getRootPane());
		ActionMap am = new ActionMap();
		EscAction escAction = new EscAction(this);
		im.put(escAction.getAccelerator(), escAction.getActionName());
		am.put(escAction.getActionName(), escAction);
		
		this.getRootPane().setInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, im);
		this.getRootPane().setActionMap(am);
		
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

	public void setVisible(boolean show)
	{
		super.setVisible(show);
		if (show)
		{
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					validate();
					repaint();
				}
			});
		}
	}
	public void hideCancelButton()
	{
		this.cancelButton.removeActionListener(this);
		this.cancelButton.setVisible(false);
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.okButton)
		{
			this.isCancelled = false;
		}
		else if ("edit-ok".equals(e.getActionCommand()))
		{
			this.isCancelled = false;
		}
		this.setVisible(false);
	}

	public boolean isCancelled()
	{
		return this.isCancelled;
	}

	public String getText() 
	{
		return this.textContainer.getText();
	}

	public void windowActivated(java.awt.event.WindowEvent e)
	{
		editor.requestFocus();
	}
	
	public void windowClosed(java.awt.event.WindowEvent e)
	{
		Settings.getInstance().storeWindowSize(this, this.settingsId);
		if (componentSettings != null) componentSettings.saveSettings();
	}
	
	public void windowClosing(java.awt.event.WindowEvent e)
	{
	}
	
	public void windowDeactivated(java.awt.event.WindowEvent e)
	{
	}
	
	public void windowDeiconified(java.awt.event.WindowEvent e)
	{
	}
	
	public void windowIconified(java.awt.event.WindowEvent e)
	{
	}
	
	public void windowOpened(java.awt.event.WindowEvent e)
	{
	}
	
}
