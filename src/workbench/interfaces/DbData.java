/*
 * DbData.java
 *
 * Created on July 2, 2003, 8:27 PM
 */

package workbench.interfaces;

/**
 *
 * @author  thomas
 */
public interface DbData
{
	long addRow();
	void deleteRow();
	boolean startEdit();
	int duplicateRow();
	void endEdit();
}
