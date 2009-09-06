/*
 * SequenceReader.java
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

import java.sql.SQLException;
import java.util.List;
import workbench.storage.DataStore;

/**
 * Read the definition of sequences from the database
 * @author support@sql-workbench.net
 */
public interface SequenceReader
{
	/**
	 *	Return a SQL String to recreate the given sequence
	 */
	CharSequence getSequenceSource(String schema, String sequence);
	void readSequenceSource(SequenceDefinition def);
	
	/**
	 * 	Get a list of sequences for the given owner. 
	 */
	List<SequenceDefinition> getSequences(String schema, String namePattern)
		throws SQLException;
	
	SequenceDefinition getSequenceDefinition(String schema, String sequence);
	DataStore getRawSequenceDefinition(String schema, String sequence);
}
