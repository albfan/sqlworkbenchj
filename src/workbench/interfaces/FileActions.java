/*
 * SaveClient.java
 *
 * Created on 5. Juli 2002, 23:07
 */

package workbench.interfaces;

import workbench.exception.WbException;

/**
 *
 * @author  thomas.kellerer@web.de
 */
public interface FileActions
{
	void saveItem() throws WbException;
	void deleteItem() throws WbException;
	void newItem() throws WbException;
}
