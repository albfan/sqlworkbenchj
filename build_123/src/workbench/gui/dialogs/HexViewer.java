/*
 * HexViewer.java
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
package workbench.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import workbench.WbManager;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.EscAction;
import workbench.gui.components.HexPanel;

/**
 * A panel to display a BLOB as a hex dump.
 *
 * It uses a HexPanel to display the data.
 *
 * @author Thomas Kellerer
 * @see workbench.gui.components.HexPanel
 */
public class HexViewer
	extends JDialog
	implements ActionListener, WindowListener
{
	private HexPanel panel;
	private JButton closeButton = new JButton(ResourceMgr.getString("LblClose"));
	private final String settingsId = "workbench.gui.imageviewer";

	public HexViewer(JDialog parent, String title)
	{
		super(parent, title, true);
		this.getContentPane().setLayout(new BorderLayout());
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		panel = new HexPanel();
		this.getContentPane().add(panel, BorderLayout.CENTER);
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
		buttonPanel.add(closeButton);
		closeButton.addActionListener(this);
		this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		this.addWindowListener(this);
		if (!Settings.getInstance().restoreWindowSize(this, settingsId))
		{
			setSize(320,200);
		}

		getRootPane().setDefaultButton(closeButton);
		new EscAction(this, this);
		WbSwingUtilities.center(this, WbManager.getInstance().getCurrentWindow());
	}

	public void setData(byte[] data)
	{
		panel.setData(data);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		this.setVisible(false);
		this.dispose();
	}

	@Override
	public void windowOpened(WindowEvent e)
	{
	}

	@Override
	public void windowClosing(WindowEvent e)
	{
	}

	@Override
	public void windowClosed(WindowEvent e)
	{
		Settings.getInstance().storeWindowSize(this, settingsId);
	}

	@Override
	public void windowIconified(WindowEvent e)
	{
	}

	@Override
	public void windowDeiconified(WindowEvent e)
	{
	}

	@Override
	public void windowActivated(WindowEvent e)
	{
	}

	@Override
	public void windowDeactivated(WindowEvent e)
	{
	}

}
