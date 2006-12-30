/*
 * ParameterPrompter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.interfaces;

/**
 *
 * @author support@sql-workbench.net
 */
public interface ParameterPrompter
{
	/** 
	 * Process the given SQL and prompt for any needed 
	 * parameters. Either WB parameters or prepared statements
	 * @return true - continue processing 
	 *         false - do not run that statement
	 */
	boolean processParameterPrompts(String sql);
}
