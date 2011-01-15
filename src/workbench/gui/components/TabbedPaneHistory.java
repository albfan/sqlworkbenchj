/*
 * TabbedPaneHistory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Component;
import java.util.LinkedList;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Thomas Kellerer
 */
public class TabbedPaneHistory
	implements ChangeListener
{
	private JTabbedPane client;
	private LinkedList<Component> history = new LinkedList<Component>();

	public TabbedPaneHistory(JTabbedPane pane)
	{
		client = pane;
		client.addChangeListener(this);
	}

	public void clear()
	{
		history.clear();
	}
	
	private void addComponent(Component comp)
	{
		if (history.contains(comp))
		{
			history.remove(comp);
		}
		history.addFirst(comp);
	}

	/**
	 * Remove the currently active tab (== the first) component from the history
	 * and activate the tab that was used before.
	 */
	public void restoreLastTab()
	{
		Component lastTab = getLastUsedComponent();

		if (lastTab == null) return;

		// remove the current tab from the history
		history.removeFirst();

		try
		{
			client.setSelectedComponent(lastTab);
		}
		catch (IllegalArgumentException e)
		{
			// the component is no longer valid
			history.remove(lastTab);
		}
	}
	
	private Component getLastUsedComponent()
	{
		if (history.size() == 0) return null;
		if (history.size() == 1) return history.get(0);

		// the element at index zero is always the active index
		// so the last used index is at index 1
		return history.get(1);
	}
	
	@Override
	public void stateChanged(ChangeEvent e)
	{
		if (e.getSource() != client) return;
		addComponent(client.getSelectedComponent());
	}

}
