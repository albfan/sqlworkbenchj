/*
 * NotifierEvent.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.awt.event.ActionListener;

/**
 * @author support@sql-workbench.net
 */
public class NotifierEvent
{
	private String iconKey;
	private String tooltip;
	private ActionListener handler;
	
	public NotifierEvent(String key, String tip, ActionListener l)
	{
		this.iconKey = key;
		this.tooltip = tip;
		this.handler = l;
	}
	
	public String getIconKey()
	{
		return iconKey;
	}
	
	public String getTooltip()
	{
		return tooltip;
	}
	
	public ActionListener getHandler()
	{
		return handler;
	}
	
	
}
