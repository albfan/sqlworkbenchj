/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import workbench.gui.components.WbTable;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  workbench@kellerer.org
 */
public class OptimizeColumnWidthAction extends WbAction
{
	private ActionListener client;
	private boolean shiftPressed = false;

	public OptimizeColumnWidthAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		this.setMenuTextByKey("MnuTxtOptimizeCol");
	}

	public boolean includeColumnLabels()
	{
		return this.shiftPressed;
	}

	public void executeAction(ActionEvent e)
	{
		this.shiftPressed = ((e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK);
		e.setSource(this);
		this.client.actionPerformed(e);
		this.shiftPressed = false;
	}
}