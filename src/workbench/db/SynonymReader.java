/*
 * SynonymReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Read the definition of synonyms from the database.
 * 
 * @author support@sql-workbench.net
 */
public interface SynonymReader
{
	String getSynonymSource(WbConnection con, String anOwner, String aSynonym)
			throws SQLException;
	
	TableIdentifier getSynonymTable(WbConnection con, String anOwner, String aSynonym)
			throws SQLException;
	
	List<String> getSynonymList(WbConnection con, String owner)
		throws SQLException;
}
