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
import workbench.gui.editor.InputHandler;
import workbench.gui.editor.JEditTextArea;
import workbench.gui.editor.TextAreaPainter;
import workbench.resource.ResourceMgr;

/**
 *
 * @author Thomas Kellerer
 */
public class ResetFontSize
	extends EditorAction
{
	private JEditTextArea textArea;

	public ResetFontSize()
	{
		super("TxtEdFntReset", KeyEvent.VK_NUMPAD0, KeyEvent.CTRL_MASK);
		setTooltip(ResourceMgr.getDescription("TxtEdFntReset"));
	}

	public ResetFontSize(JEditTextArea area)
	{
		this();
		textArea = area;
	}

	public void actionPerformed(ActionEvent evt)
	{
		JEditTextArea toUse = textArea;
		if (toUse == null)
		{
			toUse = InputHandler.getTextArea(evt);
		}
		TextAreaPainter painter = toUse.getPainter();
		painter.resetFontZoom();
	}
}
