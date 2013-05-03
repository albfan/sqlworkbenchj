/*
 * ProfileSelectionDialog.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.gui.profiles;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import workbench.interfaces.EventDisplay;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.DbDriver;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.EscAction;
import workbench.gui.components.WbButton;
import workbench.gui.help.HelpManager;

import workbench.util.EventNotifier;
import workbench.util.NotifierEvent;
import workbench.util.StringUtil;

/**
 *
 * @author  Thomas Kellerer
 */
public class ProfileSelectionDialog
	extends JDialog
	implements ActionListener, WindowListener, TreeSelectionListener, MouseListener, EventDisplay
{
	private JPanel okCancelPanel;
	private JButton okButton;
	private JButton cancelButton;
	private WbButton helpButton;
	private WbButton manageDriversButton;
	private ProfileEditorPanel profiles;
	private ConnectionProfile selectedProfile;
	private boolean cancelled;
	private String escActionCommand;
	private JLabel versionInfo;

	public ProfileSelectionDialog(Frame parent, boolean modal, String lastProfileKey)
	{
		this(parent, modal, lastProfileKey, false);
	}

	public ProfileSelectionDialog(Frame parent, boolean modal, String lastProfileKey, boolean enableVersionCheck)
	{
		super(parent, modal);
		initComponents(lastProfileKey);
		if (enableVersionCheck)
		{
			EventNotifier.getInstance().addEventDisplay(this);
		}

		JRootPane root = this.getRootPane();
		root.setDefaultButton(okButton);
		EscAction esc = new EscAction(this, this);
		escActionCommand = esc.getActionName();
	}


	private void initComponents(String lastProfileKey)
	{
		profiles = new ProfileEditorPanel(lastProfileKey);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BorderLayout(0, 0));

		JPanel toolsButtonPanel = new JPanel();
		toolsButtonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

		manageDriversButton = new WbButton();
		manageDriversButton.setResourceKey("LblEditDrivers");
		manageDriversButton.addActionListener(this);

		helpButton = new WbButton();
		helpButton.setResourceKey("LblHelp");
		helpButton.addActionListener(this);
		toolsButtonPanel.add(manageDriversButton);
		toolsButtonPanel.add(helpButton);

		okCancelPanel = new JPanel();
		buttonPanel.add(okCancelPanel, BorderLayout.EAST);
		buttonPanel.add(toolsButtonPanel, BorderLayout.WEST);
		versionInfo = new JLabel("  ");
		versionInfo.setForeground(Color.RED);
		versionInfo.setBorder(new EmptyBorder(0, 15, 0, 0));
		buttonPanel.add(versionInfo, BorderLayout.CENTER);

		okButton = new WbButton(ResourceMgr.getString(ResourceMgr.TXT_OK));
		okButton.setEnabled(profiles.getSelectedProfile() != null);

		cancelButton = new WbButton(ResourceMgr.getString(ResourceMgr.TXT_CANCEL));

		WbSwingUtilities.setJButtonPreferredWidth(manageDriversButton, okButton, helpButton, cancelButton);

		addWindowListener(this);
		okCancelPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		okCancelPanel.add(okButton);
		okButton.addActionListener(this);

		okCancelPanel.add(cancelButton);
		cancelButton.addActionListener(this);

		profiles.addListMouseListener(this);
		profiles.addSelectionListener(this);

		BorderLayout bl = new BorderLayout();
		this.getContentPane().setLayout(bl);
		getContentPane().add(profiles, BorderLayout.CENTER);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		setTitle(ResourceMgr.getString("LblSelectProfile"));
		this.restoreSize();
		// This should be done "later", otherwise the focus will not be set
		// correctly when running on Linux with the GTk+ look and feel
		WbSwingUtilities.requestFocus(profiles.getInitialFocusComponent());
	}

	@Override
	public void showAlert(final NotifierEvent event)
	{
		if (versionInfo == null) return;

		versionInfo.addMouseListener(
			new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					if (e.getButton() == MouseEvent.BUTTON1)
					{
						ActionEvent evt = new ActionEvent(ProfileSelectionDialog.this, -1, "notifierClicked");
						event.getHandler().actionPerformed(evt);
						versionInfo.removeMouseListener(this);
					}
				}
			});

		WbSwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				versionInfo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				versionInfo.setIcon(ResourceMgr.getImageByName(event.getIconKey()));
				versionInfo.setText(event.getTooltip());
				versionInfo.getParent().doLayout();
			}
		});
	}

	@Override
	public void removeAlert()
	{
		if (versionInfo == null) return;

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				versionInfo.setIcon(null);
				versionInfo.setText("");
				versionInfo.getParent().doLayout();
			}
		});
	}

	private void closeDialog()
	{
		this.saveSize();
		this.profiles.saveSettings();
		this.setVisible(false);
		dispose();
	}

	public ConnectionProfile getSelectedProfile()
	{
		return this.selectedProfile;
	}

	public void restoreSize()
	{
		if (!Settings.getInstance().restoreWindowSize(this))
		{
			this.setSize(650, 520);
		}
	}

	public void saveSize()
	{
		Settings s = Settings.getInstance();
		s.storeWindowSize(this);
	}

	public void selectProfile()
	{
		if (this.profiles.validateInput())
		{
			this.selectedProfile = this.profiles.getSelectedProfile();
			if (this.checkPassword())
			{
				this.cancelled = false;
				this.closeDialog();
				if (Settings.getInstance().getSaveProfilesImmediately())
				{
					ConnectionMgr.getInstance().saveProfiles();
				}
			}
		}
	}

	private boolean checkPassword()
	{
		if (this.selectedProfile == null) return false;
		if (this.selectedProfile.getStorePassword()) return true;

		String pwd = WbSwingUtilities.getUserInputHidden(this, ResourceMgr.getString("MsgInputPwdWindowTitle"), "");
		if (StringUtil.isEmptyString(pwd)) return false;
		this.selectedProfile.setPassword(pwd);
		return true;
	}

	public void profileListClicked(MouseEvent evt)
	{
		if (evt.getButton() == MouseEvent.BUTTON1 && evt.getClickCount() == 2)
		{
			profiles.applyProfiles();
			selectProfile();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.okButton)
		{
			profiles.applyProfiles();
			selectProfile();
		}
		else if (e.getSource() == this.cancelButton || e.getActionCommand().equals(escActionCommand))
		{
			this.selectedProfile = null;
			this.cancelled = true;
			this.closeDialog();
		}
		else if (e.getSource() == this.manageDriversButton)
		{
			showDriverEditorDialog();
		}
		else if (e.getSource() == this.helpButton)
		{
			HelpManager.showProfileHelp();
		}
	}

	public boolean isCancelled()
	{
		return this.cancelled;
	}

	@Override
	public void windowActivated(WindowEvent e)
	{
	}

	@Override
	public void windowClosed(WindowEvent e)
	{
		this.profiles.done();
	}

	@Override
	public void windowClosing(WindowEvent e)
	{
		this.cancelled = true;
		this.selectedProfile = null;
		this.closeDialog();
	}

	@Override
	public void windowDeactivated(WindowEvent e)
	{
	}

	@Override
	public void windowDeiconified(WindowEvent e)
	{
	}

	@Override
	public void windowIconified(WindowEvent e)
	{
	}

	@Override
	public void windowOpened(WindowEvent e)
	{
		this.cancelled = true;
		this.selectedProfile = null;
	}

	@Override
	public void valueChanged(TreeSelectionEvent e)
	{
		this.okButton.setEnabled(profiles.getSelectedProfile() != null);
	}

	@Override
	public void mouseClicked(MouseEvent evt)
	{
		profileListClicked(evt);
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
	}

	private void showDriverEditorDialog()
	{
		final Frame parent = (Frame)this.getParent();
		final DbDriver drv = this.profiles.getCurrentDriver();
		DriverEditorDialog.showDriverDialog(parent, drv);
	}
}
