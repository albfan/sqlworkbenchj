/*
 * @(#)MetalToolTipUI.java	1.23 01/12/03
 *
 * Copyright 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package workbench.gui.components;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicToolTipUI;
import javax.swing.plaf.metal.MetalLookAndFeel;
import workbench.gui.actions.WbAction;
import workbench.gui.components.WbToolbarButton;


/**
 * A Metal L&F extension of BasicToolTipUI.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans<sup><font size="-2">TM</font></sup>
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @version 1.23 12/03/01
 * @author Steve Wilson
 */
public class WbToolTipUI extends BasicToolTipUI
{
	
	static WbToolTipUI sharedInstance = new WbToolTipUI();
	private Font smallFont;
	// Refer to note in getAcceleratorString about this field.
	private JToolTip tip;
	public static final int padSpaceBetweenStrings = 12;
	private String acceleratorDelimiter;
	
	public WbToolTipUI()
	{
		super();
	}
	
	public static ComponentUI createUI(JComponent c)
	{
		return sharedInstance;
	}
	
	public void installUI(JComponent c)
	{
		super.installUI(c);
		tip = (JToolTip)c;
		Font f = c.getFont();
		smallFont = new Font( f.getName(), f.getStyle(), f.getSize() - 2 );
		acceleratorDelimiter = UIManager.getString( "MenuItem.acceleratorDelimiter" );
		if ( acceleratorDelimiter == null )
		{ acceleratorDelimiter = "-"; }
	}
	
	public void uninstallUI(JComponent c)
	{
		super.uninstallUI(c);
		tip = null;
	}
	
	public void paint(Graphics g, JComponent c)
	{
		JToolTip tip = (JToolTip)c;
		
		super.paint(g, c);
		
		Font font = c.getFont();
		FontMetrics metrics = Toolkit.getDefaultToolkit().getFontMetrics(font);
		String keyText = getAcceleratorString(tip);
		String tipText = tip.getTipText();
		if (tipText == null)
		{
			tipText = "";
		}
		if (! (keyText.equals("")))
		{  // only draw control key if there is one
			g.setFont(smallFont);
			g.setColor( MetalLookAndFeel.getPrimaryControlDarkShadow() );
			g.drawString(keyText,
			metrics.stringWidth(tipText) + padSpaceBetweenStrings,
			2 + metrics.getAscent());
		}
	}
	
	public Dimension getPreferredSize(JComponent c)
	{
		Dimension d = super.getPreferredSize(c);
		
		String key = getAcceleratorString((JToolTip)c);
		if (! (key.equals("")))
		{
			FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(smallFont);
			d.width += fm.stringWidth(key) + padSpaceBetweenStrings;
		}
		return d;
	}
	
	protected boolean isAcceleratorHidden()
	{
		Boolean b = (Boolean)UIManager.get("ToolTip.hideAccelerator");
		return b != null && b.booleanValue();
	}
	
	private String getAcceleratorString(JToolTip tip)
	{
		this.tip = tip;
		
		String retValue = getAcceleratorString();
		
		this.tip = null;
		return retValue;
	}
	
	// NOTE: This requires the tip field to be set before this is invoked.
	// As MetalToolTipUI is shared between all JToolTips the tip field is
	// set appropriately before this is invoked. Unfortunately this means
	// that subclasses that randomly invoke this method will see varying
	// results. If this becomes an issue, MetalToolTipUI should no longer be
	// shared.
	public String getAcceleratorString()
	{
		if (tip == null || isAcceleratorHidden())
		{
			return "";
		}
		JComponent comp = tip.getComponent();
		if (comp == null)
		{
			return "";
		}
		KeyStroke[] keys =comp.getRegisteredKeyStrokes();
		String controlKeyStr = "";
		
		for (int i = 0; i < keys.length; i++)
		{
			int mod = keys[i].getModifiers();
			int condition =  comp.getConditionForKeyStroke(keys[i]);
			int key = keys[i].getKeyCode();
			
			if ( condition == JComponent.WHEN_IN_FOCUSED_WINDOW &&
			     ( (mod & InputEvent.ALT_MASK) != 0 || (mod & InputEvent.CTRL_MASK) != 0 ||
			     (mod & InputEvent.SHIFT_MASK) != 0 || (mod & InputEvent.META_MASK) != 0 ) 
				 )
			{
				controlKeyStr = KeyEvent.getKeyModifiersText(mod) +
												acceleratorDelimiter +
												KeyEvent.getKeyText(keys[i].getKeyCode());
				break;
			}
			
			else if (mod == 0 && (key == KeyEvent.VK_F1 ||
					 key == KeyEvent.VK_F2 ||
					 key == KeyEvent.VK_F3 ||
					 key == KeyEvent.VK_F4 ||
					 key == KeyEvent.VK_F5 ||
					 key == KeyEvent.VK_F6 ||
					 key == KeyEvent.VK_F7 ||
					 key == KeyEvent.VK_F8 ||
					 key == KeyEvent.VK_F9 ||
					 key == KeyEvent.VK_F10 ||
					 key == KeyEvent.VK_F11 ||
					 key == KeyEvent.VK_F12) && 
					 (comp instanceof JMenu || comp instanceof JMenuItem)) 
			{
				controlKeyStr = KeyEvent.getKeyText(keys[i].getKeyCode());
				break;
			}
		}
	
		if (controlKeyStr.length() == 0 && comp instanceof WbToolbarButton)
		{
			WbAction action = (WbAction)((WbToolbarButton)comp).getAction();
			KeyStroke key = action.getAccelerator();
			if (key != null)
			{
				controlKeyStr = KeyEvent.getKeyModifiersText(key.getModifiers()) +
												acceleratorDelimiter +
												KeyEvent.getKeyText(key.getKeyCode());
			}
		}
		return controlKeyStr;
	}
	
}
