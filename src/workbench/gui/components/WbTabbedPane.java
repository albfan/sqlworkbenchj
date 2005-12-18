/*
 * WbTabbedPane.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.EventQueue;
import java.awt.Graphics;

import javax.swing.JTabbedPane;


/**
 *
 * @author  support@sql-workbench.net
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

	public synchronized void setSuspendRepaint(boolean suspendNow)
	{
		boolean suspended = this.suspendRepaint;
		this.suspendRepaint = suspendNow;

		// if repainting was re-enabled, then queue
		// a repaint event right away
		// I'm using invokeLater() to make sure, that
		// this is executed on the AWT thread.
		if (suspended && !suspendNow)
		{
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					validate();
					repaint();
				}
			});
		}
	}

	public boolean isRepaintSuspended()
	{
		 return this.suspendRepaint;
	}

	public void repaint()
	{
		if (this.suspendRepaint) return;
		super.repaint();
	}

	public void paintComponent(Graphics g)
	{
		if (this.suspendRepaint) return;
		super.paintComponent(g);
	}

	public void paintComponents(Graphics g)
	{
		if (this.suspendRepaint) return;
		super.paintComponents(g);
	}
}
