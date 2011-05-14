/*
 * SynonymReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
import java.util.List;

/**
 * Read the definition of synonyms from the database.
 *
 * @author Thomas Kellerer
 */
public interface SynonymReader
{
	public static final String SYN_TYPE_NAME = "SYNONYM";

	String getSynonymSource(WbConnection con, String schema, String aSynonym)
			throws SQLException;

	TableIdentifier getSynonymTable(WbConnection con, String schema, String aSynonym)
			throws SQLException;

	List<TableIdentifier> getSynonymList(WbConnection con, String schema, String namePattern)
		throws SQLException;
}
