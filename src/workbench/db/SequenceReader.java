/*
 * SequenceReader.java
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
import workbench.storage.DataStore;

/**
 * Read the definition of sequences from the database
 * @author Thomas Kellerer
 */
public interface SequenceReader
{
	/**
	 *	Return a SQL String to recreate the given sequence
	 */
	CharSequence getSequenceSource(String catalog, String schema, String sequence);
	void readSequenceSource(SequenceDefinition def);
	
	/**
	 * 	Get a list of sequences for the given owner. 
	 */
	List<SequenceDefinition> getSequences(String catalogPattern, String schemaPattern, String namePattern)
		throws SQLException;
	
	SequenceDefinition getSequenceDefinition(String catalog, String schema, String sequence);
	DataStore getRawSequenceDefinition(String catalog, String schema, String sequence);
}
