/*
 * WbLabel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.components;

import javax.swing.JLabel;

import workbench.resource.ResourceMgr;

/**
 *
 * @author Thomas Kellerer
 */
public class WbLabel
	extends JLabel
{

	public void setTextByKey(String resourceKey)
	{
		setText(ResourceMgr.getString(resourceKey));
		setToolTipText(ResourceMgr.getDescription(resourceKey, false));
	}
	
	@Override
	public void setText(String aText)
	{
		if (aText == null)
		{
			return;
		}
		int pos = aText.indexOf('&');
		if (pos > -1)
		{
			char mnemonic = aText.charAt(pos + 1);
			if (mnemonic != ' ')
			{
				aText = aText.substring(0, pos) + aText.substring(pos + 1);
			}
			super.setText(aText);
			if (mnemonic != ' ' && mnemonic != '&')
			{
				setDisplayedMnemonic(mnemonic);
				try
				{
					this.setDisplayedMnemonicIndex(pos);
				}
				catch (Exception e)
				{
				}
			}
		}
		else
		{
			super.setText(aText);
		}
	}
}
