/*
 * DataPumper.java
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
package workbench.gui.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import workbench.AppArguments;
import workbench.WbManager;
import workbench.interfaces.StatusBar;
import workbench.interfaces.ToolWindow;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.DbSettings;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.datacopy.DataCopier;
import workbench.db.datacopy.DropType;
import workbench.db.importer.DataImporter;
import workbench.db.importer.DeleteType;
import workbench.db.importer.ImportOptions;
import workbench.db.importer.ProducerFactory;
import workbench.db.importer.TableStatements;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.AutoCompletionAction;
import workbench.gui.components.DividerBorder;
import workbench.gui.components.EditWindow;
import workbench.gui.components.FlatButton;
import workbench.gui.components.RunningJobIndicator;
import workbench.gui.components.SimpleStatusBar;
import workbench.gui.components.WbButton;
import workbench.gui.components.WbSplitPane;
import workbench.gui.dialogs.dataimport.ImportFileDialog;
import workbench.gui.help.HelpManager;
import workbench.gui.profiles.ProfileSelectionDialog;
import workbench.gui.sql.EditorPanel;

import workbench.storage.RowActionMonitor;

import workbench.sql.wbcommands.CommandTester;
import workbench.sql.wbcommands.CommonArgs;
import workbench.sql.wbcommands.WbCopy;
import workbench.sql.wbcommands.WbImport;

import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 * A GUI frontend to the {@link workbench.db.datacopy.DataCopier}
 * and {@link workbench.db.importer.DataImporter} tools.
 *
 * @author Thomas Kellerer
 */
public class DataPumper
	extends JPanel
	implements ActionListener, WindowListener, PropertyChangeListener,
						 RowActionMonitor, ToolWindow
{
	private static int instanceCount;
	private int windowId;

	private File sourceFile;
	private ProducerFactory fileImporter;
	private DataCopier copier;

	private ConnectionProfile sourceProfile;
	private ConnectionProfile targetProfile;
	protected WbConnection sourceConnection;
	protected WbConnection targetConnection;

	protected AutoCompletionAction completionAction;

	private JFrame window;

	private ColumnMapper columnMapper;
	private final String copyMsg = ResourceMgr.getString("MsgCopyingRow");
	protected boolean copyRunning;
	private EditorPanel sqlEditor;
	private boolean supportsBatch;

	// used in the Jemmy Unit Test to wait for the connection thread
	boolean isConnecting = false;

	public DataPumper()
	{
		this(null, null);
	}

	public DataPumper(ConnectionProfile source, ConnectionProfile target)
	{
		super();
		this.windowId = ++instanceCount;

		this.sourceProfile = source;
		this.targetProfile = target;

		initComponents();
		WbSwingUtilities.makeEqualSize(cancelButton, closeButton, startButton, showLogButton, showWbCommand, helpButton);

		sourceTable.setAutoSyncVisible(false);

		commitEvery.setMinimumSize(commitEvery.getPreferredSize());
		batchSize.setMinimumSize(batchSize.getPreferredSize());
		this.selectSourceButton.addActionListener(this);
		this.selectTargetButton.addActionListener(this);
		this.openFileButton.addActionListener(this);
		this.closeButton.addActionListener(this);
		this.updateDisplay();
		this.startButton.addActionListener(this);
		this.cancelButton.addActionListener(this);
		this.showLogButton.addActionListener(this);
		this.helpButton.addActionListener(this);
		this.columnMapper = new ColumnMapper();
		this.mapperPanel.setLayout(new BorderLayout());
		this.mapperPanel.add(this.columnMapper, BorderLayout.CENTER);

		this.updateOptionPanel.setBorder(new DividerBorder(DividerBorder.LEFT));
		this.checkQueryButton.addActionListener(this);
		this.showWbCommand.addActionListener(this);
		this.useQueryCbx.addActionListener(this);
		this.sqlEditor = EditorPanel.createSqlEditor();
		this.sqlEditor.showFormatSql();
		this.completionAction = new AutoCompletionAction(this.sqlEditor, (StatusBar)this.statusLabel);
		this.wherePanel.add(this.sqlEditor);
		this.showWbCommand.setEnabled(false);
		this.batchSize.setEnabled(false);

		this.sourceTable.setTableDropDownName("sourceTable");
		this.targetTable.setTableDropDownName("targetTable");
	}

	@Override
	public JFrame getWindow()
	{
		return window;
	}

	public void saveSettings()
	{
		Settings s = Settings.getInstance();
		if (this.sourceProfile != null)
		{
			s.setLastConnection("workbench.datapumper.source.lastprofile", this.sourceProfile);
		}
		if (this.targetProfile != null)
		{
			s.setLastConnection("workbench.datapumper.target.lastprofile", this.targetProfile);
		}
		s.setProperty("workbench.datapumper.divider", jSplitPane1.getDividerLocation());
		s.setProperty("workbench.datapumper.target.deletetable", Boolean.toString(this.deleteTargetCbx.isSelected()));
		s.setProperty("workbench.datapumper.continue", Boolean.toString(this.continueOnErrorCbx.isSelected()));
		s.setProperty("workbench.datapumper.commitevery", this.commitEvery.getText());
		s.setProperty("workbench.datapumper.usequery", Boolean.toString(this.useQueryCbx.isSelected()));
		s.setProperty("workbench.datapumper.droptable", Boolean.toString(this.dropTargetCbx.isSelected()));
		s.setProperty("workbench.datapumper.updatemode", (String)this.modeComboBox.getSelectedItem());
		s.setProperty("workbench.datapumper.alwayssynctables", targetTable.isAutoSyncSelected());

		String where = this.sqlEditor.getText();
		if (where != null && where.length() > 0)
		{
			s.setProperty("workbench.datapumper.where", where);
		}
		else
		{
			s.setProperty("workbench.datapumper.where", "");
		}
		s.storeWindowSize(this.window, "workbench.datapumper.window");
		s.storeWindowPosition(this.window, "workbench.datapumper.window");
		s.setProperty("workbench.datapumper.batchsize", getBatchSize());
	}

	@Override
	public void activate()
	{
		this.window.setVisible(true);
		this.window.toFront();
	}

	@Override
	public WbConnection getConnection()
	{
		return null;
	}

	public void restoreSettings()
	{
		Settings s = Settings.getInstance();
		boolean delete = s.getBoolProperty("workbench.datapumper.target.deletetable", false);
		boolean cont = s.getBoolProperty("workbench.datapumper.continue", false);
		boolean drop = s.getBoolProperty("workbench.datapumper.droptable", false);
		this.deleteTargetCbx.setSelected(delete);
		this.continueOnErrorCbx.setSelected(cont);
		this.dropTargetCbx.setSelected(drop);
		if (!s.restoreWindowSize(this.window, "workbench.datapumper.window"))
		{
			this.window.setSize(800,600);
		}

		int commit = s.getIntProperty("workbench.datapumper.commitevery", 0);
		if (commit > 0)
		{
			this.commitEvery.setText(Integer.toString(commit));
		}
		String where = s.getProperty("workbench.datapumper.where", null);
		if (where != null && where.length() > 0)
		{
			this.sqlEditor.setText(where);
		}
		int loc = s.getIntProperty("workbench.datapumper.divider", -1);
		if (loc == -1)
		{
			loc = this.jSplitPane1.getHeight() / 2;
			if (loc < 10) loc = 100;
		}
		this.jSplitPane1.setDividerLocation(loc);
		boolean useQuery = s.getBoolProperty("workbench.datapumper.usequery", false);
		this.useQueryCbx.setSelected(useQuery);

		String mode = s.getProperty("workbench.datapumper.updatemode", "insert");
		this.modeComboBox.setSelectedItem(mode);

		// initialize the depending controls for the usage of a SQL query
		this.checkType();
		int size = s.getIntProperty("workbench.datapumper.batchsize", -1);
		if (size > 0)
		{
			this.batchSize.setText(Integer.toString(size));
		}
		s.getBoolProperty("workbench.datapumper.alwayssynctables", true);
		targetTable.setAutoSyncSelected(Settings.getInstance().getBoolProperty("workbench.datapumper.alwayssynctables", true));
	}

	private void selectInputFile()
	{
		ImportFileDialog dialog = new ImportFileDialog(this);
		dialog.setLastDirConfigKey("workbench.datapumper.lastdir");
		boolean ok = dialog.selectInput(ResourceMgr.getString("TxtWindowTitleSelectImportFile"), "datapumper");

		if (!ok) return;
		if (this.sourceProfile != null)
		{
			this.disconnectSource();
		}
		this.sourceFile = dialog.getSelectedFile();
		this.sourceTable.reset();
		this.sourceTable.setEnabled(false);

		this.useQueryCbx.setSelected(false);
		this.useQueryCbx.setEnabled(false);
		this.sqlEditor.setEnabled(false);
		this.useQueryCbx.setVisible(false);
		this.useQueryCbx.setVisible(false);
		this.sqlEditor.setVisible(false);

		this.fileImporter = new ProducerFactory(this.sourceFile);
		this.fileImporter.setTextOptions(dialog.getTextOptions());
		this.fileImporter.setGeneralOptions(dialog.getGeneralOptions());
		this.fileImporter.setType(dialog.getImportType());
		this.checkType();
		modeComboBox.setSelectedItem(dialog.getGeneralOptions().getMode());

		this.updateSourceDisplay();
		if (this.targetProfile != null)
		{
			initColumnMapper();
		}
	}

	private void updateTargetDisplay()
	{
		String label = ResourceMgr.getString("LblDPTargetProfile");
		if (this.targetProfile != null)
		{
			this.targetProfileLabel.setText(label + ": " + this.targetProfile.getName());
		}
		else
		{
			this.targetProfileLabel.setText(label + ": " + ResourceMgr.getString("LblPleaseSelect"));
		}
		this.updateWindowTitle();
	}

	private void updateSourceDisplay()
	{
		String label = ResourceMgr.getString("LblDPSourceProfile");
		if (this.sourceProfile != null)
		{
			this.sourceProfileLabel.setText(label + ": " + this.sourceProfile.getName());
		}
		else if (this.sourceFile != null)
		{
			this.sourceProfileLabel.setText(ResourceMgr.getString("LblDPSourceFile") + ": " + sourceFile.getAbsolutePath());
		}
		else
		{
			this.sourceProfileLabel.setText(label + ": " + ResourceMgr.getString("LblPleaseSelect"));
		}
		this.updateWindowTitle();
	}

	private void updateDisplay()
	{
		this.updateSourceDisplay();
		this.updateTargetDisplay();
		this.updateWindowTitle();
	}

	protected void updateWindowTitle()
	{
		if (this.targetProfile != null && (this.sourceProfile != null || this.sourceFile != null) && this.window != null)
		{
			String title = ResourceMgr.getString("TxtWindowTitleDataPumper");
			String sourceName = "";
			if (this.sourceProfile != null)
			{
				sourceName = this.sourceProfile.getName();
			}
			else if (this.sourceFile != null)
			{
				sourceName = this.sourceFile.getName();
			}
			title = title + " [" + sourceName + " -> " + this.targetProfile.getName() + "]";
			if (this.copier != null && this.copyRunning)
			{
				title = RunningJobIndicator.TITLE_PREFIX + title;
			}
			this.window.setTitle(title);
		}
	}

	protected void checkConnections()
	{
		this.connectSource(this.sourceProfile);
		this.connectTarget(this.targetProfile);
	}

	private void connectSource(final ConnectionProfile profile)
	{
		if (profile == null) return;

		Thread t = new WbThread("DataPumper source connection")
		{
			@Override
			public void run()
			{
				doConnectSource(profile);
			}
		};
		t.start();
	}

	private void doConnectSource(ConnectionProfile profile)
	{
		this.disconnectSource();

		this.sourceProfile = profile;
		this.isConnecting = true;

		String label = ResourceMgr.getString("MsgConnectingTo") + " " + this.sourceProfile.getName() + " ...";
		this.sourceProfileLabel.setIcon(ResourceMgr.getGifIcon("wait"));
		this.sourceProfileLabel.setText(label);

		try
		{
			this.sourceConnection = ConnectionMgr.getInstance().getConnection(this.sourceProfile, "Dp-Source" + this.windowId);
			this.sourceConnection.getMetadata().disableOutput();
		}
		catch (Exception e)
		{
			LogMgr.logError("DataPumper.doConnectSource()", "Error when connecting to profile: " + (sourceProfile == null ? "n/a" : this.sourceProfile.getName()), e);
			this.sourceProfile = null;
			WbSwingUtilities.showFriendlyErrorMessage(this, ResourceMgr.getString("ErrConnectFailed"), ExceptionUtil.getDisplay(e));
		}
		finally
		{
			this.sourceProfileLabel.setIcon(null);
			this.updateSourceDisplay();
		}

		this.sourceFile = null;
		this.fileImporter = null;
		this.checkType();

		if (this.useQueryCbx.isSelected())
		{
			initColumnMapper();
		}

		if (this.sourceConnection != null)
		{
			this.sourceTable.setChangeListener(this, "source-table");

			Thread t = new WbThread("Retrieve source tables")
			{
				@Override
				public void run()
				{
					try
					{
						LogMgr.logDebug("DataPumper.connectSource()", "Source connection established retrieving tables.");
						sourceTable.setConnection(sourceConnection);
						completionAction.setConnection(sourceConnection);
					}
					finally
					{
						isConnecting = false;
					}
				}
			};
			t.start();
		}
		else
		{
			isConnecting = false;
		}
	}

	private void connectTarget(final ConnectionProfile profile)
	{
		if (profile == null) return;

		Thread t = new WbThread("DataPumper target connection")
		{
			@Override
			public void run()
			{
				doConnectTarget(profile);
			}
		};
		t.start();
	}

	private void doConnectTarget(ConnectionProfile profile)
	{
		this.isConnecting = true;

		this.disconnectTarget();
		this.targetProfile = profile;

		String label = ResourceMgr.getString("MsgConnectingTo") + " " + this.targetProfile.getName() + " ...";
		this.targetProfileLabel.setText(label);
		this.targetProfileLabel.setIcon(ResourceMgr.getGifIcon("wait"));

		try
		{
			this.targetConnection = ConnectionMgr.getInstance().getConnection(this.targetProfile, "Dp-Target" + windowId);
			this.targetConnection.getMetadata().disableOutput();
		}
		catch (Exception e)
		{
			LogMgr.logError("DataPumper.doConnectSource()", "Error when connecting to profile: " + this.targetProfile.getName(), e);
			this.targetProfile = null;
			WbSwingUtilities.showFriendlyErrorMessage(this, ResourceMgr.getString("ErrConnectFailed"), ExceptionUtil.getDisplay(e));
		}
		finally
		{
			this.targetProfileLabel.setIcon(null);
			this.updateTargetDisplay();
		}

		if (this.targetConnection != null)
		{
			this.targetTable.setChangeListener(this, "target-table");
			this.supportsBatch = this.targetConnection.getMetadata().supportsBatchUpdates();
			this.checkUseBatch();
			checkType();

		  Thread t = new WbThread("Retrieve target tables")
		  {
				@Override
			  public void run()
			  {
					try
					{
						LogMgr.logDebug("DataPumper.connectSource()", "Target connection established retrieving tables.");
						targetTable.setConnection(targetConnection);
					}
					finally
					{
						isConnecting = false;
					}
			  }
		  };
		  t.start();
		}
		else
		{
			isConnecting = false;
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
    GridBagConstraints gridBagConstraints;

    jPanel6 = new JPanel();
    sourceProfilePanel = new JPanel();
    sourceProfileLabel = new JLabel();
    selectSourceButton = new FlatButton();
    openFileButton = new FlatButton();
    targetProfilePanel = new JPanel();
    targetProfileLabel = new JLabel();
    selectTargetButton = new FlatButton();
    sourceTable = new TableSelectorPanel();
    targetTable = new TableSelectorPanel();
    targetHeader = new JLabel();
    sourceHeader = new JLabel();
    jSplitPane1 = new WbSplitPane();
    mapperPanel = new JPanel();
    optionsPanel = new JPanel();
    jPanel2 = new JPanel();
    sqlPanel = new JPanel();
    wherePanel = new JPanel();
    sqlEditorLabel = new JLabel();
    useQueryCbx = new JCheckBox();
    checkQueryButton = new FlatButton();
    updateOptionPanel = new JPanel();
    commitLabel = new JLabel();
    commitEvery = new JTextField();
    continueOnErrorCbx = new JCheckBox();
    deleteTargetCbx = new JCheckBox();
    ignoreIdentityCbx = new JCheckBox();
    dropTargetCbx = new JCheckBox();
    modeComboBox = new JComboBox();
    modeLabel = new JLabel();
    jPanel1 = new JPanel();
    jLabel1 = new JLabel();
    batchSizeLabel = new JLabel();
    batchSize = new JTextField();
    ignoreDropError = new JCheckBox();
    jLabel2 = new JLabel();
    jLabel3 = new JLabel();
    preTableStmt = new JTextField();
    postTableStmt = new JTextField();
    statusLabel = new SimpleStatusBar();
    buttonPanel = new JPanel();
    jPanel3 = new JPanel();
    startButton = new WbButton();
    cancelButton = new JButton();
    jPanel4 = new JPanel();
    showLogButton = new JButton();
    showWbCommand = new JButton();
    jPanel5 = new JPanel();
    helpButton = new JButton();
    closeButton = new JButton();

    setLayout(new GridBagLayout());

    sourceProfilePanel.setLayout(new GridBagLayout());

    sourceProfileLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(0, 2, 0, 0)));
    sourceProfileLabel.setMaximumSize(new Dimension(32768, 24));
    sourceProfileLabel.setMinimumSize(new Dimension(25, 24));
    sourceProfileLabel.setPreferredSize(new Dimension(50, 24));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    sourceProfilePanel.add(sourceProfileLabel, gridBagConstraints);

    selectSourceButton.setText("...");
    selectSourceButton.setMaximumSize(new Dimension(22, 22));
    selectSourceButton.setMinimumSize(new Dimension(22, 22));
    selectSourceButton.setName("selectSource"); // NOI18N
    selectSourceButton.setPreferredSize(new Dimension(22, 22));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.EAST;
    gridBagConstraints.insets = new Insets(0, 2, 0, 0);
    sourceProfilePanel.add(selectSourceButton, gridBagConstraints);

    openFileButton.setIcon(ResourceMgr.getGifIcon("Open"));
    openFileButton.setToolTipText(ResourceMgr.getString("d_DataPumperOpenFile"));
    openFileButton.setIconTextGap(0);
    openFileButton.setPreferredSize(new Dimension(22, 22));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(0, 2, 0, 0);
    sourceProfilePanel.add(openFileButton, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.SOUTHWEST;
    gridBagConstraints.weightx = 0.5;
    gridBagConstraints.insets = new Insets(0, 2, 0, 2);
    add(sourceProfilePanel, gridBagConstraints);

    targetProfilePanel.setLayout(new GridBagLayout());

    targetProfileLabel.setHorizontalAlignment(SwingConstants.LEFT);
    targetProfileLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(0, 2, 0, 0)));
    targetProfileLabel.setMaximumSize(new Dimension(32768, 24));
    targetProfileLabel.setMinimumSize(new Dimension(25, 24));
    targetProfileLabel.setPreferredSize(new Dimension(0, 24));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    targetProfilePanel.add(targetProfileLabel, gridBagConstraints);

    selectTargetButton.setText("...");
    selectTargetButton.setMaximumSize(new Dimension(22, 22));
    selectTargetButton.setMinimumSize(new Dimension(22, 22));
    selectTargetButton.setName("selectTarget"); // NOI18N
    selectTargetButton.setPreferredSize(new Dimension(22, 22));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.EAST;
    gridBagConstraints.insets = new Insets(0, 2, 0, 0);
    targetProfilePanel.add(selectTargetButton, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 0.5;
    gridBagConstraints.insets = new Insets(0, 2, 0, 2);
    add(targetProfilePanel, gridBagConstraints);

    sourceTable.setMaximumSize(new Dimension(2147483647, 65));
    sourceTable.setMinimumSize(new Dimension(25, 50));
    sourceTable.setPreferredSize(new Dimension(25, 50));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(5, 2, 6, 2);
    add(sourceTable, gridBagConstraints);

    targetTable.setToolTipText("");
    targetTable.setMaximumSize(new Dimension(2147483647, 65));
    targetTable.setMinimumSize(new Dimension(25, 50));
    targetTable.setPreferredSize(new Dimension(25, 50));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(5, 4, 6, 2);
    add(targetTable, gridBagConstraints);

    targetHeader.setBackground(new Color(255, 255, 255));
    targetHeader.setHorizontalAlignment(SwingConstants.CENTER);
    targetHeader.setText("<html><b>" + ResourceMgr.getString("LblTargetConnection") + "</b></html>");
    targetHeader.setMaximumSize(new Dimension(23768, 22));
    targetHeader.setMinimumSize(new Dimension(25, 22));
    targetHeader.setOpaque(true);
    targetHeader.setPreferredSize(new Dimension(25, 22));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 0.5;
    gridBagConstraints.insets = new Insets(4, 2, 4, 2);
    add(targetHeader, gridBagConstraints);

    sourceHeader.setBackground(new Color(255, 255, 255));
    sourceHeader.setHorizontalAlignment(SwingConstants.CENTER);
    sourceHeader.setText("<html><b>" + ResourceMgr.getString("LblSourceConnection") + "</b></html>");
    sourceHeader.setMaximumSize(new Dimension(32768, 22));
    sourceHeader.setMinimumSize(new Dimension(25, 22));
    sourceHeader.setOpaque(true);
    sourceHeader.setPreferredSize(new Dimension(50, 22));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 0.5;
    gridBagConstraints.insets = new Insets(4, 2, 4, 2);
    add(sourceHeader, gridBagConstraints);

    jSplitPane1.setDividerLocation(100);
    jSplitPane1.setOrientation(JSplitPane.VERTICAL_SPLIT);
    jSplitPane1.setTopComponent(mapperPanel);

    optionsPanel.setLayout(new GridBagLayout());

    jPanel2.setLayout(new GridBagLayout());

    sqlPanel.setLayout(new GridBagLayout());

    wherePanel.setLayout(new BorderLayout());

    sqlEditorLabel.setText(ResourceMgr.getString("LblDPAdditionalWhere")); // NOI18N
    wherePanel.add(sqlEditorLabel, BorderLayout.NORTH);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gridBagConstraints.gridheight = 7;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(5, 0, 4, 1);
    sqlPanel.add(wherePanel, gridBagConstraints);

    useQueryCbx.setText(ResourceMgr.getString("LblDPUseSQLSource")); // NOI18N
    useQueryCbx.setToolTipText(ResourceMgr.getString("d_LblDPUseSQLSource")); // NOI18N
    useQueryCbx.setHorizontalAlignment(SwingConstants.LEFT);
    useQueryCbx.setHorizontalTextPosition(SwingConstants.LEFT);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 0.2;
    gridBagConstraints.insets = new Insets(2, 2, 0, 2);
    sqlPanel.add(useQueryCbx, gridBagConstraints);

    checkQueryButton.setText(ResourceMgr.getString("LblDPCheckQuery")); // NOI18N
    checkQueryButton.setToolTipText(ResourceMgr.getString("d_LblDPCheckQuery")); // NOI18N
    checkQueryButton.setEnabled(false);
    checkQueryButton.setMargin(new Insets(2, 5, 2, 5));
    checkQueryButton.setMaximumSize(new Dimension(200, 24));
    checkQueryButton.setMinimumSize(new Dimension(120, 24));
    checkQueryButton.setPreferredSize(new Dimension(130, 24));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 0.1;
    gridBagConstraints.insets = new Insets(0, 0, 2, 0);
    sqlPanel.add(checkQueryButton, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 0.3;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(0, 0, 0, 5);
    jPanel2.add(sqlPanel, gridBagConstraints);

    updateOptionPanel.setLayout(new GridBagLayout());

    commitLabel.setHorizontalAlignment(SwingConstants.LEFT);
    commitLabel.setText(ResourceMgr.getString("LblDPCommitEvery")); // NOI18N
    commitLabel.setToolTipText(ResourceMgr.getString("d_LblDPCommitEvery")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(4, 8, 0, 0);
    updateOptionPanel.add(commitLabel, gridBagConstraints);

    commitEvery.setColumns(8);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(2, 4, 0, 0);
    updateOptionPanel.add(commitEvery, gridBagConstraints);

    continueOnErrorCbx.setText(ResourceMgr.getString("MsgDPContinueOnError")); // NOI18N
    continueOnErrorCbx.setToolTipText(ResourceMgr.getString("d_MsgDPContinueOnError")); // NOI18N
    continueOnErrorCbx.setHorizontalTextPosition(SwingConstants.RIGHT);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(0, 5, 0, 0);
    updateOptionPanel.add(continueOnErrorCbx, gridBagConstraints);

    deleteTargetCbx.setText(ResourceMgr.getString("LblDeleteTargetTable")); // NOI18N
    deleteTargetCbx.setToolTipText(ResourceMgr.getString("d_LblDeleteTargetTable")); // NOI18N
    deleteTargetCbx.setHorizontalTextPosition(SwingConstants.RIGHT);
    deleteTargetCbx.setName("deleteTargetCbx"); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(6, 5, 0, 0);
    updateOptionPanel.add(deleteTargetCbx, gridBagConstraints);

    ignoreIdentityCbx.setText(ResourceMgr.getString("LblIgnoreIdentity")); // NOI18N
    ignoreIdentityCbx.setToolTipText(ResourceMgr.getString("d_LblIgnoreIdentity")); // NOI18N
    ignoreIdentityCbx.setHorizontalTextPosition(SwingConstants.RIGHT);
    ignoreIdentityCbx.setName("deleteTargetCbx"); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(0, 5, 0, 0);
    updateOptionPanel.add(ignoreIdentityCbx, gridBagConstraints);

    dropTargetCbx.setText(ResourceMgr.getString("LblDPDropTable")); // NOI18N
    dropTargetCbx.setToolTipText(ResourceMgr.getString("d_LblDPDropTable")); // NOI18N
    dropTargetCbx.setEnabled(false);
    dropTargetCbx.setHorizontalTextPosition(SwingConstants.RIGHT);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(0, 5, 0, 0);
    updateOptionPanel.add(dropTargetCbx, gridBagConstraints);

    modeComboBox.setModel(new DefaultComboBoxModel(new String[] { "insert", "update", "insert,update", "update,insert" }));
    modeComboBox.setName("modeSelector"); // NOI18N
    modeComboBox.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent evt)
      {
        modeComboBoxActionPerformed(evt);
      }
    });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 4, 0, 0);
    updateOptionPanel.add(modeComboBox, gridBagConstraints);

    modeLabel.setText(ResourceMgr.getString("LblDPMode")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(4, 8, 0, 0);
    updateOptionPanel.add(modeLabel, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 15;
    gridBagConstraints.weighty = 1.0;
    updateOptionPanel.add(jPanel1, gridBagConstraints);

    jLabel1.setText(ResourceMgr.getString("LblDPUpdateOptions")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new Insets(0, 7, 0, 5);
    updateOptionPanel.add(jLabel1, gridBagConstraints);

    batchSizeLabel.setHorizontalAlignment(SwingConstants.LEFT);
    batchSizeLabel.setText(ResourceMgr.getString("LblBatchSize")); // NOI18N
    batchSizeLabel.setToolTipText(ResourceMgr.getString("d_LblBatchSize")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(4, 8, 0, 0);
    updateOptionPanel.add(batchSizeLabel, gridBagConstraints);

    batchSize.setColumns(8);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(2, 4, 0, 0);
    updateOptionPanel.add(batchSize, gridBagConstraints);

    ignoreDropError.setText(ResourceMgr.getString("LblIgnoreDropErrors")); // NOI18N
    ignoreDropError.setToolTipText(ResourceMgr.getString("d_LblIgnoreDropErrors")); // NOI18N
    ignoreDropError.setEnabled(false);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    updateOptionPanel.add(ignoreDropError, gridBagConstraints);

    jLabel2.setText(ResourceMgr.getString("LblBeforeTable")); // NOI18N
    jLabel2.setToolTipText(ResourceMgr.getString("d_LblBeforeTable")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(7, 8, 0, 0);
    updateOptionPanel.add(jLabel2, gridBagConstraints);

    jLabel3.setText(ResourceMgr.getString("LblAfterTable")); // NOI18N
    jLabel3.setToolTipText(ResourceMgr.getString("d_LblAfterTable")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(4, 8, 0, 0);
    updateOptionPanel.add(jLabel3, gridBagConstraints);

    preTableStmt.setToolTipText(ResourceMgr.getString("d_LblBeforeTable")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(8, 4, 0, 20);
    updateOptionPanel.add(preTableStmt, gridBagConstraints);

    postTableStmt.setToolTipText(ResourceMgr.getString("d_LblAfterTable")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(5, 4, 0, 20);
    updateOptionPanel.add(postTableStmt, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 0.5;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(5, 5, 0, 0);
    jPanel2.add(updateOptionPanel, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    optionsPanel.add(jPanel2, gridBagConstraints);

    jSplitPane1.setRightComponent(optionsPanel);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.anchor = GridBagConstraints.NORTH;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(0, 2, 0, 2);
    add(jSplitPane1, gridBagConstraints);

    statusLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(0, 2, 0, 0)));
    statusLabel.setMaximumSize(new Dimension(32768, 24));
    statusLabel.setMinimumSize(new Dimension(4, 24));
    statusLabel.setPreferredSize(new Dimension(4, 24));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.SOUTHEAST;
    gridBagConstraints.insets = new Insets(0, 2, 0, 2);
    add(statusLabel, gridBagConstraints);

    buttonPanel.setLayout(new GridBagLayout());

    jPanel3.setLayout(new GridBagLayout());

    startButton.setText(ResourceMgr.getString("LblStartDataPumper")); // NOI18N
    startButton.setEnabled(false);
    startButton.setName("startButton"); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.insets = new Insets(0, 2, 0, 0);
    jPanel3.add(startButton, gridBagConstraints);

    cancelButton.setText(ResourceMgr.getString("LblCancelCopy")); // NOI18N
    cancelButton.setEnabled(false);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.insets = new Insets(0, 5, 0, 0);
    jPanel3.add(cancelButton, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    buttonPanel.add(jPanel3, gridBagConstraints);

    jPanel4.setLayout(new GridBagLayout());

    showLogButton.setText(ResourceMgr.getString("LblShowDataPumperLog")); // NOI18N
    showLogButton.setEnabled(false);
    jPanel4.add(showLogButton, new GridBagConstraints());

    showWbCommand.setText(ResourceMgr.getString("LblShowScript")); // NOI18N
    showWbCommand.setToolTipText(ResourceMgr.getString("d_LblShowScript")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.insets = new Insets(0, 5, 0, 0);
    jPanel4.add(showWbCommand, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 0.4;
    buttonPanel.add(jPanel4, gridBagConstraints);

    jPanel5.setLayout(new GridBagLayout());

    helpButton.setText(ResourceMgr.getString("LblHelp")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.EAST;
    jPanel5.add(helpButton, gridBagConstraints);

    closeButton.setText(ResourceMgr.getString("LblClose")); // NOI18N
    closeButton.setName("closeButton"); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.EAST;
    gridBagConstraints.insets = new Insets(0, 11, 0, 2);
    jPanel5.add(closeButton, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.EAST;
    gridBagConstraints.weightx = 0.2;
    buttonPanel.add(jPanel5, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(9, 0, 8, 0);
    add(buttonPanel, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents

	private void modeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_modeComboBoxActionPerformed
		String mode = (String)modeComboBox.getSelectedItem();
		if (mode == null) return;
		int modevalue = DataImporter.getModeValue(mode);
		if (DataImporter.isDeleteTableAllowed(modevalue))
		{
			this.deleteTargetCbx.setEnabled(true);
		}
		else
		{
			this.deleteTargetCbx.setSelected(false);
			this.deleteTargetCbx.setEnabled(false);
		}
		checkUseBatch();
	}//GEN-LAST:event_modeComboBoxActionPerformed


  // Variables declaration - do not modify//GEN-BEGIN:variables
  protected JTextField batchSize;
  protected JLabel batchSizeLabel;
  protected JPanel buttonPanel;
  protected JButton cancelButton;
  protected JButton checkQueryButton;
  protected JButton closeButton;
  protected JTextField commitEvery;
  protected JLabel commitLabel;
  protected JCheckBox continueOnErrorCbx;
  protected JCheckBox deleteTargetCbx;
  protected JCheckBox dropTargetCbx;
  protected JButton helpButton;
  protected JCheckBox ignoreDropError;
  protected JCheckBox ignoreIdentityCbx;
  protected JLabel jLabel1;
  protected JLabel jLabel2;
  protected JLabel jLabel3;
  protected JPanel jPanel1;
  protected JPanel jPanel2;
  protected JPanel jPanel3;
  protected JPanel jPanel4;
  protected JPanel jPanel5;
  protected JPanel jPanel6;
  protected JSplitPane jSplitPane1;
  protected JPanel mapperPanel;
  protected JComboBox modeComboBox;
  protected JLabel modeLabel;
  protected JButton openFileButton;
  protected JPanel optionsPanel;
  protected JTextField postTableStmt;
  protected JTextField preTableStmt;
  protected JButton selectSourceButton;
  protected JButton selectTargetButton;
  protected JButton showLogButton;
  protected JButton showWbCommand;
  protected JLabel sourceHeader;
  protected JLabel sourceProfileLabel;
  protected JPanel sourceProfilePanel;
  protected TableSelectorPanel sourceTable;
  protected JLabel sqlEditorLabel;
  protected JPanel sqlPanel;
  protected JButton startButton;
  protected JLabel statusLabel;
  protected JLabel targetHeader;
  protected JLabel targetProfileLabel;
  protected JPanel targetProfilePanel;
  protected TableSelectorPanel targetTable;
  protected JPanel updateOptionPanel;
  protected JCheckBox useQueryCbx;
  protected JPanel wherePanel;
  // End of variables declaration//GEN-END:variables

	public void showWindow()
	{
		showWindow(null);
	}

	public void showWindow(MainWindow aParent)
	{
		this.window  = new JFrame(ResourceMgr.getString("TxtWindowTitleDataPumper"))
		{
			@Override
			public void setVisible(boolean visible)
			{
				if (!visible) saveSettings();
				super.setVisible(visible);
			}
		};

		this.window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		ResourceMgr.setWindowIcons(window, "datapumper");

		this.window.getContentPane().add(this);
		this.restoreSettings();
		this.window.addWindowListener(this);
		WbManager.getInstance().registerToolWindow(this);

		if (aParent == null)
		{
			if (!Settings.getInstance().restoreWindowPosition(this.window, "workbench.datapumper.window"))
			{
				WbSwingUtilities.center(this.window, null);
			}
		}
		else
		{
			WbSwingUtilities.center(this.window, aParent);
		}

		this.window .setVisible(true);
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				checkConnections();
			}
		});
	}

	private void disconnectTarget()
	{
		if (this.targetConnection == null) return;

		try
		{
			String label = ResourceMgr.getString("MsgDisconnecting");
			this.targetProfileLabel.setText(label);
			this.targetProfileLabel.setIcon(ResourceMgr.getGifIcon("wait"));

			this.targetTable.removeChangeListener();
			this.targetConnection.disconnect();
			this.targetTable.setConnection(null);
		}
		catch (Exception e)
		{
			LogMgr.logError("DataPumper.disconnectTarget()", "Error disconnecting target connection", e);
		}
		finally
		{
			this.targetConnection = null;
			this.targetProfile = null;
			this.updateTargetDisplay();
			this.targetProfileLabel.setIcon(null);
		}
	}

	private void disconnectSource()
	{
		if (this.sourceConnection == null) return;

		try
		{
			String label = ResourceMgr.getString("MsgDisconnecting");
			this.sourceProfileLabel.setText(label);
			this.sourceProfileLabel.setIcon(ResourceMgr.getGifIcon("wait"));

			this.sourceTable.removeChangeListener();
			this.sourceConnection.disconnect();
			this.sourceTable.setConnection(null);
			this.completionAction.setConnection(null);
		}
		catch (Exception e)
		{
			LogMgr.logError("DataPumper.disconnectSource()", "Error disconnecting source connection", e);
		}
		finally
		{
			this.sourceConnection = null;
			this.sourceProfile = null;
			this.updateSourceDisplay();
			this.sourceProfileLabel.setIcon(null);
		}
	}

	private void selectTargetConnection()
	{
		ConnectionProfile profile = this.selectConnection("workbench.datapumper.target.lastprofile");
		this.connectTarget(profile);
	}

	private void selectSourceConnection()
	{
		ConnectionProfile profile = this.selectConnection("workbench.datapumper.source.lastprofile");
		this.connectSource(profile);
	}

	private ConnectionProfile selectConnection(String lastProfileKey)
	{
		ConnectionProfile prof = null;
		try
		{
			WbSwingUtilities.showWaitCursor(this.window);
			ProfileSelectionDialog dialog = new ProfileSelectionDialog(this.window, true, lastProfileKey);
			WbSwingUtilities.center(dialog, this.window);
			WbSwingUtilities.showDefaultCursor(this.window);
			dialog.setVisible(true);
			prof = dialog.getSelectedProfile();
			boolean cancelled = dialog.isCancelled();
			if (!cancelled)
			{
				prof = dialog.getSelectedProfile();
				if (prof != null)
				{
					Settings.getInstance().setProperty(lastProfileKey, prof.getName());
				}
				else
				{
					LogMgr.logError("DataPumper.selectConnection()", "NULL Profile selected!", null);
				}
			}
			dialog.setVisible(false);
			dialog.dispose();
		}
		catch (Throwable th)
		{
			LogMgr.logError("DataPumper.selectConnection()", "Error during connect", th);
			prof = null;
		}
		return prof;
	}

	private void checkUseBatch()
	{
		if (this.supportsBatch)
		{
			String mode = (String)this.modeComboBox.getSelectedItem();
			if ("insert".equals(mode) || "update".equals(mode))
			{
				this.batchSize.setEnabled(this.supportsBatch);
				return;
			}
		}
		this.batchSize.setEnabled(false);
		this.batchSize.setText("");
	}

	private void showHelp()
	{
		HelpManager.showDataPumperHelp();
	}

	@Override
	public void actionPerformed(java.awt.event.ActionEvent e)
	{
		if (e.getSource() == this.closeButton)
		{
			this.closeWindow();
		}
		else if (e.getSource() == this.helpButton)
		{
			this.showHelp();
		}
		else if (e.getSource() == this.cancelButton)
		{
			this.cancelCopy();
		}
		else if (e.getSource() == this.selectTargetButton)
		{
			this.selectTargetConnection();
		}
		else if (e.getSource() == this.openFileButton)
		{
			this.selectInputFile();
		}
		else if (e.getSource() == this.selectSourceButton)
		{
			this.selectSourceConnection();
		}
		else if (e.getSource() == this.showWbCommand)
		{
			this.showCopyCommand();
		}
		else if (e.getSource() == this.startButton)
		{
			if (this.copyRunning)
			{
				this.cancelCopy();
			}
			else if (this.columnMapper != null)
			{
				this.startCopy();
			}
		}
		else if (e.getSource() == this.useQueryCbx)
		{
			this.resetColumnMapper();
			this.checkType();
		}
		else if (e.getSource() == this.checkQueryButton)
		{
			this.initColumnMapper();
		}
		else if (e.getSource() == this.showLogButton)
		{
			this.showLog();
		}
	}

	/**
	 *	Check the controls depending on the state of the useQuery CheckBox
	 */
	private void checkType()
	{
		boolean useQuery = this.useQueryCbx.isSelected();
		boolean allowSource = (!useQuery && this.fileImporter == null);

		this.sourceTable.setEnabled(allowSource);

		TableIdentifier target = this.targetTable.getSelectedTable();
		boolean isCopy = (this.fileImporter == null);
		this.sqlEditor.setEnabled(isCopy);
		this.checkQueryButton.setEnabled(isCopy && useQuery && target != null);
		this.targetTable.allowNewTable(isCopy);

		if (useQuery)
		{
			if (target == null)
			{
				statusLabel.setText(ResourceMgr.getString("MsgTargetRequired"));
			}
			else
			{
				this.statusLabel.setText("");
			}
		}
		else
		{
			this.statusLabel.setText("");
		}
		this.useQueryCbx.setVisible(isCopy);
		this.useQueryCbx.setEnabled(isCopy);
		this.sqlEditor.setVisible(isCopy);
		this.checkQueryButton.setVisible(isCopy);
		this.sqlEditorLabel.setVisible(isCopy);


		if (useQuery)
		{
			this.sqlEditorLabel.setText(ResourceMgr.getString("LblDPQueryText"));
		}
		else
		{
			this.sqlEditorLabel.setText(ResourceMgr.getString("LblDPAdditionalWhere"));
		}
		if (!useQuery)
		{
			if (this.isSelectQuery())
			{
				String msg = ResourceMgr.getString("MsgDPRemoveQuery");
				if (WbSwingUtilities.getYesNo(this, msg))
				{
					this.sqlEditor.setText("");
				}
			}
		}
	}

	private boolean isSelectQuery()
	{
		String verb = SqlUtil.getSqlVerb(this.sqlEditor.getText());
		return verb.equalsIgnoreCase("select");
	}

	@Override
	public void windowActivated(WindowEvent e)
	{
	}

	@Override
	public void windowClosed(WindowEvent e)
	{
	}

	@Override
	public void windowClosing(WindowEvent e)
	{
		if (this.copyRunning)
		{
			this.cancelCopy();
		}
		this.closeWindow();
	}

	@Override
	public void closeWindow()
	{
		this.done();
		if (this.window != null)
		{
			this.window.removeWindowListener(this);
			this.window.dispose();
		}
	}

	@Override
	public void disconnect()
	{
		this.disconnectSource();
		this.disconnectTarget();
	}

	public void done()
	{
		this.saveSettings();

		this.sourceProfile = null;
		this.targetProfile = null;
		this.columnMapper.resetData();
		this.columnMapper = null;

		Thread t = new WbThread("DataPumper disconnect thread")
		{
			@Override
			public void run()
			{
				disconnect();
				unregister();
			}
		};
		t.start();
	}

	protected void unregister()
	{
		WbManager.getInstance().unregisterToolWindow(this);
	}


	@Override
	public void windowDeactivated(WindowEvent e)
	{
	}

	@Override
	public void windowDeiconified(WindowEvent e)
	{
	}

	@Override
	public void windowIconified(WindowEvent e)
	{
	}

	@Override
	public void windowOpened(WindowEvent e)
	{
	}

	/**
	 *	We have registered with both table selectors to be informed
	 *	when the user changes the selection. After each change (and
	 *	we don't actually care where it came from) the tables are
	 *	checked, and if both are present, we'll initialize the
	 *	ColumnMapper
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		TableIdentifier theTarget = this.targetTable.getSelectedTable();
		TableIdentifier source = this.sourceTable.getSelectedTable();

		checkType();

		if (evt.getSource() == this.sourceTable && source != null)
		{
			if (theTarget != null && theTarget.isNewTable())
			{
				this.targetTable.resetNewTableItem();
				theTarget = null;
			}
			if (theTarget == null || targetTable.isAutoSyncSelected())
			{
				this.targetTable.findAndSelectTable(source.getTableName());
			}
		}
		else if (evt.getSource() == this.targetTable && theTarget != null && source == null)
		{
			this.sourceTable.findAndSelectTable(theTarget.getTableName());
		}

		if (theTarget != null && theTarget.isNewTable())
		{
			String name = theTarget.getTableName();
			if (name == null)
			{
				String def = null;
				if (source != null)
				{
					def = source.getTableName();
				}
				name = WbSwingUtilities.getUserInput(this, ResourceMgr.getString("TxtEnterNewTableName"), def);
				if (name != null)
				{
					theTarget.parseTableIdentifier(name);
					this.targetTable.repaint();
				}
			}
		}

		if (this.hasSource() && theTarget != null)
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					initColumnMapper();
				}
			});
		}
		else
		{
			this.startButton.setEnabled(false);
			this.showWbCommand.setEnabled(false);
			this.columnMapper.resetData();
			this.dropTargetCbx.setEnabled(false);
			this.ignoreDropError.setEnabled(false);
		}
	}

	public boolean hasSource()
	{
		if (this.useQueryCbx.isSelected())
		{
			return (this.sqlEditor.getText().length() > 0);
		}
		else if (this.fileImporter != null)
		{
			return true;
		}
		else
		{
			return (this.sourceTable.getSelectedTable() != null);
		}
	}
	private List<ColumnIdentifier> getResultSetColumns()
	{
		if (this.sourceConnection == null) return null;
		String sql = this.sqlEditor.getText();

		List<ColumnIdentifier> result = null;

		try
		{
			result = SqlUtil.getResultSetColumns(sql, this.sourceConnection);
		}
		catch (SQLException e)
		{
			LogMgr.logError("DataPumper", "Error when retrieving ResultSet definition for source SQL", e);
			WbSwingUtilities.showErrorMessage(this, e.getMessage());
		}
		return result;
	}

	private void resetColumnMapper()
	{
		this.columnMapper.resetData();
	}

	private List<ColumnIdentifier> getKeyColumns()
	{
		ColumnMapper.MappingDefinition colMapping = this.columnMapper.getMapping();
		if (colMapping == null) return Collections.emptyList();
		int count = colMapping.targetColumns.length;
		List<ColumnIdentifier> keys = new ArrayList<>();

		for (int i=0; i < count; i++)
		{
			if (colMapping.targetColumns[i].isPkColumn())
			{
				keys.add(colMapping.targetColumns[i]);
			}
		}
		return keys;
	}

	private void showImportCommand()
	{
		if (this.fileImporter == null || this.targetProfile == null) return;
		StringBuilder sql = null;
		try
		{
			this.initImporter();
			sql = this.fileImporter.getWbCommand();
			appendGeneralImportOptions(sql, WbImport.VERB.length());
			sql.append(";\n");
		}
		catch (Exception e)
		{
			LogMgr.logError("DataPumper.showImportCommand()", "Error creating SQL command", e);
			sql = new StringBuilder(ExceptionUtil.getDisplay(e));
		}

		EditWindow w = new EditWindow(this.window, ResourceMgr.getString("MsgWindowTitleDPScript"), sql.toString(), "workbench.datapumper.scriptwindow", true);
		w.setVisible(true);
		w.dispose();
	}

	private void appendGeneralImportOptions(StringBuilder sql, int indentSize)
	{
		String indent = StringUtil.padRight("", indentSize + 1);
		CommonArgs.appendArgument(sql, CommonArgs.ARG_IGNORE_IDENTITY, Boolean.toString(ignoreIdentityCbx.isSelected()), indent);

		String mode = (String)this.modeComboBox.getSelectedItem();
		if (!"insert".equals(mode))
		{
			CommonArgs.appendArgument(sql, CommonArgs.ARG_IMPORT_MODE, mode, indent);
			Collection<ColumnIdentifier> keys = this.getKeyColumns();
			if (keys.size() > 0)
			{
				Iterator<ColumnIdentifier> itr = keys.iterator();
				sql.append("\n");
				sql.append(indent);
				sql.append("-" + WbCopy.PARAM_KEYS + "=");
				while (itr.hasNext())
				{
					ColumnIdentifier col = itr.next();
					sql.append(col.getColumnName());
					if (itr.hasNext()) sql.append(',');
				}
			}
		}

		CommonArgs.appendArgument(sql, CommonArgs.ARG_DELETE_TARGET, Boolean.toString(this.deleteTargetCbx.isSelected()), indent);
		CommonArgs.appendArgument(sql, CommonArgs.ARG_CONTINUE, Boolean.toString(this.continueOnErrorCbx.isSelected()), indent);

		int size = getBatchSize();
		if (size > 0)
		{
			CommonArgs.appendArgument(sql, CommonArgs.ARG_BATCHSIZE, Integer.toString(size), indent);
		}

		if (size <= 0)
		{
			int commit = StringUtil.getIntValue(this.commitEvery.getText(), -1);
			if (commit > 0)
			{
				CommonArgs.appendArgument(sql, CommonArgs.ARG_COMMIT, Integer.toString(commit), indent);
			}
		}

		if (StringUtil.isNonBlank(preTableStmt.getText()))
		{
			CommonArgs.appendArgument(sql, CommonArgs.ARG_PRE_TABLE_STMT, "\""  + preTableStmt.getText() + "\"", indent);
		}

		if (StringUtil.isNonBlank(postTableStmt.getText()))
		{
			CommonArgs.appendArgument(sql, CommonArgs.ARG_POST_TABLE_STMT, "\""  + postTableStmt.getText() + "\"", indent);
		}
	}

	private void showCopyCommand()
	{
		if (this.fileImporter != null)
		{
			this.showImportCommand();
			return;
		}
		if (this.sourceProfile == null || this.targetProfile == null) return;
		if (!this.hasSource()) return;

		CommandTester t = new CommandTester();

		StringBuilder result = new StringBuilder(150);
		result.append(t.formatVerb(WbCopy.VERB));
		result.append(" -" + WbCopy.PARAM_SOURCEPROFILE + "=");
		String indent = "\n       ";

		String s = this.sourceProfile.getName();
		if (s.indexOf(' ') >-1) result.append('\'');
		result.append(s);
		if (s.indexOf(' ') >-1) result.append('\'');

		result.append(indent);
		result.append("-" + WbCopy.PARAM_SOURCEPROFILE_GROUP + "=");
		s = this.sourceProfile.getGroup();
		if (s.indexOf(' ') >-1) result.append('\'');
		result.append(s);
		if (s.indexOf(' ') >-1) result.append('\'');

		s = this.targetProfile.getName();
		result.append(indent);
		result.append("-" + WbCopy.PARAM_TARGETPROFILE + "=");
		if (s.indexOf(' ') >-1) result.append('\'');
		result.append(s);
		if (s.indexOf(' ') >-1) result.append('\'');

		result.append(indent);
		result.append("-" + WbCopy.PARAM_TARGETPROFILE_GROUP + "=");
		s = this.targetProfile.getGroup();
		if (s.indexOf(' ') >-1) result.append('\'');
		result.append(s);
		if (s.indexOf(' ') >-1) result.append('\'');

		TableIdentifier id = this.targetTable.getSelectedTable();
		if (targetProfile == null) return;

		String tname = id.getTableExpression(targetConnection);
		result.append(indent);
		result.append("-" + WbCopy.PARAM_TARGETTABLE + "=");
		result.append(tname);

		if (id.isNewTable())
		{
			result.append(indent);
			result.append("-" + WbCopy.PARAM_CREATETARGET + "=true");
			if (this.dropTargetCbx.isSelected())
			{
				result.append(indent);
				result.append("-" + WbCopy.PARAM_DROPTARGET + "=true");
			}
			if (this.ignoreDropError.isSelected())
			{
				result.append(indent);
				result.append("-" + AppArguments.ARG_IGNORE_DROP + "=true");
			}
		}

		ColumnMapper.MappingDefinition colMapping = this.columnMapper.getMapping();
		if (colMapping == null) return;
		int count = colMapping.targetColumns.length;

		boolean allEqual = true;
		StringBuffer mapping = new StringBuffer(count*20);

		if (this.useQueryCbx.isSelected())
		{
			String sql = this.sqlEditor.getText();
			result.append(indent);
			result.append("-" + WbCopy.PARAM_SOURCEQUERY + "=\"");
			result.append(sql);
			result.append('"');
		}
		else
		{
			id = this.sourceTable.getSelectedTable();
			if (id == null) return;
			s = id.getTableExpression(sourceConnection);
			result.append(indent);
			result.append("-" + WbCopy.PARAM_SOURCETABLE + "=");
			if (s.indexOf(' ') > -1) result.append('"');
			result.append(s);
			if (s.indexOf(' ') > -1) result.append('"');

			s = sqlEditor.getText();
			if (StringUtil.isNonBlank(s))
			{
				result.append(indent);
				result.append("-" + WbCopy.PARAM_SOURCEWHERE + "=\"");
				result.append(s);
				result.append('"');
			}
		}

		mapping.append(indent);
		mapping.append("-" + WbCopy.PARAM_COLUMNS + "='");
		for (int i=0; i < count; i++)
		{
			if (i > 0) mapping.append(", ");
			String sourceCol = colMapping.sourceColumns[i].getColumnName();
			String targetCol = colMapping.targetColumns[i].getColumnName();

			if (!this.useQueryCbx.isSelected())
			{
				mapping.append(sourceCol);
				mapping.append('/');
			}
			mapping.append(targetCol);
			if (!sourceCol.equalsIgnoreCase(targetCol))
			{
				allEqual = false;
			}
		}
		mapping.append('\'');

		if (!allEqual || colMapping.hasSkippedColumns)
		{
			result.append(mapping);
		}

		appendGeneralImportOptions(result, WbCopy.VERB.length());
		result.append("\n;");

		EditWindow w = new EditWindow(this.window, ResourceMgr.getString("MsgWindowTitleDPScript"), result.toString(), "workbench.datapumper.scriptwindow", true);
		w.setVisible(true);
		w.dispose();
	}

	protected int getBatchSize()
	{
		int size = -1;
		if (this.batchSize.isEnabled())
		{
			size = StringUtil.getIntValue(batchSize.getText(), -1);
		}
		return size;
	}

	protected void initColumnMapper()
	{
		if ( (this.sourceConnection == null && this.fileImporter == null) || this.targetConnection == null || !this.hasSource())
		{
			this.startButton.setEnabled(false);
			this.showWbCommand.setEnabled(false);
			return;
		}

		TableIdentifier target = this.targetTable.getSelectedTable();
		if (target == null)
		{
			this.startButton.setEnabled(false);
			this.showWbCommand.setEnabled(false);
			return;
		}

		boolean useQuery = this.useQueryCbx.isSelected();
		try
		{
			List<ColumnIdentifier> sourceCols = null;
			if (useQuery)
			{
				sourceCols = this.getResultSetColumns();
			}
			else if (this.fileImporter != null)
			{
				sourceCols = this.fileImporter.getFileColumns();
			}
			else
			{
				TableIdentifier source = this.sourceTable.getSelectedTable();
				sourceCols = this.sourceConnection.getMetadata().getTableColumns(source);
			}

			boolean newTable = target.isNewTable();
			this.columnMapper.setAllowTargetEditing(newTable);
			// Dropping the target table is only available if it should be created
			// if the target exists, we do not support dropping and re-creating the table
			this.dropTargetCbx.setEnabled(newTable);
			this.ignoreDropError.setEnabled(newTable);

			if (newTable)
			{
				this.columnMapper.defineColumns(sourceCols, sourceCols, false, useQuery);
			}
			else
			{
				List<ColumnIdentifier> targetCols = this.targetConnection.getMetadata().getTableColumns(target);
				boolean syncDataTypes = (this.fileImporter != null);
				this.columnMapper.defineColumns(sourceCols, targetCols, syncDataTypes, useQuery);
			}

			this.columnMapper.setAllowSourceEditing(!useQuery && !newTable);
			this.startButton.setEnabled(true);
			this.showWbCommand.setEnabled(true);
		}
		catch (Exception e)
		{
			WbSwingUtilities.showFriendlyErrorMessage(this, this.window.getTitle(), ExceptionUtil.getDisplay(e));
			LogMgr.logError("DataPumper.initColumnMapper()", "Error when intializing column mapper", e);
		}
	}

	private void cancelCopy()
	{
		this.statusLabel.setText(ResourceMgr.getString("MsgCancellingCopy"));
		this.statusLabel.repaint();
		cancelButton.setEnabled(false);
		WbThread t = new WbThread("DataPumper cancel")
		{
			@Override
			public void run()
			{
				doCancel();
			}
		};
		t.start();
	}

	protected void doCancel()
	{
		if (copier != null) copier.cancel();
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				cancelButton.setEnabled(false);
				startButton.setEnabled(true);
				copyRunning = false;
				updateWindowTitle();
				statusLabel.setText(ResourceMgr.getString("MsgCopyCancelled"));
				statusLabel.repaint();
			}
		});
	}

	private void initImporter()
		throws Exception
	{
		this.fileImporter.setConnection(this.targetConnection);
		List<ColumnIdentifier> cols = columnMapper.getMappingForImport();
		this.fileImporter.setTargetTable(this.targetTable.getSelectedTable());
		this.fileImporter.setImportColumns(cols);
		ImportOptions options = fileImporter.getGeneralOptions();
		if (options != null)
		{
			options.setMode((String)modeComboBox.getSelectedItem());
		}
	}

	private void startCopy()
	{
		if (this.targetConnection == null || (this.sourceConnection == null && this.fileImporter == null)) return;

		if (this.columnMapper == null) return;

		TableIdentifier ttable = this.targetTable.getSelectedTable();

		ColumnMapper.MappingDefinition colMapping = this.columnMapper.getMapping();

		if (!this.createCopier()) return;

		DropType dropTarget = DropType.none;
		if (dropTargetCbx.isSelected())
		{
			dropTarget = DropType.regular;
		}
		boolean ignoreDrop = ignoreDropError.isSelected();

		try
		{
			this.copyRunning = true;
			String tableType = (ttable.isNewTable() ? DbSettings.DEFAULT_CREATE_TABLE_TYPE : null);

			if (this.fileImporter != null)
			{
				this.initImporter();
				this.copier.setProducer(this.fileImporter.getProducer(), this.targetConnection, this.targetTable.getSelectedTable());
				int interval = DataImporter.estimateReportIntervalFromFileSize(this.fileImporter.getSourceFile());
				this.copier.setReportInterval(interval);
			}
			else if (this.useQueryCbx.isSelected())
			{
				this.copier.copyFromQuery(this.sourceConnection, this.targetConnection, this.sqlEditor.getText(), ttable, Arrays.asList(colMapping.targetColumns), tableType, dropTarget, ignoreDrop, false);
			}
			else
			{
				boolean ignoreSelect = false;
				String where = null;
				TableIdentifier stable = this.sourceTable.getSelectedTable();
				if (this.isSelectQuery())
				{
					WbSwingUtilities.showErrorMessageKey(this, "MsgDPIgnoreSelect");
					ignoreSelect = true;
				}
				if (!ignoreSelect) where = this.sqlEditor.getText();

				Map<String, String> mapping = new HashMap<>();
				int count = colMapping.sourceColumns.length;
				for (int i=0; i < count; i++)
				{
					mapping.put(colMapping.sourceColumns[i].getColumnName(), colMapping.targetColumns[i].getColumnName());
				}
				this.copier.copyFromTable(this.sourceConnection, this.targetConnection, stable, ttable, mapping, where, tableType, dropTarget, ignoreDrop, false);
			}

			this.copier.startBackgroundCopy();
			this.showLogButton.setEnabled(false);
			this.startButton.setEnabled(false);
			this.cancelButton.setEnabled(true);

			this.updateWindowTitle();
		}
		catch (Exception e)
		{
			this.copyRunning = false;
			this.showLogButton.setEnabled(true);
			this.startButton.setEnabled(true);
			this.cancelButton.setEnabled(false);
			LogMgr.logError("DataPumper.startCopy()", "Could not execute copy process", e);
			this.statusLabel.setText(ResourceMgr.getString("MsgCopyFinishedWithErrors"));
		}
	}

	public boolean isRunning()
	{
		return this.copyRunning;
	}

	private boolean createCopier()
	{
		ColumnMapper.MappingDefinition colMapping = this.columnMapper.getMapping();
		if (colMapping == null) return false;

		this.copier = new DataCopier();
		if (this.deleteTargetCbx.isSelected())
		{
			this.copier.setDeleteTarget(DeleteType.delete);
		}
		else
		{
			this.copier.setDeleteTarget(DeleteType.none);
		}
		this.copier.setContinueOnError(this.continueOnErrorCbx.isSelected());
		String mode = (String)this.modeComboBox.getSelectedItem();
		List<ColumnIdentifier> keys = this.getKeyColumns();

		this.copier.setKeyColumns(keys);

		if (mode.indexOf("update") > -1 && keys.isEmpty())
		{
			WbSwingUtilities.showErrorMessageKey(this, "ErrDPNoKeyColumns");
			return false;
		}

		if (keys.size() == colMapping.targetColumns.length && mode.indexOf("update") > -1)
		{
			WbSwingUtilities.showErrorMessageKey(this, "ErrDPUpdateOnlyKeyColumns");
			return false;
		}

		this.copier.setMode(mode);
		int bSize = getBatchSize();
		int commit = StringUtil.getIntValue(this.commitEvery.getText(), -1);
		if (bSize <= 0) this.copier.setCommitEvery(commit);

		if (bSize > 0)
		{
			this.copier.setUseBatch(true);
			this.copier.setBatchSize(bSize);
			if (commit > 0) this.copier.setCommitBatch(true);
		}

		this.copier.setRowActionMonitor(this);
		this.copier.setReportInterval(10);
		TableStatements stmt = new TableStatements(preTableStmt.getText(), postTableStmt.getText());
		this.copier.setPerTableStatements(stmt);
		this.copier.setIgnoreIdentityColumns(ignoreIdentityCbx.isSelected());
		return true;
	}

	@Override
	public void setCurrentObject(String object, long currentRow, long total)
	{
		updateMonitor(currentRow);
	}

	@Override
	public void setCurrentRow(long currentRow, long totalRows)
	{
		updateMonitor(currentRow);
	}

	private void updateMonitor(final long currentRow)
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				if (currentRow == 1) updateWindowTitle();
				statusLabel.setText(copyMsg + " " + currentRow);
				statusLabel.repaint();
			}
		});
	}

	@Override
	public void saveCurrentType(String type)
	{
	}

	@Override
	public void restoreType(String type)
	{
	}

	@Override
	public int getMonitorType()
	{
		return RowActionMonitor.MONITOR_PLAIN;
	}

	@Override
	public void setMonitorType(int aType)
	{
	}

	@Override
	public void jobFinished()
	{
		this.copyRunning = false;
		if (this.copier.isSuccess())
		{
			String msg = this.copier.getRowsInsertedMessage();
			String msg2 = this.copier.getRowsUpdatedMessage();
			StringBuilder copied = new StringBuilder(50);
			if (msg != null)
			{
				copied.append(msg);
				if (msg2 != null)
				{
					copied.append(", ");
					copied.append(msg2);
				}
			}
			else if (msg2 != null && msg2.length() > 0)
			{
				copied.append(msg2);
			}
			else
			{
				long rows = this.copier.getAffectedRows();
				copied.append(rows);
				copied.append(' ');
				copied.append(ResourceMgr.getString("MsgCopyNumRows"));
			}

			if (copied.length() > 0)
			{
				copied.insert(0, " - ");
			}

			if (this.copier.hasWarnings())
			{
				this.statusLabel.setText(ResourceMgr.getString("MsgCopyFinishedWithWarning") + copied);
			}
			else
			{
				this.statusLabel.setText(ResourceMgr.getString("MsgCopyFinishedWithSuccess") + copied);
			}
		}
		else
		{
			this.statusLabel.setText(ResourceMgr.getString("MsgCopyFinishedWithErrors"));
		}

		this.startButton.setEnabled(true);
		this.cancelButton.setEnabled(false);

		if (this.copier.hasWarnings() || !this.copier.isSuccess())
		{
			this.showLogButton.setEnabled(true);
		}

		this.updateWindowTitle();

		if (!this.copier.isSuccess())
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					showLog();
				}
			});
		}
	}

	protected void showLog()
	{
		if (this.copier == null)
		{
			return;
		}

		CharSequence log = null;
		try
		{
			log = this.copier.getAllMessages();
		}
		catch (Exception e)
		{
			LogMgr.logError("DataPumper.showLog()", "Error when retrieving log information", e);
			log = ExceptionUtil.getDisplay(e);
		}

		EditWindow w = new EditWindow(this.window,
					ResourceMgr.getString("MsgWindowTitleDPLog"),
					(log == null ? "" : log.toString()),
					"workbench.datapumper.logwindow"
				);

		w.setVisible(true); // EditWindow is modal
		w.dispose();
	}

}
