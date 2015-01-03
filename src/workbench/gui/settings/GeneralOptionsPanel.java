/*
 * GeneralOptionsPanel.java
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.SystemTray;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Locale;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import workbench.interfaces.Restoreable;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.KeepAliveDaemon;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbLabelField;
import workbench.gui.sql.IconHandler;

import workbench.util.MacOSHelper;
import workbench.util.WbFile;
import workbench.util.WbLocale;

/**
 *
 * @author  Thomas Kellerer
 */
public class GeneralOptionsPanel
	extends JPanel
	implements Restoreable, ActionListener, Disposable
{

	public GeneralOptionsPanel()
	{
		super();
		initComponents();

		brushedMetal.setVisible(MacOSHelper.isMacOS());
		brushedMetal.setEnabled(MacOSHelper.isMacOS());
		if (!brushedMetal.isVisible())
		{
			GridBagLayout layout = (GridBagLayout)jPanel2.getLayout();
			GridBagConstraints constraints = layout.getConstraints(singlePageHelp);
			constraints.weightx = 1.0;
			layout.setConstraints(singlePageHelp, constraints);
		}

		String[] updTypes = new String[] {
			ResourceMgr.getString("LblUpdCheckNever"),
			ResourceMgr.getString("LblUpdCheckDaily"),
			ResourceMgr.getString("LblUpdCheck7"),
			ResourceMgr.getString("LblUpdCheck14"),
			ResourceMgr.getString("LblUpdCheck30")
		};
		checkInterval.setModel(new DefaultComboBoxModel(updTypes));
		useSystemTray.setVisible(SystemTray.isSupported());
		WbSwingUtilities.repaintLater(iconCombobox);
		WbSwingUtilities.repaintLater(cancelIconCombo);
		useSystemTray.setEnabled(SystemTray.isSupported());
	}

	@Override
	public void restoreSettings()
	{
		logLevel.setSelectedItem(LogMgr.getLevel());
		int days = Settings.getInstance().getUpdateCheckInterval();
		if (days == 1)
		{
			checkInterval.setSelectedIndex(1);
		}
		else if (days == 7)
		{
			checkInterval.setSelectedIndex(2);
		}
		else if (days == 14)
		{
			checkInterval.setSelectedIndex(3);
		}
		else if (days == 30)
		{
			checkInterval.setSelectedIndex(4);
		}
		else
		{
			checkInterval.setSelectedIndex(0);
		}
		languageDropDown.removeAllItems();
		String currentLang = Settings.getInstance().getLanguage().getLanguage();

		Collection<WbLocale> locales = Settings.getInstance().getLanguages();
		int index = 0;
		int currentIndex = -1;
		for (WbLocale l : locales)
		{
			languageDropDown.addItem(l);
			if (l.getLocale().getLanguage().equals(currentLang))
			{
				currentIndex = index;
			}
			index++;
		}
		if (currentIndex != -1)
		{
			languageDropDown.setSelectedIndex(currentIndex);
		}
		WbFile configFile = Settings.getInstance().getConfigFile();
		String s = ResourceMgr.getFormattedString("LblSettingsLocation", configFile.getFullPath());
		settingsfilename.setText(s);
		settingsfilename.setBorder(new EmptyBorder(0,0,0,0));

		WbFile logFile = LogMgr.getLogfile();
		logfileLabel.setText(ResourceMgr.getFormattedString("LblLogLocation", logFile == null ? "": logFile.getFullPath()));
		logfileLabel.setCaretPosition(0);
		logfileLabel.setBorder(new EmptyBorder(1, 0, 1, 0));

		singlePageHelp.setSelected(Settings.getInstance().useSinglePageHelp());
		int tabPolicy = Settings.getInstance().getIntProperty(Settings.PROPERTY_TAB_POLICY, JTabbedPane.WRAP_TAB_LAYOUT);
		scrollTabs.setSelected(tabPolicy == JTabbedPane.SCROLL_TAB_LAYOUT);
		confirmTabClose.setSelected(GuiSettings.getConfirmTabClose());
		brushedMetal.setSelected(GuiSettings.getUseBrushedMetal());
		showTabCloseButton.setSelected(GuiSettings.getShowSqlTabCloseButton());
		showResultTabClose.setSelected(GuiSettings.getShowResultTabCloseButton());
		onlyActiveTab.setSelected(GuiSettings.getCloseActiveTabOnly());
		closeButtonRightSide.setSelected(GuiSettings.getShowCloseButtonOnRightSide());
		tabLRUclose.setSelected(GuiSettings.getUseLRUForTabs());
		showFinishAlert.setSelected(GuiSettings.showScriptFinishedAlert());
		useSystemTray.setEnabled(SystemTray.isSupported() && GuiSettings.showScriptFinishedAlert());
		useSystemTray.setSelected(GuiSettings.useSystemTrayForAlert());
		long duration = GuiSettings.getScriptFinishedAlertDuration();
		String durationDisplay = KeepAliveDaemon.getTimeDisplay(duration);
		alertDuration.setText(durationDisplay);
		alertDuration.setEnabled(showFinishAlert.isSelected());
		logAllStatements.setSelected(Settings.getInstance().getLogAllStatements());
		autoSaveProfiles.setSelected(Settings.getInstance().getSaveProfilesImmediately());
		enableQuickFilter.setSelected(GuiSettings.enableProfileQuickFilter());

		String iconName = Settings.getInstance().getProperty(IconHandler.PROP_LOADING_IMAGE, IconHandler.DEFAULT_BUSY_IMAGE);
		LoadingImage img = new LoadingImage();
		img.setName(iconName);
		iconCombobox.setSelectedItem(img);

		iconName = Settings.getInstance().getProperty(IconHandler.PROP_CANCEL_IMAGE, IconHandler.DEFAULT_CANCEL_IMAGE);
		img = new LoadingImage();
		img.setName(iconName);
		cancelIconCombo.setSelectedItem(img);
	}

	@Override
	public void saveSettings()
	{
		Settings set = Settings.getInstance();

		// General settings
		GuiSettings.setShowCloseButtonOnRightSide(closeButtonRightSide.isSelected());
		GuiSettings.setCloseActiveTabOnly(onlyActiveTab.isSelected());
		GuiSettings.setShowTabCloseButton(showTabCloseButton.isSelected());
		GuiSettings.setShowResultTabCloseButton(showResultTabClose.isSelected());
		GuiSettings.setShowTabIndex(showTabIndex.isSelected());
		GuiSettings.setConfirmTabClose(confirmTabClose.isSelected());
		GuiSettings.setEnableProfileQuickFilter(enableQuickFilter.isSelected());
		set.setUseEncryption(this.useEncryption.isSelected());
		set.setConsolidateLogMsg(this.consolidateLog.isSelected());
		set.setExitOnFirstConnectCancel(exitOnConnectCancel.isSelected());
		set.setShowConnectDialogOnStartup(autoConnect.isSelected());
		set.setLogAllStatements(logAllStatements.isSelected());
		int index = checkInterval.getSelectedIndex();
		switch (index)
		{
			case 1:
				set.setUpdateCheckInterval(1);
				break;
			case 2:
				set.setUpdateCheckInterval(7);
				break;
			case 3:
				set.setUpdateCheckInterval(14);
				break;
			case 4:
				set.setUpdateCheckInterval(30);
				break;
			default:
				set.setUpdateCheckInterval(-1);
				break;
		}
		String level = (String)logLevel.getSelectedItem();
		LogMgr.setLevel(level);
		set.setProperty("workbench.log.level", level);
		set.setLanguage(getSelectedLanguage());
		set.setUseSinglePageHelp(singlePageHelp.isSelected());
		if (scrollTabs.isSelected())
		{
			set.setProperty(Settings.PROPERTY_TAB_POLICY, JTabbedPane.SCROLL_TAB_LAYOUT);
		}
		else
		{
			set.setProperty(Settings.PROPERTY_TAB_POLICY, JTabbedPane.WRAP_TAB_LAYOUT);
		}
		if (brushedMetal.isVisible())
		{
			GuiSettings.setUseBrushedMetal(brushedMetal.isSelected());
		}
		GuiSettings.setUseLRUForTabs(tabLRUclose.isSelected());
		GuiSettings.setShowScriptFinishedAlert(showFinishAlert.isSelected());
		String v = alertDuration.getText().trim();
		long duration = KeepAliveDaemon.parseTimeInterval(v);
		GuiSettings.setScriptFinishedAlertDuration(duration);
		set.setSaveProfilesImmediately(autoSaveProfiles.isSelected());
		if (SystemTray.isSupported())
		{
			GuiSettings.setUseSystemTrayForAlert(useSystemTray.isSelected());
		}
		LoadingImage img = (LoadingImage)iconCombobox.getSelectedItem();
		Settings.getInstance().setProperty(IconHandler.PROP_LOADING_IMAGE, img.getName());

		LoadingImage cancelImg = (LoadingImage)cancelIconCombo.getSelectedItem();
		Settings.getInstance().setProperty(IconHandler.PROP_CANCEL_IMAGE, cancelImg.getName());
	}

	@Override
	public void dispose()
	{
		((IconListCombobox)iconCombobox).done();
		((IconListCombobox)cancelIconCombo).done();
	}

	private Locale getSelectedLanguage()
	{
		WbLocale wl = (WbLocale)languageDropDown.getSelectedItem();
		return wl.getLocale();
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

    jPanel2 = new javax.swing.JPanel();
    useEncryption = new javax.swing.JCheckBox();
    consolidateLog = new javax.swing.JCheckBox();
    exitOnConnectCancel = new javax.swing.JCheckBox();
    autoConnect = new javax.swing.JCheckBox();
    singlePageHelp = new javax.swing.JCheckBox();
    brushedMetal = new javax.swing.JCheckBox();
    autoSaveProfiles = new javax.swing.JCheckBox();
    enableQuickFilter = new javax.swing.JCheckBox();
    settingsfilename = new WbLabelField();
    jPanel1 = new javax.swing.JPanel();
    showTabIndex = new javax.swing.JCheckBox();
    scrollTabs = new javax.swing.JCheckBox();
    confirmTabClose = new javax.swing.JCheckBox();
    showTabCloseButton = new javax.swing.JCheckBox();
    showResultTabClose = new javax.swing.JCheckBox();
    onlyActiveTab = new javax.swing.JCheckBox();
    closeButtonRightSide = new javax.swing.JCheckBox();
    jSeparator1 = new javax.swing.JSeparator();
    tabLRUclose = new javax.swing.JCheckBox();
    imagePanel = new javax.swing.JPanel();
    iconCombobox = new IconListCombobox();
    busyIconLabel = new javax.swing.JLabel();
    cancelIconCombo = new IconListCombobox();
    cancelIconLabel = new javax.swing.JLabel();
    jSeparator2 = new javax.swing.JSeparator();
    jSeparator3 = new javax.swing.JSeparator();
    jPanel3 = new javax.swing.JPanel();
    logLevelLabel = new javax.swing.JLabel();
    logLevel = new javax.swing.JComboBox();
    jSeparator4 = new javax.swing.JSeparator();
    jPanel4 = new javax.swing.JPanel();
    showFinishAlert = new javax.swing.JCheckBox();
    jLabel2 = new javax.swing.JLabel();
    alertDuration = new javax.swing.JTextField();
    useSystemTray = new javax.swing.JCheckBox();
    jPanel5 = new javax.swing.JPanel();
    langLabel = new javax.swing.JLabel();
    languageDropDown = new javax.swing.JComboBox();
    checkUpdatesLabel = new javax.swing.JLabel();
    checkInterval = new javax.swing.JComboBox();
    logAllStatements = new javax.swing.JCheckBox();
    jSeparator5 = new javax.swing.JSeparator();
    logfileLabel = new WbLabelField();

    setLayout(new java.awt.GridBagLayout());

    jPanel2.setLayout(new java.awt.GridBagLayout());

    useEncryption.setSelected(Settings.getInstance().getUseEncryption());
    useEncryption.setText(ResourceMgr.getString("LblUseEncryption")); // NOI18N
    useEncryption.setToolTipText(ResourceMgr.getString("d_LblUseEncryption")); // NOI18N
    useEncryption.setBorder(null);
    useEncryption.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    useEncryption.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    useEncryption.setIconTextGap(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 10, 4, 0);
    jPanel2.add(useEncryption, gridBagConstraints);

    consolidateLog.setSelected(Settings.getInstance().getConsolidateLogMsg());
    consolidateLog.setText(ResourceMgr.getString("LblConsolidateLog")); // NOI18N
    consolidateLog.setToolTipText(ResourceMgr.getString("d_LblConsolidateLog")); // NOI18N
    consolidateLog.setBorder(null);
    consolidateLog.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    consolidateLog.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    consolidateLog.setIconTextGap(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 10, 4, 0);
    jPanel2.add(consolidateLog, gridBagConstraints);

    exitOnConnectCancel.setSelected(Settings.getInstance().getExitOnFirstConnectCancel());
    exitOnConnectCancel.setText(ResourceMgr.getString("LblExitOnConnectCancel")); // NOI18N
    exitOnConnectCancel.setToolTipText(ResourceMgr.getString("d_LblExitOnConnectCancel")); // NOI18N
    exitOnConnectCancel.setBorder(null);
    exitOnConnectCancel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    exitOnConnectCancel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    exitOnConnectCancel.setIconTextGap(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
    jPanel2.add(exitOnConnectCancel, gridBagConstraints);

    autoConnect.setSelected(Settings.getInstance().getShowConnectDialogOnStartup());
    autoConnect.setText(ResourceMgr.getString("LblShowConnect")); // NOI18N
    autoConnect.setToolTipText(ResourceMgr.getString("d_LblShowConnect")); // NOI18N
    autoConnect.setBorder(null);
    autoConnect.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    autoConnect.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    autoConnect.setIconTextGap(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
    jPanel2.add(autoConnect, gridBagConstraints);

    singlePageHelp.setText(ResourceMgr.getString("LblHelpSingle")); // NOI18N
    singlePageHelp.setToolTipText(ResourceMgr.getString("d_LblHelpSingle")); // NOI18N
    singlePageHelp.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 10, 4, 0);
    jPanel2.add(singlePageHelp, gridBagConstraints);

    brushedMetal.setText(ResourceMgr.getString("LblBrushedMetal")); // NOI18N
    brushedMetal.setToolTipText(ResourceMgr.getString("d_LblBrushedMetal")); // NOI18N
    brushedMetal.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 10, 4, 0);
    jPanel2.add(brushedMetal, gridBagConstraints);

    autoSaveProfiles.setSelected(Settings.getInstance().getConsolidateLogMsg());
    autoSaveProfiles.setText(ResourceMgr.getString("LblAutoSaveProfiles")); // NOI18N
    autoSaveProfiles.setToolTipText(ResourceMgr.getString("d_LblAutoSaveProfiles")); // NOI18N
    autoSaveProfiles.setBorder(null);
    autoSaveProfiles.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    autoSaveProfiles.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    autoSaveProfiles.setIconTextGap(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
    jPanel2.add(autoSaveProfiles, gridBagConstraints);

    enableQuickFilter.setText(ResourceMgr.getString("LblProfileQuickFilter")); // NOI18N
    enableQuickFilter.setToolTipText(ResourceMgr.getString("d_LblProfileQuickFilter")); // NOI18N
    enableQuickFilter.setBorder(null);
    enableQuickFilter.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    enableQuickFilter.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    enableQuickFilter.setIconTextGap(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weighty = 1.0;
    jPanel2.add(enableQuickFilter, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    gridBagConstraints.insets = new java.awt.Insets(10, 12, 0, 15);
    add(jPanel2, gridBagConstraints);

    settingsfilename.setText("Settings");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 16;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 12, 2, 15);
    add(settingsfilename, gridBagConstraints);

    jPanel1.setLayout(new java.awt.GridBagLayout());

    showTabIndex.setSelected(GuiSettings.getShowTabIndex());
    showTabIndex.setText(ResourceMgr.getString("LblShowTabIndex")); // NOI18N
    showTabIndex.setToolTipText(ResourceMgr.getString("d_LblShowTabIndex")); // NOI18N
    showTabIndex.setBorder(null);
    showTabIndex.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    showTabIndex.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    showTabIndex.setIconTextGap(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 16, 1, 0);
    jPanel1.add(showTabIndex, gridBagConstraints);

    scrollTabs.setText(ResourceMgr.getString("LblScrolTabs")); // NOI18N
    scrollTabs.setToolTipText(ResourceMgr.getString("d_LblScrolTabs")); // NOI18N
    scrollTabs.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 16, 0, 0);
    jPanel1.add(scrollTabs, gridBagConstraints);

    confirmTabClose.setText(ResourceMgr.getString("LblConfirmTabClose")); // NOI18N
    confirmTabClose.setToolTipText(ResourceMgr.getString("d_LblConfirmTabClose")); // NOI18N
    confirmTabClose.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 0);
    jPanel1.add(confirmTabClose, gridBagConstraints);

    showTabCloseButton.setText(ResourceMgr.getString("LblShowTabClose")); // NOI18N
    showTabCloseButton.setToolTipText(ResourceMgr.getString("d_LblShowTabClose")); // NOI18N
    showTabCloseButton.setBorder(null);
    showTabCloseButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    showTabCloseButton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    showTabCloseButton.setIconTextGap(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 2, 0);
    jPanel1.add(showTabCloseButton, gridBagConstraints);

    showResultTabClose.setText(ResourceMgr.getString("LblShowResultClose")); // NOI18N
    showResultTabClose.setToolTipText(ResourceMgr.getString("d_LblShowResultClose")); // NOI18N
    showResultTabClose.setBorder(null);
    showResultTabClose.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    showResultTabClose.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    showResultTabClose.setIconTextGap(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 16, 2, 0);
    jPanel1.add(showResultTabClose, gridBagConstraints);

    onlyActiveTab.setText(ResourceMgr.getString("LblCloseActive")); // NOI18N
    onlyActiveTab.setToolTipText(ResourceMgr.getString("d_LblCloseActive")); // NOI18N
    onlyActiveTab.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(3, 4, 5, 0);
    jPanel1.add(onlyActiveTab, gridBagConstraints);

    closeButtonRightSide.setText(ResourceMgr.getString("LblCloseOnRight")); // NOI18N
    closeButtonRightSide.setToolTipText(ResourceMgr.getString("d_LblCloseOnRight")); // NOI18N
    closeButtonRightSide.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(3, 16, 5, 0);
    jPanel1.add(closeButtonRightSide, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 3, 3, 0);
    jPanel1.add(jSeparator1, gridBagConstraints);

    tabLRUclose.setText(ResourceMgr.getString("LblTabOrderLRU")); // NOI18N
    tabLRUclose.setToolTipText(ResourceMgr.getString("d_LblTabOrderLRU")); // NOI18N
    tabLRUclose.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 0);
    jPanel1.add(tabLRUclose, gridBagConstraints);

    imagePanel.setLayout(new java.awt.GridBagLayout());

    iconCombobox.setModel(IconListCombobox.getBusyIcons());
    iconCombobox.setToolTipText(ResourceMgr.getString("d_LblBusyIcon")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    imagePanel.add(iconCombobox, gridBagConstraints);

    busyIconLabel.setLabelFor(iconCombobox);
    busyIconLabel.setText(ResourceMgr.getString("LblBusyIcon")); // NOI18N
    busyIconLabel.setToolTipText(ResourceMgr.getString("d_LblBusyIcon")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    imagePanel.add(busyIconLabel, gridBagConstraints);

    cancelIconCombo.setModel(IconListCombobox.getCancelIcons());
    cancelIconCombo.setToolTipText(ResourceMgr.getString("d_LblBusyIcon")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    imagePanel.add(cancelIconCombo, gridBagConstraints);

    cancelIconLabel.setLabelFor(cancelIconCombo);
    cancelIconLabel.setText(ResourceMgr.getString("LblCancelIcon")); // NOI18N
    cancelIconLabel.setToolTipText(ResourceMgr.getString("d_LblCancelIcon")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    imagePanel.add(cancelIconLabel, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridheight = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 12, 1, 0);
    jPanel1.add(imagePanel, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 7, 0, 10);
    add(jPanel1, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(3, 7, 3, 10);
    add(jSeparator2, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(4, 7, 3, 10);
    add(jSeparator3, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 13;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    add(jPanel3, gridBagConstraints);

    logLevelLabel.setText(ResourceMgr.getString("LblLogLevel")); // NOI18N
    logLevelLabel.setToolTipText(ResourceMgr.getString("d_LblLogLevel")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 12;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 11, 0, 0);
    add(logLevelLabel, gridBagConstraints);

    logLevel.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ERROR", "WARNING", "INFO", "DEBUG" }));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 12;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
    add(logLevel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(0, 7, 6, 10);
    add(jSeparator4, gridBagConstraints);

    jPanel4.setLayout(new java.awt.GridBagLayout());

    showFinishAlert.setText(ResourceMgr.getString("LblShowScriptEndAlert")); // NOI18N
    showFinishAlert.setBorder(null);
    showFinishAlert.addActionListener(this);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
    jPanel4.add(showFinishAlert, gridBagConstraints);

    jLabel2.setText(ResourceMgr.getString("LblScriptEndAlertDuration")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(7, 11, 0, 0);
    jPanel4.add(jLabel2, gridBagConstraints);

    alertDuration.setColumns(8);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 8, 0, 0);
    jPanel4.add(alertDuration, gridBagConstraints);

    useSystemTray.setText(ResourceMgr.getString("LblAlertSysTray")); // NOI18N
    useSystemTray.setBorder(null);
    useSystemTray.setMargin(new java.awt.Insets(0, 0, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
    jPanel4.add(useSystemTray, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    add(jPanel4, gridBagConstraints);

    jPanel5.setLayout(new java.awt.GridBagLayout());

    langLabel.setText(ResourceMgr.getString("LblLanguage")); // NOI18N
    langLabel.setToolTipText(ResourceMgr.getString("d_LblLanguage")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(10, 12, 0, 0);
    jPanel5.add(langLabel, gridBagConstraints);

    languageDropDown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "English", "German" }));
    languageDropDown.setToolTipText(ResourceMgr.getDescription("LblLanguage"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 8);
    jPanel5.add(languageDropDown, gridBagConstraints);

    checkUpdatesLabel.setText(ResourceMgr.getString("LblCheckForUpdate")); // NOI18N
    checkUpdatesLabel.setToolTipText(ResourceMgr.getString("d_LblCheckForUpdate")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(6, 7, 0, 0);
    jPanel5.add(checkUpdatesLabel, gridBagConstraints);

    checkInterval.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "never", "daily", "7 days", "14 days", "30 days" }));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(10, 7, 0, 0);
    jPanel5.add(checkInterval, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    add(jPanel5, gridBagConstraints);

    logAllStatements.setSelected(Settings.getInstance().getConsolidateLogMsg());
    logAllStatements.setText(ResourceMgr.getString("LblLogAllSql")); // NOI18N
    logAllStatements.setToolTipText(ResourceMgr.getString("d_LblLogAllSql")); // NOI18N
    logAllStatements.setBorder(null);
    logAllStatements.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    logAllStatements.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    logAllStatements.setIconTextGap(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 10;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(3, 11, 5, 0);
    add(logAllStatements, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 11;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(0, 7, 9, 10);
    add(jSeparator5, gridBagConstraints);

    logfileLabel.setText("Logfile");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 15;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 15);
    add(logfileLabel, gridBagConstraints);
  }

  // Code for dispatching events from components to event handlers.

  public void actionPerformed(java.awt.event.ActionEvent evt)
  {
    if (evt.getSource() == showFinishAlert)
    {
      GeneralOptionsPanel.this.showFinishAlertActionPerformed(evt);
    }
  }// </editor-fold>//GEN-END:initComponents

	private void showFinishAlertActionPerformed(ActionEvent evt)//GEN-FIRST:event_showFinishAlertActionPerformed
	{//GEN-HEADEREND:event_showFinishAlertActionPerformed
		this.alertDuration.setEnabled(showFinishAlert.isSelected());
		this.useSystemTray.setEnabled(SystemTray.isSupported() && showFinishAlert.isSelected());
	}//GEN-LAST:event_showFinishAlertActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JTextField alertDuration;
  private javax.swing.JCheckBox autoConnect;
  private javax.swing.JCheckBox autoSaveProfiles;
  private javax.swing.JCheckBox brushedMetal;
  private javax.swing.JLabel busyIconLabel;
  private javax.swing.JComboBox cancelIconCombo;
  private javax.swing.JLabel cancelIconLabel;
  private javax.swing.JComboBox checkInterval;
  private javax.swing.JLabel checkUpdatesLabel;
  private javax.swing.JCheckBox closeButtonRightSide;
  private javax.swing.JCheckBox confirmTabClose;
  private javax.swing.JCheckBox consolidateLog;
  private javax.swing.JCheckBox enableQuickFilter;
  private javax.swing.JCheckBox exitOnConnectCancel;
  private javax.swing.JComboBox iconCombobox;
  private javax.swing.JPanel imagePanel;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JPanel jPanel2;
  private javax.swing.JPanel jPanel3;
  private javax.swing.JPanel jPanel4;
  private javax.swing.JPanel jPanel5;
  private javax.swing.JSeparator jSeparator1;
  private javax.swing.JSeparator jSeparator2;
  private javax.swing.JSeparator jSeparator3;
  private javax.swing.JSeparator jSeparator4;
  private javax.swing.JSeparator jSeparator5;
  private javax.swing.JLabel langLabel;
  private javax.swing.JComboBox languageDropDown;
  private javax.swing.JCheckBox logAllStatements;
  private javax.swing.JComboBox logLevel;
  private javax.swing.JLabel logLevelLabel;
  private javax.swing.JTextField logfileLabel;
  private javax.swing.JCheckBox onlyActiveTab;
  private javax.swing.JCheckBox scrollTabs;
  private javax.swing.JTextField settingsfilename;
  private javax.swing.JCheckBox showFinishAlert;
  private javax.swing.JCheckBox showResultTabClose;
  private javax.swing.JCheckBox showTabCloseButton;
  private javax.swing.JCheckBox showTabIndex;
  private javax.swing.JCheckBox singlePageHelp;
  private javax.swing.JCheckBox tabLRUclose;
  private javax.swing.JCheckBox useEncryption;
  private javax.swing.JCheckBox useSystemTray;
  // End of variables declaration//GEN-END:variables

}
