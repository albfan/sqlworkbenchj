/*
 * TableSearchConsumer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.interfaces;

import workbench.db.TableIdentifier;
import workbench.storage.DataStore;

/**
 * @author Thomas Kellerer  
 */ 
public interface TableSearchConsumer
{
	void setCurrentTable(String aTablename, String aStatement);
	void error(String msg);
	void tableSearched(TableIdentifier table, DataStore result);
	void setStatusText(String aStatustext);
	void searchStarted();
	void searchEnded();
}
