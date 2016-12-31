/*
 * TabbedPaneTraversalPolicy.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
import javax.swing.JTabbedPane;

/**
 *
 * @author  Thomas Kellerer
 */
public class TabbedPaneTraversalPolicy
	extends FocusTraversalPolicy
{

	private JTabbedPane tabPane;

	public TabbedPaneTraversalPolicy(JTabbedPane pane)
	{
		tabPane = pane;
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
		if (focusCycleRoot != tabPane) return null;
		int newindex = tabPane.indexOfComponent(aComponent) + 1;
		if (newindex >= tabPane.getTabCount()) newindex = 0;
		return tabPane.getComponentAt(newindex);
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
		if (focusCycleRoot != tabPane) return null;
		int newindex = tabPane.indexOfComponent(aComponent) - 1;
		if (newindex < 0) newindex = tabPane.getTabCount() - 1;
		return tabPane.getComponentAt(newindex);
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
		return tabPane.getComponentAt(0);
	}

	/**
	 * Returns the first Component in the traversal cycle. This method is used
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
		return tabPane.getComponentAt(0);
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
		return tabPane.getComponentAt(tabPane.getTabCount() - 1);
	}

}
