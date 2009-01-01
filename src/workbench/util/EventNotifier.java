/*
 * EventNotifier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.util.LinkedList;
import java.util.List;
import workbench.interfaces.EventDisplay;

/**
 *
 * @author support@sql-workbench.net
 */
public class EventNotifier 
{
	private List<EventDisplay> displayClients = new LinkedList<EventDisplay>();
	private NotifierEvent lastEvent = null;
	private static EventNotifier instance = new EventNotifier();
	
	private EventNotifier()
	{
	}

	public static EventNotifier getInstance() 
	{
		return instance;
	}
	
	public synchronized void addEventDisplay(EventDisplay d)
	{
		displayClients.add(d);
		if (this.lastEvent != null)
		{
			d.showAlert(lastEvent);
		}
	}
	
	public synchronized void removeEventDisplay(EventDisplay d)
	{
		displayClients.remove(d);
	}

	public synchronized void displayNotification(NotifierEvent e)
	{
		this.lastEvent = e;
		for (EventDisplay d : displayClients)
		{
			d.showAlert(e);
		}
	}

	public synchronized void removeNotification()
	{
		for (EventDisplay d : displayClients)
		{
			d.removeAlert();
		}
	}
	
}
