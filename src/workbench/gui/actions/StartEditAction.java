/*
 * ClearAction.java
 *
 * Created on December 2, 2001, 1:32 AM
 */
package workbench.gui.actions;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import workbench.gui.components.WbToolbarButton;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.ClipboardSupport;
import workbench.interfaces.Exporter;
import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  sql.workbench@freenet.de
 */
public class StartEditAction extends WbAction
{
	private SqlPanel client;
	private Border enabledBorder = new CompoundBorder(new BevelBorder(BevelBorder.LOWERED), new EmptyBorder(2,2,2,2));
	private Border originalBorder;
	
	private boolean switchedOn = false;
	private JToggleButton toggleButton;
	private JCheckBoxMenuItem toggleMenu;
	
	public StartEditAction(SqlPanel aClient)
	{
		super();
		this.client = aClient;
		this.putValue(Action.NAME, ResourceMgr.getString("MnuTxtStartEdit"));
		this.putValue(WbAction.MAIN_MENU_ITEM, ResourceMgr.MNU_TXT_DATA);
		this.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getDescription("MnuTxtStartEdit"));
		this.putValue(Action.SMALL_ICON, ResourceMgr.getImage("editor"));
		this.setEnabled(false);
	}

	public void actionPerformed(ActionEvent e)
	{
		this.setSwitchedOn(!this.switchedOn);
		if (this.switchedOn)
			this.client.startEdit();
		else
			this.client.endEdit();
	}
	
	public boolean isSwitchedOn() { return this.switchedOn; }
	
	public void setSwitchedOn(boolean aFlag)
	{
		this.switchedOn = aFlag;
		if (this.toggleMenu != null) this.toggleMenu.setSelected(aFlag);
		if (this.toggleButton != null) 
		{
			this.toggleButton.setSelected(aFlag);
		}
	}
	
	public JToggleButton createButton()
	{
		this.toggleButton = new JToggleButton(this);
		this.toggleButton.setText(null);
		this.toggleButton.setMargin(WbToolbarButton.MARGIN);
		return this.toggleButton;
	}
	
	public void addToToolbar(JToolBar aToolbar)
	{
		if (this.toggleButton == null) this.createButton();
		aToolbar.add(this.toggleButton);
	}
	
	public void addToMenu(JMenu aMenu)
	{
		if (this.toggleMenu == null)
		{
			this.toggleMenu= new JCheckBoxMenuItem(this);
			this.toggleMenu.setIcon(null);
		}
		aMenu.add(this.toggleMenu);
	}		
	public void setEnabled(boolean aFlag)
	{
		boolean last = this.isEnabled();
		super.setEnabled(aFlag);
		if (!this.enabled || (last != this.enabled))
			this.setSwitchedOn(false);
	}
}
