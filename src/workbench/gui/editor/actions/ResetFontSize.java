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
package workbench.gui.editor.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import workbench.gui.components.FontZoomer;
import workbench.gui.editor.InputHandler;
import workbench.gui.editor.JEditTextArea;
import workbench.resource.ResourceMgr;

/**
 *
 * @author Thomas Kellerer
 */
public class ResetFontSize
	extends EditorAction
{
	private FontZoomer zoomer;

	public ResetFontSize()
	{
		super("TxtEdFntReset", KeyEvent.VK_NUMPAD0, KeyEvent.CTRL_MASK);
		setTooltip(ResourceMgr.getDescription("TxtEdFntReset"));
	}

	public ResetFontSize(FontZoomer fontZoomer)
	{
		super("TxtEdFntReset", KeyEvent.VK_NUMPAD0, KeyEvent.CTRL_MASK);
		setTooltip(ResourceMgr.getDescription("TxtEdFntReset"));
		zoomer = fontZoomer;
	}
	
	public ResetFontSize(String key, FontZoomer fontZoomer)
	{
		this();
		setAccelerator(null);
		setMenuTextByKey(key);
		setTooltip(ResourceMgr.getDescription(key));
		zoomer = fontZoomer;
	}

	public void actionPerformed(ActionEvent evt)
	{
		FontZoomer toUse = zoomer;
		if (toUse == null)
		{
			JEditTextArea text = InputHandler.getTextArea(evt);
			toUse = text.getFontZoomer();
		}
		toUse.resetFontZoom();
	}
}
