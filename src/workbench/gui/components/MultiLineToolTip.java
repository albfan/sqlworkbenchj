/*
 * MultiLineToolTip.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.components;

import javax.swing.JToolTip;

public class MultiLineToolTip
	extends JToolTip
{
	private static final MultiLineToolTipUI SHARED_UI = new MultiLineToolTipUI();
	
	public MultiLineToolTip()
	{
    setOpaque(true);
		updateUI();
	}
	
	public void updateUI()
	{
		setUI(SHARED_UI);
	}
	
}

