/*
 * ClipboardSupport.java
 *
 * Created on December 2, 2001, 1:40 AM
 */

package workbench.interfaces;

/**
 *
 * @author  workbench@kellerer.org
 */
public interface ClipboardSupport
{
	/**
	 *	Copy the currently selected contents into the clipboard
	 */
	void copy();

	/**
	 *	Select the entire Text
	 */
	void selectAll();

	/**
	 *	Delete the currently selected text without copying it
	 *	into the system clipboard
	 */
	void clear();

	/**
	 *	Delete the currently selected text and put it into
	 *	the clipboard
	 */
	void cut();

	/**
	 *	Paste the contents of the clipboard into
	 *	the component
	 */
	void paste();
}

