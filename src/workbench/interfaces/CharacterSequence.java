/*
 * CharacterSequence.java
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
 * An interface to get parts of a character source.
 * 
 * I'm not using CharSequence because I need the {@link #done()} method 
 * to cleanup any resource that were used by the sequence.
 * 
 * The IteratingParser uses a FileMappedSequence which used NIO
 * to read the characters and this implementation needs a cleanup
 * method to close the file handles, which would not be offered by 
 * the CharSequence interface.
 * 
 * @author support@sql-workbench.net
 */
public interface CharacterSequence
{
	/**
	 * return the substring define by start and end
	 */
	String substring(int start, int end);
	
	/**
	 * return the character at the given position
	 */
	char charAt(int index);
	
	/**
	 * Release any resources used by the CharacterSequence
	 */
	void done();
}
