/*
 * WbScrollPane.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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

import java.awt.Component;

import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import workbench.gui.WbSwingUtilities;

/**
 *
 * @author  Thomas Kellerer
 */
public class WbScrollPane
	extends JScrollPane
{
	private static boolean useCustomizedBorder = true;

	public WbScrollPane()
	{
		super();
		this.initDefaults();
	}

	public WbScrollPane(Component view)
	{
		super(view);
		this.initDefaults();
	}

	public WbScrollPane(Component view, int vsbPolicy, int hsbPolicy)
	{
		super(view, vsbPolicy, hsbPolicy);
		this.initDefaults();
	}

	public WbScrollPane(int vsbPolicy, int hsbPolicy)
	{
		super(vsbPolicy, hsbPolicy);
		this.initDefaults();
	}

	private void initDefaults()
	{
    setDoubleBuffered(true);
		if (useCustomizedBorder)
		{
			try
			{
				// With some Linux distributions (Debian) creating this border during
				// initialization fails. So if we can't create our own border
				// we simply skip this for the future
				Border myBorder = new CompoundBorder(WbSwingUtilities.getBevelBorder(), new EmptyBorder(0,1,0,0));
				if (myBorder == null)
				{
					useCustomizedBorder = false;
				}
				else
				{
					this.setBorder(myBorder);
				}
			}
			catch (Throwable e)
			{
				useCustomizedBorder = false;
			}
		}
	}

}
