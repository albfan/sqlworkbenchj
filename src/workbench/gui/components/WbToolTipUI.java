package workbench.gui.components;


import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JToolTip;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolTipUI;
import javax.swing.plaf.metal.MetalLookAndFeel;

import workbench.gui.actions.WbAction;

/**
 *	A copy of Sun's original MetalToolTipUI.
 *
 *	This UI fixes a problem with the incorrect display of shortcuts
 *	in the tooltip. If the shortcut for a menu item does not contain
 *	a modifief (e.g. when the shortcut is F5) the original tooltip will 
 *	display an incorrect shortcut (e.g. Alt-e).
 *	This class fixes this bug.
 *	To enable this ToolTipUI use:
 *	<code>
 *	UIManager.put("ToolTipUI", "workbench.gui.components.WbToolTipUI");
 *	</code>
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
		{ 
			acceleratorDelimiter = "-"; 
		}
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
		FontMetrics metrics = g.getFontMetrics(font);
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
			g.drawString(keyText,metrics.stringWidth(tipText) + padSpaceBetweenStrings,2 + metrics.getAscent());
		}
	}
	
	public Dimension getPreferredSize(JComponent c)
	{
		Dimension d = super.getPreferredSize(c);
		
		String key = getAcceleratorString((JToolTip)c);
		if (! (key.equals("")))
		{
			//FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(smallFont);
			FontMetrics fm = c.getFontMetrics(smallFont);
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
