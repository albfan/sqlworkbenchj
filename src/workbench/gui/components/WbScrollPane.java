/*
 * WbScrollPane.java
 *
 * Created on August 9, 2002, 12:11 PM
 */

package workbench.gui.components;

import java.awt.Component;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

/**
 *
 * @author  sql.workbench@freenet.de
 */
public class WbScrollPane extends JScrollPane
{
	
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
		Border scrollBorder = new CompoundBorder(new EtchedBorder(), new EmptyBorder(1,1,1,1) );
		this.setBorder(scrollBorder);
	}
	
	
}
