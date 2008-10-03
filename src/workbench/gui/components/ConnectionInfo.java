/*
 * ConnectionInfo.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.WbAction;
import workbench.gui.tools.ConnectionInfoPanel;
import workbench.resource.ResourceMgr;

/**
 * @author  support@sql-workbench.net
 */
public class ConnectionInfo
	extends JComponent
	implements PropertyChangeListener, ActionListener
{
	private WbLabelField display;
	private WbConnection sourceConnection;
	private Color defaultBackground;
	
	public ConnectionInfo(Color aBackground)
	{
		super();

		this.setLayout(new GridLayout(1,1,0,0));
		super.setBackground(aBackground);
		defaultBackground = aBackground;
		WbAction a = new WbAction(this, "show-info");
		a.setMenuTextByKey("MnuTxtConnInfo");
		this.display = new WbLabelField();
		this.display.addPopupAction(a);
		this.add(this.display);
		updateDisplay();
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
		
		final Color newBackground = bkg;
		
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				setInfoColor(newBackground);
				updateDisplay();
			}
		});
	}

	private void setInfoColor(Color c)
	{
		if (c == null)
		{
			this.setBackground(this.defaultBackground);
		}
		else
		{
			this.setBackground(c);
		}
	}

	@Override
	public void setBackground(Color c)
	{
		super.setBackground(c);
		if (this.display != null)
		{
			this.display.setBackground(c);
		}
	}
	
	private void updateDisplay()
	{
		if (this.sourceConnection != null)
		{
			this.display.setText(" " + this.sourceConnection.getDisplayString());
			StringBuilder tip = new StringBuilder(30);
			tip.append("<html>");
			tip.append(this.sourceConnection.getDatabaseProductName());
			tip.append(" ");
			tip.append(this.sourceConnection.getDatabaseVersion());
			tip.append("<br>");
			tip.append(ResourceMgr.getFormattedString("TxtDrvVersion", this.sourceConnection.getDriverVersion()));
			tip.append("</html>");
			this.display.setToolTipText(tip.toString());
		}
		else
		{
			this.display.setText(" " + ResourceMgr.getString("TxtNotConnected"));
			this.display.setToolTipText(null);
		}
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
		ConnectionInfoPanel.showConnectionInfo(sourceConnection);
	}

}
