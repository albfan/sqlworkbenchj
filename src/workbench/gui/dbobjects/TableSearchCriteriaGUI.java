/*
 * TableSearchCriteriaGUI.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.event.KeyListener;
import workbench.db.search.TableDataSearcher;
import workbench.interfaces.PropertyStorage;

/**
 *
 * @author Thomas Kellerer
 */
public interface TableSearchCriteriaGUI
{
	void disableControls();
	void enableControls();

	/**
	 * Return a TableDataSearcher initialized
	 * according to the selection of the user in the panel
	 */
	TableDataSearcher getSearcher();
	
	void saveSettings(String prefix, PropertyStorage props);
	void restoreSettings(String prefix, PropertyStorage props);

	void addKeyListenerForCriteria(KeyListener listener);
}
