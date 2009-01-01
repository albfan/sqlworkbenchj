/*
 * ResettableSplitPane.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.util.HashSet;
import java.util.Set;
import workbench.gui.components.WbSplitPane;
import workbench.interfaces.Resettable;

/**
 * @author support@sql-workbench.net
 */
public class ResettableSplitPane
	extends WbSplitPane
	implements Resettable
{
	private Set<Resettable> clients = new HashSet<Resettable>();
	
	public ResettableSplitPane(int type)
	{
		super(type);
	}
	
	public void addClient(Resettable r)
	{
		clients.add(r);
	}
	
	public void reset()
	{
		for (Resettable r : clients)
		{
			r.reset();
		}
	}
	
	
}
