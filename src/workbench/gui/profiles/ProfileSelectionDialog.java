/*
 * ProfileSelectionDialog.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.profiles;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import workbench.db.ConnectionMgr;

import workbench.db.ConnectionProfile;
import workbench.db.DbDriver;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.EscAction;
import workbench.gui.components.WbButton;
import workbench.gui.help.HelpManager;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 *
 * @author  Thomas Kellerer
 */
public class ProfileSelectionDialog
	extends JDialog
	implements ActionListener, WindowListener, TreeSelectionListener, MouseListener
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

	public ProfileSelectionDialog(Frame parent, boolean modal)
	{
		this(parent, modal, null);
	}

	public ProfileSelectionDialog(Frame parent, boolean modal, String lastProfileKey)
	{
		super(parent, modal);
		initComponents(lastProfileKey);

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

		okButton = new WbButton(ResourceMgr.getString(ResourceMgr.TXT_OK));
		okButton.setEnabled(profiles.getSelectedProfile() != null);

		cancelButton = new WbButton(ResourceMgr.getString(ResourceMgr.TXT_CANCEL));

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
		// This should be "posted", otherwise the focus will not be set
		// correctly when running on Linux with the GTk+ look and feel
		WbSwingUtilities.requestFocus(profiles.getInitialFocusComponent());
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
			this.setSize(650, 510);
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

		String pwd = WbSwingUtilities.getUserInput(this, ResourceMgr.getString("MsgInputPwdWindowTitle"), "", true);
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
		else if (e.getSource() == this.cancelButton ||
			e.getActionCommand().equals(escActionCommand))
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

	private boolean checkModifiedGroups()
	{
		if (profiles.profilesChanged())
		{
			int result = WbSwingUtilities.getYesNoCancel(this, ResourceMgr.getString("MsgProfilesChanged"));
			if (result == JOptionPane.YES_OPTION)
			{
				profiles.applyProfiles();
				return true;
			}
			if (result == JOptionPane.NO_OPTION)
			{
				return true;
			}
			return false;
		}
		return true;
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
