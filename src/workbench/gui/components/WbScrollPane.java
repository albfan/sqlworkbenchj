/*
 * WbScrollPane.java
 *
 * Created on August 9, 2002, 12:11 PM
 */

package workbench.gui.components;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import workbench.gui.WbSwingUtilities;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbScrollPane extends JScrollPane
{

	private static Border scrollBorder = new CompoundBorder(new EmptyBorder(1,1,2,1), WbSwingUtilities.BEVEL_BORDER);
	public static final JPanel CORNER;
	static
	{
		CORNER = new JPanel();
		CORNER.setBackground(Color.RED);
		CORNER.setBorder(new EtchedBorder());
	}
	/** Creates a new instance of WbScrollPane */
	public WbScrollPane()
	{
		super();
		this.initDefaults();
	}

	public WbScrollPane(Component view)
	{
		super(view);
		this.initDefaults();
	}
	public WbScrollPane(Component view, int vsbPolicy, int hsbPolicy)
	{
		super(view, vsbPolicy, hsbPolicy);
		this.initDefaults();
	}
	public WbScrollPane(int vsbPolicy, int hsbPolicy)
	{
		super(vsbPolicy, hsbPolicy);
		this.initDefaults();
	}

	private void initDefaults()
	{
		this.setBorder(scrollBorder);
		//this.setViewportBorder(WbSwingUtilities.EMPTY_BORDER);
		//this.setCorner(JScrollPane.UPPER_RIGHT_CORNER, CORNER);
	}


}
