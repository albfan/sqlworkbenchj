/*
 * SettingsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
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
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.JButton;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.EscAction;
import workbench.gui.components.WbButton;
import workbench.gui.help.HelpManager;
import workbench.interfaces.ValidatingComponent;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 * @author support@sql-workbench.net
 */
public class SettingsPanel
	extends JPanel
	implements ActionListener, ListSelectionListener, WindowListener,
						ValidatingComponent
{
	private JButton cancelButton;
	private JButton helpButton;
	private JPanel content;
	private JList pageList;
	private JPanel currentPanel;

	private JButton okButton;

	private JDialog dialog;
	private EscAction escAction;
	private List<OptionPanelPage> pages;

	public SettingsPanel()
	{
		super();
		pages = new ArrayList<OptionPanelPage>();
		pages.add(new OptionPanelPage("GeneralOptionsPanel", "LblSettingsGeneral"));
		pages.add(new OptionPanelPage("EditorOptionsPanel", "LblSettingsEditor"));
		pages.add(new OptionPanelPage("EditorColorsPanel", "LblEditorColors"));
		pages.add(new OptionPanelPage("FontOptionsPanel", "LblSettingsFonts"));
		pages.add(new OptionPanelPage("WorkspaceOptions", "LblSettingsWorkspace"));
		pages.add(new OptionPanelPage("DataDisplayOptions", "LblSettingsDataDisplay"));
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

		JPanel buttonPanel = new JPanel(new GridBagLayout());

		GridBagConstraints constraints;
		constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(0, 5, 0, 0);
		buttonPanel.add(helpButton, constraints);

		constraints.gridx ++;
		constraints.anchor = GridBagConstraints.EAST;
		constraints.insets = new Insets(7, 0, 7, 10);
		constraints.weightx = 1.0;
		buttonPanel.add(okButton, constraints);

		constraints.gridx ++;
		constraints.anchor = GridBagConstraints.EAST;
		constraints.weightx = 0.0;
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
		dialog.addWindowListener(this);
		int width = Settings.getInstance().getWindowWidth(this.getClass().getName());
		int height = Settings.getInstance().getWindowHeight(this.getClass().getName());

		if (width > 0 && height > 0)
		{
			this.dialog.setSize(width, height);
		}
		else
		{
			this.dialog.setSize(630,540);
		}

		this.dialog.getRootPane().setDefaultButton(this.okButton);

		escAction = new EscAction(dialog, this);

		WbSwingUtilities.center(this.dialog, aReference);
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				pageList.setSelectedIndex(0);
				pageList.requestFocusInWindow();
			}
		});
		this.dialog.setVisible(true);
	}

	private void closeWindow()
	{
		Settings.getInstance().setWindowSize(this.getClass().getName(), this.dialog.getWidth(), this.dialog.getHeight());
		DataDisplayOptions.clearLocales();
		this.dialog.setVisible(false);
		this.dialog.dispose();
		this.dialog = null;
	}

	public void actionPerformed(java.awt.event.ActionEvent e)
	{
		if (e.getSource() == escAction || e.getSource() == cancelButton)
		{
			this.closeWindow();
		}
		else if (e.getSource() == okButton)
		{
			if (validateInput())
			{
				this.saveSettings();
				this.closeWindow();
			}
		}
		else if (e.getSource() == helpButton)
		{
			HelpManager.showOptionsHelp();
		}
	}

	public void windowActivated(WindowEvent e)
	{
	}

	public void windowClosed(WindowEvent e)
	{
	}

	public void windowClosing(WindowEvent e)
	{
		closeWindow();
	}

	public void windowDeactivated(WindowEvent e)
	{
	}

	public void windowDeiconified(WindowEvent e)
	{
	}

	public void windowIconified(WindowEvent e)
	{
	}

	public void windowOpened(WindowEvent e)
	{
		// readLocales() will read the available locales in a background
		// thread, so that they are available once the user switches
		// to the DataFormattingOptionsPanel
		// Reading the locales can take up to 2 seconds which is too
		// long to be done when switching to the panel
		DataDisplayOptions.readLocales();
	}

	public boolean validateInput()
	{
		for (int i=0; i < pages.size(); i++)
		{
			OptionPanelPage page = pages.get(i);
			final int index = i;
			if (!page.validateInput())
			{
				SwingUtilities.invokeLater(new Runnable() 
				{
					public void run()
					{
						pageList.setSelectedIndex(index);
					}
				});
				return false;
			}
		}
		return true;
	}

	public void componentDisplayed()
	{
	}


}
