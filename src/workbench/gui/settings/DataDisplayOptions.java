/*
 * DataFormattingOptionsPanel.java
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
 * @author  support@sql-workbench.net
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
		dataFont.setSelectedFont(Settings.getInstance().getDataFont(false));
		autoColWidth.setSelected(GuiSettings.getAutomaticOptimalWidth());
		includeHeaderWidth.setSelected(GuiSettings.getIncludeHeaderInOptimalWidth());
		ignoreEmptyRows.setSelected(GuiSettings.getIgnoreWhitespaceForAutoRowHeight());
		minColSizeField.setText(Integer.toString(GuiSettings.getMinColumnWidth()));
		maxColSizeField.setText(Integer.toString(GuiSettings.getMaxColumnWidth()));
		nullColor.setSelectedColor(GuiSettings.getNullColor());
		selectSummary.setSelected(GuiSettings.getShowSelectionSummary());
		multiLineThreshold.setText(Integer.toString(GuiSettings.getMultiLineThreshold()));
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
		Settings.getInstance().setDataFont(dataFont.getSelectedFont());
		GuiSettings.setMaxColumnWidth(((NumberField)this.maxColSizeField).getValue());
		GuiSettings.setMinColumnWidth(((NumberField)this.minColSizeField).getValue());
		GuiSettings.setAutomaticOptimalWidth(autoColWidth.isSelected());
		GuiSettings.setIncludeHeaderInOptimalWidth(includeHeaderWidth.isSelected());
		GuiSettings.setNullColor(nullColor.getSelectedColor());
		GuiSettings.setAutomaticOptimalRowHeight(autoRowHeight.isSelected());
		GuiSettings.setAutRowHeightMaxLines(((NumberField)this.maxRowHeight).getValue());
		GuiSettings.setIgnoreWhitespaceForAutoRowHeight(ignoreEmptyRows.isSelected());
		GuiSettings.setShowSelectionSummary(selectSummary.isSelected());
		DisplayLocale dl = (DisplayLocale)localeDropDown.getSelectedItem();
		Settings.getInstance().setSortLocale(dl.getLocale());
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

    dataFontLabel = new javax.swing.JLabel();
    dataFont = new workbench.gui.components.WbFontPicker();
    colWidthPanel = new javax.swing.JPanel();
    jPanel3 = new javax.swing.JPanel();
    autoColWidth = new javax.swing.JCheckBox();
    includeHeaderWidth = new javax.swing.JCheckBox();
    jPanel4 = new javax.swing.JPanel();
    minColSizeLabel1 = new javax.swing.JLabel();
    minColSizeField = new NumberField();
    maxColSizeLabel1 = new javax.swing.JLabel();
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
    jLabel1 = new javax.swing.JLabel();
    localeDropDown = new javax.swing.JComboBox();
    selectSummary = new javax.swing.JCheckBox();
    jLabel3 = new javax.swing.JLabel();
    multiLineThreshold = new javax.swing.JTextField();

    setLayout(new java.awt.GridBagLayout());

    dataFontLabel.setText(ResourceMgr.getString("LblDataFont")); // NOI18N
    dataFontLabel.setToolTipText(ResourceMgr.getString("d_LblDataFont")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
    add(dataFontLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 7, 0, 10);
    add(dataFont, gridBagConstraints);

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
    gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
    colWidthPanel.add(jPanel3, gridBagConstraints);

    jPanel4.setLayout(new java.awt.GridBagLayout());

    minColSizeLabel1.setText(ResourceMgr.getString("LblMinColsize")); // NOI18N
    minColSizeLabel1.setToolTipText(ResourceMgr.getString("d_LblMinColsize")); // NOI18N
    minColSizeLabel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 5, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 6);
    jPanel4.add(minColSizeLabel1, gridBagConstraints);

    minColSizeField.setColumns(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    jPanel4.add(minColSizeField, gridBagConstraints);

    maxColSizeLabel1.setText(ResourceMgr.getString("LblMaxColsize")); // NOI18N
    maxColSizeLabel1.setToolTipText(ResourceMgr.getString("d_LblMaxColsize")); // NOI18N
    maxColSizeLabel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 5));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 2);
    jPanel4.add(maxColSizeLabel1, gridBagConstraints);

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
    gridBagConstraints.insets = new java.awt.Insets(6, 5, 2, 0);
    colWidthPanel.add(jPanel4, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(8, 7, 0, 10);
    add(colWidthPanel, gridBagConstraints);

    rowHeightPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(ResourceMgr.getString("TxtRowHeightSettings"))); // NOI18N
    rowHeightPanel.setLayout(new java.awt.GridBagLayout());

    autoRowHeight.setText(ResourceMgr.getString("LblRowHeightAuto")); // NOI18N
    autoRowHeight.setToolTipText(ResourceMgr.getString("LblRowHeightAuto")); // NOI18N
    autoRowHeight.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    rowHeightPanel.add(autoRowHeight, gridBagConstraints);

    ignoreEmptyRows.setText(ResourceMgr.getString("LblIgnoreRowHeightEmptyLine")); // NOI18N
    ignoreEmptyRows.setToolTipText(ResourceMgr.getString("d_LblIgnoreRowHeightEmptyLine")); // NOI18N
    ignoreEmptyRows.setBorder(null);
    ignoreEmptyRows.setMargin(new java.awt.Insets(0, 10, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 4, 0, 0);
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
    gridBagConstraints.insets = new java.awt.Insets(5, 11, 0, 0);
    rowHeightPanel.add(rowHeightResize, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 7, 0, 10);
    add(rowHeightPanel, gridBagConstraints);

    jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(ResourceMgr.getString("LblColors"))); // NOI18N
    jPanel2.setLayout(new java.awt.GridBagLayout());

    alternateColorLabel.setText(ResourceMgr.getString("LblAlternateRowColor")); // NOI18N
    alternateColorLabel.setToolTipText(ResourceMgr.getString("d_LblAlternateRowColor")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    jPanel2.add(alternateColorLabel, gridBagConstraints);

    alternateColor.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    jPanel2.add(alternateColor, gridBagConstraints);

    nullColor.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    jPanel2.add(nullColor, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanel2.add(jPanel1, gridBagConstraints);

    jLabel2.setText(ResourceMgr.getString("LblNullValueColor")); // NOI18N
    jLabel2.setToolTipText(ResourceMgr.getString("d_LblNullValueColor")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
    jPanel2.add(jLabel2, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(7, 7, 0, 10);
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
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(11, 7, 0, 10);
    add(localeDropDown, gridBagConstraints);

    selectSummary.setText(ResourceMgr.getString("LblSelectionSummary")); // NOI18N
    selectSummary.setToolTipText(ResourceMgr.getString("d_LblSelectionSummary")); // NOI18N
    selectSummary.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(10, 10, 3, 0);
    add(selectSummary, gridBagConstraints);

    jLabel3.setLabelFor(multiLineThreshold);
    jLabel3.setText(ResourceMgr.getString("LblMultiLineLimit")); // NOI18N
    jLabel3.setToolTipText(ResourceMgr.getString("d_LblMultiLineLimit")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(12, 12, 0, 0);
    add(jLabel3, gridBagConstraints);

    multiLineThreshold.setColumns(6);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(10, 7, 0, 10);
    add(multiLineThreshold, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private workbench.gui.components.WbColorPicker alternateColor;
  private javax.swing.JLabel alternateColorLabel;
  private javax.swing.JCheckBox autoColWidth;
  private javax.swing.JCheckBox autoRowHeight;
  private javax.swing.JPanel colWidthPanel;
  private workbench.gui.components.WbFontPicker dataFont;
  private javax.swing.JLabel dataFontLabel;
  private javax.swing.JCheckBox ignoreEmptyRows;
  private javax.swing.JCheckBox includeHeaderWidth;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JPanel jPanel2;
  private javax.swing.JPanel jPanel3;
  private javax.swing.JPanel jPanel4;
  private javax.swing.JComboBox localeDropDown;
  private javax.swing.JTextField maxColSizeField;
  private javax.swing.JLabel maxColSizeLabel1;
  private javax.swing.JTextField maxRowHeight;
  private javax.swing.JLabel maxRowHeightLabel;
  private javax.swing.JTextField minColSizeField;
  private javax.swing.JLabel minColSizeLabel1;
  private javax.swing.JTextField multiLineThreshold;
  private workbench.gui.components.WbColorPicker nullColor;
  private javax.swing.JPanel rowHeightPanel;
  private javax.swing.JCheckBox rowHeightResize;
  private javax.swing.JCheckBox selectSummary;
  // End of variables declaration//GEN-END:variables

}
