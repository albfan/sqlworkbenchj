/*
 * DbObjectDropper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.interfaces;

import java.sql.SQLException;
import java.util.List;
import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

/**
 *
 * @author support@sql-workbench.net
 */
public interface ObjectDropper
{
	boolean supportsCascade();
	void setCascade(boolean flag);
	void setConnection(WbConnection con);
	void setObjectTable(TableIdentifier tbl);
	
	void setObjects(List<? extends DbObject> objects);
	List<? extends DbObject> getObjects();
	
	void dropObjects()
		throws SQLException;
	
	void cancel()
		throws SQLException;
}
