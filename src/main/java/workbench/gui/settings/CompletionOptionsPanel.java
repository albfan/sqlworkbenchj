/*
 * CompletionOptionsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import workbench.interfaces.Restoreable;
import workbench.interfaces.ValidatingComponent;
import workbench.resource.ColumnSortType;
import workbench.resource.GeneratedIdentifierCase;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.objectcache.ObjectCacheStorage;

import workbench.gui.WbSwingUtilities;

import workbench.util.DurationNumber;

/**
 *
 * @author tkellerer
 */
public class CompletionOptionsPanel
	extends JPanel
	implements Restoreable, ValidatingComponent
{
	public CompletionOptionsPanel()
	{
		initComponents();

		DefaultComboBoxModel model = new DefaultComboBoxModel(ObjectCacheStorage.values());
		localStorageType.setModel(model);

		WbSwingUtilities.setMinimumSizeFromCols(maxAgeField);
	}

	@Override
	public void restoreSettings()
	{

		completionPasteCase.setModel(new DefaultComboBoxModel(GeneratedIdentifierCase.values()));
		GeneratedIdentifierCase idCase = Settings.getInstance().getAutoCompletionPasteCase();
		completionPasteCase.setSelectedItem(idCase);

		completionColumnSort.setModel(new DefaultComboBoxModel(ColumnSortType.values()));
		ColumnSortType sort = Settings.getInstance().getAutoCompletionColumnSortType();
		this.completionColumnSort.setSelectedItem(sort);

		closePopup.setSelected(Settings.getInstance().getCloseAutoCompletionWithSearch());
		filterSearch.setSelected(GuiSettings.getFilterCompletionSearch());
		partialMatch.setSelected(GuiSettings.getPartialCompletionSearch());
		sortColumns.setSelected(GuiSettings.getSortCompletionColumns());
		cyleEntries.setSelected(GuiSettings.getCycleCompletionPopup());
		addParens.setSelected(Settings.getInstance().getJoinCompletionUseParens());
		preferUSING.setSelected(Settings.getInstance().getJoinCompletionPreferUSING());
		ObjectCacheStorage storage = GuiSettings.getLocalStorageForObjectCache();
		localStorageType.setSelectedItem(storage);
		localStorageType.doLayout();

		maxAgeField.setText(GuiSettings.getLocalStorageMaxAge());
		WbSwingUtilities.makeEqualWidth(completionColumnSort, completionPasteCase, localStorageType);
	}

	@Override
	public void saveSettings()
	{
		Settings config = Settings.getInstance();
		GeneratedIdentifierCase genCase = (GeneratedIdentifierCase)this.completionPasteCase.getSelectedItem();
		config.setAutoCompletionPasteCase(genCase);
		config.setCloseAutoCompletionWithSearch(closePopup.isSelected());
		ColumnSortType sort = (ColumnSortType)completionColumnSort.getSelectedItem();
		config.setAutoCompletionColumnSort(sort);
		config.setJoinCompletionPreferUSING(preferUSING.isSelected());
		config.setJoinCompletionUseParens(addParens.isSelected());
		GuiSettings.setFilterCompletionSearch(filterSearch.isSelected());
		GuiSettings.setPartialCompletionSearch(partialMatch.isSelected());
		GuiSettings.setSortCompletionColumns(sortColumns.isSelected());
		GuiSettings.setCycleCompletionPopup(cyleEntries.isSelected());
		ObjectCacheStorage storage = (ObjectCacheStorage)localStorageType.getSelectedItem();
		GuiSettings.setLocalStorageForObjectCache(storage);
		GuiSettings.setLocalStorageMaxAge(maxAgeField.getText().trim().toLowerCase());
	}

	@Override
	public boolean validateInput()
	{
		String duration = maxAgeField.getText();
		DurationNumber n = new DurationNumber();

		if (n.isValid(duration)) return true;

		WbSwingUtilities.showErrorMessageKey(this, "ErrInvalidAge");
		maxAgeField.selectAll();
		WbSwingUtilities.requestFocus(maxAgeField);
		return false;
	}

  @Override
  public void componentWillBeClosed()
  {
		// nothing to do
  }

	@Override
	public void componentDisplayed()
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
    GridBagConstraints gridBagConstraints;

    pasteLabel = new JLabel();
    completionPasteCase = new JComboBox();
    closePopup = new JCheckBox();
    completionColumnSort = new JComboBox();
    pasterOrderLabel = new JLabel();
    sortColumns = new JCheckBox();
    partialMatch = new JCheckBox();
    filterSearch = new JCheckBox();
    cyleEntries = new JCheckBox();
    localStorageLabel = new JLabel();
    localStorageType = new JComboBox();
    maxAgeLabel = new JLabel();
    maxAgeField = new JTextField();
    jPanel1 = new JPanel();
    addParens = new JCheckBox();
    preferUSING = new JCheckBox();

    setLayout(new GridBagLayout());

    pasteLabel.setText(ResourceMgr.getString("LblPasteCase")); // NOI18N
    pasteLabel.setToolTipText(ResourceMgr.getString("d_LblPasteCase")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(2, 0, 0, 0);
    add(pasteLabel, gridBagConstraints);

    completionPasteCase.setModel(new DefaultComboBoxModel(new String[] { "Lowercase", "Uppercase", "As is" }));
    completionPasteCase.setToolTipText(ResourceMgr.getDescription("LblPasteCase"));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(0, 11, 0, 15);
    add(completionPasteCase, gridBagConstraints);

    closePopup.setText(ResourceMgr.getString("TxtCloseCompletion")); // NOI18N
    closePopup.setToolTipText(ResourceMgr.getString("d_TxtCloseCompletion")); // NOI18N
    closePopup.setBorder(null);
    closePopup.setHorizontalAlignment(SwingConstants.LEFT);
    closePopup.setHorizontalTextPosition(SwingConstants.RIGHT);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(11, 0, 0, 0);
    add(closePopup, gridBagConstraints);

    completionColumnSort.setToolTipText(ResourceMgr.getDescription("LblPasteSort"));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(6, 11, 0, 15);
    add(completionColumnSort, gridBagConstraints);

    pasterOrderLabel.setText(ResourceMgr.getString("LblPasteSort")); // NOI18N
    pasterOrderLabel.setToolTipText(ResourceMgr.getString("d_LblPasteSort")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(9, 0, 0, 0);
    add(pasterOrderLabel, gridBagConstraints);

    sortColumns.setText(ResourceMgr.getString("LblCompletionSortCols")); // NOI18N
    sortColumns.setToolTipText(ResourceMgr.getString("d_LblCompletionSortCols")); // NOI18N
    sortColumns.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(11, 0, 0, 0);
    add(sortColumns, gridBagConstraints);

    partialMatch.setText(ResourceMgr.getString("LblCompletionPartialMatch")); // NOI18N
    partialMatch.setToolTipText(ResourceMgr.getString("d_LblCompletionPartialMatch")); // NOI18N
    partialMatch.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(9, 0, 0, 0);
    add(partialMatch, gridBagConstraints);

    filterSearch.setText(ResourceMgr.getString("LblCompletionFilterSearch")); // NOI18N
    filterSearch.setToolTipText(ResourceMgr.getString("d_LblCompletionFilterSearch")); // NOI18N
    filterSearch.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(9, 0, 0, 0);
    add(filterSearch, gridBagConstraints);

    cyleEntries.setText(ResourceMgr.getString("LblCompletionCycle")); // NOI18N
    cyleEntries.setToolTipText(ResourceMgr.getString("d_LblCompletionCycle")); // NOI18N
    cyleEntries.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(9, 0, 0, 0);
    add(cyleEntries, gridBagConstraints);

    localStorageLabel.setText(ResourceMgr.getString("LblLocalStorageType")); // NOI18N
    localStorageLabel.setToolTipText(ResourceMgr.getString("d_LblLocalStorageType")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(9, 0, 0, 0);
    add(localStorageLabel, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(6, 11, 0, 15);
    add(localStorageType, gridBagConstraints);

    maxAgeLabel.setText(ResourceMgr.getString("LblLocalMaxAge")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(9, 0, 0, 0);
    add(maxAgeLabel, gridBagConstraints);

    maxAgeField.setColumns(6);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(6, 11, 0, 15);
    add(maxAgeField, gridBagConstraints);

    jPanel1.setBorder(BorderFactory.createTitledBorder(ResourceMgr.getString("MnuTxtAutoCompleteJoin"))); // NOI18N
    jPanel1.setLayout(new GridBagLayout());

    addParens.setText(ResourceMgr.getString("LblJoinComplParens")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(2, 0, 0, 0);
    jPanel1.add(addParens, gridBagConstraints);

    preferUSING.setText(ResourceMgr.getString("LblJoinComplUSING")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(0, 0, 4, 0);
    jPanel1.add(preferUSING, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 10;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(12, 0, 0, 10);
    add(jPanel1, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private JCheckBox addParens;
  private JCheckBox closePopup;
  private JComboBox completionColumnSort;
  private JComboBox completionPasteCase;
  private JCheckBox cyleEntries;
  private JCheckBox filterSearch;
  private JPanel jPanel1;
  private JLabel localStorageLabel;
  private JComboBox localStorageType;
  private JTextField maxAgeField;
  private JLabel maxAgeLabel;
  private JCheckBox partialMatch;
  private JLabel pasteLabel;
  private JLabel pasterOrderLabel;
  private JCheckBox preferUSING;
  private JCheckBox sortColumns;
  // End of variables declaration//GEN-END:variables
}
