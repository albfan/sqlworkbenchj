/*
 * Created on 27.11.2003
 *
 */
package workbench.interfaces;

/**
 * @author workbench@kellerer.org
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface Commitable
{
	void commit();
	void rollback();
}
