/*
 * ConnectionInfo.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
	extends WbLabelField
	implements PropertyChangeListener, ActionListener
{
	private WbConnection sourceConnection;
	private Color defaultBackground;
	private WbAction showInfoAction;

	public ConnectionInfo(Color aBackground)
	{
		super();
		if (aBackground != null)
		{
			setBackground(aBackground);
			defaultBackground = aBackground;
		}
		else
		{
			defaultBackground = getBackground();
		}
		showInfoAction = new WbAction(this, "show-info");
		showInfoAction.setMenuTextByKey("MnuTxtConnInfo");
		showInfoAction.setEnabled(false);
		addPopupAction(showInfoAction);
		setText(ResourceMgr.getString("TxtNotConnected"));

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
		if (this.sourceConnection != null)
		{
			this.setText(this.sourceConnection.getDisplayString());
			StringBuilder tip = new StringBuilder(30);
			tip.append("<html>");
			tip.append(this.sourceConnection.getDatabaseProductName());
			tip.append(" ");
			tip.append(this.sourceConnection.getDatabaseVersion());
			tip.append("<br>");
			tip.append(ResourceMgr.getFormattedString("TxtDrvVersion", this.sourceConnection.getDriverVersion()));
			tip.append("</html>");
			setToolTipText(tip.toString());
		}
		else
		{
			setText(ResourceMgr.getString("TxtNotConnected"));
			setToolTipText(null);
		}
		setCaretPosition(0);

		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				if (getParent() != null)
				{
					// this seems to be the only way to resize the component
					// approriately after setting a new text when using the dreaded GTK+ look and feel
					getParent().doLayout();
				}
			}
		});
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getSource() == this.sourceConnection
			  && (WbConnection.PROP_CATALOG.equals(evt.getPropertyName()) ||
			      WbConnection.PROP_SCHEMA.equals(evt.getPropertyName())))
		{
			this.updateDisplay();
		}
	}

	public void actionPerformed(ActionEvent e)
	{
		if (this.sourceConnection == null) return;
		if (!WbSwingUtilities.checkConnection(this, sourceConnection)) return;

		ConnectionInfoPanel.showConnectionInfo(sourceConnection);
	}

}
