/*
 * SettingsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.settings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.EscAction;
import workbench.gui.components.WbButton;
import workbench.gui.components.WbTabbedPane;
import workbench.gui.help.HtmlViewer;
import workbench.interfaces.Restoreable;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *
 * @author  sql.workbench@freenet.de
 */
public class SettingsPanel
	extends JPanel
	implements ActionListener, ChangeListener
{
  private JPanel buttonPanel;
  private JButton cancelButton;
  private JButton helpButton;
  private WbTabbedPane mainTab;
  private JButton okButton;

	private JDialog dialog;
	private String escActionCommand;
	private List pages;

	public SettingsPanel()
	{
		initComponents();
		pages = new LinkedList();
		JPanel page = new GeneralOptionsPanel();
		mainTab.add(ResourceMgr.getString("LblSettingsGeneral"), page);
		pages.add(page);

		mainTab.addTab(ResourceMgr.getString("LblSettingsEditor"), new JPanel());
		mainTab.addTab(ResourceMgr.getString("LblDataEdit"), new JPanel());
		mainTab.addTab(ResourceMgr.getString("LblSettingsDbExplorer"), new JPanel());
		mainTab.addTab(ResourceMgr.getString("LblSqlFormat"), new JPanel());
		mainTab.addTab(ResourceMgr.getString("LblExternalTools"), new JPanel());
		mainTab.addTab(ResourceMgr.getString("LblLnFOptions"), new JPanel());
		mainTab.addChangeListener(this);
	}
	public void stateChanged(ChangeEvent e)
	{
		Component c = this.mainTab.getSelectedComponent();
		if (c instanceof Restoreable) return;
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			int index = mainTab.getSelectedIndex();
			JPanel page = null;
			switch (index)
			{
				case 1:
					page = new EditorOptionsPanel();
					break;
				case 2:
					page = new DataEditOptionsPanel();
					break;
				case 3:
					page = new DbExplorerOptionsPanel();
					break;
				case 4:
					page = new FormatterOptionsPanel();
					break;
				case 5:
					page = new ExternalToolsPanel();
					break;
				case 6:
					page = new LnFOptionsPanel();
					break;
			}
			pages.add(page);
			mainTab.setComponentAt(index, page);
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
		}
	}


  private void initComponents()
  {
    mainTab = new WbTabbedPane();
		mainTab.setBorder(WbSwingUtilities.EMPTY_BORDER);
		
    okButton = new WbButton(ResourceMgr.getString("LblOK"));
    cancelButton = new WbButton(ResourceMgr.getString("LblCancel"));
    helpButton = new JButton(ResourceMgr.getString("LblHelp"));

    okButton.addActionListener(this);
    cancelButton.addActionListener(this);
    helpButton.addActionListener(this);
		
    setLayout(new BorderLayout());

    buttonPanel = new JPanel(new GridBagLayout());

    GridBagConstraints constraints;
    constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(0, 5, 0, 0);
    buttonPanel.add(helpButton, constraints);

    constraints = new GridBagConstraints();
    constraints.gridx = 1;
    constraints.gridy = 0;
    constraints.anchor = GridBagConstraints.EAST;
    constraints.weightx = 1.0;
    constraints.insets = new Insets(7, 0, 7, 10);
    buttonPanel.add(okButton, constraints);

    constraints = new GridBagConstraints();
    constraints.gridx = 2;
    constraints.gridy = 0;
    constraints.anchor = GridBagConstraints.EAST;
    constraints.insets = new Insets(7, 0, 7, 4);
    buttonPanel.add(cancelButton, constraints);

    add(mainTab, BorderLayout.CENTER);
    add(buttonPanel, BorderLayout.SOUTH);

  }

	private void saveSettings()
	{
		Iterator itr = pages.iterator();
		while (itr.hasNext())
		{
			Restoreable p = (Restoreable)itr.next();
			p.saveSettings();
		}
	}

	public void showSettingsDialog(JFrame aReference)
	{
		this.dialog = new JDialog(aReference, true);
		this.dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.dialog.setTitle(ResourceMgr.getString("TxtSettingsDialogTitle"));
		this.dialog.getContentPane().add(this);
		int width = Settings.getInstance().getWindowWidth(this.getClass().getName());
		int height = Settings.getInstance().getWindowHeight(this.getClass().getName());
		if (width > 0 && height > 0)
		{
			this.dialog.setSize(width, height);
		}
		else
		{
			this.dialog.setSize(600,480);
		}

		this.dialog.getRootPane().setDefaultButton(this.okButton);

		JRootPane root = dialog.getRootPane();
		InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = root.getActionMap();
		EscAction esc = new EscAction(this);
		escActionCommand = esc.getActionName();
		im.put(esc.getAccelerator(), esc.getActionName());
		am.put(esc.getActionName(), esc);

		WbSwingUtilities.center(this.dialog, aReference);
		this.dialog.setVisible(true);
	}

	private void closeWindow()
	{
		Settings.getInstance().setWindowSize(this.getClass().getName(), this.dialog.getWidth(), this.dialog.getHeight());
		this.dialog.setVisible(false);
		this.dialog.dispose();
	}

	public void actionPerformed(java.awt.event.ActionEvent e)
	{
		if (e.getActionCommand().equals(escActionCommand))
		{
			this.closeWindow();
		}
		else if (e.getSource() == okButton)
		{
			this.saveSettings();
			this.closeWindow();
		}
		else if (e.getSource() == cancelButton)
		{
			this.closeWindow();
		}
		else if (e.getSource() == helpButton)
		{
			HtmlViewer viewer = new HtmlViewer(this.dialog);
			viewer.showOptionsHelp();
			viewer.setVisible(true);
		}
	}

}
