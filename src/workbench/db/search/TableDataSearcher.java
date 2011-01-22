/*
 * TableDataSearcher.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.search;

import java.util.List;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.interfaces.TableSearchConsumer;
import workbench.storage.filter.ColumnExpression;

/**
 *
 * @author Thomas Kellerer
 */
public interface TableDataSearcher
{

	void cancelSearch();

	String getCriteria();

	boolean isRunning();

	void search();

	void startBackgroundSearch();

	void setConnection(WbConnection conn);

	void setCriteria(String search, boolean ignoreCase);

	void setConsumer(TableSearchConsumer consumer);

	void setExcludeLobColumns(boolean flag);

	void setMaxRows(int max);

	void setTableNames(List<TableIdentifier> tables);

	ColumnExpression getSearchExpression();

}
