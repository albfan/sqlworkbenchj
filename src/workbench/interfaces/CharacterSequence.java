/*
 * CharacterSequence.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.interfaces;

/**
 * An interface to get parts of a character source
 * @author tkellerer
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
