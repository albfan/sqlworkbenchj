/*
 * DbExplorerOptionsPanel.java
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
package workbench.gui.settings;

import java.awt.event.ActionListener;

import javax.swing.JPanel;

import workbench.interfaces.Restoreable;
import workbench.resource.DbExplorerSettings;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;

/**
 *
 * @author  Thomas Kellerer
 */
public class DbExplorerOptionsPanel
	extends JPanel
	implements Restoreable, ActionListener
{
	public DbExplorerOptionsPanel()
	{
		super();
		initComponents();
	}

	@Override
	public void saveSettings()
	{
		DbExplorerSettings.setRetrieveDbExplorer(retrieveDbExplorer.isSelected());
		DbExplorerSettings.setShowDbExplorerInMainWindow(this.showDbExplorer.isSelected());
		DbExplorerSettings.setStoreExplorerObjectType(this.rememberObject.isSelected());
		DbExplorerSettings.setAutoGeneratePKName(autogeneratePK.isSelected());
		DbExplorerSettings.setShowTriggerPanel(showTriggerPanel.isSelected());
		DbExplorerSettings.setSelectDataPanelAfterRetrieve(autoselectDataPanel.isSelected());
		DbExplorerSettings.setSelectSourcePanelAfterRetrieve(selectSrcPanel.isSelected());
		DbExplorerSettings.setRememberSortInDbExplorer(rememberSort.isSelected());
		DbExplorerSettings.setRememberColumnOrder(rememberColOrder.isSelected());
		DbExplorerSettings.setShowFocusInDbExplorer(showFocus.isSelected());
		DbExplorerSettings.setDefaultExplorerObjectType(this.defTableType.getText());
		DbExplorerSettings.setDbExpFilterDuringTyping(filterWhileTyping.isSelected());
		DbExplorerSettings.setDbExpUsePartialMatch(partialMatchSearch.isSelected());
		DbExplorerSettings.setGenerateTableGrants(generateTableGrants.isSelected());
		DbExplorerSettings.setDbExpGenerateDrop(generateDrop.isSelected());
		GuiSettings.setUseRegexInQuickFilter(useQuickFilterRegex.isSelected());
		DbExplorerSettings.setAllowAlterInDbExplorer(allowTableAlter.isSelected());
		DbExplorerSettings.setAutoRetrieveFKTree(retrieveFKTree.isSelected());
		DbExplorerSettings.setApplySQLSortInDbExplorer(applySQLSort.isSelected());
		DbExplorerSettings.setShowSynonymTargetInDbExplorer(showSynDetails.isSelected());
		DbExplorerSettings.setDbExplorerShowTableHistory(showTableHistory.isSelected());
		DbExplorerSettings.setGenerateColumnListInViews(generateViewColumns.isSelected());
		DbExplorerSettings.setUseFilterForRetrieve(filterRetrieval.isSelected());
		((PlacementChooser)tabPlacement).saveSelection();
	}

	@Override
	public void restoreSettings()
	{
		filterRetrieval.setSelected(DbExplorerSettings.getUseFilterForRetrieve());
		autogeneratePK.setSelected(DbExplorerSettings.getAutoGeneratePKName());
		partialMatchSearch.setSelected(DbExplorerSettings.getDbExpUsePartialMatch());
		retrieveFKTree.setSelected(DbExplorerSettings.getAutoRetrieveFKTree());
		allowTableAlter.setSelected(DbExplorerSettings.allowAlterInDbExplorer());
		defTableType.setText(DbExplorerSettings.getDefaultExplorerObjectType());
		rememberColOrder.setSelected(DbExplorerSettings.getRememberColumnOrder());
		rememberSort.setSelected(DbExplorerSettings.getRememberSortInDbExplorer());
		generateTableGrants.setSelected(DbExplorerSettings.getGenerateTableGrants());
		generateDrop.setSelected(DbExplorerSettings.getDbExpGenerateDrop());
		useQuickFilterRegex.setSelected(GuiSettings.getUseRegexInQuickFilter());
		filterWhileTyping.setSelected(DbExplorerSettings.getDbExpFilterDuringTyping());
		selectSrcPanel.setSelected(DbExplorerSettings.getSelectSourcePanelAfterRetrieve());
		((PlacementChooser)tabPlacement).showPlacement();
		partialMatchSearch.setEnabled(!useQuickFilterRegex.isSelected());
		applySQLSort.setSelected(DbExplorerSettings.getApplySQLSortInDbExplorer());
		showSynDetails.setSelected(GuiSettings.showSynonymTargetInDbExplorer());
		showTableHistory.setSelected(DbExplorerSettings.getDbExplorerShowTableHistory());
		generateViewColumns.setSelected(DbExplorerSettings.getGenerateColumnListInViews());
		showDbExplorer.setSelected(DbExplorerSettings.getShowDbExplorerInMainWindow());
		showTriggerPanel.setSelected(DbExplorerSettings.getShowTriggerPanel());
		showFocus.setSelected(DbExplorerSettings.showFocusInDbExplorer());
    retrieveDbExplorer.setSelected(DbExplorerSettings.getRetrieveDbExplorer());
    rememberObject.setSelected(DbExplorerSettings.getStoreExplorerObjectType());
    applySQLSort.setSelected(DbExplorerSettings.getRememberSortInDbExplorer());
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {
    java.awt.GridBagConstraints gridBagConstraints;

    showDbExplorer = new javax.swing.JCheckBox();
    showTriggerPanel = new javax.swing.JCheckBox();
    autoselectDataPanel = new javax.swing.JCheckBox();
    showFocus = new javax.swing.JCheckBox();
    jPanel1 = new javax.swing.JPanel();
    jPanel2 = new javax.swing.JPanel();
    tabPlacement = new PlacementChooser();
    jLabel1 = new javax.swing.JLabel();
    retrieveFKTree = new javax.swing.JCheckBox();
    selectSrcPanel = new javax.swing.JCheckBox();
    showSynDetails = new javax.swing.JCheckBox();
    showTableHistory = new javax.swing.JCheckBox();
    jPanel5 = new javax.swing.JPanel();
    rememberObject = new javax.swing.JCheckBox();
    partialMatchSearch = new javax.swing.JCheckBox();
    useQuickFilterRegex = new javax.swing.JCheckBox();
    allowTableAlter = new javax.swing.JCheckBox();
    filterWhileTyping = new javax.swing.JCheckBox();
    retrieveDbExplorer = new javax.swing.JCheckBox();
    jPanel7 = new javax.swing.JPanel();
    defTableTypeLabel = new javax.swing.JLabel();
    defTableType = new javax.swing.JTextField();
    filterRetrieval = new javax.swing.JCheckBox();
    jPanel6 = new javax.swing.JPanel();
    jPanel3 = new javax.swing.JPanel();
    autogeneratePK = new javax.swing.JCheckBox();
    generateTableGrants = new javax.swing.JCheckBox();
    generateDrop = new javax.swing.JCheckBox();
    generateViewColumns = new javax.swing.JCheckBox();
    jPanel4 = new javax.swing.JPanel();
    rememberColOrder = new javax.swing.JCheckBox();
    rememberSort = new javax.swing.JCheckBox();
    applySQLSort = new javax.swing.JCheckBox();

    setLayout(new java.awt.GridBagLayout());

    showDbExplorer.setText(ResourceMgr.getString("LblDbExplorerCheckBox")); // NOI18N
    showDbExplorer.setToolTipText(ResourceMgr.getString("d_LblDbExplorerCheckBox")); // NOI18N
    showDbExplorer.setBorder(null);
    showDbExplorer.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    showDbExplorer.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    showDbExplorer.setIconTextGap(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(8, 8, 0, 0);
    add(showDbExplorer, gridBagConstraints);

    showTriggerPanel.setText(ResourceMgr.getString("LblShowTriggerPanel")); // NOI18N
    showTriggerPanel.setToolTipText(ResourceMgr.getString("d_LblShowTriggerPanel")); // NOI18N
    showTriggerPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    showTriggerPanel.setMargin(new java.awt.Insets(0, 0, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(8, 8, 0, 0);
    add(showTriggerPanel, gridBagConstraints);

    autoselectDataPanel.setText(ResourceMgr.getString("LblSelectDataPanel")); // NOI18N
    autoselectDataPanel.setToolTipText(ResourceMgr.getString("d_LblSelectDataPanel")); // NOI18N
    autoselectDataPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    autoselectDataPanel.setMargin(new java.awt.Insets(0, 0, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(8, 10, 0, 10);
    add(autoselectDataPanel, gridBagConstraints);

    showFocus.setText(ResourceMgr.getString("LblShowFocus")); // NOI18N
    showFocus.setToolTipText(ResourceMgr.getString("d_LblShowFocus")); // NOI18N
    showFocus.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    showFocus.setMargin(new java.awt.Insets(0, 0, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(8, 10, 0, 10);
    add(showFocus, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 16;
    gridBagConstraints.weighty = 1.0;
    add(jPanel1, gridBagConstraints);

    jPanel2.setLayout(new java.awt.GridBagLayout());

    tabPlacement.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Top", "Bottom", "Left", "Right" }));
    tabPlacement.setToolTipText(ResourceMgr.getString("d_LblObjTabPos")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 9, 0, 0);
    jPanel2.add(tabPlacement, gridBagConstraints);

    jLabel1.setText(ResourceMgr.getString("LblObjTabPos")); // NOI18N
    jLabel1.setToolTipText(ResourceMgr.getString("d_LblObjTabPos")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    jPanel2.add(jLabel1, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(8, 9, 0, 20);
    add(jPanel2, gridBagConstraints);

    retrieveFKTree.setText(ResourceMgr.getString("LblRetrieveFkTree")); // NOI18N
    retrieveFKTree.setToolTipText(ResourceMgr.getString("d_LblRetrieveFkTree")); // NOI18N
    retrieveFKTree.setBorder(null);
    retrieveFKTree.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    retrieveFKTree.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    retrieveFKTree.setIconTextGap(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(8, 10, 0, 10);
    add(retrieveFKTree, gridBagConstraints);

    selectSrcPanel.setText(ResourceMgr.getString("LblSelectSourcePanel")); // NOI18N
    selectSrcPanel.setToolTipText(ResourceMgr.getString("d_LblSelectSourcePanel")); // NOI18N
    selectSrcPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
    selectSrcPanel.setMargin(new java.awt.Insets(0, 0, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(8, 10, 0, 10);
    add(selectSrcPanel, gridBagConstraints);

    showSynDetails.setText(ResourceMgr.getString("LblShowSynTarget")); // NOI18N
    showSynDetails.setToolTipText(ResourceMgr.getString("d_LblShowSynTarget")); // NOI18N
    showSynDetails.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(8, 8, 0, 0);
    add(showSynDetails, gridBagConstraints);

    showTableHistory.setText(ResourceMgr.getString("LblShowTableHist")); // NOI18N
    showTableHistory.setToolTipText(ResourceMgr.getString("d_LblShowTableHist")); // NOI18N
    showTableHistory.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    showTableHistory.setMargin(new java.awt.Insets(0, 0, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(8, 8, 0, 0);
    add(showTableHistory, gridBagConstraints);

    jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder(ResourceMgr.getString("LblDbExpObjListOpts"))); // NOI18N
    jPanel5.setLayout(new java.awt.GridBagLayout());

    rememberObject.setText(ResourceMgr.getString("LblRememberObjectType")); // NOI18N
    rememberObject.setToolTipText(ResourceMgr.getString("d_LblRememberObjectType")); // NOI18N
    rememberObject.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    rememberObject.setMargin(new java.awt.Insets(0, 0, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 8, 1, 10);
    jPanel5.add(rememberObject, gridBagConstraints);

    partialMatchSearch.setText(ResourceMgr.getString("LblPartialMatch")); // NOI18N
    partialMatchSearch.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(8, 8, 0, 10);
    jPanel5.add(partialMatchSearch, gridBagConstraints);

    useQuickFilterRegex.setText(ResourceMgr.getString("LblQuickFilterRegex")); // NOI18N
    useQuickFilterRegex.setToolTipText(ResourceMgr.getString("d_LblQuickFilterRegex")); // NOI18N
    useQuickFilterRegex.setBorder(null);
    useQuickFilterRegex.addActionListener(this);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 8, 0, 10);
    jPanel5.add(useQuickFilterRegex, gridBagConstraints);

    allowTableAlter.setText(ResourceMgr.getString("LblAllowTblAlter")); // NOI18N
    allowTableAlter.setToolTipText(ResourceMgr.getString("d_LblAllowTblAlter")); // NOI18N
    allowTableAlter.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(8, 8, 0, 10);
    jPanel5.add(allowTableAlter, gridBagConstraints);

    filterWhileTyping.setText(ResourceMgr.getString("LblFilterWhileType")); // NOI18N
    filterWhileTyping.setToolTipText(ResourceMgr.getString("d_LblFilterWhileType")); // NOI18N
    filterWhileTyping.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(8, 8, 0, 10);
    jPanel5.add(filterWhileTyping, gridBagConstraints);

    retrieveDbExplorer.setText(ResourceMgr.getString("LblRetrieveDbExplorer")); // NOI18N
    retrieveDbExplorer.setToolTipText(ResourceMgr.getString("d_LblRetrieveDbExplorer")); // NOI18N
    retrieveDbExplorer.setBorder(null);
    retrieveDbExplorer.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    retrieveDbExplorer.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    retrieveDbExplorer.setIconTextGap(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 8, 0, 10);
    jPanel5.add(retrieveDbExplorer, gridBagConstraints);

    jPanel7.setLayout(new java.awt.GridBagLayout());

    defTableTypeLabel.setLabelFor(autogeneratePK);
    defTableTypeLabel.setText(ResourceMgr.getString("LblDefTableType")); // NOI18N
    defTableTypeLabel.setToolTipText(ResourceMgr.getString("d_LblDefTableType")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    jPanel7.add(defTableTypeLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 9, 0, 0);
    jPanel7.add(defTableType, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 8, 0, 6);
    jPanel5.add(jPanel7, gridBagConstraints);

    filterRetrieval.setText(ResourceMgr.getString("LblDbExpUseRetrievalFilter")); // NOI18N
    filterRetrieval.setToolTipText(ResourceMgr.getString("d_LblDbExpUseRetrievalFilter")); // NOI18N
    filterRetrieval.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(8, 8, 0, 10);
    jPanel5.add(filterRetrieval, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(9, 4, 0, 8);
    add(jPanel5, gridBagConstraints);

    jPanel6.setLayout(new java.awt.GridBagLayout());

    jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(ResourceMgr.getString("LblDbExpGenOpts"))); // NOI18N
    jPanel3.setLayout(new java.awt.GridBagLayout());

    autogeneratePK.setText(ResourceMgr.getString("LblGeneratePkName")); // NOI18N
    autogeneratePK.setToolTipText(ResourceMgr.getString("d_LblGeneratePkName")); // NOI18N
    autogeneratePK.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    autogeneratePK.setMargin(new java.awt.Insets(0, 0, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 8, 0, 10);
    jPanel3.add(autogeneratePK, gridBagConstraints);

    generateTableGrants.setText(ResourceMgr.getString("LblGenerateTblGrants")); // NOI18N
    generateTableGrants.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    generateTableGrants.setMargin(new java.awt.Insets(0, 0, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(8, 8, 0, 10);
    jPanel3.add(generateTableGrants, gridBagConstraints);

    generateDrop.setText(ResourceMgr.getString("LblGenerateDrop")); // NOI18N
    generateDrop.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    generateDrop.setMargin(new java.awt.Insets(0, 0, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(8, 8, 3, 10);
    jPanel3.add(generateDrop, gridBagConstraints);

    generateViewColumns.setText(ResourceMgr.getString("LblGenerateViewCols")); // NOI18N
    generateViewColumns.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    generateViewColumns.setMargin(new java.awt.Insets(0, 0, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(6, 8, 3, 10);
    jPanel3.add(generateViewColumns, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    jPanel6.add(jPanel3, gridBagConstraints);

    jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(ResourceMgr.getString("LblDbExpDataOpts"))); // NOI18N
    jPanel4.setLayout(new java.awt.GridBagLayout());

    rememberColOrder.setText(ResourceMgr.getString("LblRememberDbExpColOrder")); // NOI18N
    rememberColOrder.setToolTipText(ResourceMgr.getString("d_LblRememberDbExpColOrder")); // NOI18N
    rememberColOrder.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 9, 0, 10);
    jPanel4.add(rememberColOrder, gridBagConstraints);

    rememberSort.setText(ResourceMgr.getString("LblRememberDbExpSort")); // NOI18N
    rememberSort.setToolTipText(ResourceMgr.getString("d_LblRememberDbExpSort")); // NOI18N
    rememberSort.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    rememberSort.setMargin(new java.awt.Insets(0, 0, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(8, 9, 0, 10);
    jPanel4.add(rememberSort, gridBagConstraints);

    applySQLSort.setText(ResourceMgr.getString("LblApplySQLSort")); // NOI18N
    applySQLSort.setToolTipText(ResourceMgr.getString("d_LblApplySQLSort")); // NOI18N
    applySQLSort.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    applySQLSort.setMargin(new java.awt.Insets(0, 0, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(8, 9, 3, 10);
    jPanel4.add(applySQLSort, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    jPanel6.add(jPanel4, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(8, 4, 0, 0);
    add(jPanel6, gridBagConstraints);
  }

  // Code for dispatching events from components to event handlers.

  public void actionPerformed(java.awt.event.ActionEvent evt)
  {
    if (evt.getSource() == useQuickFilterRegex)
    {
      DbExplorerOptionsPanel.this.useQuickFilterRegexActionPerformed(evt);
    }
  }// </editor-fold>//GEN-END:initComponents

  private void useQuickFilterRegexActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_useQuickFilterRegexActionPerformed
  {//GEN-HEADEREND:event_useQuickFilterRegexActionPerformed
    partialMatchSearch.setEnabled(!useQuickFilterRegex.isSelected());
  }//GEN-LAST:event_useQuickFilterRegexActionPerformed


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JCheckBox allowTableAlter;
  private javax.swing.JCheckBox applySQLSort;
  private javax.swing.JCheckBox autogeneratePK;
  private javax.swing.JCheckBox autoselectDataPanel;
  private javax.swing.JTextField defTableType;
  private javax.swing.JLabel defTableTypeLabel;
  private javax.swing.JCheckBox filterRetrieval;
  private javax.swing.JCheckBox filterWhileTyping;
  private javax.swing.JCheckBox generateDrop;
  private javax.swing.JCheckBox generateTableGrants;
  private javax.swing.JCheckBox generateViewColumns;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JPanel jPanel2;
  private javax.swing.JPanel jPanel3;
  private javax.swing.JPanel jPanel4;
  private javax.swing.JPanel jPanel5;
  private javax.swing.JPanel jPanel6;
  private javax.swing.JPanel jPanel7;
  private javax.swing.JCheckBox partialMatchSearch;
  private javax.swing.JCheckBox rememberColOrder;
  private javax.swing.JCheckBox rememberObject;
  private javax.swing.JCheckBox rememberSort;
  private javax.swing.JCheckBox retrieveDbExplorer;
  private javax.swing.JCheckBox retrieveFKTree;
  private javax.swing.JCheckBox selectSrcPanel;
  private javax.swing.JCheckBox showDbExplorer;
  private javax.swing.JCheckBox showFocus;
  private javax.swing.JCheckBox showSynDetails;
  private javax.swing.JCheckBox showTableHistory;
  private javax.swing.JCheckBox showTriggerPanel;
  private javax.swing.JComboBox tabPlacement;
  private javax.swing.JCheckBox useQuickFilterRegex;
  // End of variables declaration//GEN-END:variables

}
