/*
 * DataDisplayOptions.java
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import workbench.interfaces.Restoreable;
import workbench.interfaces.ValidatingComponent;
import workbench.log.LogMgr;
import workbench.resource.DataTooltipType;
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
	implements Restoreable, ValidatingComponent, ActionListener
{

	public DataDisplayOptions()
	{
		super();
		initComponents();
		ComboBoxModel model = new DefaultComboBoxModel(new String[] {ResourceMgr.getString("TxtTabRight"), ResourceMgr.getString("TxtTabLeft") });
		alignmentDropDown.setModel(model);

		WbSwingUtilities.setMinimumSizeFromCols(defMaxRows);
		WbSwingUtilities.setMinimumSizeFromCols(nullString);
		WbSwingUtilities.setMinimumSizeFromCols(maxRowHeight);
		WbSwingUtilities.setMinimumSizeFromCols(multiLineThreshold);
		WbSwingUtilities.setMinimumSizeFromCols(minColSizeField);
		WbSwingUtilities.setMinimumSizeFromCols(maxColSizeField);
		WbSwingUtilities.makeEqualWidth(nullString, defMaxRows, alignmentDropDown);
	}

	@Override
	public void restoreSettings()
	{

    String[] items = new String[]
    {
      ResourceMgr.getString("LblResultToolTipNone"),
      ResourceMgr.getString("LblResultToolTipLastExec"),
      ResourceMgr.getString("LblResultToolTipFull"),
    };

    tooltipConfig.setModel(new DefaultComboBoxModel(items));
    tooltipConfig.setSelectedIndex(typeToIndex(GuiSettings.getDataTooltipType()));

		appendResults.setSelected(GuiSettings.getDefaultAppendResults());
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
		wrapMultineRender.setSelected(GuiSettings.getWrapMultilineRenderer());
		wrapMultlineEdit.setSelected(GuiSettings.getWrapMultilineEditor());

		defMaxRows.setText(Integer.toString(GuiSettings.getDefaultMaxRows()));
		retrieveComments.setSelected(GuiSettings.getRetrieveQueryComments());
		showRowNumbers.setSelected(GuiSettings.getShowTableRowNumbers());
		showMaxRowsWarn.setSelected(GuiSettings.getShowMaxRowsReached());
		showMaxRowsTooltip.setSelected(GuiSettings.getShowMaxRowsTooltip());
		nullString.setText(GuiSettings.getDisplayNullString());
		showGeneratingSQL.setSelected(GuiSettings.getShowResultSQL());
		useTableName.setSelected(GuiSettings.getUseTablenameAsResultName());
		int align = GuiSettings.getNumberDataAlignment();
		if (align == SwingConstants.LEFT)
		{
			alignmentDropDown.setSelectedIndex(1);
		}
		else
		{
			alignmentDropDown.setSelectedIndex(0);
		}
		boldHeader.setSelected(GuiSettings.showTableHeaderInBold());
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
		GuiSettings.setShowTableHeaderInBold(boldHeader.isSelected());
		GuiSettings.setWrapMultilineEditor(wrapMultlineEdit.isSelected());
		GuiSettings.setWrapMultilineRenderer(wrapMultineRender.isSelected());
		GuiSettings.setShowMaxRowsTooltip(showMaxRowsTooltip.isSelected());
		GuiSettings.setDefaultAppendResults(appendResults.isSelected());
		GuiSettings.setUseTablenameAsResultName(useTableName.isSelected());
    GuiSettings.setDataTooltipType(indexToType(tooltipConfig.getSelectedIndex()));
		DisplayLocale dl = (DisplayLocale)localeDropDown.getSelectedItem();
		Settings.getInstance().setSortLocale(dl.getLocale());
		if (alignmentDropDown.getSelectedIndex() == 1)
		{
			GuiSettings.setNumberDataAlignment("left");
		}
		else
		{
			GuiSettings.setNumberDataAlignment("right");
		}
	}

  private DataTooltipType indexToType(int index)
	{
    switch (index)
    {
      case 0:
        return DataTooltipType.none;
      case 1:
        return DataTooltipType.lastExec;
      case 2:
        return DataTooltipType.full;
    }
    return DataTooltipType.full;
	}

	private int typeToIndex(DataTooltipType value)
	{
    switch (value)
    {
      case none:
        return 0;
      case lastExec:
        return 1;
      case full:
        return 2;
    }
    return 1;
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
		LogMgr.logDebug("DataDisplayOptions.readLocales()", "Reading " + locales.length + " locales took: " + duration + "ms");

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
		LogMgr.logDebug("DataDisplayOptions.readLocales()", "Sorting locales took: " + duration + "ms");
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

    generalPanel = new javax.swing.JPanel();
    selectSummary = new javax.swing.JCheckBox();
    retrieveComments = new javax.swing.JCheckBox();
    jLabel5 = new javax.swing.JLabel();
    defMaxRows = new javax.swing.JTextField();
    showRowNumbers = new javax.swing.JCheckBox();
    showMaxRowsWarn = new javax.swing.JCheckBox();
    showMaxRowsTooltip = new javax.swing.JCheckBox();
    showGeneratingSQL = new javax.swing.JCheckBox();
    alignmentDropDown = new javax.swing.JComboBox();
    boldHeader = new javax.swing.JCheckBox();
    nullStringLabel = new javax.swing.JLabel();
    nullString = new javax.swing.JTextField();
    appendResults = new javax.swing.JCheckBox();
    useTableName = new javax.swing.JCheckBox();
    alignLabel = new javax.swing.JLabel();
    jPanel1 = new javax.swing.JPanel();
    toolTipConfigLabel = new javax.swing.JLabel();
    tooltipConfig = new javax.swing.JComboBox();
    jLabel1 = new javax.swing.JLabel();
    localeDropDown = new javax.swing.JComboBox();
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
    multiLinePanel = new javax.swing.JPanel();
    wrapMultineRender = new javax.swing.JCheckBox();
    wrapMultlineEdit = new javax.swing.JCheckBox();
    multilineThresholLabel = new javax.swing.JLabel();
    multiLineThreshold = new NumberField();

    setLayout(new java.awt.GridBagLayout());

    generalPanel.setLayout(new java.awt.GridBagLayout());

    selectSummary.setText(ResourceMgr.getString("LblSelectionSummary")); // NOI18N
    selectSummary.setToolTipText(ResourceMgr.getString("d_LblSelectionSummary")); // NOI18N
    selectSummary.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    generalPanel.add(selectSummary, gridBagConstraints);

    retrieveComments.setText(ResourceMgr.getString("LblRetrieveColComments")); // NOI18N
    retrieveComments.setToolTipText(ResourceMgr.getString("d_LblRetrieveColComments")); // NOI18N
    retrieveComments.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
    generalPanel.add(retrieveComments, gridBagConstraints);

    jLabel5.setLabelFor(defMaxRows);
    jLabel5.setText(ResourceMgr.getString("LblDefMaxRows")); // NOI18N
    jLabel5.setToolTipText(ResourceMgr.getString("d_LblDefMaxRows")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 16, 0, 7);
    generalPanel.add(jLabel5, gridBagConstraints);

    defMaxRows.setColumns(8);
    defMaxRows.setToolTipText(ResourceMgr.getString("d_LblDefMaxRows")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 10);
    generalPanel.add(defMaxRows, gridBagConstraints);

    showRowNumbers.setText(ResourceMgr.getString("LblShowRowNumbers")); // NOI18N
    showRowNumbers.setToolTipText(ResourceMgr.getString("d_LblShowRowNumbers")); // NOI18N
    showRowNumbers.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
    generalPanel.add(showRowNumbers, gridBagConstraints);

    showMaxRowsWarn.setText(ResourceMgr.getString("LblShowMaxRowsWarning")); // NOI18N
    showMaxRowsWarn.setToolTipText(ResourceMgr.getString("d_LblShowMaxRowsWarning")); // NOI18N
    showMaxRowsWarn.setBorder(null);
    showMaxRowsWarn.addActionListener(this);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(6, 16, 0, 0);
    generalPanel.add(showMaxRowsWarn, gridBagConstraints);

    showMaxRowsTooltip.setText(ResourceMgr.getString("LblShowMaxRowsTooltip")); // NOI18N
    showMaxRowsTooltip.setToolTipText(ResourceMgr.getString("d_LblShowMaxRowsTooltip")); // NOI18N
    showMaxRowsTooltip.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(6, 16, 0, 0);
    generalPanel.add(showMaxRowsTooltip, gridBagConstraints);

    showGeneratingSQL.setText(ResourceMgr.getString("LblShowGenSQL")); // NOI18N
    showGeneratingSQL.setToolTipText(ResourceMgr.getString("d_LblShowGenSQL")); // NOI18N
    showGeneratingSQL.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
    generalPanel.add(showGeneratingSQL, gridBagConstraints);

    alignmentDropDown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Left", "Right" }));
    alignmentDropDown.setSelectedItem("Right");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 10);
    generalPanel.add(alignmentDropDown, gridBagConstraints);

    boldHeader.setText(ResourceMgr.getString("LblBoldHeader")); // NOI18N
    boldHeader.setToolTipText(ResourceMgr.getString("d_LblBoldHeader")); // NOI18N
    boldHeader.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
    generalPanel.add(boldHeader, gridBagConstraints);

    nullStringLabel.setText(ResourceMgr.getString("LblNullString")); // NOI18N
    nullStringLabel.setToolTipText(ResourceMgr.getString("d_LblNullDisp")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 16, 0, 0);
    generalPanel.add(nullStringLabel, gridBagConstraints);

    nullString.setColumns(8);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 10);
    generalPanel.add(nullString, gridBagConstraints);

    appendResults.setText(ResourceMgr.getString("LblAppendDefault")); // NOI18N
    appendResults.setToolTipText(ResourceMgr.getString("d_LblAppendDefault")); // NOI18N
    appendResults.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 0);
    generalPanel.add(appendResults, gridBagConstraints);

    useTableName.setText(ResourceMgr.getString("LblUseTblName")); // NOI18N
    useTableName.setToolTipText(ResourceMgr.getString("d_LblUseTblName")); // NOI18N
    useTableName.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 16, 0, 7);
    generalPanel.add(useTableName, gridBagConstraints);

    alignLabel.setText(ResourceMgr.getString("LblAlignNum")); // NOI18N
    alignLabel.setToolTipText(ResourceMgr.getString("d_LblAlignNum")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(2, 16, 0, 0);
    generalPanel.add(alignLabel, gridBagConstraints);

    jPanel1.setLayout(new java.awt.GridBagLayout());

    toolTipConfigLabel.setText(ResourceMgr.getString("LblResultTabTooltip")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
    jPanel1.add(toolTipConfigLabel, gridBagConstraints);

    tooltipConfig.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Left", "Right" }));
    tooltipConfig.setSelectedItem("Right");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(4, 7, 0, 10);
    jPanel1.add(tooltipConfig, gridBagConstraints);

    jLabel1.setText(ResourceMgr.getString("LblSortLocale")); // NOI18N
    jLabel1.setToolTipText(ResourceMgr.getString("d_LblSortLocale")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
    jPanel1.add(jLabel1, gridBagConstraints);

    localeDropDown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(8, 6, 0, 0);
    jPanel1.add(localeDropDown, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
    generalPanel.add(jPanel1, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
    add(generalPanel, gridBagConstraints);

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

    minColSizeField.setColumns(6);
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

    maxColSizeField.setColumns(6);
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
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 7);
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

    maxRowHeight.setColumns(8);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
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
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 7);
    add(rowHeightPanel, gridBagConstraints);

    multiLinePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(ResourceMgr.getString("LblMultiLineCols"))); // NOI18N
    multiLinePanel.setLayout(new java.awt.GridBagLayout());

    wrapMultineRender.setText(ResourceMgr.getString("LblMultiWrapRender")); // NOI18N
    wrapMultineRender.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
    multiLinePanel.add(wrapMultineRender, gridBagConstraints);

    wrapMultlineEdit.setText(ResourceMgr.getString("LblMultiWrapEdit")); // NOI18N
    wrapMultlineEdit.setBorder(null);
    wrapMultlineEdit.setMargin(new java.awt.Insets(0, 10, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 2, 0);
    multiLinePanel.add(wrapMultlineEdit, gridBagConstraints);

    multilineThresholLabel.setText(ResourceMgr.getString("LblMultiLineLimit")); // NOI18N
    multilineThresholLabel.setToolTipText(ResourceMgr.getString("d_LblMultiLineLimit")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 11, 0, 0);
    multiLinePanel.add(multilineThresholLabel, gridBagConstraints);

    multiLineThreshold.setColumns(8);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(1, 5, 0, 0);
    multiLinePanel.add(multiLineThreshold, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 7);
    add(multiLinePanel, gridBagConstraints);
  }

  // Code for dispatching events from components to event handlers.

  public void actionPerformed(java.awt.event.ActionEvent evt)
  {
    if (evt.getSource() == showMaxRowsWarn)
    {
      DataDisplayOptions.this.showMaxRowsWarnActionPerformed(evt);
    }
  }// </editor-fold>//GEN-END:initComponents

  private void showMaxRowsWarnActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_showMaxRowsWarnActionPerformed
  {//GEN-HEADEREND:event_showMaxRowsWarnActionPerformed
    showMaxRowsTooltip.setEnabled(showMaxRowsWarn.isSelected());
  }//GEN-LAST:event_showMaxRowsWarnActionPerformed


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JLabel alignLabel;
  private javax.swing.JComboBox alignmentDropDown;
  private javax.swing.JCheckBox appendResults;
  private javax.swing.JCheckBox autoColWidth;
  private javax.swing.JCheckBox autoRowHeight;
  private javax.swing.JCheckBox boldHeader;
  private javax.swing.JPanel colWidthPanel;
  private javax.swing.JTextField defMaxRows;
  private javax.swing.JPanel generalPanel;
  private javax.swing.JCheckBox ignoreEmptyRows;
  private javax.swing.JCheckBox includeHeaderWidth;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel4;
  private javax.swing.JLabel jLabel5;
  private javax.swing.JLabel jLabel6;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JPanel jPanel3;
  private javax.swing.JPanel jPanel4;
  private javax.swing.JComboBox localeDropDown;
  private javax.swing.JTextField maxColSizeField;
  private javax.swing.JLabel maxColSizeLabel;
  private javax.swing.JTextField maxRowHeight;
  private javax.swing.JLabel maxRowHeightLabel;
  private javax.swing.JTextField minColSizeField;
  private javax.swing.JLabel minColSizeLabel;
  private javax.swing.JPanel multiLinePanel;
  private javax.swing.JTextField multiLineThreshold;
  private javax.swing.JLabel multilineThresholLabel;
  private javax.swing.JTextField nullString;
  private javax.swing.JLabel nullStringLabel;
  private javax.swing.JCheckBox retrieveComments;
  private javax.swing.JPanel rowHeightPanel;
  private javax.swing.JCheckBox rowHeightResize;
  private javax.swing.JCheckBox selectSummary;
  private javax.swing.JCheckBox showGeneratingSQL;
  private javax.swing.JCheckBox showMaxRowsTooltip;
  private javax.swing.JCheckBox showMaxRowsWarn;
  private javax.swing.JCheckBox showRowNumbers;
  private javax.swing.JLabel toolTipConfigLabel;
  private javax.swing.JComboBox tooltipConfig;
  private javax.swing.JCheckBox useTableName;
  private javax.swing.JCheckBox wrapMultineRender;
  private javax.swing.JCheckBox wrapMultlineEdit;
  // End of variables declaration//GEN-END:variables

}
