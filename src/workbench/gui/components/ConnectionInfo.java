/*
 * ConnectionInfo.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.WbAction;
import workbench.gui.tools.ConnectionInfoPanel;
import workbench.resource.ResourceMgr;

/**
 * @author  Thomas Kellerer
 */
public class ConnectionInfo
	extends JPanel
	implements PropertyChangeListener, ActionListener
{
	private WbConnection sourceConnection;
	private Color defaultBackground;
	private WbAction showInfoAction;
	private WbLabelField infoText;
	private JLabel iconLabel;
	private final Runnable updater;

	public ConnectionInfo(Color aBackground)
	{
		super(new GridBagLayout());
		infoText = new WbLabelField();
		infoText.setOpaque(false);

		setOpaque(true);

		if (aBackground != null)
		{
			setBackground(aBackground);
			defaultBackground = aBackground;
		}
		else
		{
			defaultBackground = infoText.getBackground();
		}
		showInfoAction = new WbAction(this, "show-info");
		showInfoAction.setMenuTextByKey("MnuTxtConnInfo");
		showInfoAction.setEnabled(false);
		infoText.addPopupAction(showInfoAction);
		infoText.setText(" " + ResourceMgr.getString("TxtNotConnected"));
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.gridx = 1;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		add(infoText, c);
		updater = new Runnable()
		{
			@Override
			public void run()
			{
				_updateDisplay();
			}
		};
	}

	public void setConnection(WbConnection aConnection)
	{
		if (this.sourceConnection != null)
		{
			this.sourceConnection.removeChangeListener(this);
		}

		this.sourceConnection = aConnection;

		Color bkg = null;

		if (this.sourceConnection != null)
		{
			this.sourceConnection.addChangeListener(this);
			ConnectionProfile p = aConnection.getProfile();
			if (p != null)
			{
				bkg = p.getInfoDisplayColor();
			}
		}
		showInfoAction.setEnabled(this.sourceConnection != null);

		if (bkg == null)
		{
			setBackground(defaultBackground);
		}
		else
		{
			setBackground(bkg);
		}

		updateDisplay();
	}

	private void updateDisplay()
	{
		WbSwingUtilities.invoke(updater);
	}

	private void _updateDisplay()
	{
		if (this.sourceConnection != null)
		{
			infoText.setText(this.sourceConnection.getDisplayString());
			StringBuilder tip = new StringBuilder(30);
			tip.append("<html>");
			tip.append(this.sourceConnection.getDatabaseProductName());
			tip.append(" ");
			tip.append(this.sourceConnection.getDatabaseVersion());
			tip.append("<br>");
			tip.append(ResourceMgr.getFormattedString("TxtDrvVersion", this.sourceConnection.getDriverVersion()));
			tip.append("</html>");
			infoText.setToolTipText(tip.toString());
		}
		else
		{
			infoText.setText(ResourceMgr.getString("TxtNotConnected"));
			infoText.setToolTipText(null);
		}
		infoText.setBackground(this.getBackground());
		infoText.setCaretPosition(0);
		showMode();

		validate();

		if (getParent() != null)
		{
			// this seems to be the only way to resize the component
			// approriately after setting a new text when using the dreaded GTK+ look and feel
			getParent().validate();
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getSource() == this.sourceConnection)
		{
			this.updateDisplay();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (this.sourceConnection == null) return;
		if (!WbSwingUtilities.checkConnection(this, sourceConnection)) return;

		ConnectionInfoPanel.showConnectionInfo(sourceConnection);
	}

	private void showMode()
	{
		if (sourceConnection == null)
		{
			hideIcon();
		}
		else
		{
			ConnectionProfile profile = sourceConnection.getProfile();
			boolean readOnly = profile.isReadOnly();
			boolean sessionReadonly = profile.readOnlySession();
			if (readOnly && !sessionReadonly)
			{
				// the profile is set to read only, but it was changed temporarily
				showIcon("unlocked");
			}
			else if (readOnly || sessionReadonly)
			{
				showIcon("lock");
			}
			else
			{
				hideIcon();
			}
		}
		invalidate();
	}

	private void hideIcon()
	{
		if (iconLabel != null)
		{
			this.remove(iconLabel);
			iconLabel = null;
		}
	}

	private void showIcon(String name)
	{
		if (iconLabel == null)
		{
			this.iconLabel = new JLabel();
			iconLabel.setOpaque(false);
		}
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0;
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		ImageIcon png = ResourceMgr.getPng(name);
		iconLabel.setIcon(png);
		add(iconLabel, c);
	}

}
