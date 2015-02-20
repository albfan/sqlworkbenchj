/*
 * WbAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.gui.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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

import workbench.log.LogMgr;
import workbench.resource.IconMgr;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.resource.ShortcutManager;

import workbench.gui.components.WbMenuItem;
import workbench.gui.components.WbToolbarButton;

/**
 * Base class for Actions in SQL Workbench/J
 * the actual work should be implemented in executeAction()
 * which is guaranteed to be called on the EDT.
 *
 * @author  Thomas Kellerer
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
	protected JButton toolbarButton;
	private ActionListener delegate;
	protected WbAction proxy;
	private WbAction original;
	private String iconKey;
	private final List<WeakReference<JMenuItem>> createdItems = new LinkedList<>();
	protected boolean isConfigurable = true;
	private String descriptiveName;
	private boolean isPngIcon;

	public WbAction()
	{
		super();
		String c = this.getClass().getName();
		actionName = "wb-" + c.substring(c.lastIndexOf('.') + 1);
		putValue(ACTION_COMMAND_KEY, this.actionName);
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
		super();
		delegate = l;
		actionName = aName;
		putValue(ACTION_COMMAND_KEY, this.actionName);
	}

	public static boolean invokedByMouse(ActionEvent e)
	{
		boolean mouse = ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0);
		return mouse;
	}

	public static boolean isAltPressed(ActionEvent e)
	{
		boolean altPressed = ((e.getModifiers() & ActionEvent.ALT_MASK) == ActionEvent.ALT_MASK);
		return altPressed;
	}

	public static boolean isShiftPressed(ActionEvent e)
	{
		return isShiftPressed(e.getModifiers());
	}

	public static boolean isShiftPressed(int modifiers)
	{
		boolean shiftPressed = ((modifiers & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK);
		return shiftPressed;
	}

	public static boolean isCtrlPressed(ActionEvent e)
	{
		return isCtrlPressed(e.getModifiers());
	}

	public static boolean isCtrlPressed(int modifiers)
	{
		int ctrl = PlatformShortcuts.getDefaultModifier();
		boolean ctrlPressed = ((modifiers & ctrl) == ctrl);
		return ctrlPressed;
	}

	public boolean isConfigurable()
	{
		return isConfigurable;
	}

	public boolean allowDuplicate()
	{
		return false;
	}

	public boolean hasShiftModifier()
	{
		return false;
	}

	public boolean hasCtrlModifier()
	{
		return false;
	}

	public void setTooltip(String aText)
	{
		putValue(Action.SHORT_DESCRIPTION, aText);
	}

	public String getToolTipText()
	{
		return (String) getValue(Action.SHORT_DESCRIPTION);
	}

	public String getTooltipTextWithKeys()
	{
		return getToolTipText() + " (" + this.getAcceleratorDisplay() + ")";
	}

	public void clearAccelerator()
	{
		putValue(Action.ACCELERATOR_KEY, null);
	}

	public String getActionCommand()
	{
		return (String) getValue(ACTION_COMMAND_KEY);
	}

	protected void setActionName(String aName)
	{
		actionName = aName;
	}

	public String getMenuLabel()
	{
		return (String) this.getValue(Action.NAME);
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
		initMenuDefinition(aKey, null);
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
		setMenuTextByKey(aKey);
		setDefaultAccelerator(defaultKey);
		initializeShortcut();
	}

	protected void initializeShortcut()
	{
		if (this.isConfigurable())
		{
			ShortcutManager mgr = ShortcutManager.getInstance();
			mgr.registerAction(this);
			KeyStroke key = mgr.getCustomizedKeyStroke(this);
			setAccelerator(key);
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
		setMenuText(aMenuText);
		setTooltip(aTooltip);
		setDefaultAccelerator(defaultKey);
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
		setMenuText(ResourceMgr.getString(aKey));
		setTooltip(ResourceMgr.getDescription(aKey, true));
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
		if (text == null)
		{
			return;
		}
		int pos = text.indexOf('&');
		if (pos > -1)
		{
			char mnemonic = text.charAt(pos + 1);
			text = text.substring(0, pos) + text.substring(pos + 1);
			Integer keycode = Integer.valueOf((int) mnemonic);
			Integer index = Integer.valueOf(pos);
			this.putValue(Action.MNEMONIC_KEY, keycode);
			this.putValue(WbAction.MNEMONIC_INDEX, index);
		}
		putValue(Action.NAME, text);
	}

	public void setAlternateAccelerator(KeyStroke key)
	{
		putValue(ALTERNATE_ACCELERATOR, key);
	}

	public KeyStroke getAlternateAccelerator()
	{
		return (KeyStroke) this.getValue(ALTERNATE_ACCELERATOR);
	}

	public void setAccelerator(KeyStroke key)
	{
		putValue(Action.ACCELERATOR_KEY, key);

		Iterator<WeakReference<JMenuItem>> itr = this.createdItems.iterator();
		while (itr.hasNext())
		{
			WeakReference<JMenuItem> ref = itr.next();
			if (ref == null)
			{
				itr.remove();
				continue;
			}
			JMenuItem item = ref.get();
			if (item == null)
			{
				itr.remove();
			}
			else
			{
				item.setAccelerator(key);
			}
		}
	}

	public String getDescriptiveName()
	{
		if (this.descriptiveName == null)
		{
			return getMenuLabel();
		}
		return this.descriptiveName;
	}

	protected void setDescriptiveName(String name)
	{
		this.descriptiveName = name;
	}

	public KeyStroke getAccelerator()
	{
		return (KeyStroke) this.getValue(Action.ACCELERATOR_KEY);
	}

	public JButton getToolbarButton()
	{
		return this.getToolbarButton(false);
	}

	public JButton getToolbarButton(boolean createNew)
	{
		JButton result;
		if (this.toolbarButton == null || createNew)
		{
			WbToolbarButton b = new WbToolbarButton();
			b.setAction(this);
			b.setMnemonic(0);
			if (this.toolbarButton == null)
			{
				this.toolbarButton = b;
			}
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
		aToolbar.add(getToolbarButton());
	}

	public void addToMenu(JMenu aMenu)
	{
		aMenu.add(getMenuItem());
	}

	public JMenuItem getMenuItem()
	{
		// one menu item can only be put into a single menu
		// so if the same action is used more than one menu
		// multiple menu items need to be created.
		JMenuItem item = new WbMenuItem();
		item.setAction(this);
		item.setAccelerator(getAccelerator());
		item.setHorizontalTextPosition(JButton.TRAILING);
		item.setVerticalTextPosition(JButton.CENTER);
		Integer index = (Integer) getValue(WbAction.MNEMONIC_INDEX);
		if (index != null)
		{
			try
			{
				item.setDisplayedMnemonicIndex(index.intValue());
			}
			catch (Exception e)
			{
			}
		}

		// All MenuItems that are created are stored in the List in order to be able
		// to update the shortcut if the shortcut for the action changes.
		// for some reason, only changing the shortcut on the action does not seem to be enough
		this.createdItems.add(new WeakReference(item));
		return item;
	}

	public String getMenuItemName()
	{
		return (String) this.getValue(WbAction.MAIN_MENU_ITEM);
	}

	public void setMenuItemName(String aKey)
	{
		putValue(WbAction.MAIN_MENU_ITEM, aKey);
	}

	public boolean getCreateToolbarSeparator()
	{
		Boolean flag = (Boolean) getValue(WbAction.TBAR_SEPARATOR);
		if (flag == null)
		{
			return false;
		}
		return flag.booleanValue();
	}

	public void setCreateToolbarSeparator(boolean flag)
	{
		putValue(WbAction.TBAR_SEPARATOR, (flag ? Boolean.TRUE : Boolean.FALSE));
	}

	public boolean getCreateMenuSeparator()
	{
		Boolean flag = (Boolean) getValue(WbAction.MENU_SEPARATOR);
		if (flag == null)
		{
			return false;
		}
		return flag.booleanValue();
	}

	public void setCreateMenuSeparator(boolean flag)
	{
		putValue(WbAction.MENU_SEPARATOR, (flag ? Boolean.TRUE : Boolean.FALSE));
	}

	public String getActionName()
	{
		return actionName;
	}

	public void addToInputMap(JComponent c)
	{
		addToInputMap(c.getInputMap(), c.getActionMap());
	}

	public void addToInputMap(JComponent c, int which)
	{
		addToInputMap(c.getInputMap(which), c.getActionMap());
	}

	public void addToInputMap(InputMap im, ActionMap am)
	{
		if (this.getAccelerator() == null)
		{
			return;
		}

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
			im.put(KeyStroke.getKeyStroke(key, modifiers | PlatformShortcuts.getDefaultModifier()), this.getActionName());
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
			im.remove(KeyStroke.getKeyStroke(key, modifiers | PlatformShortcuts.getDefaultModifier()));
		}
	}

	public void setDefaultAccelerator(KeyStroke key)
	{
		putValue(DEFAULT_ACCELERATOR, key);
	}

	public KeyStroke getDefaultAccelerator()
	{
		return (KeyStroke) getValue(DEFAULT_ACCELERATOR);
	}

	public void setPngIcon(String key)
	{
		this.iconKey = key;
		this.isPngIcon = true;
		if (key == null)
		{
			removeIcon();
		}
	}

	public void setIcon(String key)
	{
		// Just store the key for the resource manager
		// the actual item will be retrieved when it's really
		// needed in getValue()
		// this will retrieve the icons "lazily" so not all
		// of them are actually loaded during startup
		iconKey = key;
		isPngIcon = false;
		if (key == null)
		{
			removeIcon();
		}
	}

	@Override
	public Object getValue(String key)
	{
		if (key.equals(Action.SMALL_ICON))
		{
			return getIcon(Action.SMALL_ICON, IconMgr.getInstance().getSizeForMenuItem());
		}
		else if (key.equals(Action.LARGE_ICON_KEY))
		{
			return getIcon(Action.LARGE_ICON_KEY, Settings.getInstance().getToolbarIconSize());
		}
		return super.getValue(key);
	}


	private ImageIcon getIcon(String key, int size)
	{
		// No resource key assigned --> no icon
		if (this.iconKey == null)
		{
			return null;
		}
		ImageIcon icon = (ImageIcon)super.getValue(key);
		if (icon != null) return icon;
		icon = IconMgr.getInstance().getIcon(iconKey, size, isPngIcon);
		if (icon != null)
		{
			this.putValue(key, icon);
		}
		return icon;
	}

	public void removeIcon()
	{
		iconKey = null;
		putValue(Action.SMALL_ICON, null);
		putValue(Action.LARGE_ICON_KEY, null);
	}

	@Override
	public void actionPerformed(final ActionEvent e)
	{
		if (isEnabled())
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
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
			if (e != null) e.setSource(this);
			delegate.actionPerformed(e);
		}
	}

	private String getAcceleratorDisplay()
	{
		String acceleratorDelimiter = UIManager.getString("MenuItem.acceleratorDelimiter");
		if (acceleratorDelimiter == null)
		{
			acceleratorDelimiter = "-";
		}
		KeyStroke key = getDefaultAccelerator();
		int mod = key.getModifiers();
		int keycode = key.getKeyCode();

		String display = KeyEvent.getKeyModifiersText(mod) + acceleratorDelimiter + KeyEvent.getKeyText(keycode);
		return display;
	}

	@Override
	public void setEnabled(boolean flag)
	{
		super.setEnabled(flag);
		if (this.proxy != null)
		{
			this.proxy.setEnabled(flag);
		}
	}

	public void setOriginal(WbAction org)
	{
		if (this.original != null)
		{
			this.original.setProxy(null);
			if (org == null)
			{
				setEnabled(false);
			}
		}
		this.original = null;
		if (org != null)
		{
			setEnabled(org.isEnabled());
			this.original = org;
			this.original.setProxy(this);
		}
	}

	@Override
	public String toString()
	{
		return this.getActionName() + ", " + this.getAccelerator();
	}

	protected void setProxy(WbAction p)
	{
		this.proxy = p;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof WbAction)
		{
			return ((WbAction)other).actionName.equals(this.actionName);
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 59 * hash + (this.actionName != null ? this.actionName.hashCode() : 0);
		return hash;
	}

	public void dispose()
	{
		for (WeakReference<JMenuItem> ref : createdItems)
		{
			JMenuItem item = ref.get();
			if (item != null)
			{
				item.setAction(null);
				item.setIcon(null);
				item.removeAll();
			}
		}
		createdItems.clear();
		delegate = null;
		original = null;
		proxy = null;
		if (toolbarButton != null)
		{
			toolbarButton.setAction(null);
			toolbarButton.setIcon(null);
			toolbarButton = null;
		}
	}

	public static void dispose(WbAction ... actions)
	{
		if (actions == null) return;

		for (WbAction action : actions)
		{
			if (action != null) action.dispose();
		}
	}
}
