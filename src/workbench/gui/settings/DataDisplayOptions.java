/*
 * DataDisplayOptions.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.settings;

import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import javax.swing.JPanel;
import workbench.gui.components.NumberField;
import workbench.gui.components.WbColorPicker;
import workbench.interfaces.Restoreable;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.DisplayLocale;
import workbench.util.StringUtil;
import workbench.util.WbLocale;
import workbench.util.WbThread;

/**
 *
 * @author  Thomas Kellerer
 */
public class DataDisplayOptions
	extends JPanel
	implements Restoreable
{
	private static Locale[] locales;
	private static final Object localeLock = new Object();

	public DataDisplayOptions()
	{
		super();
		initComponents();
	}

	public void restoreSettings()
	{
		rowHeightResize.setSelected(GuiSettings.getAllowRowHeightResizing());
		autoRowHeight.setSelected(GuiSettings.getAutomaticOptimalRowHeight());
		maxRowHeight.setText(Integer.toString(GuiSettings.getAutRowHeightMaxLines()));
		alternateColor.setSelectedColor(GuiSettings.getAlternateRowColor());
		autoColWidth.setSelected(GuiSettings.getAutomaticOptimalWidth());
		includeHeaderWidth.setSelected(GuiSettings.getIncludeHeaderInOptimalWidth());
		ignoreEmptyRows.setSelected(GuiSettings.getIgnoreWhitespaceForAutoRowHeight());
		minColSizeField.setText(Integer.toString(GuiSettings.getMinColumnWidth()));
		maxColSizeField.setText(Integer.toString(GuiSettings.getMaxColumnWidth()));
		nullColor.setSelectedColor(GuiSettings.getNullColor());
		selectSummary.setSelected(GuiSettings.getShowSelectionSummary());
		multiLineThreshold.setText(Integer.toString(GuiSettings.getMultiLineThreshold()));

		stdBackground.setDefaultLabelKey("LblDefaultIndicator");
		textColor.setDefaultLabelKey("LblDefaultIndicator");
		selectionColor.setDefaultLabelKey("LblDefaultIndicator");
		selectedTextColor.setDefaultLabelKey("LblDefaultIndicator");

		stdBackground.setSelectedColor(Settings.getInstance().getColor("workbench.gui.table.background", null));
		textColor.setSelectedColor(Settings.getInstance().getColor("workbench.gui.table.foreground", null));
		selectionColor.setSelectedColor(Settings.getInstance().getColor("workbench.gui.table.selection.background", null));
		selectedTextColor.setSelectedColor(Settings.getInstance().getColor("workbench.gui.table.selection.foreground", null));
		maxRowsColor.setSelectedColor(GuiSettings.getMaxRowsWarningColor());
		defMaxRows.setText(Integer.toString(GuiSettings.getDefaultMaxRows()));
		retrieveComments.setSelected(GuiSettings.getRetrieveQueryComments());
		showRowNumbers.setSelected(GuiSettings.getShowTableRowNumbers());
		showMaxRowsWarn.setSelected(GuiSettings.getShowMaxRowsReached());
		fillLanguageDropDown();
	}

	public void saveSettings()
	{
		Color c = alternateColor.getSelectedColor();
		GuiSettings.setUseAlternateRowColor(c != null);
		GuiSettings.setAlternateRowColor(alternateColor.getSelectedColor());
		int value = StringUtil.getIntValue(multiLineThreshold.getText(), -1);
		if (value > 0) GuiSettings.setMultiLineThreshold(value);
		GuiSettings.setAllowRowHeightResizing(rowHeightResize.isSelected());
		GuiSettings.setMaxColumnWidth(((NumberField)this.maxColSizeField).getValue());
		GuiSettings.setMinColumnWidth(((NumberField)this.minColSizeField).getValue());
		GuiSettings.setAutomaticOptimalWidth(autoColWidth.isSelected());
		GuiSettings.setIncludeHeaderInOptimalWidth(includeHeaderWidth.isSelected());
		GuiSettings.setNullColor(nullColor.getSelectedColor());
		GuiSettings.setAutomaticOptimalRowHeight(autoRowHeight.isSelected());
		GuiSettings.setAutRowHeightMaxLines(((NumberField)this.maxRowHeight).getValue());
		GuiSettings.setIgnoreWhitespaceForAutoRowHeight(ignoreEmptyRows.isSelected());
		GuiSettings.setShowSelectionSummary(selectSummary.isSelected());
		GuiSettings.setDefaultMaxRows(StringUtil.getIntValue(defMaxRows.getText(), 0));
		GuiSettings.setRetrieveQueryComments(retrieveComments.isSelected());
		GuiSettings.setShowTableRowNumbers(showRowNumbers.isSelected());
		GuiSettings.setMaxRowsWarningColor(maxRowsColor.getSelectedColor());
		GuiSettings.setShowMaxRowsReached(showMaxRowsWarn.isSelected());
		DisplayLocale dl = (DisplayLocale)localeDropDown.getSelectedItem();

		Settings.getInstance().setSortLocale(dl.getLocale());

		Settings.getInstance().setColor("workbench.gui.table.background", stdBackground.getSelectedColor());
		Settings.getInstance().setColor("workbench.gui.table.foreground", textColor.getSelectedColor());
		Settings.getInstance().setColor("workbench.gui.table.selection.background", selectionColor.getSelectedColor());
		Settings.getInstance().setColor("workbench.gui.table.selection.foreground", selectedTextColor.getSelectedColor());
	}

	public static void clearLocales()
	{
		synchronized (localeLock)
		{
			locales = null;
		}
	}

	public static void readLocales()
	{
		if (locales != null) return;

		WbThread readThread = new WbThread("Locale Read Thread")
		{
			@Override
			public void run()
			{
				synchronized (localeLock)
				{
					// Sleep a bit in order to not slow down the opening the Options dialog
					// by the retrieval of the locales
					sleepSilently(150);
					locales = Locale.getAvailableLocales();
					Comparator<Locale> localeComp = new Comparator<Locale>()
					{
						private Locale l = Settings.getInstance().getLanguage();
						public int compare(Locale o1, Locale o2)
						{
							return o1.getDisplayLanguage(l).compareTo(o2.getDisplayLanguage(l));
						}
					};
					Arrays.sort(locales, localeComp);
				}
			}
		};
		readThread.setPriority(Thread.MIN_PRIORITY);
		readThread.start();
	}

	private void fillLanguageDropDown()
	{
		Locale guiLocale = Settings.getInstance().getLanguage();
		DisplayLocale currentSortLocale = new DisplayLocale(new WbLocale(Settings.getInstance().getSortLocale()));

		synchronized (localeLock)
		{
			if (locales == null)
			{
				locales = Locale.getAvailableLocales();
			}
		}
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
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    colWidthPanel = new javax.swing.JPanel();
    jPanel3 = new javax.swing.JPanel();
    autoColWidth = new javax.swing.JCheckBox();
    includeHeaderWidth = new javax.swing.JCheckBox();
    jPanel4 = new javax.swing.JPanel();
    minColSizeLabel = new javax.swing.JLabel();
    minColSizeField = new NumberField();
    maxColSizeLabel = new javax.swing.JLabel();
    maxColSizeField = new NumberField();
    rowHeightPanel = new javax.swing.JPanel();
    autoRowHeight = new javax.swing.JCheckBox();
    ignoreEmptyRows = new javax.swing.JCheckBox();
    maxRowHeightLabel = new javax.swing.JLabel();
    maxRowHeight = new NumberField();
    rowHeightResize = new javax.swing.JCheckBox();
    jPanel2 = new javax.swing.JPanel();
    alternateColorLabel = new javax.swing.JLabel();
    alternateColor = new WbColorPicker(true);
    nullColor = new WbColorPicker(true);
    jPanel1 = new javax.swing.JPanel();
    jLabel2 = new javax.swing.JLabel();
    stdBackground = new WbColorPicker(true);
    stdBackgroundLabel = new javax.swing.JLabel();
    textColorLabel = new javax.swing.JLabel();
    textColor = new WbColorPicker(true);
    selectionColorLabel = new javax.swing.JLabel();
    selectionColor = new WbColorPicker(true);
    selectedTextColorLabel = new javax.swing.JLabel();
    selectedTextColor = new WbColorPicker(true);
    maxRowsColorLabel = new javax.swing.JLabel();
    maxRowsColor = new WbColorPicker(true);
    jLabel1 = new javax.swing.JLabel();
    localeDropDown = new javax.swing.JComboBox();
    jPanel5 = new javax.swing.JPanel();
    selectSummary = new javax.swing.JCheckBox();
    jLabel3 = new javax.swing.JLabel();
    multiLineThreshold = new javax.swing.JTextField();
    jPanel6 = new javax.swing.JPanel();
    retrieveComments = new javax.swing.JCheckBox();
    jLabel5 = new javax.swing.JLabel();
    defMaxRows = new javax.swing.JTextField();
    showRowNumbers = new javax.swing.JCheckBox();
    showMaxRowsWarn = new javax.swing.JCheckBox();

    setLayout(new java.awt.GridBagLayout());

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
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 6);
    jPanel4.add(minColSizeLabel, gridBagConstraints);

    minColSizeField.setColumns(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    jPanel4.add(minColSizeField, gridBagConstraints);

    maxColSizeLabel.setText(ResourceMgr.getString("LblMaxColsize")); // NOI18N
    maxColSizeLabel.setToolTipText(ResourceMgr.getString("d_LblMaxColsize")); // NOI18N
    maxColSizeLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 5));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 2);
    jPanel4.add(maxColSizeLabel, gridBagConstraints);

    maxColSizeField.setColumns(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    jPanel4.add(maxColSizeField, gridBagConstraints);

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
    gridBagConstraints.gridy = 6;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 8, 0, 7);
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
    gridBagConstraints.gridy = 7;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(3, 8, 0, 7);
    add(rowHeightPanel, gridBagConstraints);

    jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(ResourceMgr.getString("LblColors"))); // NOI18N
    jPanel2.setLayout(new java.awt.GridBagLayout());

    alternateColorLabel.setText(ResourceMgr.getString("LblAlternateRowColor")); // NOI18N
    alternateColorLabel.setToolTipText(ResourceMgr.getString("d_LblAlternateRowColor")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 0);
    jPanel2.add(alternateColorLabel, gridBagConstraints);

    alternateColor.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
    jPanel2.add(alternateColor, gridBagConstraints);

    nullColor.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
    jPanel2.add(nullColor, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanel2.add(jPanel1, gridBagConstraints);

    jLabel2.setText(ResourceMgr.getString("LblNullValueColor")); // NOI18N
    jLabel2.setToolTipText(ResourceMgr.getString("d_LblNullValueColor")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 7, 0, 0);
    jPanel2.add(jLabel2, gridBagConstraints);

    stdBackground.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    jPanel2.add(stdBackground, gridBagConstraints);

    stdBackgroundLabel.setText(ResourceMgr.getString("LblTableBkgColor")); // NOI18N
    stdBackgroundLabel.setToolTipText(ResourceMgr.getString("d_LblTableBkgColor")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    jPanel2.add(stdBackgroundLabel, gridBagConstraints);

    textColorLabel.setText(ResourceMgr.getString("LblTableTextColor")); // NOI18N
    textColorLabel.setToolTipText(ResourceMgr.getString("d_LblTableTextColor")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 7, 0, 0);
    jPanel2.add(textColorLabel, gridBagConstraints);

    textColor.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    jPanel2.add(textColor, gridBagConstraints);

    selectionColorLabel.setText(ResourceMgr.getString("LblTableSelBckColor")); // NOI18N
    selectionColorLabel.setToolTipText(ResourceMgr.getString("d_LblTableSelBckColor")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 0);
    jPanel2.add(selectionColorLabel, gridBagConstraints);

    selectionColor.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
    jPanel2.add(selectionColor, gridBagConstraints);

    selectedTextColorLabel.setText(ResourceMgr.getString("LblTableSelTextColor")); // NOI18N
    selectedTextColorLabel.setToolTipText(ResourceMgr.getString("d_LblTableSelTextColor")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 7, 0, 0);
    jPanel2.add(selectedTextColorLabel, gridBagConstraints);

    selectedTextColor.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
    jPanel2.add(selectedTextColor, gridBagConstraints);

    maxRowsColorLabel.setText(ResourceMgr.getString("LblMaxRowsWarningColor")); // NOI18N
    maxRowsColorLabel.setToolTipText(ResourceMgr.getString("d_LblMaxRowsWarningColor")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 7, 0, 0);
    jPanel2.add(maxRowsColorLabel, gridBagConstraints);

    maxRowsColor.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
    jPanel2.add(maxRowsColor, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 8, 0, 7);
    add(jPanel2, gridBagConstraints);

    jLabel1.setText(ResourceMgr.getString("LblSortLocale")); // NOI18N
    jLabel1.setToolTipText(ResourceMgr.getString("d_LblSortLocale")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(10, 12, 0, 0);
    add(jLabel1, gridBagConstraints);

    localeDropDown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(10, 7, 0, 10);
    add(localeDropDown, gridBagConstraints);

    jPanel5.setLayout(new java.awt.GridBagLayout());

    selectSummary.setText(ResourceMgr.getString("LblSelectionSummary")); // NOI18N
    selectSummary.setToolTipText(ResourceMgr.getString("d_LblSelectionSummary")); // NOI18N
    selectSummary.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(1, 0, 0, 0);
    jPanel5.add(selectSummary, gridBagConstraints);

    jLabel3.setLabelFor(multiLineThreshold);
    jLabel3.setText(ResourceMgr.getString("LblMultiLineLimit")); // NOI18N
    jLabel3.setToolTipText(ResourceMgr.getString("d_LblMultiLineLimit")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 25, 0, 7);
    jPanel5.add(jLabel3, gridBagConstraints);

    multiLineThreshold.setColumns(6);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    jPanel5.add(multiLineThreshold, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.weightx = 1.0;
    jPanel5.add(jPanel6, gridBagConstraints);

    retrieveComments.setText(ResourceMgr.getString("LblRetrieveColComments")); // NOI18N
    retrieveComments.setToolTipText(ResourceMgr.getString("d_LblRetrieveColComments")); // NOI18N
    retrieveComments.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(7, 0, 0, 0);
    jPanel5.add(retrieveComments, gridBagConstraints);

    jLabel5.setLabelFor(defMaxRows);
    jLabel5.setText(ResourceMgr.getString("LblDefMaxRows")); // NOI18N
    jLabel5.setToolTipText(ResourceMgr.getString("d_LblDefMaxRows")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 25, 0, 7);
    jPanel5.add(jLabel5, gridBagConstraints);

    defMaxRows.setColumns(6);
    defMaxRows.setToolTipText(ResourceMgr.getString("d_LblDefMaxRows")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
    jPanel5.add(defMaxRows, gridBagConstraints);

    showRowNumbers.setText(ResourceMgr.getString("LblShowRowNumbers")); // NOI18N
    showRowNumbers.setToolTipText(ResourceMgr.getString("d_LblShowRowNumbers")); // NOI18N
    showRowNumbers.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(7, 0, 0, 0);
    jPanel5.add(showRowNumbers, gridBagConstraints);

    showMaxRowsWarn.setText(ResourceMgr.getString("LblShowMaxRowsWarning")); // NOI18N
    showMaxRowsWarn.setToolTipText(ResourceMgr.getString("d_LblShowMaxRowsWarning")); // NOI18N
    showMaxRowsWarn.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(7, 25, 0, 0);
    jPanel5.add(showMaxRowsWarn, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 12, 2, 0);
    add(jPanel5, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private workbench.gui.components.WbColorPicker alternateColor;
  private javax.swing.JLabel alternateColorLabel;
  private javax.swing.JCheckBox autoColWidth;
  private javax.swing.JCheckBox autoRowHeight;
  private javax.swing.JPanel colWidthPanel;
  private javax.swing.JTextField defMaxRows;
  private javax.swing.JCheckBox ignoreEmptyRows;
  private javax.swing.JCheckBox includeHeaderWidth;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JLabel jLabel5;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JPanel jPanel2;
  private javax.swing.JPanel jPanel3;
  private javax.swing.JPanel jPanel4;
  private javax.swing.JPanel jPanel5;
  private javax.swing.JPanel jPanel6;
  private javax.swing.JComboBox localeDropDown;
  private javax.swing.JTextField maxColSizeField;
  private javax.swing.JLabel maxColSizeLabel;
  private javax.swing.JTextField maxRowHeight;
  private javax.swing.JLabel maxRowHeightLabel;
  private workbench.gui.components.WbColorPicker maxRowsColor;
  private javax.swing.JLabel maxRowsColorLabel;
  private javax.swing.JTextField minColSizeField;
  private javax.swing.JLabel minColSizeLabel;
  private javax.swing.JTextField multiLineThreshold;
  private workbench.gui.components.WbColorPicker nullColor;
  private javax.swing.JCheckBox retrieveComments;
  private javax.swing.JPanel rowHeightPanel;
  private javax.swing.JCheckBox rowHeightResize;
  private javax.swing.JCheckBox selectSummary;
  private workbench.gui.components.WbColorPicker selectedTextColor;
  private javax.swing.JLabel selectedTextColorLabel;
  private workbench.gui.components.WbColorPicker selectionColor;
  private javax.swing.JLabel selectionColorLabel;
  private javax.swing.JCheckBox showMaxRowsWarn;
  private javax.swing.JCheckBox showRowNumbers;
  private workbench.gui.components.WbColorPicker stdBackground;
  private javax.swing.JLabel stdBackgroundLabel;
  private workbench.gui.components.WbColorPicker textColor;
  private javax.swing.JLabel textColorLabel;
  // End of variables declaration//GEN-END:variables

}
