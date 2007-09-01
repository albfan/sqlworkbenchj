/*
 * SettingsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.settings;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionListener;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.EscAction;
import workbench.gui.components.WbButton;
import workbench.gui.help.HtmlViewer;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *
 * @author  sql.workbench@freenet.de
 */
public class SettingsPanel
	extends JPanel
	implements ActionListener, ListSelectionListener
{
  private JPanel buttonPanel;
  private JButton cancelButton;
  private JButton helpButton;
//  private WbSplitPane splitPane;
	private JPanel content;
	private JList pageList;
	private JPanel currentPanel;
	
  private JButton okButton;

	private JDialog dialog;
	private String escActionCommand;
	private List<OptionPanelPage> pages;

	public SettingsPanel()
	{
		pages = new ArrayList<OptionPanelPage>();
		pages.add(new OptionPanelPage("GeneralOptionsPanel", "LblSettingsGeneral"));
		pages.add(new OptionPanelPage("EditorOptionsPanel", "LblSettingsEditor"));
		pages.add(new OptionPanelPage("DataFormattingOptionsPanel", "LblSettingsDataFormat"));
		pages.add(new OptionPanelPage("DataEditOptionsPanel", "LblDataEdit"));
		pages.add(new OptionPanelPage("DbExplorerOptionsPanel", "LblSettingsDbExplorer"));
		pages.add(new OptionPanelPage("WindowTitleOptionsPanel", "LblSettingsWinTitle"));
		pages.add(new OptionPanelPage("FormatterOptionsPanel", "LblSqlFormat"));
		pages.add(new OptionPanelPage("SqlGenerationOptionsPanel", "LblSqlGeneration"));
		pages.add(new OptionPanelPage("ExternalToolsPanel", "LblExternalTools"));
		pages.add(new OptionPanelPage("LnFOptionsPanel", "LblLnFOptions"));
		
		initComponents();
	}
	
	public void valueChanged(ListSelectionEvent e)
	{
		if (e.getValueIsAdjusting()) return;
		
		int index = this.pageList.getSelectedIndex();
		
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			OptionPanelPage option = pages.get(index);
			JPanel panel = option.getPanel();
			if (currentPanel != null)
			{
				content.remove(currentPanel);
			}
			content.add(panel, BorderLayout.CENTER);
			content.validate();
			content.repaint();
			this.currentPanel = panel;
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
		}
	}


  private void initComponents()
  {
		ListModel model = new AbstractListModel()
		{
			public Object getElementAt(int index)
			{
				return pages.get(index);
			}
		
			public int getSize()
			{
				return pages.size();
			}
		};
		
		pageList = new JList(model);
		pageList.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(2, 2, 2, 2)));
		pageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		pageList.addListSelectionListener(this);
		
		content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(2,2,2,2));
		content.add(pageList, BorderLayout.WEST);
		
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

    add(content, BorderLayout.CENTER);
    add(buttonPanel, BorderLayout.SOUTH);

  }

	private void saveSettings()
	{
		for (OptionPanelPage page : pages)
		{
			page.saveSettings();
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
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				pageList.setSelectedIndex(0);
			}
		});		
		this.dialog.setVisible(true);
	}

	private void closeWindow()
	{
		Settings.getInstance().setWindowSize(this.getClass().getName(), this.dialog.getWidth(), this.dialog.getHeight());
		this.dialog.setVisible(false);
		this.dialog.dispose();
		this.dialog = null;
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
