/*
 * WbAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import workbench.gui.components.WbMenuItem;
import workbench.gui.components.WbToolbarButton;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.resource.ShortcutManager;

/**
 * Base class for Actions in SQL Workbench/J
 * the actual work should be implemented in executeAction()
 * which is guaranteed to be called on the EDT.
 * 
 * @author  support@sql-workbench.net
 */
public class WbAction 
	extends AbstractAction
{
	private static final String MAIN_MENU_ITEM = "MainMenuItem";
	private static final String MENU_SEPARATOR = "MenuSep";
	private static final String TBAR_SEPARATOR = "TbarSep";
	private static final String ALTERNATE_ACCELERATOR = "AltAcc";
	private static final String DEFAULT_ACCELERATOR = "DefaultAcc";
	private static final String MNEMONIC_INDEX = "MnemonicIndex";
	
	private String actionName;
	protected JMenuItem menuItem;
	protected JButton toolbarButton;
	private ActionListener delegate = null;
	protected WbAction proxy;
	private WbAction original;
	
	public WbAction()
	{
		String c = this.getClass().getName();
		this.actionName = "wb-" + c.substring(c.lastIndexOf('.')  + 1);
    this.putValue(ACTION_COMMAND_KEY, this.actionName);
	}

	/**
	 * Creates a WbAction which dispatches its {@link #executeAction(ActionEvent)} 
	 * event to the passed ActionListener, instead of executing it itself.
	 * This is intended for situations where an Action is needed, but not 
	 * implemented with a subclass of WbAction, but with an ActionListener
	 * instead.
	 */
	public WbAction(ActionListener l, String aName)
	{
		this();
		this.delegate = l;
		this.actionName = aName;
    this.putValue(ACTION_COMMAND_KEY, this.actionName);
	}

	public static boolean isAltPressed(ActionEvent e)
	{
		boolean altPressed = ((e.getModifiers() & ActionEvent.ALT_MASK) == ActionEvent.ALT_MASK);		
		return altPressed;
	}

	public static boolean isShiftPressed(ActionEvent e)
	{
		boolean shiftPressed = ((e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK);		
		return shiftPressed;
	}
	
	public static boolean isCtrlPressed(ActionEvent e)
	{
		boolean ctrlPressed = ((e.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK);
		return ctrlPressed;
	}
	
	public boolean allowDuplicate()
	{
		return false;
	}
	
	public boolean hasShiftModifier() { return false; }
	public boolean hasCtrlModifier() { return false; }
	
	public void setTooltip(String aText)
	{
		this.putValue(Action.SHORT_DESCRIPTION, aText);
	}
	
	public String getTooltipText()
	{
		return (String)getValue(Action.SHORT_DESCRIPTION);
	}

	public String getTooltipTextWithKeys()
	{
		return getTooltipText() + " (" + this.getAcceleratorDisplay() + ")";
	}
	
	public void clearAccelerator()
	{
		this.putValue(Action.ACCELERATOR_KEY, null);
	}

	public String getActionCommand()
	{
		return (String)getValue(ACTION_COMMAND_KEY);
	}
	
	protected void setActionName(String aName)
	{
		this.actionName = aName;
	}

	public String getMenuLabel()
	{
		return (String)this.getValue(Action.NAME);
	}
	
	/**
	 * Initialize the menu definition for this action. The passed key will 
	 * be used to initialize the menu label and tooltip.
	 * This method will register the action with the ShortcutManager even though
	 * no shortcut is defined.
	 * @param aKey Translation key for ResourceMgr
	 * @see #setMenuTextByKey(String) 
	 * @see workbench.resource.ShortcutManager#registerAction(WbAction)
	 */
	public void initMenuDefinition(String aKey)
	{
		this.initMenuDefinition(aKey, null);
	}

	/**
	 * Initialize the menu definition for this action. The passed key will 
	 * be used to initialize the menu label and tooltip.
	 * This method will register the action with the ShortcutManager.
	 * @param aKey         Translation key for ResourceMgr
	 * @param defaultKey   Default shortcut key, may be null
	 * @see #setMenuTextByKey(String)
	 * @see workbench.resource.ShortcutManager#registerAction(WbAction)
	 */
	public void initMenuDefinition(String aKey, KeyStroke defaultKey)
	{
		this.setMenuTextByKey(aKey);
		this.setDefaultAccelerator(defaultKey);
		initializeShortcut();
	}

	protected void initializeShortcut()
	{
		ShortcutManager mgr = Settings.getInstance().getShortcutManager(); 
		mgr.registerAction(this);
		KeyStroke key = mgr.getCustomizedKeyStroke(this);
		this.setAccelerator(key);
	}
	/**
	 * Initialize the menu definition for this action. The menu text and tooltip
	 * will be used directly without retrieving it from the ResourceMgr.  
	 * This method will register the action with the ShortcutManager.
	 * @param aMenuText    The text to be displayed in the menu item
	 * @param aTooltip     The tooltip for the menu item
	 * @param defaultKey   Default shortcut key, may be null
	 * @see workbench.resource.ShortcutManager#registerAction(WbAction)
	 */
	public void initMenuDefinition(String aMenuText, String aTooltip, KeyStroke defaultKey)
	{
		this.setMenuText(aMenuText);
		this.setTooltip(aTooltip);
		this.setDefaultAccelerator(defaultKey);
		initializeShortcut();
	}
	
	/**
	 * Define the displayed menu text and tooltip. The passed key
	 * will be used to retrieve the real text from the ResourceManager.
	 * This will not register the Action with the ShortcutManager. 
	 * @param aKey  The key for the ResourceManager
	 * @see workbench.resource.ResourceMgr#getString(String)
	 * @see workbench.resource.ResourceMgr#getDescription(String)
	 * @see #setMenuText(String)
	 */
	public void setMenuTextByKey(String aKey)
	{
		this.setMenuText(ResourceMgr.getString(aKey));
		this.setTooltip(ResourceMgr.getDescription(aKey, true));
	}
	
	/**
	 * Define the displayed text for the associcated menu item
	 * If the text contains a & sign, the character after the 
	 * & sign will be used as the Mnemonic for the menu item.
	 * Once the mnemonic is identified the passed text (after
	 * removing the & sign) will be set using
	 * putValue(Actin.NAME, Object)
	 *
	 * @param text the text for the menu item 
	 * @see #setMenuTextByKey(String)
	 */
	public void setMenuText(String text)
	{
		if (text == null) return;
		int pos = text.indexOf('&');
		if (pos > -1)
		{
			char mnemonic = text.charAt(pos + 1);
			text = text.substring(0, pos) + text.substring(pos + 1);
			Integer keycode = new Integer((int)mnemonic);
			Integer index = new Integer(pos);
			this.putValue(Action.MNEMONIC_KEY, keycode);
			this.putValue(WbAction.MNEMONIC_INDEX, index);
		}
		putValue(Action.NAME, text);
	}	
	
	public void setAlternateAccelerator(KeyStroke key)
	{
		this.putValue(ALTERNATE_ACCELERATOR, key);
	}
	
	public KeyStroke getAlternateAccelerator()
	{
		return (KeyStroke)this.getValue(ALTERNATE_ACCELERATOR);
	}
	
	public void setAccelerator(KeyStroke key)
	{
		KeyStroke old = this.getAccelerator();
		this.putValue(Action.ACCELERATOR_KEY, key);

		boolean isNew = false;
		if (old != null && key != null) 
		{	
			isNew = !key.equals(old);
		}
		else
		{
			isNew = (old != null || key != null);
		}
  
		if (isNew && this.menuItem != null)
		{
			// to force a re-initialization of the menu item
			// we need to first clear the action and then re-assign it
			this.menuItem.setAction(null);
			this.menuItem.setAction(this);
		}
	}
	
	public KeyStroke getAccelerator()
	{
		return (KeyStroke)this.getValue(Action.ACCELERATOR_KEY);
	}

	public JButton getToolbarButton()
  {
    return this.getToolbarButton(false);
  }
	
	public JButton getToolbarButton(boolean createNew )
	{
    JButton result;
		if (this.toolbarButton == null || createNew)
		{
      WbToolbarButton b = new WbToolbarButton();
			b.setAction(this);
			b.setMnemonic(0);
      if (this.toolbarButton == null) this.toolbarButton = b;
      result = b;
		}
    else
    {
      result = this.toolbarButton;
    }
    
		return result;
	}

	public void addToToolbar(JToolBar aToolbar)
	{
		aToolbar.add(this.getToolbarButton());
	}

	public void addToMenu(JMenu aMenu)
	{
		aMenu.add(this.getMenuItem());
	}

	public JMenuItem getMenuItem()
	{
		this.menuItem = new WbMenuItem();
		this.menuItem.setAction(this);
		this.menuItem.setAccelerator(this.getAccelerator());
		//this.menuItem.setLocale(Settings.getInstance().getLanguage());
		Integer index = (Integer)this.getValue(WbAction.MNEMONIC_INDEX);
		if (index != null)
		{
			try
			{
				this.menuItem.setDisplayedMnemonicIndex(index.intValue());
			}
			catch (Exception e)
			{
			}
		}
		return this.menuItem;
	}

	public String getMenuItemName()
	{
		return (String)this.getValue(WbAction.MAIN_MENU_ITEM);
	}
	
	public void setMenuItemName(String aKey)
	{
		this.putValue(WbAction.MAIN_MENU_ITEM, aKey);
	}
	
	public boolean getCreateToolbarSeparator()
	{
		Boolean flag = (Boolean)getValue(WbAction.TBAR_SEPARATOR);
		if (flag == null) return false;
		return flag.booleanValue();
	}
	
	public void setCreateToolbarSeparator(boolean flag)
	{
		putValue(WbAction.TBAR_SEPARATOR, (flag ? Boolean.TRUE : Boolean.FALSE));
	}

	public boolean getCreateMenuSeparator()
	{
		Boolean flag = (Boolean)getValue(WbAction.MENU_SEPARATOR);
		if (flag == null) return false;
		return flag.booleanValue();
	}
	
	public void setCreateMenuSeparator(boolean flag)
	{
		this.putValue(WbAction.MENU_SEPARATOR, (flag ? Boolean.TRUE : Boolean.FALSE));
	}

	public String getActionName()
	{
		return this.actionName;
	}

	public void addToInputMap(JComponent c)
	{
		addToInputMap(c.getInputMap(), c.getActionMap());
	}
	
	public void addToInputMap(InputMap im, ActionMap am)
	{
		if (this.getAccelerator() == null) return;
		
		im.put(this.getAccelerator(), this.getActionName());
		am.put(this.getActionName(), this);
		
		KeyStroke alternate = getAlternateAccelerator();
		if (alternate != null)
		{
			im.put(alternate, getActionName());
		}
		
		int key = this.getAccelerator().getKeyCode();
		int modifiers = this.getAccelerator().getModifiers();
		
		if (this.hasShiftModifier())
		{
			im.put(KeyStroke.getKeyStroke(key, modifiers | InputEvent.SHIFT_MASK), this.getActionName());
		}
		
		if (this.hasCtrlModifier())
		{
			im.put(KeyStroke.getKeyStroke(key, modifiers | InputEvent.CTRL_MASK), this.getActionName());
		}
	}

	public void removeFromInputMap(InputMap im, ActionMap am)
	{
		am.remove(this.getActionName());
		im.remove(this.getAccelerator());
		KeyStroke alternate = this.getAlternateAccelerator();
		if (alternate != null)
		{
			im.remove(alternate);
		}
		
		int key = this.getAccelerator().getKeyCode();
		int modifiers = this.getAccelerator().getModifiers();
		
		if (this.hasShiftModifier())
		{
			im.remove(KeyStroke.getKeyStroke(key, modifiers | InputEvent.SHIFT_MASK));
		}
		if (this.hasCtrlModifier())
		{
			im.remove(KeyStroke.getKeyStroke(key, modifiers | InputEvent.CTRL_MASK));
		}
	}
	
	public void setDefaultAccelerator(KeyStroke key)
	{
		this.putValue(DEFAULT_ACCELERATOR, key);
	}
	
	public KeyStroke getDefaultAccelerator()
	{
		return (KeyStroke)this.getValue(DEFAULT_ACCELERATOR);
	}
	
	public void setIcon(ImageIcon icon)
	{
		this.putValue(Action.SMALL_ICON, icon);
	}
	
	public void removeIcon()
	{
		this.putValue(Action.SMALL_ICON, null);
	}
	
	public void actionPerformed(final ActionEvent e)
	{
		if (this.isEnabled()) 
		{
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					if (original != null) 
					{
						original.executeAction(e);
					}
					else
					{
						executeAction(e);
					}
				}
			});
		}
	}
	
	public void executeAction(ActionEvent e)
	{
		if (this.isEnabled() && this.delegate != null)
		{
			e.setSource(this);
			this.delegate.actionPerformed(e);
		}
	}
	
	private String getAcceleratorDisplay()
	{
		String acceleratorDelimiter = UIManager.getString( "MenuItem.acceleratorDelimiter" );
		if ( acceleratorDelimiter == null )
		{ 
			acceleratorDelimiter = "-"; 
		}
		KeyStroke key = getDefaultAccelerator();
		int mod = key.getModifiers();
		int keycode = key.getKeyCode();
			
		String display = KeyEvent.getKeyModifiersText(mod) +
										acceleratorDelimiter + 
										KeyEvent.getKeyText(keycode);
    return display;
	}

	public void setEnabled(boolean flag)
	{
		super.setEnabled(flag);
		if (this.proxy != null) this.proxy.setEnabled(flag);
	}
	
	public void setOriginal(WbAction org)
	{
		if (this.original != null)
		{
			this.original.setProxy(null);
			if (org == null) setEnabled(false);
		}
		this.original = null;
		if (org != null)
		{
			setEnabled(org.isEnabled());
			this.original = org;
			this.original.setProxy(this);
		}
	}
	
	protected void setProxy(WbAction p)
	{
		this.proxy = p;
	}
}
