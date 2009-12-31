/*
 * Committer.java
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
public interface Committer
{
	/**
	 * The integer value that identifies the fact that no COMMIT statement
	 * at all should be written to the output file
	 */
	static final int NO_COMMIT_FLAG = Integer.MIN_VALUE;
	
	/** 
	 * Define the interval when commits should be send to the DBMS
	 * @param interval the number of statement after which to commit. 0 means each statement
	 */
	void setCommitEvery(int interval);
	
	/**
	 * Never commit anything
	 */
	void commitNothing();
	
}
