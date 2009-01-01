/*
 * ShareableDisplay.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.interfaces;

import javax.swing.JTable;

/**
 *
 * @author  support@sql-workbench.net
 */
public interface ShareableDisplay
	extends Reloadable
{
	void addTableListDisplayClient(JTable aClient);
	void removeTableListDisplayClient(JTable aClient);
}
