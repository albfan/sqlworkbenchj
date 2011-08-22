/*
 * GeneralOptionsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.settings;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Locale;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import workbench.db.KeepAliveDaemon;
import workbench.gui.components.WbLabelField;
import workbench.interfaces.Restoreable;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.MacOSHelper;
import workbench.util.WbFile;
import workbench.util.WbLocale;

/**
 *
 * @author  Thomas Kellerer
 */
public class GeneralOptionsPanel
	extends JPanel
	implements Restoreable, ActionListener
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
			GridBagConstraints constraints = layout.getConstraints(autoSaveProfiles);
			constraints.weightx = 1.0;
			layout.setConstraints(autoSaveProfiles, constraints);
		}

		String[] updTypes = new String[] {
			ResourceMgr.getString("LblUpdCheckNever"),
			ResourceMgr.getString("LblUpdCheckDaily"),
			ResourceMgr.getString("LblUpdCheck7"),
			ResourceMgr.getString("LblUpdCheck14"),
			ResourceMgr.getString("LblUpdCheck30")
		};
		checkInterval.setModel(new DefaultComboBoxModel(updTypes));
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
		long duration = GuiSettings.getScriptFinishedAlertDuration();
		String durationDisplay = KeepAliveDaemon.getTimeDisplay(duration);
		alertDuration.setText(durationDisplay);
		alertDuration.setEnabled(showFinishAlert.isSelected());
		logAllStatements.setSelected(Settings.getInstance().getLogAllStatements());
		autoSaveProfiles.setSelected(Settings.getInstance().getSaveProfilesImmediately());
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
		set.setUseEncryption(this.useEncryption.isSelected());
		GuiSettings.setUseAnimatedIcon(this.enableAnimatedIcon.isSelected());
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
    private void initComponents() {
		GridBagConstraints gridBagConstraints;

        jPanel2 = new JPanel();
        useEncryption = new JCheckBox();
        consolidateLog = new JCheckBox();
        exitOnConnectCancel = new JCheckBox();
        autoConnect = new JCheckBox();
        singlePageHelp = new JCheckBox();
        brushedMetal = new JCheckBox();
        autoSaveProfiles = new JCheckBox();
        settingsfilename = new WbLabelField();
        jPanel1 = new JPanel();
        showTabIndex = new JCheckBox();
        scrollTabs = new JCheckBox();
        enableAnimatedIcon = new JCheckBox();
        confirmTabClose = new JCheckBox();
        showTabCloseButton = new JCheckBox();
        showResultTabClose = new JCheckBox();
        onlyActiveTab = new JCheckBox();
        closeButtonRightSide = new JCheckBox();
        jSeparator1 = new JSeparator();
        tabLRUclose = new JCheckBox();
        jSeparator2 = new JSeparator();
        jSeparator3 = new JSeparator();
        jPanel3 = new JPanel();
        logLevelLabel = new JLabel();
        logLevel = new JComboBox();
        jSeparator4 = new JSeparator();
        jPanel4 = new JPanel();
        showFinishAlert = new JCheckBox();
        jLabel2 = new JLabel();
        alertDuration = new JTextField();
        jPanel5 = new JPanel();
        langLabel = new JLabel();
        languageDropDown = new JComboBox();
        checkUpdatesLabel = new JLabel();
        checkInterval = new JComboBox();
        logAllStatements = new JCheckBox();
        jSeparator5 = new JSeparator();

        setLayout(new GridBagLayout());

        jPanel2.setLayout(new GridBagLayout());

        useEncryption.setSelected(Settings.getInstance().getUseEncryption());
        useEncryption.setText(ResourceMgr.getString("LblUseEncryption"));         useEncryption.setToolTipText(ResourceMgr.getString("d_LblUseEncryption"));         useEncryption.setBorder(null);
        useEncryption.setHorizontalAlignment(SwingConstants.LEFT);
        useEncryption.setHorizontalTextPosition(SwingConstants.RIGHT);
        useEncryption.setIconTextGap(5);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 15, 1, 0);
        jPanel2.add(useEncryption, gridBagConstraints);

        consolidateLog.setSelected(Settings.getInstance().getConsolidateLogMsg());
        consolidateLog.setText(ResourceMgr.getString("LblConsolidateLog"));         consolidateLog.setToolTipText(ResourceMgr.getString("d_LblConsolidateLog"));         consolidateLog.setBorder(null);
        consolidateLog.setHorizontalAlignment(SwingConstants.LEFT);
        consolidateLog.setHorizontalTextPosition(SwingConstants.RIGHT);
        consolidateLog.setIconTextGap(5);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(7, 15, 1, 0);
        jPanel2.add(consolidateLog, gridBagConstraints);

        exitOnConnectCancel.setSelected(Settings.getInstance().getExitOnFirstConnectCancel());
        exitOnConnectCancel.setText(ResourceMgr.getString("LblExitOnConnectCancel"));         exitOnConnectCancel.setToolTipText(ResourceMgr.getString("d_LblExitOnConnectCancel"));         exitOnConnectCancel.setBorder(null);
        exitOnConnectCancel.setHorizontalAlignment(SwingConstants.LEFT);
        exitOnConnectCancel.setHorizontalTextPosition(SwingConstants.RIGHT);
        exitOnConnectCancel.setIconTextGap(5);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(7, 0, 1, 0);
        jPanel2.add(exitOnConnectCancel, gridBagConstraints);

        autoConnect.setSelected(Settings.getInstance().getShowConnectDialogOnStartup());
        autoConnect.setText(ResourceMgr.getString("LblShowConnect"));         autoConnect.setToolTipText(ResourceMgr.getString("d_LblShowConnect"));         autoConnect.setBorder(null);
        autoConnect.setHorizontalAlignment(SwingConstants.LEFT);
        autoConnect.setHorizontalTextPosition(SwingConstants.RIGHT);
        autoConnect.setIconTextGap(5);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 0, 1, 0);
        jPanel2.add(autoConnect, gridBagConstraints);

        singlePageHelp.setText(ResourceMgr.getString("LblHelpSingle"));         singlePageHelp.setToolTipText(ResourceMgr.getString("d_LblHelpSingle"));         singlePageHelp.setBorder(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(6, 0, 1, 0);
        jPanel2.add(singlePageHelp, gridBagConstraints);

        brushedMetal.setText(ResourceMgr.getString("LblBrushedMetal"));         brushedMetal.setToolTipText(ResourceMgr.getString("d_LblBrushedMetal"));         brushedMetal.setBorder(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(0, 15, 1, 0);
        jPanel2.add(brushedMetal, gridBagConstraints);

        autoSaveProfiles.setSelected(Settings.getInstance().getConsolidateLogMsg());
        autoSaveProfiles.setText(ResourceMgr.getString("LblAutoSaveProfiles"));         autoSaveProfiles.setToolTipText(ResourceMgr.getString("d_LblAutoSaveProfiles"));         autoSaveProfiles.setBorder(null);
        autoSaveProfiles.setHorizontalAlignment(SwingConstants.LEFT);
        autoSaveProfiles.setHorizontalTextPosition(SwingConstants.RIGHT);
        autoSaveProfiles.setIconTextGap(5);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(7, 15, 1, 0);
        jPanel2.add(autoSaveProfiles, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(10, 12, 0, 15);
        add(jPanel2, gridBagConstraints);

        settingsfilename.setText("jTextField1");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new Insets(5, 12, 2, 15);
        add(settingsfilename, gridBagConstraints);

        jPanel1.setLayout(new GridBagLayout());

        showTabIndex.setSelected(GuiSettings.getShowTabIndex());
        showTabIndex.setText(ResourceMgr.getString("LblShowTabIndex"));         showTabIndex.setToolTipText(ResourceMgr.getString("d_LblShowTabIndex"));         showTabIndex.setBorder(null);
        showTabIndex.setHorizontalAlignment(SwingConstants.LEFT);
        showTabIndex.setHorizontalTextPosition(SwingConstants.RIGHT);
        showTabIndex.setIconTextGap(5);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(2, 4, 1, 0);
        jPanel1.add(showTabIndex, gridBagConstraints);

        scrollTabs.setText(ResourceMgr.getString("LblScrolTabs"));         scrollTabs.setToolTipText(ResourceMgr.getString("d_LblScrolTabs"));         scrollTabs.setBorder(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(2, 16, 1, 0);
        jPanel1.add(scrollTabs, gridBagConstraints);

        enableAnimatedIcon.setSelected(GuiSettings.getUseAnimatedIcon());
        enableAnimatedIcon.setText(ResourceMgr.getString("LblEnableAnimatedIcon"));         enableAnimatedIcon.setToolTipText(ResourceMgr.getString("d_LblEnableAnimatedIcon"));         enableAnimatedIcon.setBorder(null);
        enableAnimatedIcon.setHorizontalAlignment(SwingConstants.LEFT);
        enableAnimatedIcon.setHorizontalTextPosition(SwingConstants.RIGHT);
        enableAnimatedIcon.setIconTextGap(5);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(6, 16, 1, 0);
        jPanel1.add(enableAnimatedIcon, gridBagConstraints);

        confirmTabClose.setText(ResourceMgr.getString("LblConfirmTabClose"));         confirmTabClose.setToolTipText(ResourceMgr.getString("d_LblConfirmTabClose"));         confirmTabClose.setBorder(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(6, 4, 1, 0);
        jPanel1.add(confirmTabClose, gridBagConstraints);

        showTabCloseButton.setText(ResourceMgr.getString("LblShowTabClose"));         showTabCloseButton.setToolTipText(ResourceMgr.getString("d_LblShowTabClose"));         showTabCloseButton.setBorder(null);
        showTabCloseButton.setHorizontalAlignment(SwingConstants.LEFT);
        showTabCloseButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        showTabCloseButton.setIconTextGap(5);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(0, 4, 5, 0);
        jPanel1.add(showTabCloseButton, gridBagConstraints);

        showResultTabClose.setText(ResourceMgr.getString("LblShowResultClose"));         showResultTabClose.setToolTipText(ResourceMgr.getString("d_LblShowResultClose"));         showResultTabClose.setBorder(null);
        showResultTabClose.setHorizontalAlignment(SwingConstants.LEFT);
        showResultTabClose.setHorizontalTextPosition(SwingConstants.RIGHT);
        showResultTabClose.setIconTextGap(5);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(0, 16, 5, 0);
        jPanel1.add(showResultTabClose, gridBagConstraints);

        onlyActiveTab.setText(ResourceMgr.getString("LblCloseActive"));         onlyActiveTab.setToolTipText(ResourceMgr.getString("d_LblCloseActive"));         onlyActiveTab.setBorder(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(3, 4, 5, 0);
        jPanel1.add(onlyActiveTab, gridBagConstraints);

        closeButtonRightSide.setText(ResourceMgr.getString("LblCloseOnRight"));         closeButtonRightSide.setToolTipText(ResourceMgr.getString("d_LblCloseOnRight"));         closeButtonRightSide.setBorder(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(3, 16, 5, 0);
        jPanel1.add(closeButtonRightSide, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(8, 3, 8, 0);
        jPanel1.add(jSeparator1, gridBagConstraints);

        tabLRUclose.setText(ResourceMgr.getString("LblTabOrderLRU"));         tabLRUclose.setToolTipText(ResourceMgr.getString("d_LblTabOrderLRU"));         tabLRUclose.setBorder(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(6, 4, 0, 0);
        jPanel1.add(tabLRUclose, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(6, 7, 0, 10);
        add(jPanel1, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(6, 7, 7, 10);
        add(jSeparator2, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(7, 7, 2, 10);
        add(jSeparator3, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(jPanel3, gridBagConstraints);

        logLevelLabel.setText(ResourceMgr.getString("LblLogLevel"));         logLevelLabel.setToolTipText(ResourceMgr.getString("d_LblLogLevel"));         gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 11, 0, 0);
        add(logLevelLabel, gridBagConstraints);

        logLevel.setModel(new DefaultComboBoxModel(new String[] { "ERROR", "WARNING", "INFO", "DEBUG" }));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 10, 0, 10);
        add(logLevel, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(7, 7, 11, 10);
        add(jSeparator4, gridBagConstraints);

        jPanel4.setLayout(new GridBagLayout());

        showFinishAlert.setText(ResourceMgr.getString("LblShowScriptEndAlert"));         showFinishAlert.setBorder(null);
        showFinishAlert.addActionListener(this);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 10, 7, 0);
        jPanel4.add(showFinishAlert, gridBagConstraints);

        jLabel2.setText(ResourceMgr.getString("LblScriptEndAlertDuration"));         gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 11, 0, 0);
        jPanel4.add(jLabel2, gridBagConstraints);

        alertDuration.setColumns(8);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        jPanel4.add(alertDuration, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        add(jPanel4, gridBagConstraints);

        jPanel5.setLayout(new GridBagLayout());

        langLabel.setText(ResourceMgr.getString("LblLanguage"));         langLabel.setToolTipText(ResourceMgr.getString("d_LblLanguage"));         gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(10, 12, 0, 0);
        jPanel5.add(langLabel, gridBagConstraints);

        languageDropDown.setModel(new DefaultComboBoxModel(new String[] { "English", "German" }));
        languageDropDown.setToolTipText(ResourceMgr.getDescription("LblLanguage"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(10, 10, 0, 8);
        jPanel5.add(languageDropDown, gridBagConstraints);

        checkUpdatesLabel.setText(ResourceMgr.getString("LblCheckForUpdate"));         checkUpdatesLabel.setToolTipText(ResourceMgr.getString("d_LblCheckForUpdate"));         gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(6, 7, 0, 0);
        jPanel5.add(checkUpdatesLabel, gridBagConstraints);

        checkInterval.setModel(new DefaultComboBoxModel(new String[] { "never", "daily", "7 days", "14 days", "30 days" }));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(10, 7, 0, 0);
        jPanel5.add(checkInterval, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        add(jPanel5, gridBagConstraints);

        logAllStatements.setSelected(Settings.getInstance().getConsolidateLogMsg());
        logAllStatements.setText(ResourceMgr.getString("LblLogAllSql"));         logAllStatements.setToolTipText(ResourceMgr.getString("d_LblLogAllSql"));         logAllStatements.setBorder(null);
        logAllStatements.setHorizontalAlignment(SwingConstants.LEFT);
        logAllStatements.setHorizontalTextPosition(SwingConstants.RIGHT);
        logAllStatements.setIconTextGap(5);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(3, 11, 8, 0);
        add(logAllStatements, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(2, 7, 12, 10);
        add(jSeparator5, gridBagConstraints);
    }

    // Code for dispatching events from components to event handlers.

    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() == showFinishAlert) {
            GeneralOptionsPanel.this.showFinishAlertActionPerformed(evt);
        }
    }// </editor-fold>//GEN-END:initComponents

	private void showFinishAlertActionPerformed(ActionEvent evt)//GEN-FIRST:event_showFinishAlertActionPerformed
	{//GEN-HEADEREND:event_showFinishAlertActionPerformed
		this.alertDuration.setEnabled(showFinishAlert.isSelected());
	}//GEN-LAST:event_showFinishAlertActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JTextField alertDuration;
    private JCheckBox autoConnect;
    private JCheckBox autoSaveProfiles;
    private JCheckBox brushedMetal;
    private JComboBox checkInterval;
    private JLabel checkUpdatesLabel;
    private JCheckBox closeButtonRightSide;
    private JCheckBox confirmTabClose;
    private JCheckBox consolidateLog;
    private JCheckBox enableAnimatedIcon;
    private JCheckBox exitOnConnectCancel;
    private JLabel jLabel2;
    private JPanel jPanel1;
    private JPanel jPanel2;
    private JPanel jPanel3;
    private JPanel jPanel4;
    private JPanel jPanel5;
    private JSeparator jSeparator1;
    private JSeparator jSeparator2;
    private JSeparator jSeparator3;
    private JSeparator jSeparator4;
    private JSeparator jSeparator5;
    private JLabel langLabel;
    private JComboBox languageDropDown;
    private JCheckBox logAllStatements;
    private JComboBox logLevel;
    private JLabel logLevelLabel;
    private JCheckBox onlyActiveTab;
    private JCheckBox scrollTabs;
    private JTextField settingsfilename;
    private JCheckBox showFinishAlert;
    private JCheckBox showResultTabClose;
    private JCheckBox showTabCloseButton;
    private JCheckBox showTabIndex;
    private JCheckBox singlePageHelp;
    private JCheckBox tabLRUclose;
    private JCheckBox useEncryption;
    // End of variables declaration//GEN-END:variables

}
