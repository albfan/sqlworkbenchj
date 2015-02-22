/*
 * WbToolbarSeparator.java
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

import java.awt.Dimension;

import javax.swing.JPanel;

import workbench.resource.IconMgr;


/**
 *
 * @author  Thomas Kellerer
 */
public class WbToolbarSeparator
	extends JPanel
{

	public WbToolbarSeparator()
	{
		super();
    // this dummy button is used to calculate the width of a regular toolbar button
    // so that the separator can be adjusted to the same height
    WbToolbarButton button = new WbToolbarButton(IconMgr.getInstance().getToolbarIcon("save"));
    Dimension bs = button.getPreferredSize();

		Dimension d = new Dimension((int)(bs.width * 0.3), bs.height);
		setOpaque(false);
		this.setPreferredSize(d);
		this.setPreferredSize(d);
		this.setMaximumSize(d);
		this.setBorder(new DividerBorder(DividerBorder.VERTICAL_MIDDLE));
	}

}
