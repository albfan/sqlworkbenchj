/*
 * Searchable.java
 *
 * Created on August 8, 2002, 9:08 AM
 */

package workbench.interfaces;

/**
 *
 * @author  workbench@kellerer.org
 */
public interface Replaceable

{
	/**
	 *	Initiate the replace Dialog 
	 */
	void replace();
	
	/**
	 *	Find and highlight the first occurance of the String
	 *	@returns true if an occurance was found
	 */
	int findFirst(String aValue, boolean ignoreCase, boolean wholeWord);

	/**
	 *	Find and highlight the next occurance of the expression
	 *	initially found with findFirst()
	 *
	 *	If findFirst() has not been called before, -1 should be returned.
	 */
	int findNext();
	
	/**
	 *	Find and highlight the next occurance of the given search string
	 *	If the parameters passed to this method differ from the 
	 *	last call to findFirst() an implicit findFirst() is issued
	 */
	int find(String aValue, boolean ignoreCase, boolean wholeWord);
	
	/**
	 *	Replace the currently highlighted (=found) text with the given value
	 */
	boolean replaceCurrent(String aReplacement);
	
	
	/**
	 *	Find and replace the next occurance.
	 *  Only valid if findFirst() was called.
	 *	@returns true if an occurance was found
	 */
	boolean replaceNext(String aReplacement);
	
	/**
	 *	Find and replace all occurances of the given value 
	 *	with replacement.
	 *	@return the number of occurances replaced
	 */
	int replaceAll(String value, String replacement, boolean selectedText, boolean ignoreCase, boolean wholeWord);
	
	boolean isTextSelected();
}
