/*
 * ShortcutEditor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.sql.Types;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ActionRegistration;
import workbench.gui.actions.EscAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.DividerBorder;
import workbench.gui.components.ColumnWidthOptimizer;
import workbench.gui.components.WbButton;
import workbench.gui.components.WbTable;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.resource.ShortcutDefinition;
import workbench.resource.ShortcutManager;
import workbench.resource.StoreableKeyStroke;
import workbench.sql.macros.MacroDefinition;
import workbench.sql.macros.MacroManager;
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

	public ShortcutEditor(Frame fparent)
	{
		super();
		this.parent = fparent;

		// make sure actions that are not created upon startup are
		// registered with us!
		ActionRegistration.registerActions();
	}

	public void showWindow()
	{
		WbSwingUtilities.showWaitCursor(parent);
		window = new JDialog(parent, ResourceMgr.getString("LblConfigureShortcutWindowTitle"), true);
		window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		window.addWindowListener(this);
		JPanel contentPanel = new JPanel(new BorderLayout());

		this.keysTable = new WbTable();
		this.keysTable.useMultilineTooltip(false);
		this.keysTable.setShowPopupMenu(false);
		this.keysTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		this.setLayout(new BorderLayout());
		JScrollPane scroll = new JScrollPane(this.keysTable);
		contentPanel.add(scroll, BorderLayout.CENTER);

		EscAction esc = new EscAction(window, this);
		escActionCommand = esc.getActionName();

		this.createModel();
		this.keysTable.setRowSelectionAllowed(true);
		this.keysTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.keysTable.setAdjustToColumnLabel(true);
		ColumnWidthOptimizer optimizer = new ColumnWidthOptimizer(this.keysTable);
		optimizer.optimizeAllColWidth(80,-1,true);
		this.keysTable.addMouseListener(this);
		this.cancelButton = new WbButton(ResourceMgr.getString("LblCancel"));
		this.cancelButton.addActionListener(this);

		okButton = new WbButton(ResourceMgr.getString("LblOK"));
		okButton.addActionListener(this);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(this.okButton);
		buttonPanel.add(this.cancelButton);
		this.add(buttonPanel, BorderLayout.SOUTH);

		JPanel editPanel = new JPanel();
		editPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,10,5,10);
		this.assignButton = new WbButton(ResourceMgr.getString("LblAssignShortcut"));
		this.assignButton.setToolTipText(ResourceMgr.getDescription("LblAssignShortcut"));
		this.assignButton.addActionListener(this);
		this.assignButton.setEnabled(false);
		editPanel.add(assignButton, c);

		c.gridy ++;
		this.clearButton = new WbButton(ResourceMgr.getString("LblClearShortcut"));
		this.clearButton.setToolTipText(ResourceMgr.getDescription("LblClearShortcut"));
		this.clearButton.addActionListener(this);
		this.clearButton.setEnabled(false);
		editPanel.add(clearButton, c);

		c.gridy ++;
		c.insets = new Insets(15,10,5,10);
		this.resetButton = new WbButton(ResourceMgr.getString("LblResetShortcut"));
		this.resetButton.setToolTipText(ResourceMgr.getDescription("LblResetShortcut"));
		this.resetButton.addActionListener(this);
		this.resetButton.setEnabled(false);
		editPanel.add(resetButton, c);

		c.gridy ++;
		c.insets = new Insets(2,10,5,10);
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		this.resetAllButton = new WbButton(ResourceMgr.getString("LblResetAllShortcuts"));
		this.resetAllButton.setToolTipText(ResourceMgr.getDescription("LblResetAllShortcuts"));
		this.resetAllButton.addActionListener(this);
		editPanel.add(resetAllButton, c);

		contentPanel.add(editPanel, BorderLayout.EAST);

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
		ShortcutManager mgr = ShortcutManager.getInstance();
		ShortcutDefinition[] keys = mgr.getDefinitions();

		String[] cols = new String[] { ResourceMgr.getString("LblKeyDefCommandCol"),
                                   ResourceMgr.getString("LblKeyDefKeyCol"),
				                           ResourceMgr.getString("LblKeyDefDefaultCol") };
		int[] types = new int[] { Types.VARCHAR, Types.OTHER, Types.OTHER };

		this.definitions = new DataStore(cols, types);


		for (int i=0; i < keys.length; i++)
		{
			int row = this.definitions.addRow();
			String cls = keys[i].getActionClass();
			String title = mgr.getActionNameForClass(cls);
			String tooltip = mgr.getTooltip(cls);
			ActionDisplay disp = new ActionDisplay(title, tooltip);
			this.definitions.setValue(row, 0, disp);
			this.definitions.setValue(row, 1, new ShortcutDisplay(keys[i], ShortcutDisplay.TYPE_PRIMARY_KEY));
			this.definitions.setValue(row, 2, new ShortcutDisplay(keys[i], ShortcutDisplay.TYPE_DEFAULT_KEY));
		}
		this.definitions.sortByColumn(0, true);
		this.model = new DataStoreTableModel(this.definitions);
		this.model.setAllowEditing(false);
		this.keysTable.setModel(model, true);
		TableColumn col = this.keysTable.getColumnModel().getColumn(0);
		col.setCellRenderer(new ActionDisplayRenderer());
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
		ShortcutManager mgr = ShortcutManager.getInstance();
		int count = this.definitions.getRowCount();
		boolean modified = false;
		for (int row = 0; row < count; row++)
		{
			ShortcutDisplay d = (ShortcutDisplay)this.definitions.getValue(row, 1);
			ShortcutDefinition def = d.getShortcut();
			if (d.isModified())
			{
				modified = true;
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
		if (modified)
		{
			mgr.updateActions();
			mgr.fireShortcutsChanged();
		}
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

		KeyStroke key = KeyboardMapper.getKeyStroke(this);
		if (key != null)
		{

			ShortcutDisplay d = (ShortcutDisplay)this.definitions.getValue(row, 1);

			int oldrow = this.findKey(key);
			if (oldrow > -1)
			{
				String name = this.definitions.getValueAsString(oldrow, 0);
				String msg = ResourceMgr.getFormattedString("MsgShortcutAlreadyAssigned", name);
				boolean choice = WbSwingUtilities.getYesNo(this, msg);
				if (!choice) return;

				ShortcutDisplay old = (ShortcutDisplay)this.definitions.getValue(oldrow, 1);
				old.clearKey();
				this.model.fireTableRowsUpdated(oldrow, oldrow);
			}

			MacroDefinition def = MacroManager.getInstance().getMacroForKeyStroke(key);
			if (def != null)
			{
				String msg = ResourceMgr.getFormattedString("MsgShortcutMacroAlreadyAssigned", def.getName());
				boolean choice = WbSwingUtilities.getYesNo(this, msg);
				if (!choice) return;
				def.setShortcut(null);
			}

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
		WbSwingUtilities.repaintNow(this);
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
