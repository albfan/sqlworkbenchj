/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;
import workbench.gui.components.WbTable;
import javax.swing.InputMap;
import javax.swing.ActionMap;

/**
 *	@author  workbench@kellerer.org
 */
public class OptimizeAllColumnsAction extends WbAction
{
	private WbTable client;

	public OptimizeAllColumnsAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtOptimizeAllCol",KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_MASK));
	}

	public void disableShortcut()
	{
		this.setAccelerator(null);
	}

	public void executeAction(ActionEvent e)
	{
		boolean shiftPressed = ((e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK);
		this.client.optimizeAllColWidth(shiftPressed);
	}

	public void addToInputMap(InputMap im, ActionMap am)
	{
		super.addToInputMap(im, am);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK), this.getActionName());
	}

}