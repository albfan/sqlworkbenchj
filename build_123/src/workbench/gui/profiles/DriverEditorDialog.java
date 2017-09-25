/*
 * DriverEditorDialog.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.gui.profiles;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.DbDriver;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.EscAction;
import workbench.gui.components.WbButton;
import workbench.gui.help.HelpManager;

/**
 * @author Thomas Kellerer
 */
public class DriverEditorDialog
	extends JDialog
	implements ActionListener
{
	private JPanel dummyPanel;
	private JPanel buttonPanel;
	private JButton okButton;
	private WbButton helpButton;

	private DriverlistEditorPanel driverListPanel;
	private JButton cancelButton;
	private boolean cancelled = true;
	private EscAction escAction;

	public DriverEditorDialog(Frame parent)
	{
		super(parent, true);
		initComponents();

		getRootPane().setDefaultButton(this.okButton);
		escAction = new EscAction(this, this);

		if (!Settings.getInstance().restoreWindowSize(this))
		{
			this.pack();
			WbSwingUtilities.scale(this, 1.05, 1.10);
		}

		// when invoked from the connection dialog, it seems that under
		// Linux the dialog is not visible (because it's behind the connection
		// dialog), so we're trying to make this window visible
		EventQueue.invokeLater(() ->
    {
      toFront();
      requestFocus();
    });
	}

	private void initComponents()
	{
		driverListPanel = new DriverlistEditorPanel();
		buttonPanel = new JPanel(new GridBagLayout());
		okButton = new WbButton(ResourceMgr.getString(ResourceMgr.TXT_OK));
		cancelButton = new WbButton(ResourceMgr.getString(ResourceMgr.TXT_CANCEL));
		dummyPanel = new JPanel();

		helpButton = new WbButton();
		helpButton.setResourceKey("LblHelp");
		helpButton.addActionListener(this);

		setTitle(ResourceMgr.getString("TxtDriverEditorWindowTitle"));
		setModal(true);
		setName("DriverEditorDialog");
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent evt)
			{
				closeDialog(evt);
			}
		});

		driverListPanel.setBorder(BorderFactory.createEtchedBorder());
		getContentPane().add(driverListPanel, BorderLayout.CENTER);

		okButton.addActionListener(this);
		cancelButton.addActionListener(this);

		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(5,5,5,0);
		buttonPanel.add(helpButton, c);

		c.anchor = GridBagConstraints.EAST;
		c.gridx++;
		c.weightx = 1.0;
		c.insets = new Insets(5,5,5,5);
		buttonPanel.add(okButton, c);
		c.gridx++;
		c.weightx = 0;
		buttonPanel.add(cancelButton, c);

		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		dummyPanel.setMaximumSize(new Dimension(2, 2));
		dummyPanel.setMinimumSize(new Dimension(1, 1));
		dummyPanel.setPreferredSize(new Dimension(2, 2));
		getContentPane().add(dummyPanel, BorderLayout.NORTH);
		WbSwingUtilities.makeEqualWidth(okButton, cancelButton, helpButton);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == okButton)
		{
			okButtonActionPerformed(e);
		}
		else if (e.getSource() == cancelButton || e.getActionCommand().equals(escAction.getActionName()))
		{
			cancelButtonActionPerformed();
		}
		else if (e.getSource() == helpButton)
		{
			HelpManager.showDriverHelp();
		}
	}

	private void cancelButtonActionPerformed()
	{
		this.cancelled = true;
		this.closeDialog();
	}

	/**
	 *	Sets the driver name to be pre-selected in the list
	 */
	public void setDriverName(String name)
	{
		if (this.driverListPanel != null && name != null)
		{
			this.driverListPanel.selectDriver(name);
		}
	}

	private void okButtonActionPerformed(ActionEvent evt)
	{
		try
		{
			if (driverListPanel.canChangeSelection())
			{
				this.driverListPanel.saveItem();
				this.cancelled = false;
				this.closeDialog();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DriverEditorDialog.okButton()", "Error closing dialog", e);
		}
	}

	public boolean isCancelled()
	{
		return this.cancelled;
	}

	private void closeDialog(WindowEvent evt)
	{
		this.closeDialog();
	}

	public void closeDialog()
	{
		Settings.getInstance().storeWindowSize(this);
		setVisible(false);
		dispose();
	}

	/**
	 * @param parentFrame
	 * @param current
	 */
	public static void showDriverDialog(final Frame parentFrame, final DbDriver current)
	{
		EventQueue.invokeLater(() ->
    {
      DriverEditorDialog d = new DriverEditorDialog(parentFrame);
      d.setDriverName(current != null ? current.getName() : null);
      WbSwingUtilities.center(d, parentFrame);
      d.setVisible(true);
      d.dispose();
    });
	}
}
