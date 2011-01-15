/*
 * DecreaseFontSize.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.fontzoom;

import java.awt.event.ActionEvent;
import javax.swing.KeyStroke;
import workbench.gui.actions.WbAction;
import workbench.resource.ResourceMgr;

/**
 * An action to be used for font zooming. 
 *
 * @author Thomas Kellerer
 */
public abstract class FontSizeAction
	extends WbAction
{

	private FontZoomer zoomer;

	protected FontSizeAction(String resourceKey, int keyCode, int keyMask)
	{
		this(resourceKey, keyCode, keyMask, null);
	}

	protected FontSizeAction(String resourceKey, int keyCode, int keyMask, FontZoomer fontZoomer)
	{
		super();
		setMenuTextByKey(resourceKey);
		setTooltip(ResourceMgr.getDescription(resourceKey));
		setDefaultAccelerator(KeyStroke.getKeyStroke(keyCode, keyMask));
		initializeShortcut();
		zoomer = fontZoomer;
	}

	public void actionPerformed(ActionEvent evt)
	{
		FontZoomer toUse = zoomer;

		if (toUse == null && evt.getSource() instanceof FontZoomProvider)
		{
			// If no zoomer has been registered, check if the source component
			// can be zoomed. If yes, then use the zoomer provided by it
			FontZoomProvider provider = (FontZoomProvider) evt.getSource();
			toUse = provider.getFontZoomer();
		}

		if (toUse != null)
		{
			doFontChange(toUse);
		}
	}

	public abstract void doFontChange(FontZoomer fontZoomer);

}
