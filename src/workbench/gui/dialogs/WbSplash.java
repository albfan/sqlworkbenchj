/*
 * WbSplash.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
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
		extends Window
{
	private Image splashImage;
	private final int windowWidth = 280;
	private final int windowHeight = 230;

	private final int imageWidth = 172;
	private final int imageHeight = 128;
	private final int imageX;
	private final int imageY = 25;

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
		super(new Frame());
		splashImage = ResourceMgr.getPicture("hitchguide").getImage();
		/*
		URL location = ResourceMgr.class.getClassLoader().getResource("workbench/resource/images/hitchguide.gif");
		splashImage = Toolkit.getDefaultToolkit().getImage(location);

		MediaTracker tracker = new MediaTracker(this);
		tracker.addImage(splashImage, 1);
		try
		{
			tracker.waitForID(1, 0);
		}
		catch (InterruptedException e)
		{
			System.out.println("INTERRUPTED while loading Image");
		}
		int loadStatus = tracker.statusID(1, false);
		tracker.removeImage(splashImage, 1);
		*/

		setBackground(Color.LIGHT_GRAY);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setBounds((screenSize.width-windowWidth)/2, (screenSize.height-windowHeight)/2, windowWidth, windowHeight);
		imageX = (int)((windowWidth - imageWidth) / 2);

		panicFont = new Font("Serif", Font.PLAIN, 36);
		loadingFont = new Font("Dialog", Font.PLAIN, 12);
		FontMetrics fm = getFontMetrics(panicFont);
		int w = fm.stringWidth(dontPanic);
		panicX = (int)((windowWidth - w) / 2);
		fm = getFontMetrics(loadingFont);
		w = fm.stringWidth(loading);
		loadingX = (int)((windowWidth - w) / 2);
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

//	public static void main(String args[])
//	{
//		try
//		{
//			WbSplash s = new WbSplash();
//			s.show();
//		}
//		catch (Throwable th)
//		{
//			th.printStackTrace();
//		}
//		System.out.println("Done.");
//	}

}
