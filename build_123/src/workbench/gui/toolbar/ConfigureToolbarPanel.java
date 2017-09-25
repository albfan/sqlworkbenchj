/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.toolbar;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import workbench.interfaces.ValidatingComponent;
import workbench.resource.IconMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.WbAction;

import workbench.util.FileDialogUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class ConfigureToolbarPanel
  extends JPanel
  implements ValidatingComponent
{
  private List<WbAction> allActions;

  public ConfigureToolbarPanel(List<WbAction> actions)
  {
    initComponents();
    addItem.setIcon(IconMgr.getInstance().getLabelIcon("move-right"));
    addItem.setText("");
    removeItem.setIcon(IconMgr.getInstance().getLabelIcon("move-left"));
    removeItem.setText("");

    allActions = new ArrayList<>(actions);
    allActions.sort((WbAction o1, WbAction o2) -> StringUtil.compareStrings(o1.getMenuLabel(), o2.getMenuLabel(), true));

    allActionList.setCellRenderer(new ActionRenderer());
    configuredActions.setCellRenderer(new ActionRenderer());

    List<String> commands = ToolbarBuilder.getConfiguredToolbarCommands();
    allActionList.setModel(new ActionListModel(getAvailableToolbarActions(commands)));
    configuredActions.setModel(new ActionListModel(getConfiguredActions(commands)));
  }

  private void reset()
  {
    List<String> commands = ToolbarBuilder.getDefaultToolbarCommands();
    allActionList.setModel(new ActionListModel(getAvailableToolbarActions(commands)));
    configuredActions.setModel(new ActionListModel(getConfiguredActions(commands)));
  }

  private boolean isEmpty(int[] values)
  {
    return values == null || values.length == 0;
  }

  private boolean isNotEmpty(int[] values)
  {
    return values != null || values.length > 0;
  }

  private void addItem()
  {
    int[] selected = allActionList.getSelectedIndices();
    ActionListModel sourceModel = (ActionListModel)allActionList.getModel();
    ActionListModel targetModel = (ActionListModel)configuredActions.getModel();

    if (isEmpty(selected)) return;
    int targetIndex = -1;

    if (configuredActions.getMaxSelectionIndex() > -1)
    {
      targetIndex = configuredActions.getMaxSelectionIndex() + 1;
    }
    else
    {
      targetIndex = targetModel.getSize();
    }

    for (int i=selected.length - 1; i >=0 ; i--)
    {
      Object obj = sourceModel.getElementAt(selected[i]);
      targetModel.addItem(targetIndex, obj);
      targetIndex ++;
      sourceModel.removeItem(selected[i]);
    }
    configuredActions.setSelectedIndex(targetIndex - 1);
  }

  private void removeItem()
  {
    int[] selected = configuredActions.getSelectedIndices();
    if (isEmpty(selected)) return;

    ActionListModel sourceModel = (ActionListModel)configuredActions.getModel();
    ActionListModel targetModel = (ActionListModel)allActionList.getModel();

    for (int i=selected.length - 1; i >=0 ; i--)
    {
      Object obj = sourceModel.getElementAt(selected[i]);

      // Don't add a separator to the available actions
      if (obj instanceof WbAction)
      {
        targetModel.addItem(obj);
      }
      sourceModel.removeItem(selected[i]);
    }
    configuredActions.clearSelection();
  }

  private void checkAddButton()
  {
    int[] selected = allActionList.getSelectedIndices();
    addItem.setEnabled(isNotEmpty(selected));
    addSeparator.setEnabled(isNotEmpty(selected));
  }

  private void checkRemoveButton()
  {
    int[] selected = configuredActions.getSelectedIndices();
    removeItem.setEnabled(isNotEmpty(selected));
  }

  private void selectIcon()
  {
    int index = configuredActions.getSelectedIndex();
    ActionListModel model = (ActionListModel)configuredActions.getModel();
    Object obj = model.getElementAt(index);
    if (obj instanceof WbAction)
    {
      WbFile icon = FileDialogUtil.selectPngFile(this, "workbench.toolbaricon.lastdir");
      if (icon != null)
      {
        String name = FileDialogUtil.removeConfigDir(icon);
        ((WbAction)obj).setCustomIconFile(name);
        configuredActions.invalidate();
        WbSwingUtilities.repaintLater(configuredActions);
      }
    }
  }

  private void removeCustomIcon()
  {
    int index = configuredActions.getSelectedIndex();
    ActionListModel model = (ActionListModel)configuredActions.getModel();
    Object obj = model.getElementAt(index);
    if (obj instanceof WbAction)
    {
      WbAction action = (WbAction)obj;
      action.setCustomIconFile(null);
      configuredActions.invalidate();
      WbSwingUtilities.repaintLater(configuredActions);
    }
  }

  private void checkIconButtons()
  {
    assignIconButton.setEnabled(false);
    removeIconButton.setEnabled(false);
    int[] selected = configuredActions.getSelectedIndices();
    if (selected.length == 1)
    {
      ActionListModel model = (ActionListModel)configuredActions.getModel();
      Object obj = model.getElementAt(selected[0]);
      if (obj instanceof WbAction)
      {
        WbAction action = (WbAction)obj;
        removeIconButton.setEnabled(action.hasCustomIcon());
        assignIconButton.setEnabled(true);
      }
    }
  }

  private void checkMoveButtons()
  {
    int[] selected = configuredActions.getSelectedIndices();
    if (isEmpty(selected))
    {
      moveDown.setEnabled(false);
      moveUp.setEnabled(false);
      return;
    }

    moveUp.setEnabled(true);
    moveDown.setEnabled(true);

    if (selected[0] == 0)
    {
      moveUp.setEnabled(false);
    }
    if (selected[selected.length-1] == configuredActions.getModel().getSize()-1)
    {
      moveDown.setEnabled(false);
    }
  }

  private void addSeparator()
  {
    int index = configuredActions.getMaxSelectionIndex();
    ActionListModel model = (ActionListModel)configuredActions.getModel();

    int toSelect = -1;

    if (index == model.getSize() - 1 || index < 0)
    {
      model.addItem(ToolbarBuilder.SEPARATOR_KEY);
      toSelect = model.getSize() - 1;
    }
    else
    {
      model.addItem(index + 1, ToolbarBuilder.SEPARATOR_KEY);
      toSelect = index + 1;
    }
    configuredActions.setSelectedIndex(toSelect);
  }

  private void moveUp()
  {
    int[] selected = configuredActions.getSelectedIndices();
    if (selected == null || selected.length == 0) return;
    ActionListModel model = (ActionListModel)configuredActions.getModel();
    int firstIndex = selected[0];
    if (firstIndex <= 0) return;

    int lastIndex = selected[selected.length -1];

    Object oldFirst = model.getElementAt(firstIndex - 1);

    for (int i=0; i < selected.length; i++)
    {
      model.set(selected[i] - 1, model.getElementAt(selected[i]));
    }
    model.set(selected[selected.length - 1], oldFirst);

    configuredActions.setSelectionInterval(firstIndex - 1, lastIndex -1);
  }

  private void moveDown()
  {
    int[] selected = configuredActions.getSelectedIndices();
    if (selected == null || selected.length == 0) return;
    ActionListModel model = (ActionListModel)configuredActions.getModel();

    int lastIndex = selected[selected.length -1];
    if (lastIndex >= model.getSize() -1) return;

    int firstIndex = selected[0];

    Object oldLast = model.getElementAt(lastIndex + 1);

    for (int i=selected.length - 1; i >=0 ; i--)
    {
      model.set(selected[i] + 1, model.getElementAt(selected[i]));
    }
    model.set(selected[0], oldLast);

    configuredActions.setSelectionInterval(firstIndex + 1, lastIndex + 1);
  }

  private List getConfiguredActions(List<String> commands)
  {
    List<WbAction> actions = getAllToolbarActions();
    List result = new ArrayList(actions.size());

    for (String cmd : commands)
    {
      if (cmd.equals(ToolbarBuilder.SEPARATOR_KEY))
      {
        result.add(cmd);
      }
      else
      {
        WbAction action = findAction(actions, cmd);
        if (action != null)
        {
          result.add(action);
        }
      }
    }
    return result;
  }

  private List<WbAction> getAvailableToolbarActions(List<String> commands)
  {
    List<WbAction> actions = getAllToolbarActions();
    List result = new ArrayList(actions.size());
    for (WbAction action : actions)
    {
      if (!commands.contains(action.getActionCommand()))
      {
        result.add(action);
      }
    }
    return result;
  }

  private List<WbAction> getAllToolbarActions()
  {
    List<WbAction> result = new ArrayList<>(allActions.size());
    for (WbAction action : allActions)
    {
      if (action.useInToolbar() && !result.contains(action))
      {
        result.add(action);
      }
    }
    return result;
  }

  private WbAction findAction(List<WbAction> actions, String actionCommand)
  {
    for (WbAction action : actions)
    {
      if (action.getActionCommand().equals(actionCommand)) return action;
    }
    return null;
  }

  @Override
  public boolean validateInput()
  {
    // this is only called when the OK button is clicked, so we need store the new configuration
    ActionListModel model = (ActionListModel)configuredActions.getModel();
    String commands = model.getToolbarCommands();
    Settings.getInstance().setProperty(ToolbarBuilder.CONFIG_PROPERTY, commands);
    List<WbAction> actions = model.getActions();
    for (WbAction action : actions)
    {
      if (action.hasCustomIcon())
      {
        String iconFile = action.getCustomIconFile();
        Settings.getInstance().setProperty(action.getCustomIconProperty(), iconFile);
      }
      else
      {
        Settings.getInstance().setProperty(action.getCustomIconProperty(), null);
      }
    }
    return true;
  }

  @Override
  public void componentDisplayed()
  {
  }

  @Override
  public void componentWillBeClosed()
  {
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
    java.awt.GridBagConstraints gridBagConstraints;

    jScrollPane1 = new javax.swing.JScrollPane();
    allActionList = new javax.swing.JList<>();
    jPanel1 = new javax.swing.JPanel();
    addItem = new javax.swing.JButton();
    removeItem = new javax.swing.JButton();
    addSeparator = new javax.swing.JButton();
    resetButton = new javax.swing.JButton();
    jPanel3 = new javax.swing.JPanel();
    jScrollPane2 = new javax.swing.JScrollPane();
    configuredActions = new javax.swing.JList<>();
    jPanel2 = new javax.swing.JPanel();
    moveUp = new javax.swing.JButton();
    moveDown = new javax.swing.JButton();
    filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
    assignIconButton = new javax.swing.JButton();
    removeIconButton = new javax.swing.JButton();

    setLayout(new java.awt.GridBagLayout());

    allActionList.setModel(new javax.swing.AbstractListModel<String>()
    {
      String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
      public int getSize() { return strings.length; }
      public String getElementAt(int i) { return strings[i]; }
    });
    allActionList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    allActionList.addListSelectionListener(new javax.swing.event.ListSelectionListener()
    {
      public void valueChanged(javax.swing.event.ListSelectionEvent evt)
      {
        allActionListValueChanged(evt);
      }
    });
    jScrollPane1.setViewportView(allActionList);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
    add(jScrollPane1, gridBagConstraints);

    jPanel1.setLayout(new java.awt.GridBagLayout());

    addItem.setText(">>");
    addItem.setEnabled(false);
    addItem.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        addItemActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
    jPanel1.add(addItem, gridBagConstraints);

    removeItem.setText("<<");
    removeItem.setEnabled(false);
    removeItem.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        removeItemActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
    jPanel1.add(removeItem, gridBagConstraints);

    addSeparator.setText(ResourceMgr.getString("LblAddSep")); // NOI18N
    addSeparator.setToolTipText(ResourceMgr.getString("d_LblAddSep")); // NOI18N
    addSeparator.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        addSeparatorActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(14, 0, 0, 0);
    jPanel1.add(addSeparator, gridBagConstraints);

    resetButton.setText(ResourceMgr.getString("LblReset")); // NOI18N
    resetButton.setToolTipText(ResourceMgr.getString("d_LblReset")); // NOI18N
    resetButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        resetButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanel1.add(resetButton, gridBagConstraints);

    jPanel3.setLayout(new java.awt.GridLayout(0, 1));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    jPanel1.add(jPanel3, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
    gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
    add(jPanel1, gridBagConstraints);

    configuredActions.setModel(new javax.swing.AbstractListModel<String>()
    {
      String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
      public int getSize() { return strings.length; }
      public String getElementAt(int i) { return strings[i]; }
    });
    configuredActions.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    configuredActions.addListSelectionListener(new javax.swing.event.ListSelectionListener()
    {
      public void valueChanged(javax.swing.event.ListSelectionEvent evt)
      {
        configuredActionsValueChanged(evt);
      }
    });
    jScrollPane2.setViewportView(configuredActions);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_END;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 5);
    add(jScrollPane2, gridBagConstraints);

    jPanel2.setLayout(new java.awt.GridLayout(0, 1));

    moveUp.setIcon(IconMgr.getInstance().getLabelIcon("Up"));
    moveUp.setEnabled(false);
    moveUp.setPreferredSize(new java.awt.Dimension(33, 18));
    moveUp.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        moveUpActionPerformed(evt);
      }
    });
    jPanel2.add(moveUp);

    moveDown.setIcon(IconMgr.getInstance().getLabelIcon("Down"));
    moveDown.setEnabled(false);
    moveDown.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        moveDownActionPerformed(evt);
      }
    });
    jPanel2.add(moveDown);
    jPanel2.add(filler2);

    assignIconButton.setIcon(IconMgr.getInstance().getLabelIcon("open"));
    assignIconButton.setToolTipText(ResourceMgr.getString("d_LblSelectTbIcon")); // NOI18N
    assignIconButton.setEnabled(false);
    assignIconButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        assignIconButtonActionPerformed(evt);
      }
    });
    jPanel2.add(assignIconButton);

    removeIconButton.setIcon(IconMgr.getInstance().getLabelIcon("delete"));
    removeIconButton.setToolTipText(ResourceMgr.getString("d_LblRemoveTbIcon")); // NOI18N
    removeIconButton.setEnabled(false);
    removeIconButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        removeIconButtonActionPerformed(evt);
      }
    });
    jPanel2.add(removeIconButton);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_END;
    gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 10);
    add(jPanel2, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents

  private void configuredActionsValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_configuredActionsValueChanged
  {//GEN-HEADEREND:event_configuredActionsValueChanged
    checkMoveButtons();
    checkRemoveButton();
    checkIconButtons();
  }//GEN-LAST:event_configuredActionsValueChanged

  private void moveUpActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_moveUpActionPerformed
  {//GEN-HEADEREND:event_moveUpActionPerformed
    moveUp();
  }//GEN-LAST:event_moveUpActionPerformed

  private void moveDownActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_moveDownActionPerformed
  {//GEN-HEADEREND:event_moveDownActionPerformed
    moveDown();
  }//GEN-LAST:event_moveDownActionPerformed

  private void allActionListValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_allActionListValueChanged
  {//GEN-HEADEREND:event_allActionListValueChanged
    checkAddButton();
  }//GEN-LAST:event_allActionListValueChanged

  private void addItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_addItemActionPerformed
  {//GEN-HEADEREND:event_addItemActionPerformed
    addItem();
  }//GEN-LAST:event_addItemActionPerformed

  private void removeItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_removeItemActionPerformed
  {//GEN-HEADEREND:event_removeItemActionPerformed
    removeItem();
  }//GEN-LAST:event_removeItemActionPerformed

  private void resetButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_resetButtonActionPerformed
  {//GEN-HEADEREND:event_resetButtonActionPerformed
    reset();
  }//GEN-LAST:event_resetButtonActionPerformed

  private void addSeparatorActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_addSeparatorActionPerformed
  {//GEN-HEADEREND:event_addSeparatorActionPerformed
    addSeparator();
  }//GEN-LAST:event_addSeparatorActionPerformed

  private void removeIconButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_removeIconButtonActionPerformed
  {//GEN-HEADEREND:event_removeIconButtonActionPerformed
    removeCustomIcon();
  }//GEN-LAST:event_removeIconButtonActionPerformed

  private void assignIconButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_assignIconButtonActionPerformed
  {//GEN-HEADEREND:event_assignIconButtonActionPerformed
    selectIcon();
  }//GEN-LAST:event_assignIconButtonActionPerformed


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton addItem;
  private javax.swing.JButton addSeparator;
  private javax.swing.JList<String> allActionList;
  private javax.swing.JButton assignIconButton;
  private javax.swing.JList<String> configuredActions;
  private javax.swing.Box.Filler filler2;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JPanel jPanel2;
  private javax.swing.JPanel jPanel3;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JScrollPane jScrollPane2;
  private javax.swing.JButton moveDown;
  private javax.swing.JButton moveUp;
  private javax.swing.JButton removeIconButton;
  private javax.swing.JButton removeItem;
  private javax.swing.JButton resetButton;
  // End of variables declaration//GEN-END:variables
}
