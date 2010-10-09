/*
 * StatementParameterPrompter
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.interfaces;

import workbench.sql.preparedstatement.StatementParameters;

public interface StatementParameterPrompter
{
	/**
	 * Prompt the user for values of the named parameters.
	 *
	 * @param parms the parameters identified by the caller
	 * @param showNames true if then parameter names should be displayed to the user
	 * @return
	 */
	boolean showParameterDialog(final StatementParameters parms, final boolean showNames);
}
