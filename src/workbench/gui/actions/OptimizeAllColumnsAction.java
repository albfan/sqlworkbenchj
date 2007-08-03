/*
 * OptimizeAllColumnsAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;
import workbench.gui.components.TableColumnOptimizer;

import workbench.gui.components.WbTable;
import workbench.resource.Settings;
import workbench.util.WbThread;

/**
 *	@author  support@sql-workbench.net
 */
public class OptimizeAllColumnsAction 
	extends WbAction
{
	protected TableColumnOptimizer optimizer;

	public OptimizeAllColumnsAction(WbTable client)
	{
		super();
		this.setClient(client);
		this.initMenuDefinition("MnuTxtOptimizeAllCol",KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_MASK));
	}

	public void disableShortcut()
	{
		this.setAccelerator(null);
	}

	public void executeAction(ActionEvent e)
	{
		if (optimizer == null) return;
		final boolean shiftPressed = isShiftPressed(e);
		Thread t = new WbThread("OptimizeAllCols Thread") 
		{ 	
			public void run()	
			{ 
				optimizer.optimizeAllColWidth(shiftPressed || Settings.getInstance().getIncludeHeaderInOptimalWidth()); 
			}  
		};
		t.start();
	}

	public boolean hasShiftModifier() { return true; }
	
	public void setClient(WbTable client)
	{
		this.optimizer = (client != null ? new TableColumnOptimizer(client) : null);
		this.setEnabled(client != null);
	}
}
