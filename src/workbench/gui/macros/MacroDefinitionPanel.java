/*
 * MacroDefinitionPanel.java
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
package workbench.gui.macros;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import workbench.interfaces.StatusBar;
import workbench.resource.ResourceMgr;
import workbench.resource.ShortcutManager;
import workbench.resource.StoreableKeyStroke;

import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.AutoCompletionAction;
import workbench.gui.components.BooleanPropertyEditor;
import workbench.gui.components.StringPropertyEditor;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.settings.KeyboardMapper;
import workbench.gui.sql.EditorPanel;

import workbench.sql.macros.MacroDefinition;

/**
 * A panel displaying the definition of a macro.
 *
 * The following macro properties can be maintained with this panel:
 * <ul>
 *	<li>The macro name</li>
 *	<li>The macro text</li>
 *	<li>The flag to show the macro in the menu</li>
 *	<li>The keyboard shortcut for this macro</li>
 * </ul>
 * @author Thomas Kellerer
 */
public class MacroDefinitionPanel
	extends javax.swing.JPanel
	implements ActionListener
{
	private EditorPanel macroEditor;
	private MacroDefinition currentMacro;
	private AutoCompletionAction completeAction;

	/**
	 * Create a new macro panel
	 * @param l the listener to be notified when the macro name changes
	 */
	public MacroDefinitionPanel(PropertyChangeListener l)
	{
		initComponents();
		macroEditor = EditorPanel.createSqlEditor();
		macroEditor.showFindOnPopupMenu();
		macroEditor.showFormatSql();
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.insets = new Insets(10, 2, 2, 5);
		add(macroEditor, c);

		WbSwingUtilities.setMinimumSize(macroEditor, 25, 70);

		WbTraversalPolicy policy = new WbTraversalPolicy();
		policy.addComponent(tfName);
		policy.addComponent(visibleInMenu);
		policy.addComponent(assignShortcutButton);
		policy.addComponent(clearShortcutButton);
		policy.addComponent(macroEditor);
		policy.setDefaultComponent(macroEditor);
		setFocusTraversalPolicy(policy);

		if (l != null)
		{
			tfName.addPropertyChangeListener(l);
		}
	}

	/**
	 * Displays the passed macro.
	 *
	 * Updates to the macro properties are applied immediately to the
	 * macro instance with the exception of the macro text (as
	 * it uses the standard editor which does not implement the SimplePropertyEditor
	 * interface).
	 *
	 * To make sure the macro instance is up-to-date, applyChanges() must
	 * be called.
	 *
	 * @param macro
	 * @see #applyChanges()
	 */
	public void setMacro(MacroDefinition macro)
	{
		applyChanges();
		currentMacro = macro;
		assignShortcutButton.setEnabled(currentMacro != null);

		BooleanPropertyEditor menu = (BooleanPropertyEditor) visibleInMenu;
		menu.setSourceObject(macro, "visibleInMenu");
		menu.setImmediateUpdate(true);

		BooleanPropertyEditor popup = (BooleanPropertyEditor)visibleInPopup;
		popup.setSourceObject(macro, "visibleInPopup");
		popup.setImmediateUpdate(true);

		BooleanPropertyEditor expand = (BooleanPropertyEditor)doExpansion;
		expand.setSourceObject(macro, "expandWhileTyping");
		expand.setImmediateUpdate(true);

		BooleanPropertyEditor append = (BooleanPropertyEditor)appendResults;
		append.setSourceObject(macro, "appendResult");
		append.setImmediateUpdate(true);

		StringPropertyEditor name = (StringPropertyEditor) tfName;
		name.setSourceObject(macro, "name");
		name.setImmediateUpdate(true);

		updateShortcutDisplay();
		appendResults.setEnabled(!doExpansion.isSelected());

		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				if (currentMacro != null)
				{
					macroEditor.setText(currentMacro.getText());
					macroEditor.setCaretPosition(0);
				}
				else
				{
					macroEditor.setText("");
				}
			}
		});
	}

	public void setCurrentConnection(WbConnection conn)
	{
		macroEditor.setDatabaseConnection(conn);
		if (conn != null)
		{
			completeAction = new AutoCompletionAction(macroEditor, new StatusBar()
			{

				@Override
				public void setStatusMessage(String message, int duration)
				{
				}

				@Override
				public void setStatusMessage(String message)
				{
				}

				@Override
				public void clearStatusMessage()
				{
				}

				@Override
				public void doRepaint()
				{
				}

				@Override
				public String getText()
				{
					return "";
				}
			});

			completeAction.setConnection(conn);
			completeAction.setEnabled(true);
		}
		else if (completeAction != null)
		{
			completeAction.setConnection(null);
			completeAction.setEnabled(false);
			macroEditor.removeKeyBinding(completeAction.getAccelerator());
		}

	}

	public void applyChanges()
	{
		if (currentMacro != null && macroEditor.isModified())
		{
			currentMacro.setText(macroEditor.getText());
		}
	}

	private void updateShortcutDisplay()
	{
		StoreableKeyStroke key = (currentMacro == null ? null : currentMacro.getShortcut());
		if (key != null)
		{
			shortcutLabel.setText("<html>" + ResourceMgr.getString("LblKeyDefKeyCol") + ": <span style=\"background-color:white;font-weight:bold\"><tt>&nbsp;" + key.toString() + "&nbsp;</tt></span></html>");
		}
		else
		{
			shortcutLabel.setText(ResourceMgr.getString("LblKeyDefKeyCol") + ": " + ResourceMgr.getString("LblNone"));
		}
	}

	/**
	 * Puts the focus to the macro name input field.
	 */
	public void selectMacroName()
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				tfName.requestFocusInWindow();
			}
		});
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {
    GridBagConstraints gridBagConstraints;

    jLabel1 = new JLabel();
    tfName = new StringPropertyEditor();
    optionsPanel = new JPanel();
    visibleInMenu = new BooleanPropertyEditor();
    jSeparator1 = new JSeparator();
    visibleInPopup = new BooleanPropertyEditor();
    jSeparator2 = new JSeparator();
    doExpansion = new BooleanPropertyEditor();
    jSeparator3 = new JSeparator();
    appendResults = new BooleanPropertyEditor();
    shortcutPanel = new JPanel();
    shortcutLabel = new JLabel();
    assignShortcutButton = new JButton();
    clearShortcutButton = new JButton();

    setLayout(new GridBagLayout());

    jLabel1.setText(ResourceMgr.getString("LblMacroName")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 5, 0, 0);
    add(jLabel1, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridwidth = 9;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(5, 4, 0, 5);
    add(tfName, gridBagConstraints);

    optionsPanel.setBorder(BorderFactory.createEtchedBorder());
    optionsPanel.setLayout(new GridBagLayout());

    visibleInMenu.setText(ResourceMgr.getString("LblMacroGrpMenu")); // NOI18N
    visibleInMenu.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(0, 5, 0, 5);
    optionsPanel.add(visibleInMenu, gridBagConstraints);

    jSeparator1.setOrientation(SwingConstants.VERTICAL);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.VERTICAL;
    optionsPanel.add(jSeparator1, gridBagConstraints);

    visibleInPopup.setText(ResourceMgr.getString("LblMacroGrpPop")); // NOI18N
    visibleInPopup.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(0, 5, 0, 5);
    optionsPanel.add(visibleInPopup, gridBagConstraints);

    jSeparator2.setOrientation(SwingConstants.VERTICAL);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.VERTICAL;
    optionsPanel.add(jSeparator2, gridBagConstraints);

    doExpansion.setText(ResourceMgr.getString("LblExpandMacro")); // NOI18N
    doExpansion.setToolTipText(ResourceMgr.getString("d_LblExpandMacro")); // NOI18N
    doExpansion.addActionListener(this);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(0, 3, 0, 3);
    optionsPanel.add(doExpansion, gridBagConstraints);

    jSeparator3.setOrientation(SwingConstants.VERTICAL);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.VERTICAL;
    optionsPanel.add(jSeparator3, gridBagConstraints);

    appendResults.setText(ResourceMgr.getString("LblAppendMacroData")); // NOI18N
    appendResults.setToolTipText(ResourceMgr.getString("d_LblAppendMacroData")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 6;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(0, 3, 0, 3);
    optionsPanel.add(appendResults, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(8, 4, 0, 5);
    add(optionsPanel, gridBagConstraints);

    shortcutPanel.setLayout(new GridBagLayout());

    shortcutLabel.setText(ResourceMgr.getString("LblKeyDefKeyCol")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 0, 0, 5);
    shortcutPanel.add(shortcutLabel, gridBagConstraints);

    assignShortcutButton.setText(ResourceMgr.getString("LblAssignShortcut")); // NOI18N
    assignShortcutButton.addActionListener(this);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(6, 7, 0, 0);
    shortcutPanel.add(assignShortcutButton, gridBagConstraints);

    clearShortcutButton.setText(ResourceMgr.getString("LblClearShortcut")); // NOI18N
    clearShortcutButton.addActionListener(this);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(6, 10, 0, 0);
    shortcutPanel.add(clearShortcutButton, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.insets = new Insets(1, 4, 0, 5);
    add(shortcutPanel, gridBagConstraints);
  }

  // Code for dispatching events from components to event handlers.

  public void actionPerformed(ActionEvent evt)
  {
    if (evt.getSource() == doExpansion)
    {
      MacroDefinitionPanel.this.doExpansionActionPerformed(evt);
    }
    else if (evt.getSource() == assignShortcutButton)
    {
      MacroDefinitionPanel.this.assignShortcutButtonActionPerformed(evt);
    }
    else if (evt.getSource() == clearShortcutButton)
    {
      MacroDefinitionPanel.this.clearShortcutButtonActionPerformed(evt);
    }
  }// </editor-fold>//GEN-END:initComponents

	private void assignShortcutButtonActionPerformed(ActionEvent evt)//GEN-FIRST:event_assignShortcutButtonActionPerformed
	{//GEN-HEADEREND:event_assignShortcutButtonActionPerformed
		if (this.currentMacro != null)
		{
			KeyStroke key = KeyboardMapper.getKeyStroke(this);
			if (key != null)
			{
				String clazz = ShortcutManager.getInstance().getActionClassForKey(key);
				String name = ShortcutManager.getInstance().getActionNameForClass(clazz);
				if (name != null)
				{
					String msg = ResourceMgr.getFormattedString("MsgShortcutAlreadyAssigned", name);
					boolean choice = WbSwingUtilities.getYesNo(this, msg);
					if (!choice)
					{
						return;
					}
					ShortcutManager.getInstance().removeShortcut(clazz);
					ShortcutManager.getInstance().updateActions();
				}
				StoreableKeyStroke stroke = new StoreableKeyStroke(key);
				currentMacro.setShortcut(stroke);
				updateShortcutDisplay();
			}
		}
	}//GEN-LAST:event_assignShortcutButtonActionPerformed

	private void clearShortcutButtonActionPerformed(ActionEvent evt)//GEN-FIRST:event_clearShortcutButtonActionPerformed
	{//GEN-HEADEREND:event_clearShortcutButtonActionPerformed
		if (this.currentMacro != null)
		{
			currentMacro.setShortcut(null);
			updateShortcutDisplay();
		}
	}//GEN-LAST:event_clearShortcutButtonActionPerformed

  private void doExpansionActionPerformed(ActionEvent evt)//GEN-FIRST:event_doExpansionActionPerformed
  {//GEN-HEADEREND:event_doExpansionActionPerformed
    appendResults.setEnabled(!doExpansion.isSelected());
  }//GEN-LAST:event_doExpansionActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private JCheckBox appendResults;
  private JButton assignShortcutButton;
  private JButton clearShortcutButton;
  private JCheckBox doExpansion;
  private JLabel jLabel1;
  private JSeparator jSeparator1;
  private JSeparator jSeparator2;
  private JSeparator jSeparator3;
  private JPanel optionsPanel;
  private JLabel shortcutLabel;
  private JPanel shortcutPanel;
  private JTextField tfName;
  private JCheckBox visibleInMenu;
  private JCheckBox visibleInPopup;
  // End of variables declaration//GEN-END:variables
}
