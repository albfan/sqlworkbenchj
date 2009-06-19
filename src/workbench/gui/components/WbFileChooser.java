/*
 * WbFileChooser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */

package workbench.gui.components;

import java.awt.Component;
import java.awt.HeadlessException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import workbench.interfaces.ValidatingComponent;
import workbench.resource.Settings;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbFileChooser
		extends JFileChooser
		implements PropertyChangeListener
{
	private String windowSettingsId;
	
	public WbFileChooser(String currentDirectoryPath)
	{
		super(currentDirectoryPath);
		addPropertyChangeListener("JFileChooserDialogIsClosingProperty", this);
	}

	public void setSettingsID(String id)
	{
		this.windowSettingsId = id;
	}
	
	@Override
	public JDialog createDialog(Component parent)
		throws HeadlessException
	{
		JDialog d = super.createDialog(parent);
		if (windowSettingsId != null)
		{
			Settings.getInstance().restoreWindowSize(d, windowSettingsId);
		}
		return d;
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals("JFileChooserDialogIsClosingProperty") &&
				windowSettingsId != null)
		{
			try
			{
				JDialog d = (JDialog)evt.getOldValue();
				Settings.getInstance().storeWindowSize(d, windowSettingsId);
			}
			catch (Throwable th)
			{
				// ignore
			}
		}
	}

	public boolean validateInput()
	{
		JComponent accessory = getAccessory();
		if (accessory instanceof ValidatingComponent)
		{
			ValidatingComponent vc = (ValidatingComponent)accessory;
			return vc.validateInput();
		}
		return true;
	}

	@Override
	public void approveSelection()
	{
		if (validateInput())
		{
			super.approveSelection();
		}
	}

}
