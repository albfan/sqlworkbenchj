/*
 * EditWindow.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
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

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.EscAction;
import workbench.gui.sql.EditorPanel;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;


/**
 *
 * @author  thomas
 */
public class EditWindow
	extends JDialog
	implements ActionListener, WindowListener
{
	
	private EditorPanel editor;
	private JButton okButton = new WbButton(ResourceMgr.getString("LabelOK"));
	private JButton cancelButton = new WbButton(ResourceMgr.getString("LabelCancel"));
	private boolean isCancelled = true;
	private String settingsId = null;
	
	public EditWindow(Frame owner, String title, String text)
	{
		this(owner, title, text, null);
	}
	
	public EditWindow(Frame owner, String title, String text, String settingsId)
	{
		this(owner, title, text, settingsId, false);
	}
	
	public EditWindow(Frame owner, String title, String text, String settingsId, boolean createSqlEditor)
	{
		this(owner, title, text, settingsId, createSqlEditor, true);
	}
	
	public EditWindow(Frame owner, String title, String text, String settingsId, boolean createSqlEditor, boolean modal)
	{
		super(owner, title, modal);
		this.settingsId = settingsId;
		this.getContentPane().setLayout(new BorderLayout());
		if (createSqlEditor)
		{
			this.editor = EditorPanel.createSqlEditor();
		}
		else
		{
			this.editor = EditorPanel.createTextEditor();
		}
		this.editor.showFindOnPopupMenu();
		this.editor.showFormatSql();
		
		this.getContentPane().add(editor, BorderLayout.CENTER);
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(this.okButton);
		buttonPanel.add(this.cancelButton);
		this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		
		this.editor.setText(text);
		this.editor.setMinimumSize(new Dimension(100,100));
		this.editor.setPreferredSize(new Dimension(300,200));
		this.editor.setCaretPosition(0);
		
		
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
		
		// pack() needs to be called before center() !!!
		WbSwingUtilities.center(this, owner);
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
		this.hide();
	}

	public boolean isCancelled()
	{
		return this.isCancelled;
	}

	public String getText() 
	{
		return this.editor.getText();
	}

	public void windowActivated(java.awt.event.WindowEvent e)
	{
	}
	
	public void windowClosed(java.awt.event.WindowEvent e)
	{
		Settings.getInstance().storeWindowSize(this, this.settingsId);
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
