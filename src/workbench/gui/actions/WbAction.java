package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import workbench.WbManager;
import workbench.gui.components.WbMenuItem;
import workbench.gui.components.WbToolbarButton;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.resource.ShortcutManager;

/**
 *	@author  workbench@kellerer.org
 */
public class WbAction 
	extends AbstractAction
{
	public static final String ADD_TO_TOOLBAR = "AddToToolbar";
	public static final String MAIN_MENU_ITEM = "MainMenuItem";
	public static final String MENU_SEPARATOR = "MenuSepBefore";
	public static final String TBAR_SEPARATOR = "TbarSepBefore";
	public static final String ALTERNATE_ACCELERATOR = "AlternateAccelerator";
	public static final String DEFAULT_ACCELERATOR = "DefaultAccelerator";
	public static final String MNEMONIC_INDEX = "MnemonicIndex";
	
	private String actionName;
	protected JMenuItem menuItem;
	protected JButton toolbarButton;
	private ActionListener delegate = null;
	
	public WbAction(ActionListener l)
	{
		this();
		this.delegate = l;
	}
	
	public WbAction(ActionListener l, String aName)
	{
		this();
		this.delegate = l;
		this.actionName = "wb-" + aName;
	}
	
	public WbAction()
	{
		String c = this.getClass().getName();
		this.actionName = "wb-" + c.substring(c.lastIndexOf('.')  + 1);
    this.putValue(ACTION_COMMAND_KEY, this.actionName);
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage("blank"));
	}

	public void setTooltip(String aText)
	{
		this.putValue(Action.SHORT_DESCRIPTION, aText);
	}
	
	public void clearAccelerator()
	{
		this.putValue(Action.ACCELERATOR_KEY, null);
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
	 * @param aKey         Translation key for ResourceMgr
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
		ShortcutManager mgr = Settings.getInstance().getShortcutManager(); 
		mgr.registerAction(this);
		KeyStroke key = mgr.getCustomizedKeyStroke(this);
		if (key != null)
		{
			this.setAccelerator(key);
		}
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
		this.putValue(Action.NAME, aMenuText);
		this.setTooltip(aTooltip);
		
		this.setDefaultAccelerator(defaultKey);
		ShortcutManager mgr = Settings.getInstance().getShortcutManager(); 
		mgr.registerAction(this);
		KeyStroke custom = mgr.getCustomizedKeyStroke(this);
		if (custom != null)
		{
			this.setAccelerator(custom);
		}
	}
	
	/**
	 * Define the displayed menu text and tooltip. The passed key
	 * will be used to retrieve the real text from the ResourceManager.
	 * This will not register the Action with the ShortcutManager. 
	 * @param aKey  The key for the ResourceManager
	 * @see workbench.resource.ResourceMgr#getString(String)
	 * @see workbench.resource.ResourceMgr#getDescription(String)
	 */
	public void setMenuTextByKey(String aKey)
	{
		this.putValue(Action.NAME, ResourceMgr.getString(aKey));
		this.setTooltip(ResourceMgr.getDescription(aKey));
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

	public void setMenuItemName(String aKey)
	{
		this.putValue(WbAction.MAIN_MENU_ITEM, aKey);
	}
	
	public void setCreateToolbarSeparator(boolean aFlag)
	{
		if (aFlag)
		{
			this.putValue(WbAction.TBAR_SEPARATOR, "true");
		}
		else
		{
			putValue(WbAction.TBAR_SEPARATOR, "false");
		}
	}
	public void setCreateMenuSeparator(boolean aFlag)
	{
		if (aFlag)
		{
			this.putValue(WbAction.MENU_SEPARATOR, "true");
		}
		else
		{
			putValue(WbAction.MENU_SEPARATOR, "false");
		}
	}

	public String getActionName()
	{
		return this.actionName;
	}

	public void addToInputMap(InputMap im, ActionMap am)
	{
		im.put(this.getAccelerator(), this.getActionName());
		am.put(this.getActionName(), this);
		
		KeyStroke alternate = this.getAlternateAccelerator();
		if (alternate != null)
		{
			im.put(alternate, this.getActionName());
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
	}
	
	public void putValue(String key, Object newValue)
	{
		if (Action.NAME.equals(key) && (newValue instanceof String) && (newValue != null))
		{
			String name = newValue.toString();
			int pos = name.indexOf('&');
			if (pos > -1)
			{
				char mnemonic = name.charAt(pos + 1);
				name = name.substring(0, pos) + name.substring(pos + 1);
				Integer keycode = new Integer((int)mnemonic);
				Integer index = new Integer(pos);
				this.putValue(Action.MNEMONIC_KEY, keycode);
				this.putValue(WbAction.MNEMONIC_INDEX, index);
			}
			super.putValue(key, name);
		}
		else
		{
			super.putValue(key, newValue);
		}
		
		/*
		// if the accelerator is beeing set, check if we already
		// have a default key. If not, then set the current accelerator
		// as the default key. This way, the first shortcut defined
		// for this action, will define the default shortcut.
		if (Action.ACCELERATOR_KEY.equals(key))
		{
			if (this.getDefaultAccelerator() == null)
			{
				super.putValue(DEFAULT_ACCELERATOR, this.getAccelerator());
			}
		}
		
		String label = this.getMenuLabel();
		KeyStroke def = this.getDefaultAccelerator();

		// if label and default accelerator are defined
		// ask the ShortcutManager if a customized shortcut is available
		// if it is, we'll overwrite the current accelerator 
		// with the value from the ShortcutManager
		if (label != null && def != null)
		{
			ShortcutManager mgr = WbManager.getShortcutManager(); 
			mgr.registerAction(this);
			KeyStroke custom = mgr.getCustomizedKeyStroke(this);
			if (custom != null)
			{
				// call the ancestor directly, otherwise we'll run into an infitinite loop!
				super.putValue(Action.ACCELERATOR_KEY, custom);
			}
		}
		*/
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
		executeAction(e);
	}
	
	public void executeAction(ActionEvent e)
	{
		if (this.delegate != null)
		{
			e.setSource(this);
			this.delegate.actionPerformed(e);
		}
	}

	public String getTooltipTextWithKeys()
	{
		String tooltip = (String)getValue(Action.SHORT_DESCRIPTION);
		return tooltip + " (" + this.getAcceleratorDisplay() + ")";
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
}
