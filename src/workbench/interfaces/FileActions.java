/*
 * SaveClient.java
 *
 * Created on 5. Juli 2002, 23:07
 */

package workbench.interfaces;

import workbench.exception.WbException;

/**
 *
 * @author  workbench@kellerer.org
 */
public interface FileActions
{
	void saveItem() throws WbException;
	void deleteItem() throws WbException;
	void newItem(boolean copyCurrent) throws WbException;
}
