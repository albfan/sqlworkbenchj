/*
 * ProfileEditor.java
 *
 * Created on 1. Juli 2002, 18:34
 */

package workbench.gui.macros;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import workbench.WbManager;
import workbench.exception.WbException;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.DeleteListEntryAction;
import workbench.gui.actions.FormatSqlAction;
import workbench.gui.actions.NewListEntryAction;
import workbench.gui.actions.SaveListFileAction;
import workbench.gui.components.StringPropertyEditor;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbToolbar;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.sql.EditorPanel;
import workbench.interfaces.FileActions;
import workbench.resource.ResourceMgr;
import workbench.sql.MacroManager;

public class MacroManagerGui
	extends JPanel
	implements FileActions, ListSelectionListener, PropertyChangeListener
{
	private JToolBar toolbar;
	private int lastIndex = -1;
	private MacroEntry currentEntry;
	private JSplitPane jSplitPane1;
	private JList macroList;
	private EditorPanel macroEditor;
	private StringPropertyEditor macroNameField;
	private MacroListModel model;

	public MacroManagerGui()
	{
		this.toolbar = new WbToolbar();
		this.toolbar.add(new NewListEntryAction(this));
		this.toolbar.add(new SaveListFileAction(this));
		this.toolbar.addSeparator();
		this.toolbar.add(new DeleteListEntryAction(this));

		JPanel listPanel = new JPanel();
		listPanel.setLayout(new BorderLayout());
		listPanel.add(this.toolbar, BorderLayout.NORTH);

		this.jSplitPane1 = new WbSplitPane();

		this.model = new MacroListModel();
		this.macroList = new JList(this.model);

		this.setLayout(new BorderLayout());

		jSplitPane1.setDividerLocation(100);

		JScrollPane scroll = new JScrollPane(this.macroList);
		scroll.setBorder(WbSwingUtilities.EMPTY_BORDER);
		listPanel.add(scroll, java.awt.BorderLayout.CENTER);

		this.macroEditor = EditorPanel.createSqlEditor();
		this.macroEditor.showFindOnPopupMenu();
		this.macroEditor.showFormatSql();

		jSplitPane1.setLeftComponent(listPanel);

		JPanel namePanel = new JPanel();
		namePanel.setLayout(new BorderLayout());
		JLabel l = new JLabel(ResourceMgr.getString("LabelMacroName"));
		l.setBorder(new CompoundBorder(new EmptyBorder(0,5,0,5), l.getBorder()));
		this.macroNameField = new StringPropertyEditor(); //new JTextField(40);
		this.macroNameField.setColumns(40);
		this.macroNameField.setImmediateUpdate(true);
		this.macroNameField.addPropertyChangeListener(this);
		//this.macroNameField.setBorder(new CompoundBorder(new EmptyBorder(0,0,0,5), this.macroNameField.getBorder()));

		namePanel.add(l, BorderLayout.WEST);
		namePanel.add(this.macroNameField, BorderLayout.CENTER);

		// Create some visiual space above and below the entry field
		JPanel p = new JPanel();
		namePanel.add(p, BorderLayout.SOUTH);
		p = new JPanel();
		namePanel.add(p, BorderLayout.NORTH);


		JPanel editor = new JPanel();
		editor.setLayout(new BorderLayout());
		editor.add(namePanel, BorderLayout.NORTH);
		editor.add(macroEditor, BorderLayout.CENTER);
		jSplitPane1.setRightComponent(editor);

		add(jSplitPane1, BorderLayout.CENTER);
		macroList.addListSelectionListener(this);

		WbTraversalPolicy policy = new WbTraversalPolicy();
		policy.addComponent(macroList);
		policy.addComponent(macroNameField);
		policy.addComponent(macroEditor);
		policy.setDefaultComponent(macroList);
		this.setFocusTraversalPolicy(policy);
		this.setFocusCycleRoot(true);
	}

	public JList getMacroList() { return this.macroList; }

	public String getSelectedMacroName()
	{
		int index = this.macroList.getSelectedIndex();
		if (index < 0) return null;
		String name = this.model.getKeyAt(index);
		return name;
	}

	public void deleteItem()
		throws WbException
	{
		int index = this.macroList.getSelectedIndex();
		if (index < 0) return;
		this.macroList.clearSelection();
		//this.macroList.setValueIsAdjusting(true);
		this.macroEditor.setText("");
		this.macroNameField.setText("");
		this.model.removeElementAt(index);

		// check if the last driver was deleted
		if (index > this.model.getSize() - 1) index--;
		//this.macroList.setValueIsAdjusting(false);

		if (index >= 0)
		{
			this.macroList.setSelectedIndex(index);
			this.macroList.repaint();
		}
	}

	/**
	 *	Create a new profile. This will only be
	 *	created in the ListModel.
	 */
	public void newItem(boolean copyCurrent) throws WbException
	{
		String key;
		String text;
		if (copyCurrent)
		{
			int index = this.macroList.getSelectedIndex();
			key = this.model.getKeyAt(index);
			if (key == null)
			{
				key = ResourceMgr.getString("TxtEmptyMacroName");
				text = "";
			}
			else
			{
				text = MacroManager.getInstance().getMacroText(key);
			}
		}
		else
		{
			key = ResourceMgr.getString("TxtEmptyMacroName");
			text = "";
		}
		MacroEntry entry = this.model.addMacro(key, text);
		this.macroList.setSelectedIndex(this.model.getSize() - 1);
		this.macroList.updateUI();
		this.showMacro(entry);
	}

	private void selectListLater()
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				macroList.requestFocusInWindow();
			}
		});
	}

	public void saveItem() throws WbException
	{
		int index = this.macroList.getSelectedIndex();
		if (index >= 0)
		{
			this.model.setMacroAt(index, this.macroNameField.getText(), this.macroEditor.getText());
		}
		this.model.saveMacros();
	}

	public void saveSettings()
	{
		int location = this.jSplitPane1.getDividerLocation();
		WbManager.getSettings().setProperty(this.getClass().getName(), "divider", location);
		String macro = this.getSelectedMacroName();
		if (macro != null)
		{
			WbManager.getSettings().setProperty(this.getClass().getName(), "lastmacro", macro);
		}
	}

	public void restoreSettings()
	{
		int location = WbManager.getSettings().getIntProperty(this.getClass().getName(), "divider");
		if (location <= 0)
		{
			location = 140;
		}
		this.jSplitPane1.setDividerLocation(location);
		String macro = WbManager.getSettings().getProperty(this.getClass().getName(), "lastmacro", null);
		this.selectMacro(macro);
		this.selectListLater();
	}

	private void selectMacro(String macro)
	{
		if (this.model == null || this.macroList == null) return;
		int count = this.model.getSize();
		boolean selected = false;
		int index = 0;
		if (macro == null)
		{
			this.macroList.setSelectedIndex(0);
			return;
		}

		for (int i=0; i < count; i ++)
		{
			MacroEntry entry = (MacroEntry)this.model.getElementAt(i);
			if (macro.equalsIgnoreCase(entry.getName()))
			{
				macroList.setSelectedIndex(i);
				return;
			}
		}
		// if we get here, the macro was not found
		this.macroList.setSelectedIndex(0);
	}

	public void addSelectionListener(ListSelectionListener aListener)
	{
		this.macroList.addListSelectionListener(aListener);
	}

	public void valueChanged(ListSelectionEvent evt)
	{
		if (this.macroList.getValueIsAdjusting()) return;
		if (this.lastIndex >= 0)
		{
			this.model.setMacroAt(this.lastIndex, this.macroNameField.getText(), this.macroEditor.getText());
		}
		this.lastIndex = this.macroList.getSelectedIndex();
		if (this.lastIndex < 0) return;
		if (this.lastIndex > this.model.getSize() - 1) return;

		MacroEntry entry = (MacroEntry)this.model.getElementAt(this.lastIndex);
		this.showMacro(entry);
	}
	
	private void showMacro(MacroEntry entry)
	{
		this.currentEntry = entry;
		this.macroNameField.setSourceObject(this.currentEntry, "name", entry.getName());
		this.macroNameField.setImmediateUpdate(true);
		this.macroEditor.setText(entry.getText());
		this.macroEditor.setCaretPosition(0);	
	}

	public void propertyChange(java.beans.PropertyChangeEvent evt)
	{
		this.macroList.repaint();
	}

}

class MacroListModel
	extends AbstractListModel
{
	ArrayList macros;

	public MacroListModel()
	{
		List keys = MacroManager.getInstance().getMacroList();
		Collections.sort(keys);
		int size = keys.size();
		if (size == 0)
		{
			macros = new ArrayList(10);
			return;
		}
		macros = new ArrayList(size);

		for (int i=0; i < size; i++)
		{
			String key = (String)keys.get(i);
			String text = MacroManager.getInstance().getMacroText(key);
			macros.add(new MacroEntry(key, text));
		}
	}

	/*
	public void addListDataListener(javax.swing.event.ListDataListener l)
	{
	}
	*/
	public Object getElementAt(int index)
	{
		return this.macros.get(index);
	}

	public int getSize()
	{
		return this.macros.size();
	}

	/*
	public void removeListDataListener(javax.swing.event.ListDataListener l)
	{
	}
	*/

	public void setMacroAt(int index, String aName, String aText)
	{
		if (index < 0 || index >= this.macros.size()) return;
		MacroEntry entry = (MacroEntry)this.macros.get(index);
		entry.setName(aName);
		entry.setText(aText);
		this.fireContentsChanged(this, index, index);
	}

	public MacroEntry addMacro(String aKey, String aText)
	{
		MacroEntry entry = new MacroEntry(aKey, aText);
		this.macros.add(entry);
		int size = this.macros.size();
		this.fireContentsChanged(this, size, size);
		return entry;
	}

	public void removeElementAt(int index)
	{
		this.macros.remove(index);
		this.fireContentsChanged(this, index, index);
	}

	public String getKeyAt(int index)
	{
		String name = null;
		if (index > -1 && index < this.macros.size())
		{
			MacroEntry entry = (MacroEntry)this.macros.get(index);
			name = entry.getName();
		}
		return name;
	}


	public void saveMacros()
	{
		MacroManager mgr = MacroManager.getInstance();
		mgr.clearAll();
		int count = this.macros.size();
		for (int i = 0; i < count; i++)
		{
			MacroEntry entry = (MacroEntry)this.macros.get(i);
			mgr.setMacro(entry.getName(), entry.getText());
		}
		mgr.saveMacros();
	}
}