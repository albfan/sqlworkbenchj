/*
 * ProgressReporter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.interfaces;

/**
 *
 * @author Thomas Kellerer
 */
public interface ProgressReporter
{
	/**
	 * The default progress interval when reporting 
	 * progress of the export.
	 */
	int DEFAULT_PROGRESS_INTERVAL = 10;
	
	/**
	 * Define the interval in which progress messages are displayed.
	 * @param interval the interval, 0 means no progress display
	 */
	void setReportInterval(int interval);
}
