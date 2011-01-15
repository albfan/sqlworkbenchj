/*
 * MacroDefinitionPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.AutoCompletionAction;
import workbench.gui.components.BooleanPropertyEditor;
import workbench.gui.components.StringPropertyEditor;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.settings.KeyboardMapper;
import workbench.gui.sql.EditorPanel;
import workbench.interfaces.StatusBar;
import workbench.resource.ResourceMgr;
import workbench.resource.ShortcutManager;
import workbench.resource.StoreableKeyStroke;
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
		c.gridy = 2;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.insets = new Insets(10, 2, 2, 5);
		add(macroEditor, c);

		WbTraversalPolicy policy = new WbTraversalPolicy();
		policy.addComponent(jTextField1);
		policy.addComponent(jCheckBox1);
		policy.addComponent(assignShortcutButton);
		policy.addComponent(clearShortcutButton);
		policy.addComponent(macroEditor);
		policy.setDefaultComponent(macroEditor);
		setFocusTraversalPolicy(policy);

		jTextField1.addPropertyChangeListener(l);
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

		BooleanPropertyEditor menu = (BooleanPropertyEditor) jCheckBox1;
		menu.setSourceObject(macro, "visibleInMenu");
		menu.setImmediateUpdate(true);

		StringPropertyEditor name = (StringPropertyEditor) jTextField1;
		name.setSourceObject(macro, "name");
		name.setImmediateUpdate(true);

		updateShortcutDisplay();

		EventQueue.invokeLater(new Runnable()
		{
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
				public void repaint()
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
			shortcutLabel.setText(ResourceMgr.getString("LblKeyDefKeyCol") + ": " + key.toString());
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
			public void run()
			{
				jTextField1.requestFocusInWindow();
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
  private void initComponents() {
		GridBagConstraints gridBagConstraints;

    jLabel1 = new JLabel();
    jTextField1 = new StringPropertyEditor();
    jCheckBox1 = new BooleanPropertyEditor();
    shortcutLabel = new JLabel();
    assignShortcutButton = new JButton();
    clearShortcutButton = new JButton();
    jSeparator1 = new JSeparator();

    setLayout(new GridBagLayout());

    jLabel1.setText(ResourceMgr.getString("LblMacroName")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 5, 0, 0);
    add(jLabel1, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridwidth = 5;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(5, 4, 0, 5);
    add(jTextField1, gridBagConstraints);

    jCheckBox1.setText(ResourceMgr.getString("LblMacroGrpMenu")); // NOI18N
    jCheckBox1.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(8, 5, 0, 5);
    add(jCheckBox1, gridBagConstraints);

    shortcutLabel.setText(ResourceMgr.getString("LblKeyDefKeyCol")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(7, 9, 0, 5);
    add(shortcutLabel, gridBagConstraints);

    assignShortcutButton.setText(ResourceMgr.getString("LblAssignShortcut")); // NOI18N
    assignShortcutButton.addActionListener(this);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(6, 7, 0, 0);
    add(assignShortcutButton, gridBagConstraints);

    clearShortcutButton.setText(ResourceMgr.getString("LblClearShortcut")); // NOI18N
    clearShortcutButton.addActionListener(this);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(6, 10, 0, 0);
    add(clearShortcutButton, gridBagConstraints);

    jSeparator1.setOrientation(SwingConstants.VERTICAL);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new Insets(5, 4, 0, 0);
    add(jSeparator1, gridBagConstraints);
  }

  // Code for dispatching events from components to event handlers.

  public void actionPerformed(java.awt.event.ActionEvent evt) {
    if (evt.getSource() == assignShortcutButton) {
      MacroDefinitionPanel.this.assignShortcutButtonActionPerformed(evt);
    }
    else if (evt.getSource() == clearShortcutButton) {
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

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private JButton assignShortcutButton;
  private JButton clearShortcutButton;
  private JCheckBox jCheckBox1;
  private JLabel jLabel1;
  private JSeparator jSeparator1;
  private JTextField jTextField1;
  private JLabel shortcutLabel;
  // End of variables declaration//GEN-END:variables
}
