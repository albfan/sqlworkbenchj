/*
 * SaveClient.java
 *
 * Created on 5. Juli 2002, 23:07
 */

package workbench.interfaces;

/**
 *
 * @author  workbench@kellerer.org
 */
public interface FileActions
{
	void saveItem() throws Exception;
	void deleteItem() throws Exception;
	void newItem(boolean copyCurrent) throws Exception;
}