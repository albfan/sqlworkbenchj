/*
 * TableSearcher
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.search;

import java.util.List;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.interfaces.TableSearchDisplay;
import workbench.storage.filter.ColumnExpression;

/**
 *
 * @author Thomas Kellerer
 */
public interface TableSearcher
{

	void cancelSearch();

	String getCriteria();

	boolean isRunning();

	void search();

	void startBackgroundSearch();

	void setConnection(WbConnection conn);

	void setCriteria(String search);

	void setDisplay(TableSearchDisplay searchDisplay);

	void setExcludeLobColumns(boolean flag);

	void setMaxRows(int max);

	void setTableNames(List<TableIdentifier> tables);

	ColumnExpression getSearchExpression();

}
