/*
 * ExecutionController.java
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
 * @author  Thomas Kellerer
 */
public interface ExecutionController
{

	/**
	 * Confirm the execution of passed SQL command.
	 *
	 * @return true if the user chose to continue
	 */
	boolean confirmStatementExecution(String command);

	/**
	 * Confirm the execution of the statements with a user visible prompt.
	 * This is similar to the "pause" command in a Windows batch file.
	 *
	 * @param prompt the prompt to be displayed to the user
	 * @return true if the user chose to continue
	 */
	boolean confirmExecution(String prompt);
	
	String getPassword(String prompt);
}
