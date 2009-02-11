/*
 * GeneralOptionsPanel.java
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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collection;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import workbench.gui.components.WbCheckBoxLabel;
import workbench.gui.components.WbFilePicker;
import workbench.gui.components.WbLabelField;
import workbench.interfaces.Restoreable;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.PlatformHelper;
import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.WbLocale;

/**
 *
 * @author  support@sql-workbench.net
 */
public class GeneralOptionsPanel
	extends JPanel
	implements Restoreable
{
	public GeneralOptionsPanel()
	{
		super();
		initComponents();
		pdfReaderPath.setAllowMultiple(false);
		String[] updTypes = new String[] {
			ResourceMgr.getString("LblUpdCheckNever"),
			ResourceMgr.getString("LblUpdCheckDaily"),
			ResourceMgr.getString("LblUpdCheck7"),
			ResourceMgr.getString("LblUpdCheck14"),
			ResourceMgr.getString("LblUpdCheck30")
		};
		checkInterval.setModel(new DefaultComboBoxModel(updTypes));
	}

	public void restoreSettings()
	{
		String reader = Settings.getInstance().getPDFReaderPath();
		if (StringUtil.isBlank(reader))
		{
			reader = PlatformHelper.getDefaultPDFReader();
		}
		pdfReaderPath.setFilename(reader);
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
		int tabPolicy = Settings.getInstance().getIntProperty("workbench.gui.mainwindow.tabpolicy", JTabbedPane.WRAP_TAB_LAYOUT);
		scrollTabs.setSelected(tabPolicy == JTabbedPane.SCROLL_TAB_LAYOUT);
		confirmTabClose.setSelected(GuiSettings.getConfirmTabClose());
	}

	public void saveSettings()
	{
		Settings set = Settings.getInstance();

		// General settings
		GuiSettings.setShowTabIndex(showTabIndex.isSelected());
		GuiSettings.setConfirmTabClose(confirmTabClose.isSelected());
		set.setUseEncryption(this.useEncryption.isSelected());
		GuiSettings.setUseAnimatedIcon(this.enableAnimatedIcon.isSelected());
		set.setQuoteChar(this.quoteCharField.getText().trim());
		set.setConsolidateLogMsg(this.consolidateLog.isSelected());
		set.setDefaultTextDelimiter(this.textDelimiterField.getText());
		set.setPDFReaderPath(pdfReaderPath.getFilename());
		set.setExitOnFirstConnectCancel(exitOnConnectCancel.isSelected());
		set.setShowConnectDialogOnStartup(autoConnect.isSelected());
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
			set.setProperty("workbench.gui.mainwindow.tabpolicy", JTabbedPane.SCROLL_TAB_LAYOUT);
		}
		else
		{
			set.setProperty("workbench.gui.mainwindow.tabpolicy", JTabbedPane.WRAP_TAB_LAYOUT);
		}
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

    textDelimiterLabel = new JLabel();
    textDelimiterField = new JTextField();
    quoteCharLabel = new JLabel();
    quoteCharField = new JTextField();
    pdfReaderPathLabel = new JLabel();
    pdfReaderPath = new WbFilePicker();
    logLevelLabel = new JLabel();
    logLevel = new JComboBox();
    checkUpdatesLabel = new WbCheckBoxLabel();
    checkInterval = new JComboBox();
    langLabel = new JLabel();
    languageDropDown = new JComboBox();
    jPanel2 = new JPanel();
    useEncryption = new JCheckBox();
    consolidateLog = new JCheckBox();
    exitOnConnectCancel = new JCheckBox();
    autoConnect = new JCheckBox();
    singlePageHelp = new JCheckBox();
    settingsfilename = new WbLabelField();
    jPanel1 = new JPanel();
    showTabIndex = new JCheckBox();
    scrollTabs = new JCheckBox();
    enableAnimatedIcon = new JCheckBox();
    confirmTabClose = new JCheckBox();

    setLayout(new GridBagLayout());

    textDelimiterLabel.setText(ResourceMgr.getString("LblFieldDelimiter")); // NOI18N
    textDelimiterLabel.setToolTipText(ResourceMgr.getString("d_LblFieldDelimiter")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 12, 0, 0);
    add(textDelimiterLabel, gridBagConstraints);

    textDelimiterField.setColumns(6);
    textDelimiterField.setHorizontalAlignment(JTextField.LEFT);
    textDelimiterField.setText(Settings.getInstance().getDefaultTextDelimiter(true));
    textDelimiterField.setMinimumSize(new Dimension(40, 20));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 10, 0, 0);
    add(textDelimiterField, gridBagConstraints);

    quoteCharLabel.setText(ResourceMgr.getString("LblQuoteChar")); // NOI18N
    quoteCharLabel.setToolTipText(ResourceMgr.getString("d_LblQuoteChar")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 5, 0, 0);
    add(quoteCharLabel, gridBagConstraints);

    quoteCharField.setColumns(6);
    quoteCharField.setHorizontalAlignment(JTextField.LEFT);
    quoteCharField.setText(Settings.getInstance().getQuoteChar());
    quoteCharField.setMinimumSize(new Dimension(40, 20));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 7, 0, 15);
    add(quoteCharField, gridBagConstraints);

    pdfReaderPathLabel.setText(ResourceMgr.getString("LblReaderPath")); // NOI18N
    pdfReaderPathLabel.setToolTipText(ResourceMgr.getString("d_LblReaderPath")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(6, 12, 0, 0);
    add(pdfReaderPathLabel, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new Insets(5, 10, 0, 15);
    add(pdfReaderPath, gridBagConstraints);

    logLevelLabel.setText(ResourceMgr.getString("LblLogLevel")); // NOI18N
    logLevelLabel.setToolTipText(ResourceMgr.getString("d_LblLogLevel")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(6, 12, 0, 0);
    add(logLevelLabel, gridBagConstraints);

    logLevel.setModel(new DefaultComboBoxModel(new String[] { "ERROR", "WARNING", "INFO", "DEBUG" }));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 10, 0, 0);
    add(logLevel, gridBagConstraints);

    checkUpdatesLabel.setText(ResourceMgr.getString("LblCheckForUpdate")); // NOI18N
    checkUpdatesLabel.setToolTipText(ResourceMgr.getString("d_LblCheckForUpdate")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(8, 7, 0, 0);
    add(checkUpdatesLabel, gridBagConstraints);

    checkInterval.setModel(new DefaultComboBoxModel(new String[] { "never", "daily", "7 days", "14 days", "30 days" }));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(9, 7, 0, 0);
    add(checkInterval, gridBagConstraints);

    langLabel.setText(ResourceMgr.getString("LblLanguage")); // NOI18N
    langLabel.setToolTipText(ResourceMgr.getString("d_LblLanguage")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 12, 0, 0);
    add(langLabel, gridBagConstraints);

    languageDropDown.setModel(new DefaultComboBoxModel(new String[] { "English", "German" }));
    languageDropDown.setToolTipText(ResourceMgr.getDescription("LblLanguage"));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(7, 10, 0, 15);
    add(languageDropDown, gridBagConstraints);

    jPanel2.setLayout(new GridBagLayout());

    useEncryption.setSelected(Settings.getInstance().getUseEncryption());
    useEncryption.setText(ResourceMgr.getString("LblUseEncryption")); // NOI18N
    useEncryption.setToolTipText(ResourceMgr.getString("d_LblUseEncryption")); // NOI18N
    useEncryption.setBorder(null);
    useEncryption.setHorizontalAlignment(SwingConstants.LEFT);
    useEncryption.setHorizontalTextPosition(SwingConstants.RIGHT);
    useEncryption.setIconTextGap(5);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(6, 0, 1, 0);
    jPanel2.add(useEncryption, gridBagConstraints);

    consolidateLog.setSelected(Settings.getInstance().getConsolidateLogMsg());
    consolidateLog.setText(ResourceMgr.getString("LblConsolidateLog")); // NOI18N
    consolidateLog.setToolTipText(ResourceMgr.getString("d_LblConsolidateLog")); // NOI18N
    consolidateLog.setBorder(null);
    consolidateLog.setHorizontalAlignment(SwingConstants.LEFT);
    consolidateLog.setHorizontalTextPosition(SwingConstants.RIGHT);
    consolidateLog.setIconTextGap(5);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(6, 0, 1, 0);
    jPanel2.add(consolidateLog, gridBagConstraints);

    exitOnConnectCancel.setSelected(Settings.getInstance().getExitOnFirstConnectCancel());
    exitOnConnectCancel.setText(ResourceMgr.getString("LblExitOnConnectCancel")); // NOI18N
    exitOnConnectCancel.setToolTipText(ResourceMgr.getString("d_LblExitOnConnectCancel")); // NOI18N
    exitOnConnectCancel.setBorder(null);
    exitOnConnectCancel.setHorizontalAlignment(SwingConstants.LEFT);
    exitOnConnectCancel.setHorizontalTextPosition(SwingConstants.RIGHT);
    exitOnConnectCancel.setIconTextGap(5);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(0, 16, 1, 0);
    jPanel2.add(exitOnConnectCancel, gridBagConstraints);

    autoConnect.setSelected(Settings.getInstance().getShowConnectDialogOnStartup());
    autoConnect.setText(ResourceMgr.getString("LblShowConnect")); // NOI18N
    autoConnect.setToolTipText(ResourceMgr.getString("d_LblShowConnect")); // NOI18N
    autoConnect.setBorder(null);
    autoConnect.setHorizontalAlignment(SwingConstants.LEFT);
    autoConnect.setHorizontalTextPosition(SwingConstants.RIGHT);
    autoConnect.setIconTextGap(5);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(0, 0, 1, 0);
    jPanel2.add(autoConnect, gridBagConstraints);

    singlePageHelp.setText(ResourceMgr.getString("LblHelpSingle")); // NOI18N
    singlePageHelp.setToolTipText(ResourceMgr.getString("d_LblHelpSingle")); // NOI18N
    singlePageHelp.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(6, 16, 1, 0);
    jPanel2.add(singlePageHelp, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(13, 12, 0, 15);
    add(jPanel2, gridBagConstraints);

    settingsfilename.setText("jTextField1");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 11;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.SOUTHWEST;
    gridBagConstraints.insets = new Insets(5, 12, 2, 15);
    add(settingsfilename, gridBagConstraints);

    jPanel1.setBorder(BorderFactory.createTitledBorder("Tabs"));
    jPanel1.setLayout(new GridBagLayout());

    showTabIndex.setSelected(GuiSettings.getShowTabIndex());
    showTabIndex.setText(ResourceMgr.getString("LblShowTabIndex")); // NOI18N
    showTabIndex.setToolTipText(ResourceMgr.getString("d_LblShowTabIndex")); // NOI18N
    showTabIndex.setBorder(null);
    showTabIndex.setHorizontalAlignment(SwingConstants.LEFT);
    showTabIndex.setHorizontalTextPosition(SwingConstants.RIGHT);
    showTabIndex.setIconTextGap(5);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(2, 4, 1, 0);
    jPanel1.add(showTabIndex, gridBagConstraints);

    scrollTabs.setText(ResourceMgr.getString("LblScrolTabs")); // NOI18N
    scrollTabs.setToolTipText(ResourceMgr.getString("d_LblScrolTabs")); // NOI18N
    scrollTabs.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(2, 16, 1, 0);
    jPanel1.add(scrollTabs, gridBagConstraints);

    enableAnimatedIcon.setSelected(GuiSettings.getUseAnimatedIcon());
    enableAnimatedIcon.setText(ResourceMgr.getString("LblEnableAnimatedIcon")); // NOI18N
    enableAnimatedIcon.setToolTipText(ResourceMgr.getString("d_LblEnableAnimatedIcon")); // NOI18N
    enableAnimatedIcon.setBorder(null);
    enableAnimatedIcon.setHorizontalAlignment(SwingConstants.LEFT);
    enableAnimatedIcon.setHorizontalTextPosition(SwingConstants.RIGHT);
    enableAnimatedIcon.setIconTextGap(5);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(6, 16, 5, 0);
    jPanel1.add(enableAnimatedIcon, gridBagConstraints);

    confirmTabClose.setText(ResourceMgr.getString("LblConfirmTabClose")); // NOI18N
    confirmTabClose.setToolTipText(ResourceMgr.getString("d_LblConfirmTabClose")); // NOI18N
    confirmTabClose.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(6, 4, 5, 0);
    jPanel1.add(confirmTabClose, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 10;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(9, 7, 0, 10);
    add(jPanel1, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private JCheckBox autoConnect;
  private JComboBox checkInterval;
  private JLabel checkUpdatesLabel;
  private JCheckBox confirmTabClose;
  private JCheckBox consolidateLog;
  private JCheckBox enableAnimatedIcon;
  private JCheckBox exitOnConnectCancel;
  private JPanel jPanel1;
  private JPanel jPanel2;
  private JLabel langLabel;
  private JComboBox languageDropDown;
  private JComboBox logLevel;
  private JLabel logLevelLabel;
  private WbFilePicker pdfReaderPath;
  private JLabel pdfReaderPathLabel;
  private JTextField quoteCharField;
  private JLabel quoteCharLabel;
  private JCheckBox scrollTabs;
  private JTextField settingsfilename;
  private JCheckBox showTabIndex;
  private JCheckBox singlePageHelp;
  private JTextField textDelimiterField;
  private JLabel textDelimiterLabel;
  private JCheckBox useEncryption;
  // End of variables declaration//GEN-END:variables

}
