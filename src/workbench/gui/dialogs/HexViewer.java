/*
 * HexViewer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.EscAction;
import workbench.gui.components.HexPanel;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

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

	public void actionPerformed(ActionEvent e)
	{
		this.setVisible(false);
		this.dispose();
	}

	public void windowOpened(WindowEvent e)
	{
	}

	public void windowClosing(WindowEvent e)
	{
	}

	public void windowClosed(WindowEvent e)
	{
		Settings.getInstance().storeWindowSize(this, settingsId);
	}

	public void windowIconified(WindowEvent e)
	{
	}

	public void windowDeiconified(WindowEvent e)
	{
	}

	public void windowActivated(WindowEvent e)
	{
	}

	public void windowDeactivated(WindowEvent e)
	{
	}

}
