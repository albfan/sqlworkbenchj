/*
 * MultiLineToolTip.java
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

import javax.swing.JToolTip;

/**
 *
 * @author support@sql-workbench.net
 */
public class MultiLineToolTip
	extends JToolTip
{
	private static final MultiLineToolTipUI SHARED_UI = new MultiLineToolTipUI();

	public MultiLineToolTip()
	{
		super();
    setOpaque(true);
		updateUI();
	}

	public void updateUI()
	{
		setUI(SHARED_UI);
	}

}

