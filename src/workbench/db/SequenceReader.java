/*
 * SequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
	CharSequence getSequenceSource(String owner, String sequence);
	void readSequenceSource(SequenceDefinition def);
	
	/**
	 * 	Get a list of sequences for the given owner. The 
	 *  contains objects of type String.
	 */
	List<String> getSequenceList(String owner);
	
	List<SequenceDefinition> getSequences(String owner)
		throws SQLException;
	
	SequenceDefinition getSequenceDefinition(String owner, String sequence);
	DataStore getRawSequenceDefinition(String owner, String sequence);
}
