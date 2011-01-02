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
public class DecreaseFontSize
	extends EditorAction
{
	private JEditTextArea textArea;

	public DecreaseFontSize()
	{
		super("TxtEdFntDecr", KeyEvent.VK_SUBTRACT, KeyEvent.CTRL_MASK);
		setTooltip(ResourceMgr.getDescription("TxtEdFntDecr"));
	}

	public DecreaseFontSize(JEditTextArea area)
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
		painter.decreaseFontSize();
	}
}
