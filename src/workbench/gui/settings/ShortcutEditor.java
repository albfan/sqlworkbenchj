/*
 * ShortcutEditor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.settings;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.sql.Types;
import javax.swing.ActionMap;

import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.EscAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.DividerBorder;
import workbench.gui.components.WbButton;
import workbench.gui.components.WbTable;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.resource.ShortcutDefinition;
import workbench.resource.ShortcutManager;
import workbench.resource.StoreableKeyStroke;
import workbench.storage.DataStore;

/**
 * @author support@sql-workbench.net
 *
 */
public class ShortcutEditor
	extends JPanel
	implements ActionListener, ListSelectionListener, WindowListener, MouseListener
{
	private static final String KEY_WINDOW_SIZE = "workbench.shortcuteditor";
	private WbTable keysTable;
	private DataStore definitions;
	private DataStoreTableModel model;
	private JDialog window;
	private Frame parent;
	
	private JButton okButton;
	private JButton cancelButton;
	private JButton assignButton;
	private JButton resetButton;
	private JButton resetAllButton;
	private JButton clearButton;
	
	private String escActionCommand;
	
	public ShortcutEditor(Frame parent)
	{
		this.parent = parent;
	}
	
	public void showWindow()
	{
		WbSwingUtilities.showWaitCursor(parent);
		window = new JDialog(parent, ResourceMgr.getString("LblConfigureShortcutWindowTitle"), true);
		window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		window.addWindowListener(this);
		JPanel contentPanel = new JPanel(new BorderLayout());
		
		this.keysTable = new WbTable();
		this.keysTable.setShowPopupMenu(false);
		this.keysTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		this.setLayout(new BorderLayout());
		JScrollPane scroll = new JScrollPane(this.keysTable);
		contentPanel.add(scroll, BorderLayout.CENTER);
		
		JRootPane root = window.getRootPane();
		InputMap im = root.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		ActionMap am = root.getActionMap();
		EscAction esc = new EscAction(this);
		escActionCommand = esc.getActionName();
		im.put(esc.getAccelerator(), esc.getActionName());
		am.put(esc.getActionName(), esc);
		
		this.createModel();
		this.keysTable.setRowSelectionAllowed(true);
		this.keysTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.keysTable.setAdjustToColumnLabel(true);
		this.keysTable.optimizeAllColWidth(80,-1,true);
		this.keysTable.addMouseListener(this);
		this.cancelButton = new WbButton(ResourceMgr.getString("LblCancel"));
		this.cancelButton.addActionListener(this);

		im = keysTable.getInputMap(JComponent.WHEN_FOCUSED);
		am = keysTable.getActionMap();
		im.put(esc.getAccelerator(), esc.getActionName());
		am.put(esc.getActionName(), esc);
		
		this.okButton = new WbButton(ResourceMgr.getString("LblOK"));
		this.okButton.addActionListener(this);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(this.okButton);
		buttonPanel.add(this.cancelButton);
		this.add(buttonPanel, BorderLayout.SOUTH);

		Box b = Box.createHorizontalBox();
		Box editBox = Box.createVerticalBox();

		Dimension min = new Dimension(90, 24);
		Dimension max = new Dimension(160, 24);
		
		this.assignButton = new WbButton(ResourceMgr.getString("LblAssignShortcut"));
		this.assignButton.setToolTipText(ResourceMgr.getDescription("LblAssignShortcut"));
		this.assignButton.addActionListener(this);
		this.assignButton.setEnabled(false);
		this.assignButton.setPreferredSize(min);
		this.assignButton.setMinimumSize(min);
		this.assignButton.setMaximumSize(max);

		this.clearButton = new WbButton(ResourceMgr.getString("LblClearShortcut"));
		this.clearButton.setToolTipText(ResourceMgr.getDescription("LblClearShortcut"));
		this.clearButton.addActionListener(this);
		this.clearButton.setEnabled(false);
		this.clearButton.setPreferredSize(min);
		this.clearButton.setMinimumSize(min);
		this.clearButton.setMaximumSize(max);
		
		this.resetButton = new WbButton(ResourceMgr.getString("LblResetShortcut"));
		this.resetButton.setToolTipText(ResourceMgr.getDescription("LblResetShortcut"));
		this.resetButton.addActionListener(this);
		this.resetButton.setEnabled(false);
		this.resetButton.setPreferredSize(min);
		this.resetButton.setMinimumSize(min);
		this.resetButton.setMaximumSize(max);

		this.resetAllButton = new WbButton(ResourceMgr.getString("LblResetAllShortcuts"));
		this.resetAllButton.setToolTipText(ResourceMgr.getDescription("LblResetAllShortcuts"));
		this.resetAllButton.addActionListener(this);
		this.resetAllButton.setPreferredSize(min);
		this.resetAllButton.setMinimumSize(min);
		this.resetAllButton.setMaximumSize(max);
		
		editBox.add(Box.createVerticalStrut(2));
		editBox.add(this.assignButton);
		editBox.add(Box.createVerticalStrut(2));
		editBox.add(this.clearButton);
		editBox.add(Box.createVerticalStrut(15));
		editBox.add(this.resetButton);
		editBox.add(Box.createVerticalStrut(2));
		editBox.add(this.resetAllButton);
		editBox.add(Box.createVerticalGlue());
		b.add(Box.createHorizontalStrut(10));
		b.add(editBox);
		b.add(Box.createHorizontalStrut(5));
		contentPanel.add(b, BorderLayout.EAST);

		JPanel p = new JPanel();
		Dimension d = new Dimension(1,20);
		p.setMinimumSize(d);
		p.setPreferredSize(d);
		p.setBorder(new DividerBorder(DividerBorder.HORIZONTAL_MIDDLE));
		contentPanel.add(p, BorderLayout.SOUTH);

		p = new JPanel();
		d = new Dimension(5,1);
		p.setMinimumSize(d);
		p.setPreferredSize(d);
		contentPanel.add(p, BorderLayout.WEST);

		p = new JPanel();
		d = new Dimension(1,5);
		p.setMinimumSize(d);
		p.setPreferredSize(d);
		contentPanel.add(p, BorderLayout.NORTH);
		
		this.add(contentPanel, BorderLayout.CENTER);
		
		window.getContentPane().add(this);
		if (!Settings.getInstance().restoreWindowSize(this.window, KEY_WINDOW_SIZE))
		{	
			window.setSize(600,400);
		}
		WbSwingUtilities.center(window, parent);
		WbSwingUtilities.showDefaultCursor(parent);
		window.setVisible(true);
	}
	
	private void createModel()
	{
		ShortcutDefinition[] keys = Settings.getInstance().getShortcutManager().getDefinitions();
		
		String[] cols = new String[] { ResourceMgr.getString("LblKeyDefCommandCol"), 
                                   ResourceMgr.getString("LblKeyDefKeyCol"),
				                           ResourceMgr.getString("LblKeyDefDefaultCol") };
		int[] types = new int[] { Types.VARCHAR, Types.OTHER, Types.OTHER };
		
		this.definitions = new DataStore(cols, types);
		
		for (int i=0; i < keys.length; i++)
		{
			int row = this.definitions.addRow();
			this.definitions.setValue(row, 0, Settings.getInstance().getShortcutManager().getActionNameForClass(keys[i].getActionClass()));
			this.definitions.setValue(row, 1, new ShortcutDisplay(keys[i], ShortcutDisplay.TYPE_PRIMARY_KEY));
			this.definitions.setValue(row, 2, new ShortcutDisplay(keys[i], ShortcutDisplay.TYPE_DEFAULT_KEY));
		}
		this.definitions.sortByColumn(0, true);
		this.model = new DataStoreTableModel(this.definitions);
		this.model.setAllowEditing(false);
		this.keysTable.setModel(model, true);
		this.keysTable.getSelectionModel().addListSelectionListener(this);
	}

	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
		
		if (source == this.cancelButton) 
		{
			this.closeWindow();
		}
		else if (source == this.okButton)
		{
			this.saveShortcuts();
			this.closeWindow();
		}
		else if (source == this.assignButton)
		{
			this.assignKey();
		}
		else if (source == this.resetButton)
		{
			this.resetCurrentKey();
		}
		else if (source == this.resetAllButton)
		{
			this.resetAllKeys();
		}
		else if (source == this.clearButton)
		{
			this.clearKey();
		}
		else if (e.getActionCommand().equals(escActionCommand))
		{
			this.closeWindow();
		}
	}

	private void saveSettings()
	{
		Settings.getInstance().storeWindowSize(this.window, KEY_WINDOW_SIZE);
	}

	private void saveShortcuts()
	{
		ShortcutManager mgr = Settings.getInstance().getShortcutManager();
		int count = this.definitions.getRowCount();
		for (int row = 0; row < count; row++)
		{
			ShortcutDisplay d = (ShortcutDisplay)this.definitions.getValue(row, 1);
			ShortcutDefinition def = d.getShortcut();
			if (d.isModified())
			{	
				if (d.isCleared())
				{
					mgr.removeShortcut(def.getActionClass());
				}
				else if (d.doReset())
				{
					mgr.resetToDefault(def.getActionClass());
				}
				else
				{
					mgr.assignKey(def.getActionClass(), d.getNewKey().getKeyStroke());
				}
			}
		}
		mgr.updateActions();
	}
	
	private void closeWindow()
	{
		this.saveSettings();
		this.window.setVisible(false);
		this.window.dispose();
	}

	public void valueChanged(ListSelectionEvent e)
	{
		boolean enabled = (e.getFirstIndex() >= 0);
		this.resetButton.setEnabled(enabled);
		this.assignButton.setEnabled(enabled);
		this.clearButton.setEnabled(enabled);
	}
	
	private void assignKey()
	{
		int row = this.keysTable.getSelectedRow();
		if (row < 0) return;
		final KeyboardMapper mapper = new KeyboardMapper();
		SwingUtilities.invokeLater(new Runnable() {
			public void run()
			{
				mapper.grabFocus();
			}
		});
		String[] options = new String[] { ResourceMgr.getString("LblOK").replaceAll("&", ""), ResourceMgr.getString("LblCancel").replaceAll("&", "")};
		
		
		JOptionPane overwritePane = new JOptionPane(mapper, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options);
		JDialog dialog = overwritePane.createDialog(this, ResourceMgr.getString("LblEnterKeyWindowTitle"));
		
		dialog.setResizable(true);
		dialog.setVisible(true);
		Object result = overwritePane.getValue();
		dialog.dispose();
		
		if (options[0].equals(result))
		{
			KeyStroke key = mapper.getKeyStroke();
			int oldrow = this.findKey(key);
			if (oldrow > -1)
			{
				String name = this.definitions.getValueAsString(oldrow, 0);
				String msg = ResourceMgr.getString("MsgShortcutAlreadyAssigned").replaceAll("%action%", name);
				boolean choice = WbSwingUtilities.getYesNo(this, msg);
				if (!choice) return;
				ShortcutDisplay old = (ShortcutDisplay)this.definitions.getValue(oldrow, 1);
				old.clearKey();
				this.model.fireTableRowsUpdated(oldrow, oldrow);
			}
			ShortcutDisplay d = (ShortcutDisplay)this.definitions.getValue(row, 1);
			d.setNewKey(key);
			this.model.fireTableRowsUpdated(row, row);
		}
	}
	
	private void clearKey()
	{
		int row = this.keysTable.getSelectedRow();
		if (row < 0) return;
		ShortcutDisplay old = (ShortcutDisplay)this.definitions.getValue(row, 1);
		old.clearKey();
		this.model.fireTableRowsUpdated(row, row);
	}
	
	private void resetCurrentKey()
	{
		int row = this.keysTable.getSelectedRow();
		if (row < 0) return;
		ShortcutDisplay d = (ShortcutDisplay)this.definitions.getValue(row, 1);
		d.resetToDefault();
		this.model.fireTableRowsUpdated(row, row);
		this.repaint();
	}

	private void resetAllKeys()
	{
		int selected = this.keysTable.getSelectedRow();
		int count = this.keysTable.getRowCount();
		for (int row=0; row < count; row++)
		{	
			ShortcutDisplay d = (ShortcutDisplay)this.definitions.getValue(row, 1);
			d.resetToDefault();
		}
		this.model.fireTableDataChanged();
		if (selected > -1)
		{	
			this.keysTable.getSelectionModel().setSelectionInterval(selected, selected);
		}
	}
	
	private int findKey(KeyStroke key)
	{
		int count = this.definitions.getRowCount();
		for (int row = 0; row < count; row++)
		{
			ShortcutDisplay d = (ShortcutDisplay)this.definitions.getValue(row, 1);
			if (!d.isCleared() && d.isMappedTo(key))
			{
				return row;
			}
		}
		return -1;
	}

	public void windowActivated(WindowEvent e)
	{
	}

	public void windowClosed(WindowEvent e)
	{
	}

	public void windowClosing(WindowEvent e)
	{
		this.closeWindow();	
	}

	public void windowDeactivated(WindowEvent e)
	{
	}

	public void windowDeiconified(WindowEvent e)
	{
	}

	public void windowIconified(WindowEvent e)
	{
	}

	public void windowOpened(WindowEvent e)
	{
	}

	public void mouseClicked(MouseEvent e)
	{
		if (e.getSource() == this.keysTable && 
				e.getClickCount() == 2 &&
				e.getButton() == MouseEvent.BUTTON1)
		{
			this.assignKey();
		}
	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
	}

	public void mousePressed(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
	}
}

class ShortcutDisplay
{
	public static final int TYPE_DEFAULT_KEY = 1;
	public static final int TYPE_PRIMARY_KEY = 2;
	public static final int TYPE_ALTERNATE_KEY = 3;
	
	private boolean isModified = false;
	private int displayType;
	private ShortcutDefinition shortcut;
	private boolean clearKey = false;
	private boolean resetToDefault = false;
	
	private StoreableKeyStroke newKey = null;
	
	ShortcutDisplay(ShortcutDefinition def, int type)
	{
		this.shortcut = def;
		this.displayType = type;
	}
	
	public ShortcutDefinition getShortcut()
	{
		return this.shortcut;
	}

	public boolean isModified() { return this.isModified; }
	public boolean isCleared()
	{
		return this.clearKey;
	}
	
	public void clearKey()
	{
		this.newKey = null;
		this.clearKey = true;
		this.isModified = true;
		this.resetToDefault = false;
	}
	
	public void setNewKey(KeyStroke aKey)
	{
		this.newKey = new StoreableKeyStroke(aKey);
		this.isModified = true;
		this.resetToDefault = false;
		this.clearKey = false;
	}

	public StoreableKeyStroke getNewKey()
	{
		return this.newKey;
	}
	
	public boolean isMappedTo(KeyStroke aKey)
	{
		boolean mapped = false;
		if (newKey != null)
		{
			mapped = newKey.equals(aKey);
		}
		if (!mapped)
		{
			mapped = this.shortcut.isMappedTo(aKey);
		}
		return mapped;
	}
	
	public boolean doReset()
	{
		return this.resetToDefault;
	}
	
	public void resetToDefault()
	{
		this.isModified = true;
		this.newKey = null;
		this.clearKey = false;
		this.resetToDefault = true;
	}
	
	public String toString()
	{
		StoreableKeyStroke key = null;
		switch (this.displayType)
		{
			case TYPE_DEFAULT_KEY:
				key = this.shortcut.getDefaultKey();
				break;
			case TYPE_PRIMARY_KEY:
				if (this.clearKey)
				{
					key = null;
				}
				else if (this.resetToDefault)
				{
					key = this.shortcut.getDefaultKey();
				}
				else if (this.newKey == null)
				{
					key = this.shortcut.getActiveKey();
				}
				else 
				{
					key = this.newKey;
				}
				break;
			case TYPE_ALTERNATE_KEY:
				key = this.shortcut.getAlternateKey();
				break;
		}
		if (key == null) return "";
		return key.toString();
	}
}
