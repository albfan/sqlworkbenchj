/*
 * WbMenuItem.java
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
package workbench.gui.components;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenuItem;

import workbench.gui.actions.WbAction;
import workbench.resource.ResourceMgr;

/**
 *
 * @author  Thomas Kellerer
 */
public class WbMenuItem
	extends JMenuItem
{
	public WbMenuItem()
	{
		super();
	}

	public WbMenuItem(String aText)
	{
		super(aText);
	}

	public WbMenuItem(Action anAction)
	{
		super(anAction);
	}

	public WbMenuItem(String text, int mnemonic)
	{
		super(text, mnemonic);
	}

	public WbMenuItem(Icon icon)
	{
		super(icon);
	}

	public WbMenuItem(String text, Icon icon)
	{
		super(text, icon);
	}

	public void setMenuTextByKey(String key)
	{
		this.setText(ResourceMgr.getString(key));
		this.setToolTipText(ResourceMgr.getDescription(key));
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
				this.setMnemonic((int) mnemonic);
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
