/*
 * WbTraversalPolicy.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author  Thomas Kellerer
 */
public class WbTraversalPolicy
	extends FocusTraversalPolicy
{
	final private List<Component> components = new ArrayList<>();
	private Component defaultComponent = null;

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
		for (Component comp : components)
		{
			if (comp.isEnabled()) return true;
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
	@Override
	public Component getComponentAfter(Container focusCycleRoot, Component aComponent)
	{
		// Make sure we have at least one enabled component
		// otherwise the recursion would never terminate!
		if (!checkAvailable()) return null;

		Component result = null;
		int index = this.components.indexOf(aComponent);

		if (index < 0 || index == this.components.size() - 1)
		{
			result = this.components.get(0);
		}
		else
		{
			result = this.components.get(index + 1);
		}
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
	@Override
	public Component getComponentBefore(Container focusCycleRoot, Component aComponent)
	{
		// Make sure we have at least one enabled component
		// otherwise the recursion would never terminate!
		if (!checkAvailable()) return null;

		Component result = null;
		int index = this.components.indexOf(aComponent);

		if (index <= 0)
		{
			result = this.components.get(this.components.size() - 1);
		}
		else
		{
			result = this.components.get(index - 1);
		}

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
	@Override
	public Component getDefaultComponent(Container focusCycleRoot)
	{
		if (this.defaultComponent != null) return this.defaultComponent;
		if (this.components.size() > 0)
		{
			return this.components.get(0);
		}
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
	@Override
	public Component getFirstComponent(Container focusCycleRoot)
	{
		if (this.components.size() > 0)
		{
			return this.components.get(0);
		}
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
	@Override
	public Component getLastComponent(Container focusCycleRoot)
	{
		if (this.components.size() > 0)
		{
			return this.components.get(this.components.size() - 1);
		}
		return null;
	}

}
