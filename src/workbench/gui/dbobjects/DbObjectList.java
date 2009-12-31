/*
 * DbObjectList.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
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
import workbench.interfaces.Reloadable;

/**
 * @author Thomas Kellerer
 */

public interface DbObjectList
	extends Reloadable
{
	TableIdentifier getObjectTable();
	List<? extends DbObject> getSelectedObjects();
	WbConnection getConnection();
	Component getComponent();
}
