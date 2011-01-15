/*
 * DecreaseFontSize.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.fontzoom;

import java.awt.event.KeyEvent;
import workbench.resource.ResourceMgr;

/**
 *
 * @author Thomas Kellerer
 */
public class DecreaseFontSize
	extends FontSizeAction
{
	public DecreaseFontSize()
	{
		super("TxtEdFntDecr", KeyEvent.VK_SUBTRACT, KeyEvent.CTRL_MASK);
	}

	public DecreaseFontSize(FontZoomer fontZoomer)
	{
		super("TxtEdFntDecr", KeyEvent.VK_SUBTRACT, KeyEvent.CTRL_MASK);
		setTooltip(ResourceMgr.getDescription("TxtEdFntDecr"));
		setZoomer(fontZoomer);
	}

	public DecreaseFontSize(String key, FontZoomer fontZoomer)
	{
		super(key, fontZoomer);
	}

	@Override
	public void doFontChange(FontZoomer fontZoomer)
	{
		fontZoomer.decreaseFontSize();
	}
}
