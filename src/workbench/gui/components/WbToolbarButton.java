/*
 * WbToolbarButton.java
 *
 * Created on 10. Juli 2002, 10:37
 */

package workbench.gui.components;

import java.awt.Insets;

import javax.swing.Action;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbToolbarButton extends javax.swing.JButton
{

	public static final Insets MARGIN = new Insets(1,1,1,1);

	public WbToolbarButton()
	{
		super();
	}

	public WbToolbarButton(String aText)
	{
		super(aText);
		this.setMargin(MARGIN);
	}
	public WbToolbarButton(Action a)
	{
		super(a);
		this.setText(null);
		this.setMargin(MARGIN);
	}

	public void setAction(Action a)
	{
		super.setAction(a);
		this.setMargin(MARGIN);
		this.setText(null);
	}
}
