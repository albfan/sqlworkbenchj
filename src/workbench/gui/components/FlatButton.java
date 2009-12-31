/*
 * FlatButton.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import javax.swing.Action;
import javax.swing.Icon;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;

/**
 *
 * @author Thomas Kellerer
 */
public class FlatButton
	extends WbButton
{
	
	public FlatButton()
	{
		super();
		init();
	}

	public FlatButton(Action action)
	{
		super(action);
		init();
	}
	
	public FlatButton(Icon icon)
	{
		super(icon);
		init();
	}
	
	public FlatButton(String label)
	{
		super(label);
		init();
	}

	private void init()
	{
		// WbManager.getInstance() can be null if this component
		// is created e.g. in the GUI designer of NetBeans
		if (WbManager.getInstance() == null) return;
		if (WbManager.getInstance().isWindowsClassic())
		{
			setFlatLook();
		}
	}
	
	public void setFlatLook()
	{
		this.setBorder(WbSwingUtilities.FLAT_BUTTON_BORDER);
	}
}
