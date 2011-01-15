/*
 * ResetFontSize.java
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
public class ResetFontSize
	extends FontSizeAction
{
	public ResetFontSize()
	{
		super("TxtEdFntReset", KeyEvent.VK_NUMPAD0, KeyEvent.CTRL_MASK);
		setTooltip(ResourceMgr.getDescription("TxtEdFntReset"));
	}

	public ResetFontSize(FontZoomer fontZoomer)
	{
		super("TxtEdFntReset", KeyEvent.VK_NUMPAD0, KeyEvent.CTRL_MASK);
		setTooltip(ResourceMgr.getDescription("TxtEdFntReset"));
		setZoomer(fontZoomer);
	}
	
	public ResetFontSize(String key, FontZoomer fontZoomer)
	{
		super(key, fontZoomer);
	}

	@Override
	public void doFontChange(FontZoomer fontZoomer)
	{
		fontZoomer.resetFontZoom();
	}
}
