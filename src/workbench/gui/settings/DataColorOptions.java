/*
 * DataDisplayOptions.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.settings;


import java.awt.*;

import javax.swing.JPanel;
import javax.swing.JTextField;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbColorPicker;
import workbench.interfaces.Restoreable;
import workbench.interfaces.ValidatingComponent;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 *
 * @author  Thomas Kellerer
 */
public class DataColorOptions
	extends JPanel
	implements Restoreable, ValidatingComponent
{

	public DataColorOptions()
	{
		super();
		initComponents();
		WbSwingUtilities.setMinimumSize(alternateBlend, 5);
		WbSwingUtilities.setMinimumSize(selectionBlend, 5);
	}

	@Override
	public void restoreSettings()
	{
		alternateColor.setSelectedColor(GuiSettings.getAlternateRowColor());
		nullColor.setSelectedColor(GuiSettings.getNullColor());
		stdBackground.setDefaultLabelKey("LblDefaultIndicator");
		textColor.setDefaultLabelKey("LblDefaultIndicator");
		selectionColor.setDefaultLabelKey("LblDefaultIndicator");
		selectedTextColor.setDefaultLabelKey("LblDefaultIndicator");

		stdBackground.setSelectedColor(Settings.getInstance().getColor("workbench.gui.table.background", null));
		textColor.setSelectedColor(Settings.getInstance().getColor("workbench.gui.table.foreground", null));
		selectionColor.setSelectedColor(Settings.getInstance().getColor("workbench.gui.table.selection.background", null));
		selectedTextColor.setSelectedColor(Settings.getInstance().getColor("workbench.gui.table.selection.foreground", null));
		modifiedColor.setSelectedColor(GuiSettings.getColumnModifiedColor());
		selectionBlend.setText(Settings.getInstance().getProperty("workbench.gui.renderer.blend.selection", ""));
		alternateBlend.setText(Settings.getInstance().getProperty("workbench.gui.renderer.blend.alternate", ""));
	}

	@Override
	public void saveSettings()
	{
		Color c = alternateColor.getSelectedColor();
		GuiSettings.setUseAlternateRowColor(c != null);
		GuiSettings.setAlternateRowColor(alternateColor.getSelectedColor());
		GuiSettings.setNullColor(nullColor.getSelectedColor());
		GuiSettings.setColumnModifiedColor(modifiedColor.getSelectedColor());

		Settings.getInstance().setColor("workbench.gui.table.background", stdBackground.getSelectedColor());
		Settings.getInstance().setColor("workbench.gui.table.foreground", textColor.getSelectedColor());
		Settings.getInstance().setColor("workbench.gui.table.selection.background", selectionColor.getSelectedColor());
		Settings.getInstance().setColor("workbench.gui.table.selection.foreground", selectedTextColor.getSelectedColor());
		Settings.getInstance().setProperty("workbench.gui.renderer.blend.selection",selectionBlend.getText().trim());
		Settings.getInstance().setProperty("workbench.gui.renderer.blend.alternate",alternateBlend.getText().trim());
	}

	@Override
	public void componentDisplayed()
	{
	}

	@Override
	public boolean validateInput()
	{
		if (!validateTextField(selectionBlend))
		{
			return false;
		}
		if (!validateTextField(alternateBlend))
		{
			return false;
		}
		return true;
	}

	private boolean validateTextField(final JTextField field)
	{
		if (StringUtil.isEmptyString(field.getText().trim())) return true;
		String errMsg = ResourceMgr.getString("ErrInvalidBlend");

		int blend = -1;
		try
		{
			blend = Integer.parseInt(field.getText().trim());
		}
		catch (Exception ex)
		{
			blend = -1;
		}

		if (blend < 0 || blend > 256)
		{
			WbSwingUtilities.showErrorMessage(this, ResourceMgr.getString("TxtError"), errMsg);
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					field.requestFocusInWindow();
				}
			});
			return false;
		}
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

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {
    java.awt.GridBagConstraints gridBagConstraints;

    alternateColorLabel = new javax.swing.JLabel();
    alternateColor = new WbColorPicker(true);
    nullColor = new WbColorPicker(true);
    jLabel2 = new javax.swing.JLabel();
    stdBackground = new WbColorPicker(true);
    stdBackgroundLabel = new javax.swing.JLabel();
    textColorLabel = new javax.swing.JLabel();
    textColor = new WbColorPicker(true);
    selectionColorLabel = new javax.swing.JLabel();
    selectionColor = new WbColorPicker(true);
    selectedTextColorLabel = new javax.swing.JLabel();
    selectedTextColor = new WbColorPicker(true);
    jLabel7 = new javax.swing.JLabel();
    modifiedColor = new WbColorPicker(true);
    jPanel7 = new javax.swing.JPanel();
    jLabel8 = new javax.swing.JLabel();
    selectionBlend = new javax.swing.JTextField();
    alternateBlend = new javax.swing.JTextField();
    jLabel9 = new javax.swing.JLabel();

    setLayout(new java.awt.GridBagLayout());

    alternateColorLabel.setText(ResourceMgr.getString("LblAlternateRowColor")); // NOI18N
    alternateColorLabel.setToolTipText(ResourceMgr.getString("d_LblAlternateRowColor")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 0);
    add(alternateColorLabel, gridBagConstraints);

    alternateColor.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
    add(alternateColor, gridBagConstraints);

    nullColor.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
    add(nullColor, gridBagConstraints);

    jLabel2.setText(ResourceMgr.getString("LblNullValueColor")); // NOI18N
    jLabel2.setToolTipText(ResourceMgr.getString("d_LblNullValueColor")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 0);
    add(jLabel2, gridBagConstraints);

    stdBackground.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
    add(stdBackground, gridBagConstraints);

    stdBackgroundLabel.setText(ResourceMgr.getString("LblTableBkgColor")); // NOI18N
    stdBackgroundLabel.setToolTipText(ResourceMgr.getString("d_LblTableBkgColor")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 0);
    add(stdBackgroundLabel, gridBagConstraints);

    textColorLabel.setText(ResourceMgr.getString("LblTableTextColor")); // NOI18N
    textColorLabel.setToolTipText(ResourceMgr.getString("d_LblTableTextColor")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(6, 10, 0, 0);
    add(textColorLabel, gridBagConstraints);

    textColor.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
    add(textColor, gridBagConstraints);

    selectionColorLabel.setText(ResourceMgr.getString("LblTableSelBckColor")); // NOI18N
    selectionColorLabel.setToolTipText(ResourceMgr.getString("d_LblTableSelBckColor")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 0);
    add(selectionColorLabel, gridBagConstraints);

    selectionColor.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
    add(selectionColor, gridBagConstraints);

    selectedTextColorLabel.setText(ResourceMgr.getString("LblTableSelTextColor")); // NOI18N
    selectedTextColorLabel.setToolTipText(ResourceMgr.getString("d_LblTableSelTextColor")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 0);
    add(selectedTextColorLabel, gridBagConstraints);

    selectedTextColor.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
    add(selectedTextColor, gridBagConstraints);

    jLabel7.setText(ResourceMgr.getString("LblModifiedColor")); // NOI18N
    jLabel7.setToolTipText(ResourceMgr.getString("d_LblModifiedColor")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 0);
    add(jLabel7, gridBagConstraints);

    modifiedColor.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
    add(modifiedColor, gridBagConstraints);

    jPanel7.setLayout(new java.awt.GridBagLayout());

    jLabel8.setText(ResourceMgr.getString("LblSelectionBlend")); // NOI18N
    jLabel8.setToolTipText(ResourceMgr.getString("d_LblSelectionBlend")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    jPanel7.add(jLabel8, gridBagConstraints);

    selectionBlend.setToolTipText(ResourceMgr.getString("d_LblSelectionBlend")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
    jPanel7.add(selectionBlend, gridBagConstraints);

    alternateBlend.setToolTipText(ResourceMgr.getString("d_LblAlternateBlend")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(3, 10, 0, 0);
    jPanel7.add(alternateBlend, gridBagConstraints);

    jLabel9.setText(ResourceMgr.getString("LblAlternateBlend")); // NOI18N
    jLabel9.setToolTipText(ResourceMgr.getString("d_LblAlternateBlend")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 0);
    jPanel7.add(jLabel9, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(7, 10, 0, 20);
    add(jPanel7, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JTextField alternateBlend;
  private workbench.gui.components.WbColorPicker alternateColor;
  private javax.swing.JLabel alternateColorLabel;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JLabel jLabel7;
  private javax.swing.JLabel jLabel8;
  private javax.swing.JLabel jLabel9;
  private javax.swing.JPanel jPanel7;
  private workbench.gui.components.WbColorPicker modifiedColor;
  private workbench.gui.components.WbColorPicker nullColor;
  private workbench.gui.components.WbColorPicker selectedTextColor;
  private javax.swing.JLabel selectedTextColorLabel;
  private javax.swing.JTextField selectionBlend;
  private workbench.gui.components.WbColorPicker selectionColor;
  private javax.swing.JLabel selectionColorLabel;
  private workbench.gui.components.WbColorPicker stdBackground;
  private javax.swing.JLabel stdBackgroundLabel;
  private workbench.gui.components.WbColorPicker textColor;
  private javax.swing.JLabel textColorLabel;
  // End of variables declaration//GEN-END:variables

}
