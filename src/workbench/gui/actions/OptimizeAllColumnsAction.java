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

/**
 *	@author  workbench@kellerer.org
 */
public class OptimizeAllColumnsAction extends WbAction
{
	private ActionListener client;

	public OptimizeAllColumnsAction(ActionListener aClient)
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
		e.setSource(this);
		this.client.actionPerformed(e);
	}
}
