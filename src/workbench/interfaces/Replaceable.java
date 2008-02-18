/*
 * Replaceable.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.interfaces;

/**
 *
 * @author  support@sql-workbench.net
 */
public interface Replaceable

{
	/**
	 *	Initiate the replace Dialog 
	 */
	void replace();
	
	/**
	 *	Find and highlight the first occurance of the String
	 *	@return true if an occurance was found
	 */
	int findFirst(String aValue, boolean ignoreCase, boolean wholeWord, boolean useRegex);

	/**
	 *	Find and highlight the next occurance of the expression
	 *	initially found with findFirst()
	 *
	 *	If findFirst() has not been called before, -1 should be returned.
	 */
	int findNext();
	
	/**
	 *	Replace the currently highlighted (=found) text with the given value
	 */
	boolean replaceCurrent(String aReplacement, boolean useRegex);
	
	
	/**
	 *	Find and replace the next occurance.
	 *  Only valid if findFirst() was called.
	 *	@return true if an occurance was found
	 */
	boolean replaceNext(String aReplacement, boolean useRegex);
	
	/**
	 *	Find and replace all occurances of the given value 
	 *	with replacement.
	 *	@return the number of occurances replaced
	 */
	int replaceAll(String value, String replacement, boolean selectedText, boolean ignoreCase, boolean wholeWord, boolean useRegex);
	
	boolean isTextSelected();
}
