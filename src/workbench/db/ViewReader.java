/*
 * NewInterface
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.db;

import java.sql.SQLException;

/**
 *
 * @author Thomas Kellerer
 */
public interface ViewReader
{

	CharSequence getExtendedViewSource(TableIdentifier tbl)
		throws SQLException;

	CharSequence getExtendedViewSource(TableIdentifier tbl, boolean includeDrop)
		throws SQLException;

	/**
	 * Returns a complete SQL statement to (re)create the given view.
	 *
	 * This method will extend the stored source to a valid CREATE VIEW.
	 *
	 * @param view The view for which thee source should be created
	 * @param includeCommit if true, terminate the whole statement with a COMMIT
	 * @param includeDrop if true, add a DROP statement before the CREATE statement
	 *
	 * @see #getViewSource(workbench.db.TableIdentifier)
	 */
	CharSequence getExtendedViewSource(TableDefinition view, boolean includeDrop, boolean includeCommit)
		throws SQLException;

	/**
	 * Return the source of a view definition as it is stored in the database.
	 * <br/>
	 * Usually (depending on how the meta data is stored in the database) the DBMS
	 * only stores the underlying SELECT statement (but not a full CREATE VIEW),
	 * and that will be returned by this method.
	 * <br/>
	 * To create a complete SQL to re-create a view, use {@link #getExtendedViewSource(workbench.db.TableIdentifier) }
	 *
	 * @return the view source as stored in the database.
	 */
	CharSequence getViewSource(TableIdentifier viewId);

}
