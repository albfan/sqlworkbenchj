/*
 * FindPanel.java
 *
 * Created on August 8, 2002, 9:02 AM
 */

package workbench.gui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FocusTraversalPolicy;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.ActionMap;
import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.FindAction;
import workbench.gui.actions.FindAgainAction;
import workbench.gui.components.TextComponentMouseListener;
import workbench.interfaces.Searchable;
import workbench.resource.ResourceMgr;

/**
 *
 * @author  sql.workbench@freenet.de
 */
public class FindPanel 
	extends JPanel
	implements Searchable
{
	private WbTable searchTable;
	private JTextField findField;
	public WbToolbar toolbar;
	private FindAction findAction;
	private FindAgainAction findAgainAction;
	
	/** Creates a new instance of FindPanel */
	public FindPanel(WbTable aTable)
	{
		this.searchTable = aTable;
		FlowLayout fl = new FlowLayout(FlowLayout.LEFT);
		fl.setHgap(5);
		fl.setVgap(0);
		this.setLayout(fl);
		this.findField = new JTextField(15);
		this.toolbar = new WbToolbar();
		this.findAction = new FindAction(this);
		this.findAgainAction = new FindAgainAction(this);
		this.toolbar.add(this.findAction);
		this.toolbar.add(this.findAgainAction);
		this.add(toolbar);
		this.add(this.findField);
		
		Dimension d = new Dimension(32768, 18);
		this.setMaximumSize(d);
		this.findField.setMaximumSize(d);
		this.findField.addMouseListener(new TextComponentMouseListener());
		this.toolbar.setMaximumSize(d);
		
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		InputMap im = new ComponentInputMap(this);
		ActionMap am = new ActionMap();
		this.setInputMap(this.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, im);
		this.setActionMap(am);
		
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), this.findAction.getActionName());
		im.put(this.findAction.getAccelerator(), this.findAction.getActionName());
		am.put(this.findAction.getActionName(), this.findAction);
		
		im.put(this.findAgainAction.getAccelerator(), this.findAgainAction.getActionName());
		am.put(this.findAgainAction.getActionName(), this.findAgainAction);

		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.setDefaultComponent(findField);
		pol.addComponent(findField);
		pol.addComponent(findAction.getToolbarButton());
		pol.addComponent(findAgainAction.getToolbarButton());
		this.setFocusTraversalPolicy(pol);
	}
	
	public void setFocusToEntryField()
	{
		this.findField.grabFocus();
	}
	
	public void findData()
	{
		this.searchTable.search(this.findField.getText());
	}
	
	public void findNext()
	{
		Window parent = SwingUtilities.getWindowAncestor(this);
		parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		this.searchTable.searchNext();
		parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
}
