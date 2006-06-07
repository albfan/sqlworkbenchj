/*
 * FlatButton.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import workbench.WbManager;
import workbench.gui.WbSwingUtilities;

/**
 *
 * @author support@sql-workbench.net
 */
public class FlatButton
	extends WbButton
{
	
	public FlatButton()
	{
		super();
		init();
	}
	public FlatButton(String label)
	{
		super(label);
		init();
	}

	private void init()
	{
//		if (WbManager.getInstance() == null) return;
//		if (WbManager.getInstance().isWindowsClassic())
//		{
//			setFlatLook();
//			this.setBorder(WbSwingUtilities.FLAT_BUTTON_BORDER);
//		}
	}
	
	public void setFlatLook()
	{
		this.setUI(new javax.swing.plaf.basic.BasicButtonUI());
	}
}
