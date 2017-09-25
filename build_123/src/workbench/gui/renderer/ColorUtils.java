/*
 * ColorUtils.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
