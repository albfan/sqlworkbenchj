/*
 * WbToolbar.java
 *
 * Created on 16. Juli 2002, 12:33
 */

package workbench.gui.components;

import javax.swing.Action;
import javax.swing.JButton;

/**
 *
 * @author  sql.workbench@freenet.de
 */
public class WbToolbar
	extends javax.swing.JToolBar
{

	/** Creates a new instance of WbToolbar */
	public WbToolbar()
	{
		this.setFloatable(false);
		this.setRollover(true);
	}

	public JButton add(Action a)
	{
		WbToolbarButton button = new WbToolbarButton(a);
		this.add(button);
		return button;
	}

	public void addSeparator()
	{
		if (isRollover())
			this.add(new WbToolbarSeparator());
		else
			this.addSeparator();
	}
}
