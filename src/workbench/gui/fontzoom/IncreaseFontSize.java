/*
 * IncreaseFontSize.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.fontzoom;

import java.awt.event.KeyEvent;

/**
 *
 * @author Thomas Kellerer
 */
public class IncreaseFontSize
	extends FontSizeAction
{
	public IncreaseFontSize()
	{
		super("TxtEdFntInc", KeyEvent.VK_ADD, KeyEvent.CTRL_MASK, null);
	}

	public IncreaseFontSize(FontZoomer fontZoomer)
	{
		super("TxtEdFntInc", KeyEvent.VK_ADD, KeyEvent.CTRL_MASK, fontZoomer);
	}

	public IncreaseFontSize(String key, FontZoomer fontZoomer)
	{
		super(key, KeyEvent.VK_ADD, KeyEvent.CTRL_MASK, fontZoomer);
	}

	@Override
	public void doFontChange(FontZoomer fontZoomer)
	{
		fontZoomer.increaseFontSize();
	}
}
