/*
 * TableSearchDisplay.java
 *
 * Created on October 4, 2002, 10:29 AM
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
