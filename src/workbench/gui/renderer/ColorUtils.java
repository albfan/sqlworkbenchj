/*
 * ColorUtils.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.renderer;

import java.awt.Color;

/**
 * @author Thomas Kellerer
 */
public class ColorUtils
{

	/**
	 * Blend two colors.
	 *
	 * Taken from: From: http://www.java-gaming.org/index.php?topic=21434.0
	 *
	 * @param color1   the first color
	 * @param color2   the second color
	 * @param factor   the balance factor that assigns a "priority" to the passed colors
	 *                 0 returns the first color, 256 returns the second color
	 *
	 * @return the blended color
	 */
  public static Color blend(Color color1, Color color2, int factor)
  {
		if (color2 == null) return color1;
		
		if (factor <= 0) return color1;
		if (factor >= 256) return color2;
		if (color1 == null) return color2;

		int f1 = 256 - factor;
		int c1 = color1.getRGB();
		int c2 = color2.getRGB();
		int blended = ((((c1 & 0xFF00FF) * f1 + (c2 & 0xFF00FF) * factor)  & 0xFF00FF00)  | (((c1 & 0x00FF00) * f1 + (c2 & 0x00FF00) * factor ) & 0x00FF0000)) >>>8;
		return new Color(blended);
  }

}