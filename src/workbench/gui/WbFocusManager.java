/*
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 * 
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 * 
 */
package workbench.gui;

import java.awt.Component;
import java.awt.event.KeyEvent;
import javax.swing.DefaultFocusManager;
import javax.swing.KeyStroke;
import workbench.gui.actions.WbAction;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbFocusManager
	extends DefaultFocusManager
{
	private WbAction nextTab;
	private WbAction prevTab;

	private static final WbFocusManager instance = new WbFocusManager();
	
	public static WbFocusManager getInstance()
	{
		return instance;
	}

	private WbFocusManager()
	{

	}
	public void grabActions(WbAction next, WbAction prev)
	{
		nextTab = next;
		prevTab = prev;
	}

	public void processKeyEvent(Component focusedComponent, KeyEvent anEvent)
	{
		KeyStroke key = KeyStroke.getKeyStrokeForEvent(anEvent);

		if (nextTab != null && nextTab.getAccelerator().equals(key))
		{
			anEvent.consume();
			nextTab.executeAction(null);
		}
		else if (prevTab != null && prevTab.getAccelerator().equals(key))
		{
			anEvent.consume();
			prevTab.executeAction(null);
		}
		else
		{
			super.processKeyEvent(focusedComponent, anEvent);
		}
	}
}
