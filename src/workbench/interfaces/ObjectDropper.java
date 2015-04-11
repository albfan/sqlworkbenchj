/*
 * ObjectDropper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.interfaces;

import java.sql.SQLException;
import java.util.List;

import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.RowActionMonitor;

/**
 *
 * @author Thomas Kellerer
 */
public interface ObjectDropper
{
	boolean supportsCascade();
	boolean supportsFKSorting();
	void setCascade(boolean flag);
	void setConnection(WbConnection con);
	WbConnection getConnection();
	void setObjectTable(TableIdentifier tbl);

	void setObjects(List<? extends DbObject> objects);
	List<? extends DbObject> getObjects();

	void dropObjects()
		throws SQLException;

	void cancel()
		throws SQLException;

	void setRowActionMonitor(RowActionMonitor monitor);

	CharSequence getScript();
	CharSequence getDropForObject(DbObject toDrop);
  boolean supportsObject(DbObject object);
}
