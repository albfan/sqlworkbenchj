/*
 * OptimizeAllColumnsAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import workbench.gui.components.WbTable;
import workbench.resource.Settings;
import workbench.util.WbThread;

/**
 *	@author  support@sql-workbench.net
 */
public class OptimizeAllColumnsAction 
	extends WbAction
{
	protected WbTable client;

	public OptimizeAllColumnsAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtOptimizeAllCol",KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_MASK));
	}

	public void disableShortcut()
	{
		this.setAccelerator(null);
	}

	public void executeAction(ActionEvent e)
	{
		if (client == null) return;
		final boolean shiftPressed = ((e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK);
		Thread t = new WbThread("OptimizeAllCols Thread") 
		{ 	
			public void run()	
			{ 
				client.optimizeAllColWidth(shiftPressed || Settings.getInstance().getIncludeHeaderInOptimalWidth()); 
			}  
		};
		t.start();
	}

	public boolean hasShiftModifier() { return true; }
	
	public void setClient(WbTable c)
	{
		this.client = c;
		this.setEnabled(this.client != null);
	}
}
