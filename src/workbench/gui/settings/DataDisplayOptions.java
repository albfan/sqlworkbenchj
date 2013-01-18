/*
 * DataDisplayOptions.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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



import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

import javax.swing.JPanel;

import workbench.interfaces.Restoreable;
import workbench.interfaces.ValidatingComponent;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.NumberField;

import workbench.util.DisplayLocale;
import workbench.util.StringUtil;
import workbench.util.WbLocale;

/**
 *
 * @author  Thomas Kellerer
 */
public class DataDisplayOptions
	extends JPanel
	implements Restoreable, ValidatingComponent
{

	public DataDisplayOptions()
	{
		super();
		initComponents();
		WbSwingUtilities.setMinimumSize(defMaxRows, 6);
		WbSwingUtilities.setMinimumSize(multiLineThreshold, 6);
		WbSwingUtilities.setMinimumSize(minColSizeField, 6);
		WbSwingUtilities.setMinimumSize(maxColSizeField, 6);
	}

	@Override
	public void restoreSettings()
	{
		rowHeightResize.setSelected(GuiSettings.getAllowRowHeightResizing());
		autoRowHeight.setSelected(GuiSettings.getAutomaticOptimalRowHeight());
		maxRowHeight.setText(Integer.toString(GuiSettings.getAutRowHeightMaxLines()));
		autoColWidth.setSelected(GuiSettings.getAutomaticOptimalWidth());
		includeHeaderWidth.setSelected(GuiSettings.getIncludeHeaderInOptimalWidth());
		ignoreEmptyRows.setSelected(GuiSettings.getIgnoreWhitespaceForAutoRowHeight());
		minColSizeField.setText(Integer.toString(GuiSettings.getMinColumnWidth()));
		maxColSizeField.setText(Integer.toString(GuiSettings.getMaxColumnWidth()));
		selectSummary.setSelected(GuiSettings.getShowSelectionSummary());
		multiLineThreshold.setText(Integer.toString(GuiSettings.getMultiLineThreshold()));

		defMaxRows.setText(Integer.toString(GuiSettings.getDefaultMaxRows()));
		retrieveComments.setSelected(GuiSettings.getRetrieveQueryComments());
		showRowNumbers.setSelected(GuiSettings.getShowTableRowNumbers());
		showMaxRowsWarn.setSelected(GuiSettings.getShowMaxRowsReached());
		nullString.setText(GuiSettings.getDisplayNullString());
		showGeneratingSQL.setSelected(GuiSettings.getShowResultSQL());
		fillLanguageDropDown();
	}

	@Override
	public void saveSettings()
	{
		int value = StringUtil.getIntValue(multiLineThreshold.getText(), -1);
		if (value > 0) GuiSettings.setMultiLineThreshold(value);
		GuiSettings.setAllowRowHeightResizing(rowHeightResize.isSelected());
		GuiSettings.setMaxColumnWidth(((NumberField)this.maxColSizeField).getValue());
		GuiSettings.setMinColumnWidth(((NumberField)this.minColSizeField).getValue());
		GuiSettings.setAutomaticOptimalWidth(autoColWidth.isSelected());
		GuiSettings.setIncludeHeaderInOptimalWidth(includeHeaderWidth.isSelected());
		GuiSettings.setAutomaticOptimalRowHeight(autoRowHeight.isSelected());
		GuiSettings.setAutRowHeightMaxLines(((NumberField)this.maxRowHeight).getValue());
		GuiSettings.setIgnoreWhitespaceForAutoRowHeight(ignoreEmptyRows.isSelected());
		GuiSettings.setShowSelectionSummary(selectSummary.isSelected());
		GuiSettings.setDefaultMaxRows(StringUtil.getIntValue(defMaxRows.getText(), 0));
		GuiSettings.setRetrieveQueryComments(retrieveComments.isSelected());
		GuiSettings.setShowTableRowNumbers(showRowNumbers.isSelected());
		GuiSettings.setShowMaxRowsReached(showMaxRowsWarn.isSelected());
		GuiSettings.setDisplayNullString(nullString.getText());
		GuiSettings.setShowResultSQL(showGeneratingSQL.isSelected());
		DisplayLocale dl = (DisplayLocale)localeDropDown.getSelectedItem();
		Settings.getInstance().setSortLocale(dl.getLocale());
	}

	@Override
	public void componentDisplayed()
	{
	}

	@Override
	public boolean validateInput()
	{
		return true;
	}

	private Locale[] readLocales()
	{
		long start = System.currentTimeMillis();
		Locale[] locales = Locale.getAvailableLocales();
		long duration = System.currentTimeMillis() - start;
		LogMgr.logDebug("DataDisplayOptions.readLocales()", "Reading " + locales.length + " locales took: " + duration);

		start = System.currentTimeMillis();
		Comparator<Locale> localeComp = new Comparator<Locale>()
		{
			private Locale l = Settings.getInstance().getLanguage();
			@Override
			public int compare(Locale o1, Locale o2)
			{
				return o1.getDisplayLanguage(l).compareTo(o2.getDisplayLanguage(l));
			}
		};
		Arrays.sort(locales, localeComp);
		duration = System.currentTimeMillis() - start;
		LogMgr.logDebug("DataDisplayOptions.readLocales()", "Sorting locales took: " + duration);
		return locales;
	}

	private void fillLanguageDropDown()
	{
		Locale guiLocale = Settings.getInstance().getLanguage();
		DisplayLocale currentSortLocale = new DisplayLocale(new WbLocale(Settings.getInstance().getSortLocale()));

		Locale[] locales = readLocales();

		localeDropDown.removeAllItems();
		localeDropDown.addItem(new DisplayLocale(null));

		int index = 1; // 1 because we have already added a locale
		int currentIndex = -1;

		for (Locale ls : locales)
		{
			DisplayLocale wl = new DisplayLocale(new WbLocale(ls));
			wl.setDisplayLocale(guiLocale);
			localeDropDown.addItem(wl);
			if (wl.equals(currentSortLocale)) currentIndex = index;
			index ++;
		}

		if (currentIndex != -1)
		{
			localeDropDown.setSelectedIndex(currentIndex);
		}
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

    jLabel1 = new javax.swing.JLabel();
    localeDropDown = new javax.swing.JComboBox();
    generalPanel = new javax.swing.JPanel();
    selectSummary = new javax.swing.JCheckBox();
    jLabel3 = new javax.swing.JLabel();
    multiLineThreshold = new javax.swing.JTextField();
    jPanel6 = new javax.swing.JPanel();
    retrieveComments = new javax.swing.JCheckBox();
    jLabel5 = new javax.swing.JLabel();
    defMaxRows = new javax.swing.JTextField();
    showRowNumbers = new javax.swing.JCheckBox();
    showMaxRowsWarn = new javax.swing.JCheckBox();
    showGeneratingSQL = new javax.swing.JCheckBox();
    nullStringLabel = new javax.swing.JLabel();
    nullString = new javax.swing.JTextField();
    colWidthPanel = new javax.swing.JPanel();
    jPanel3 = new javax.swing.JPanel();
    autoColWidth = new javax.swing.JCheckBox();
    includeHeaderWidth = new javax.swing.JCheckBox();
    jPanel4 = new javax.swing.JPanel();
    minColSizeLabel = new javax.swing.JLabel();
    minColSizeField = new NumberField();
    jLabel4 = new javax.swing.JLabel();
    maxColSizeLabel = new javax.swing.JLabel();
    maxColSizeField = new NumberField();
    jLabel6 = new javax.swing.JLabel();
    rowHeightPanel = new javax.swing.JPanel();
    autoRowHeight = new javax.swing.JCheckBox();
    ignoreEmptyRows = new javax.swing.JCheckBox();
    maxRowHeightLabel = new javax.swing.JLabel();
    maxRowHeight = new NumberField();
    rowHeightResize = new javax.swing.JCheckBox();

    setLayout(new java.awt.GridBagLayout());

    jLabel1.setText(ResourceMgr.getString("LblSortLocale")); // NOI18N
    jLabel1.setToolTipText(ResourceMgr.getString("d_LblSortLocale")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(10, 12, 0, 0);
    add(jLabel1, gridBagConstraints);

    localeDropDown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(10, 7, 0, 10);
    add(localeDropDown, gridBagConstraints);

    generalPanel.setLayout(new java.awt.GridBagLayout());

    selectSummary.setText(ResourceMgr.getString("LblSelectionSummary")); // NOI18N
    selectSummary.setToolTipText(ResourceMgr.getString("d_LblSelectionSummary")); // NOI18N
    selectSummary.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(1, 0, 0, 0);
    generalPanel.add(selectSummary, gridBagConstraints);

    jLabel3.setLabelFor(multiLineThreshold);
    jLabel3.setText(ResourceMgr.getString("LblMultiLineLimit")); // NOI18N
    jLabel3.setToolTipText(ResourceMgr.getString("d_LblMultiLineLimit")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 25, 0, 7);
    generalPanel.add(jLabel3, gridBagConstraints);

    multiLineThreshold.setColumns(6);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    generalPanel.add(multiLineThreshold, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.weightx = 1.0;
    generalPanel.add(jPanel6, gridBagConstraints);

    retrieveComments.setText(ResourceMgr.getString("LblRetrieveColComments")); // NOI18N
    retrieveComments.setToolTipText(ResourceMgr.getString("d_LblRetrieveColComments")); // NOI18N
    retrieveComments.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(7, 0, 0, 0);
    generalPanel.add(retrieveComments, gridBagConstraints);

    jLabel5.setLabelFor(defMaxRows);
    jLabel5.setText(ResourceMgr.getString("LblDefMaxRows")); // NOI18N
    jLabel5.setToolTipText(ResourceMgr.getString("d_LblDefMaxRows")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 25, 0, 7);
    generalPanel.add(jLabel5, gridBagConstraints);

    defMaxRows.setColumns(6);
    defMaxRows.setToolTipText(ResourceMgr.getString("d_LblDefMaxRows")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
    generalPanel.add(defMaxRows, gridBagConstraints);

    showRowNumbers.setText(ResourceMgr.getString("LblShowRowNumbers")); // NOI18N
    showRowNumbers.setToolTipText(ResourceMgr.getString("d_LblShowRowNumbers")); // NOI18N
    showRowNumbers.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(7, 0, 0, 0);
    generalPanel.add(showRowNumbers, gridBagConstraints);

    showMaxRowsWarn.setText(ResourceMgr.getString("LblShowMaxRowsWarning")); // NOI18N
    showMaxRowsWarn.setToolTipText(ResourceMgr.getString("d_LblShowMaxRowsWarning")); // NOI18N
    showMaxRowsWarn.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(7, 25, 0, 0);
    generalPanel.add(showMaxRowsWarn, gridBagConstraints);

    showGeneratingSQL.setText(ResourceMgr.getString("LblShowGenSQL")); // NOI18N
    showGeneratingSQL.setToolTipText(ResourceMgr.getString("d_LblShowGenSQL")); // NOI18N
    showGeneratingSQL.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(9, 0, 0, 0);
    generalPanel.add(showGeneratingSQL, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 12, 2, 0);
    add(generalPanel, gridBagConstraints);

    nullStringLabel.setText(ResourceMgr.getString("LblNullString")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(4, 12, 0, 0);
    add(nullStringLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(5, 7, 0, 5);
    add(nullString, gridBagConstraints);

    colWidthPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(ResourceMgr.getString("TxtColWidthSettings"))); // NOI18N
    colWidthPanel.setLayout(new java.awt.GridBagLayout());

    jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));

    autoColWidth.setText(ResourceMgr.getString("LblAutoColWidth")); // NOI18N
    autoColWidth.setToolTipText(ResourceMgr.getString("d_LblAutoColWidth")); // NOI18N
    autoColWidth.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    autoColWidth.setMargin(new java.awt.Insets(0, 0, 0, 0));
    jPanel3.add(autoColWidth);

    includeHeaderWidth.setText(ResourceMgr.getString("LblIncludeHeaderColWidth")); // NOI18N
    includeHeaderWidth.setToolTipText(ResourceMgr.getString("d_LblIncludeHeaderColWidth")); // NOI18N
    includeHeaderWidth.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
    includeHeaderWidth.setMargin(new java.awt.Insets(0, 0, 0, 0));
    jPanel3.add(includeHeaderWidth);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 0.5;
    gridBagConstraints.insets = new java.awt.Insets(5, 1, 0, 0);
    colWidthPanel.add(jPanel3, gridBagConstraints);

    jPanel4.setLayout(new java.awt.GridBagLayout());

    minColSizeLabel.setText(ResourceMgr.getString("LblMinColsize")); // NOI18N
    minColSizeLabel.setToolTipText(ResourceMgr.getString("d_LblMinColsize")); // NOI18N
    minColSizeLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 5, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 6);
    jPanel4.add(minColSizeLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    jPanel4.add(minColSizeField, gridBagConstraints);

    jLabel4.setText("px");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
    jPanel4.add(jLabel4, gridBagConstraints);

    maxColSizeLabel.setText(ResourceMgr.getString("LblMaxColsize")); // NOI18N
    maxColSizeLabel.setToolTipText(ResourceMgr.getString("d_LblMaxColsize")); // NOI18N
    maxColSizeLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 5));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 2);
    jPanel4.add(maxColSizeLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    jPanel4.add(maxColSizeField, gridBagConstraints);

    jLabel6.setText("px");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
    jPanel4.add(jLabel6, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 0.5;
    gridBagConstraints.insets = new java.awt.Insets(6, 6, 2, 0);
    colWidthPanel.add(jPanel4, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(9, 8, 0, 7);
    add(colWidthPanel, gridBagConstraints);

    rowHeightPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(ResourceMgr.getString("TxtRowHeightSettings"))); // NOI18N
    rowHeightPanel.setLayout(new java.awt.GridBagLayout());

    autoRowHeight.setText(ResourceMgr.getString("LblRowHeightAuto")); // NOI18N
    autoRowHeight.setToolTipText(ResourceMgr.getString("LblRowHeightAuto")); // NOI18N
    autoRowHeight.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
    rowHeightPanel.add(autoRowHeight, gridBagConstraints);

    ignoreEmptyRows.setText(ResourceMgr.getString("LblIgnoreRowHeightEmptyLine")); // NOI18N
    ignoreEmptyRows.setToolTipText(ResourceMgr.getString("d_LblIgnoreRowHeightEmptyLine")); // NOI18N
    ignoreEmptyRows.setBorder(null);
    ignoreEmptyRows.setMargin(new java.awt.Insets(0, 10, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 2, 0);
    rowHeightPanel.add(ignoreEmptyRows, gridBagConstraints);

    maxRowHeightLabel.setText(ResourceMgr.getString("LblRowHeightMax")); // NOI18N
    maxRowHeightLabel.setToolTipText(ResourceMgr.getString("d_LblRowHeightMax")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 11, 0, 0);
    rowHeightPanel.add(maxRowHeightLabel, gridBagConstraints);

    maxRowHeight.setColumns(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(1, 5, 0, 0);
    rowHeightPanel.add(maxRowHeight, gridBagConstraints);

    rowHeightResize.setText(ResourceMgr.getString("LblRowResize")); // NOI18N
    rowHeightResize.setToolTipText(ResourceMgr.getString("d_LblRowResize")); // NOI18N
    rowHeightResize.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    rowHeightResize.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    rowHeightResize.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 11, 2, 0);
    rowHeightPanel.add(rowHeightResize, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(6, 8, 0, 7);
    add(rowHeightPanel, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JCheckBox autoColWidth;
  private javax.swing.JCheckBox autoRowHeight;
  private javax.swing.JPanel colWidthPanel;
  private javax.swing.JTextField defMaxRows;
  private javax.swing.JPanel generalPanel;
  private javax.swing.JCheckBox ignoreEmptyRows;
  private javax.swing.JCheckBox includeHeaderWidth;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JLabel jLabel4;
  private javax.swing.JLabel jLabel5;
  private javax.swing.JLabel jLabel6;
  private javax.swing.JPanel jPanel3;
  private javax.swing.JPanel jPanel4;
  private javax.swing.JPanel jPanel6;
  private javax.swing.JComboBox localeDropDown;
  private javax.swing.JTextField maxColSizeField;
  private javax.swing.JLabel maxColSizeLabel;
  private javax.swing.JTextField maxRowHeight;
  private javax.swing.JLabel maxRowHeightLabel;
  private javax.swing.JTextField minColSizeField;
  private javax.swing.JLabel minColSizeLabel;
  private javax.swing.JTextField multiLineThreshold;
  private javax.swing.JTextField nullString;
  private javax.swing.JLabel nullStringLabel;
  private javax.swing.JCheckBox retrieveComments;
  private javax.swing.JPanel rowHeightPanel;
  private javax.swing.JCheckBox rowHeightResize;
  private javax.swing.JCheckBox selectSummary;
  private javax.swing.JCheckBox showGeneratingSQL;
  private javax.swing.JCheckBox showMaxRowsWarn;
  private javax.swing.JCheckBox showRowNumbers;
  // End of variables declaration//GEN-END:variables

}
