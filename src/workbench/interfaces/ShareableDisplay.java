/*
 * ShareableDisplay.java
 *
 * Created on 2. November 2002, 14:58
 */

package workbench.interfaces;

import javax.swing.JTable;

/**
 *
 * @author  workbench@kellerer.org
 */
public interface ShareableDisplay
	extends Reloadable
{
	void addTableListDisplayClient(JTable aClient);
	void removeTableListDisplayClient(JTable aClient);
}
