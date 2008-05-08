/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.interfaces;

/**
 *
 * @author support@sql-workbench.net
 */
public interface Moveable
{
	void startMove();
	void endMove(int finalIndex);
	void moveTab(int oldIndex, int newIndex);
}
