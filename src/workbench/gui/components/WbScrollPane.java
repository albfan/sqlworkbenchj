/*
 * WbScrollPane.java
 *
 * Created on August 9, 2002, 12:11 PM
 */

package workbench.gui.components;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import workbench.gui.WbSwingUtilities;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbScrollPane extends JScrollPane
{
  private static final Border MY_BORDER = new CompoundBorder(WbSwingUtilities.BEVEL_BORDER, new EmptyBorder(0,1,0,0));
	public static final JComponent CORNER;
	static
	{
		CORNER = new JPanel();
		//CORNER.setBackground(Color.RED);
		CORNER.setBorder(new DividerBorder(DividerBorder.LEFT));
		//CORNER.setPreferredSize(new Dimension(15,15));
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
    this.setBorder(MY_BORDER);
		//this.setBorder(WbSwingUtilities.BEVEL_BORDER);
		//this.setViewportBorder(WbSwingUtilities.EMPTY_BORDER);
	}
	
	public void updateCorner()
	{
		JViewport header = this.getColumnHeader();
		Dimension visiblesize = header.getExtentSize();
		Dimension realsize = header.getViewSize();
		if (visiblesize.getWidth() < realsize.getWidth())
		{
			this.setCorner(JScrollPane.UPPER_RIGHT_CORNER, CORNER);
		}
		else
		{
			this.setCorner(JScrollPane.UPPER_RIGHT_CORNER, CORNER);
		}
	}

}
