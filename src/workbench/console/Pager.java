/*
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 * 
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 * 
 */

package workbench.console;

/**
 *
 * @author support@sql-workbench.net
 */
public interface Pager
{
	/**
	 * Controls the display of a single line.
	 * If paging is enabled the call will not return unless
	 * either the current page size is not reached, or the user
	 * has confirmed to print the next row.
	 *
	 * @param lineNumber the next line to print
	 * @return true, print the next line, false the caller should cancel printing
	 */
	boolean canPrintLine(int lineNumber);
}
