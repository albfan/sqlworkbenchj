/*
 * RowActionMonitor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

/**
 *
 * @author  Thomas Kellerer
 */
public interface RowActionMonitor
{
	/** The monitor type to show running INSERT statements */
	int MONITOR_INSERT = 0;

	/** The monitor type to show running UPDATE statements */
	int MONITOR_UPDATE = 1;

	/** The monitor type to show loading of data */
	int MONITOR_LOAD = 2;

	/** The monitor type to show exporting of data */
	int MONITOR_EXPORT = 3;

	/** The monitor type to show copying of data */
	int MONITOR_COPY = 4;

	/** The monitor type to show that one table (of several) is being processed */
	int MONITOR_PROCESS_TABLE = 5;

	/** A generic monitor type to show progress for a process that knows how many steps it will have */
	int MONITOR_PROCESS = 6;

	/** A generic monitor type to show progress for a process does not know the number of items to be processed */
	int MONITOR_PLAIN = 7;

	/** The monitor type to show deleting of data */
	int MONITOR_DELETE = 8;

	void setMonitorType(int aType);
	int getMonitorType();
	void saveCurrentType(String type);
	void restoreType(String type);
	void setCurrentObject(String object, long number, long totalObjects);
	void setCurrentRow(long currentRow, long totalRows);
	void jobFinished();
}
