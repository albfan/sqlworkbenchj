/*
 * TabCloser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

/**
 *
 * @author Thomas Kellerer
 */
public interface TabCloser
{
	boolean canCloseTab(int index);
	void tabCloseButtonClicked(int index);
}
