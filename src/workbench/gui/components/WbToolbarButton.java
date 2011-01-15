/*
 * WbToolbarButton.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Insets;
import javax.swing.Action;
import javax.swing.Icon;

/**
 *
 * @author Thomas Kellerer
 */
public class WbToolbarButton
	extends WbButton
{
	public static final Insets MARGIN = new Insets(1,1,1,1);

	public WbToolbarButton()
	{
		super();
		init();
	}

	public WbToolbarButton(String aText)
	{
		super(aText);
		init();
	}

	public WbToolbarButton(Action a)
	{
		super(a);
		this.setText(null);
		iconButton = true;
		init();
	}

	public WbToolbarButton(Icon icon)
	{
		super(icon);
		this.setText(null);
		init();
	}

	public void setAction(Action a)
	{
		super.setAction(a);
		this.setText(null);
		init();
	}

	private void init()
	{
		this.setMargin(MARGIN);
	}

}
