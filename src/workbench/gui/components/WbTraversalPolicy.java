/*
 * WbTraversalPolicy.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbTraversalPolicy
	extends FocusTraversalPolicy
{
	private ArrayList components = new ArrayList();
	private Component defaultComponent = null;

	public WbTraversalPolicy()
	{
	}

	public void setDefaultComponent(Component aComp)
	{
		this.defaultComponent = aComp;
	}

	public void addComponent(Component aComp)
	{
		if (!this.components.contains(aComp))
		{
			this.components.add(aComp);
		}
	}

	private boolean checkAvailable()
	{
		if (components.size() == 0) return false;
		Iterator itr = components.iterator();
		while (itr.hasNext())
		{
			Component c = (Component)itr.next();
			if (c.isEnabled()) return true;
		}
		return false;
	}
	
	/** 
	 * Returns the Component that should receive the focus after aComponent.
	 * focusCycleRoot must be a focus cycle root of aComponent.
	 *
	 * @param focusCycleRoot a focus cycle root of aComponent
	 * @param aComponent a (possibly indirect) child of focusCycleRoot, or
	 *        focusCycleRoot itself
	 * @return the Component that should receive the focus after aComponent, or
	 *         null if no suitable Component can be found
	 * @throws IllegalArgumentException if focusCycleRoot is not a focus cycle
	 *         root of aComponent, or if either focusCycleRoot or aComponent is
	 *         null
	 *
	 */
	public Component getComponentAfter(Container focusCycleRoot, Component aComponent)
	{
		// Make sure we have at least one enabled component
		// otherwise the recursion would never terminate!
		if (!checkAvailable()) return null;
		
		int index = this.components.indexOf(aComponent);
		Component result = null;
		if (index < 0 || index == this.components.size() - 1) 
			result = (Component)this.components.get(0);
		else 
			result = (Component)this.components.get(index + 1);
		if (result.isEnabled())
		{
			return result;
		}
		return getComponentAfter(focusCycleRoot, result);
	}

	/** 
	 * Returns the Component that should receive the focus before aComponent.
	 * focusCycleRoot must be a focus cycle root of aComponent.
	 *
	 * @param focusCycleRoot a focus cycle root of aComponent
	 * @param aComponent a (possibly indirect) child of focusCycleRoot, or
	 *        focusCycleRoot itself
	 * @return the Component that should receive the focus before aComponent,
	 *         or null if no suitable Component can be found
	 * @throws IllegalArgumentException if focusCycleRoot is not a focus cycle
	 *         root of aComponent, or if either focusCycleRoot or aComponent is
	 *         null
	 *
	 */
	public Component getComponentBefore(Container focusCycleRoot, Component aComponent)
	{
		// Make sure we have at least one enabled component
		// otherwise the recursion would never terminate!
		if (!checkAvailable()) return null;
		
		int index = this.components.indexOf(aComponent);
		
		Component result = null;
		if (index <= 0) 
			result = (Component)this.components.get(this.components.size() - 1);
		else 
			result = (Component)this.components.get(index - 1);
		
		if (result.isEnabled())
		{
			return result;
		}
		return getComponentBefore(focusCycleRoot, result);
	}

	/** Returns the default Component to focus. This Component will be the first
	 * to receive focus when traversing down into a new focus traversal cycle
	 * rooted at focusCycleRoot.
	 *
	 * @param focusCycleRoot the focus cycle root whose default Component is to
	 *        be returned
	 * @return the default Component in the traversal cycle when focusCycleRoot
	 *         is the focus cycle root, or null if no suitable Component can
	 *         be found
	 * @throws IllegalArgumentException if focusCycleRoot is null
	 *
	 */
	public Component getDefaultComponent(Container focusCycleRoot)
	{
		if (this.defaultComponent != null) return this.defaultComponent;
		if (this.components.size() > 0)
			return (Component)this.components.get(0);
		else
			return null;
	}

	/** Returns the first Component in the traversal cycle. This method is used
	 * to determine the next Component to focus when traversal wraps in the
	 * forward direction.
	 *
	 * @param focusCycleRoot the focus cycle root whose first Component is to
	 *        be returned
	 * @return the first Component in the traversal cycle when focusCycleRoot
	 *         is the focus cycle root, or null if no suitable Component can be
	 *         found
	 * @throws IllegalArgumentException if focusCycleRoot is null
	 *
	 */
	public Component getFirstComponent(Container focusCycleRoot)
	{
		if (this.components.size() > 0)
			return (Component)this.components.get(0);
		else
			return null;
	}

	/** Returns the last Component in the traversal cycle. This method is used
	 * to determine the next Component to focus when traversal wraps in the
	 * reverse direction.
	 *
	 * @param focusCycleRoot the focus cycle root whose last Component is to be
	 *         returned
	 * @return the last Component in the traversal cycle when focusCycleRoot is
	 *         the focus cycle root, or null if no suitable Component can be
	 *         found
	 * @throws IllegalArgumentException if focusCycleRoot is null
	 *
	 */
	public Component getLastComponent(Container focusCycleRoot)
	{
		if (this.components.size() > 0)
			return (Component)this.components.get(this.components.size() - 1);
		else
			return null;
	}

}
