/*
 * SynonymReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Read the definition of synonyms from the database.
 * @author support@sql-workbench.net
 */
public interface SynonymReader
{
	String getSynonymSource(Connection con, String anOwner, String aSynonym)
			throws SQLException;
	
	TableIdentifier getSynonymTable(Connection con, String anOwner, String aSynonym)
			throws SQLException;
}
