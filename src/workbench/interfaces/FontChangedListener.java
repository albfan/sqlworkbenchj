/*
 * FontChangedListener.java
 *
 * Created on August 22, 2002, 3:12 PM
 */

package workbench.interfaces;

import java.awt.Font;

/**
 *
 * @author  workbench@kellerer.org
 */
public interface FontChangedListener
{
	void fontChanged(String aFontId, Font newFont);
}
