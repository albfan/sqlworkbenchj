/*
 * TableSearchDisplay.java
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

import workbench.db.TableIdentifier;
import workbench.storage.DataStore;

/**
 * @author support@sql-workbench.net  
 */ 
public interface TableSearchDisplay
{
	void setCurrentTable(String aTablename, String aStatement);
	void error(String msg);
	//void addResultRow(TableIdentifier table, ResultSet aResult);
	void tableSearched(TableIdentifier table, DataStore result);
	void setStatusText(String aStatustext);
	void searchStarted();
	void searchEnded();
}
