/*
 * WbSplash.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dialogs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import workbench.resource.ResourceMgr;

/**
 * @author support@sql-workbench.net
 */
public class WbSplash 
		extends Frame
{
	private Image splashImage;
	private int imageWidth = 172;
	private int imageHeight = 128;
	private int imageX;
	private final int imageY = 30;
	
	private final int panicX;
	private final int panicY = imageHeight + imageY + 40;
	
	private final int loadingX;
	private final int loadingY = panicY + 25;
	
	private final String dontPanic = "Don't panic";
	private final String loading = "Loading SQL Workbench/J ...";
	private final Font panicFont;
	private final Font loadingFont;
	
	public WbSplash()
	{
		super();
		setUndecorated(true);
		splashImage = ResourceMgr.getPicture("hitchguide").getImage();
		
    setBackground(Color.lightGray);
		
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    setBounds((screenSize.width-274)/2, (screenSize.height-249)/2, 274, 249);
		int width = getWidth();
		imageX = (int)((width - imageWidth) / 2);
		
		panicFont = new Font("Serif", Font.PLAIN, 36);
		loadingFont = new Font("Dialog", Font.PLAIN, 12);
		FontMetrics fm = getFontMetrics(panicFont);
		int w = fm.stringWidth(dontPanic);
		panicX = (int)((width - w) / 2);
		fm = getFontMetrics(loadingFont);
		w = fm.stringWidth(loading);
		loadingX = (int)((width - w) / 2);
  }

	public void paint(Graphics g)
	{
		g.setColor(Color.GRAY);
		g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
		g.drawImage(splashImage, imageX, imageY, 172, 128, null);
		g.setColor(Color.BLACK);
		g.setFont(panicFont);
		g.drawString(dontPanic, panicX, panicY);
		g.setColor(Color.BLUE);
		g.setFont(loadingFont);
		g.drawString(loading, loadingX, loadingY);
	}

	public static void main(String args[])
	{
		try
		{
			WbSplash sp = new WbSplash();
			sp.show();
		}
		catch (Throwable th)
		{
			th.printStackTrace();
		}
		System.out.println("Done.");
	}
}
