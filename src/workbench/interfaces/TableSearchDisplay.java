/*
 * TableSearchDisplay.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.interfaces;

import java.sql.ResultSet;

/**
 *
 * @author  kellererth
 */
public interface TableSearchDisplay
{
	void setCurrentTable(String aTablename, String aStatement);
	void addResultRow(String aTablename, ResultSet aResult);
	void setStatusText(String aStatustext);
	void searchStarted();
	void searchEnded();
}
