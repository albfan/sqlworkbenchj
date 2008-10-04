/*
 * RowActionMonitor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

/**
 *
 * @author  support@sql-workbench.net
 */
public interface RowActionMonitor
{
	int MONITOR_INSERT = 0;
	int MONITOR_UPDATE = 1;
	int MONITOR_LOAD = 2;
	int MONITOR_EXPORT = 3;
	int MONITOR_COPY = 4;
	int MONITOR_PROCESS_TABLE = 5;
	int MONITOR_PROCESS = 6;
	int MONITOR_PLAIN = 7;
	int MONITOR_DELETE = 8;

	void setMonitorType(int aType);
	int getMonitorType();
	void saveCurrentType(String type);
	void restoreType(String type);
	void setCurrentObject(String object, long number, long totalObjects);
	void setCurrentRow(long currentRow, long totalRows);
	void jobFinished();
}
