/*
 * DbObjectList.java
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
import java.awt.Component;
import java.util.List;
import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

/**
 * @author support@sql-workbench.net
 */

public interface DbObjectList 
{
	TableIdentifier getObjectTable();
	List<? extends DbObject> getSelectedObjects();
	WbConnection getConnection();
	Component getComponent();
}
