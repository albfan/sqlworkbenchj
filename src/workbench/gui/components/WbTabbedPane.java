/*
 * WbTabbedPane.java
 *
 * Created on November 5, 2003, 9:28 AM
 */

package workbench.gui.components;

import java.awt.Graphics;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbTabbedPane
	extends JTabbedPane
{
	
	private boolean suspendRepaint = false;
	
	/** Creates a new instance of WbTabbedPane */
	public WbTabbedPane()
	{
		super();
		this.putClientProperty("jgoodies.noContentBorder", Boolean.TRUE);
	}
	
	public synchronized void setSuspendRepaint(boolean aFlag)
	{
		boolean suspend = this.suspendRepaint;
		this.suspendRepaint = aFlag;
		
		// if repainting was re-enabled, then queue
		// a repaint event right away
		// I'm using invokeLater() to make sure, that 
		// this is executed on the AWT thread.
		if (suspend && !aFlag)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					invalidate();
					repaint();
				}
			});
		}
	}
	
	public void repaint()
	{
		if (this.suspendRepaint) return;
		super.repaint();
	}
	
//	public void paintComponent(Graphics g)
//	{
//		if (this.suspendRepaint) return;
//		super.paintComponent(g);
//	}

	public void paintComponents(Graphics g)
	{
		if (this.suspendRepaint) return;
		super.paintComponents(g);
	}
	
}
