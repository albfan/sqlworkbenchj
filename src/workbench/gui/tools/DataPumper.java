/*
 * DataPumper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.tools;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import workbench.WbManager;
import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.datacopy.DataCopier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.importer.DataImporter;
import workbench.db.importer.ProducerFactory;
import workbench.util.ExceptionUtil;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.DividerBorder;
import workbench.gui.components.EditWindow;
import workbench.gui.components.WbButton;
import workbench.gui.components.WbSplitPane;
import workbench.gui.dialogs.dataimport.ImportFileDialog;
import workbench.gui.help.HtmlViewer;
import workbench.gui.profiles.ProfileSelectionDialog;
import workbench.gui.sql.EditorPanel;
import workbench.interfaces.ToolWindow;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.wbcommands.WbCopy;
import workbench.storage.RowActionMonitor;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 *
 * @author  support@sql-workbench.net
 */
public class DataPumper
	extends JPanel
	implements ActionListener, WindowListener, PropertyChangeListener, RowActionMonitor,
	           ToolWindow
{
	private ConnectionProfile sourceProfile;
	private String sourceFile;
	private ProducerFactory fileImporter;
	private ConnectionProfile targetProfile;
	private WbConnection sourceConnection;
	private WbConnection targetConnection;

	private JFrame window;
	private ColumnMapper columnMapper;
	private final String copyMsg = ResourceMgr.getString("MsgCopyingRow");
	private boolean copyRunning = false;
	private DataCopier copier;
//	private DataImporter importer;
	private EditorPanel sqlEditor;
//	private MainWindow mainWindow;
	private boolean supportsBatch = false;
	boolean allowCreateTable = true; //"true".equals(Settings.getInstance().getProperty("workbench.datapumper.allowcreate", "true"));

	/** Creates new form DataPumper */
	public DataPumper(ConnectionProfile source, ConnectionProfile target)
	{
		this.sourceProfile = source;
		this.targetProfile = target;
		initComponents();
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

		//this.separatorPanel1.setBorder(new DividerBorder(DividerBorder.VERTICAL_MIDDLE));
		this.updateOptionPanel.setBorder(new DividerBorder(DividerBorder.LEFT));
    //this.jPanel2.setBorder(new DividerBorder(DividerBorder.BOTTOM));
		this.checkQueryButton.addActionListener(this);
		this.showWbCommand.addActionListener(this);
		this.useQueryCbx.addActionListener(this);
		this.modeComboBox.addActionListener(this);
		this.sqlEditor = EditorPanel.createSqlEditor();
		this.sqlEditor.showFormatSql();
		this.wherePanel.add(this.sqlEditor);
		this.showWbCommand.setEnabled(false);
		this.useBatchCheckBox.setEnabled(false);
		if (!this.allowCreateTable)
		{
			this.dropTargetCbx.setVisible(this.allowCreateTable);
			//this.remove(this.dropTargetCbx);
			GridBagLayout grid = (GridBagLayout)this.optionsPanel.getLayout();
			grid.removeLayoutComponent(this.dropTargetCbx);
			this.optionsPanel.remove(this.dropTargetCbx);

			GridBagConstraints cons = grid.getConstraints(this.commitEvery);
			cons.gridy --;
			grid.setConstraints(this.commitEvery, cons);

			cons = grid.getConstraints(this.commitLabel);
			cons.gridy--;
			grid.setConstraints(this.commitLabel, cons);
			//grid.layoutContainer(this);

			cons = grid.getConstraints(this.modeLabel);
			cons.gridy--;
			grid.setConstraints(this.modeLabel, cons);

			cons = grid.getConstraints(this.modeComboBox);
			cons.gridy--;
			grid.setConstraints(this.modeComboBox, cons);
		}
	}

	public void saveSettings()
	{
		Settings s = Settings.getInstance();
		if (this.sourceProfile != null)
		{
			s.setProperty("workbench.datapumper.source.lastprofile", this.sourceProfile.getName());
		}
		if (this.targetProfile != null)
		{
			s.setProperty("workbench.datapumper.target.lastprofile", this.targetProfile.getName());
		}
		s.setProperty("workbench.datapumper.divider", jSplitPane1.getDividerLocation());
		s.setProperty("workbench.datapumper.target.deletetable", Boolean.toString(this.deleteTargetCbx.isSelected()));
		s.setProperty("workbench.datapumper.continue", Boolean.toString(this.continueOnErrorCbx.isSelected()));
		s.setProperty("workbench.datapumper.commitevery", this.commitEvery.getText());
		s.setProperty("workbench.datapumper.usequery", Boolean.toString(this.useQueryCbx.isSelected()));
		s.setProperty("workbench.datapumper.droptable", Boolean.toString(this.dropTargetCbx.isSelected()));
    s.setProperty("workbench.datapumper.updatemode", (String)this.modeComboBox.getSelectedItem());
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
			loc = (int)this.jSplitPane1.getHeight() / 2;
			if (loc < 10) loc = 100;
		}
		this.jSplitPane1.setDividerLocation(loc);
		boolean useQuery = s.getBoolProperty("workbench.datapumper.usequery", false);
		this.useQueryCbx.setSelected(useQuery);

    String mode = s.getProperty("workbench.datapumper.updatemode", "insert");
    this.modeComboBox.setSelectedItem(mode);

		// initialize the depending controls for the usage of a SQL query
		this.checkType();
	}

	private void selectInputFile()
	{
		ImportFileDialog dialog = new ImportFileDialog(this);
		boolean ok = dialog.selectInput(ResourceMgr.getString("TxtWindowTitleSelectImportFile"));
		if (!ok) return; 
		if (this.sourceProfile != null)
		{
			this.disconnectSource();
		}
		this.sourceFile = dialog.getSelectedFilename();
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
		this.fileImporter.setXmlOptions(dialog.getXmlOptions());
		this.fileImporter.setType(dialog.getImportType());
		this.targetTable.allowNewTable(false);
		
		this.checkType();

		this.updateSourceDisplay();
		if (this.targetProfile != null)
		{
			initColumnMapper();
		}
	}
	
	private void updateTargetDisplay()
	{
		String label = ResourceMgr.getString("LabelDPTargetProfile");
		if (this.targetProfile != null)
		{
			this.targetProfileLabel.setText(label + ": " + this.targetProfile.getName());
		}
		else
		{
			this.targetProfileLabel.setText(label + ": " + ResourceMgr.getString("LabelPleaseSelect"));
		}
		this.updateWindowTitle();
	}

	private void updateSourceDisplay()
	{
		String label = ResourceMgr.getString("LabelDPSourceProfile");
		if (this.sourceProfile != null)
		{
			this.sourceProfileLabel.setText(label + ": " + this.sourceProfile.getName());
		}
		else if (this.sourceFile != null)
		{
			File f = new File(this.sourceFile);
			this.sourceProfileLabel.setText(ResourceMgr.getString("LabelDPSourceFile") + ": " + f.getAbsolutePath());
		}
		else
		{
			this.sourceProfileLabel.setText(label + ": " + ResourceMgr.getString("LabelPleaseSelect"));
		}
		this.updateWindowTitle();
	}

	private void updateDisplay()
	{
		this.updateSourceDisplay();
		this.updateTargetDisplay();
		this.updateWindowTitle();
	}

	private void updateWindowTitle()
	{
		if (this.targetProfile != null && (this.sourceProfile != null || this.sourceFile != null) && this.window != null)
		{
			String title = ResourceMgr.getString("TxtWindowTitleDataPumper");
			String sourceName = "";
			if (this.sourceProfile != null)
				sourceName = this.sourceProfile.getName();
			else if (this.sourceFile != null) 
				sourceName = this.sourceFile;
			title = title + " [" + sourceName + " -> " + this.targetProfile.getName() + "]";
			if (this.copier != null && this.copyRunning)
			{
				title = "» " + title;
			}
			this.window.setTitle(title);
		}
	}

	private void checkConnections()
	{
		this.connectSource(this.sourceProfile);
		this.connectTarget(this.targetProfile);
	}

	private void connectSource(final ConnectionProfile profile)
	{
		if (profile == null) return;

		Thread t = new WbThread("DataPumper source connection")
		{
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
		String label = ResourceMgr.getString("MsgConnectingTo") + " " + this.sourceProfile.getName() + " ...";
		this.sourceProfileLabel.setIcon(ResourceMgr.getPicture("wait"));
		this.sourceProfileLabel.setText(label);

		try
		{
			this.sourceConnection = ConnectionMgr.getInstance().getConnection(this.sourceProfile, "Dp-Source");
		}
		catch (Exception e)
		{
			LogMgr.logError("DataPumper.doConnectSource()", "Error when connecting to profile: " + this.sourceProfile.getName(), e);
			String msg = ResourceMgr.getString("ErrorConnectionError") + "\n" + e.getMessage();
			this.sourceProfile = null;
			WbSwingUtilities.showErrorMessage(this, msg);
		}
		finally
		{
			this.sourceProfileLabel.setIcon(null);
			this.updateSourceDisplay();
		}
		
		this.sourceFile = null;
		this.fileImporter = null;
		this.checkType();

		if (this.sourceConnection != null)
		{
			this.sourceTable.setChangeListener(this, "source-table");

		  Thread t = new WbThread("Retrieve source tables")
		  {
			  public void run()
			  {
				  sourceTable.setConnection(sourceConnection);
			  }
		  };
		  t.start();
		}
	}

	private void connectTarget(final ConnectionProfile profile)
	{
		if (profile == null) return;

		Thread t = new WbThread("DataPumper target connection")
		{
			public void run()
			{
				doConnectTarget(profile);
			}
		};
		t.start();
	}

	private void doConnectTarget(ConnectionProfile profile)
	{
		this.disconnectTarget();
		this.targetProfile = profile;
		String label = ResourceMgr.getString("MsgConnectingTo") + " " + this.targetProfile.getName() + " ...";
		this.targetProfileLabel.setText(label);
		this.targetProfileLabel.setIcon(ResourceMgr.getPicture("wait"));

		try
		{
			this.targetConnection = ConnectionMgr.getInstance().getConnection(this.targetProfile, "Dp-Target");
		}
		catch (Exception e)
		{
			LogMgr.logError("DataPumper.doConnectSource()", "Error when connecting to profile: " + this.targetProfile.getName(), e);
			String msg = ResourceMgr.getString("ErrorConnectionError") + "\n" + e.getMessage();
			this.targetProfile = null;
			WbSwingUtilities.showErrorMessage(this, msg);
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

		  Thread t = new WbThread("Retrieve target tables")
		  {
			  public void run()
			  {
				  targetTable.setConnection(targetConnection);
			  }
		  };
		  t.start();
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
  // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
  private void initComponents()
  {
    java.awt.GridBagConstraints gridBagConstraints;

    sourceProfilePanel = new javax.swing.JPanel();
    sourceProfileLabel = new javax.swing.JLabel();
    selectSourceButton = new javax.swing.JButton();
    openFileButton = new javax.swing.JButton();
    targetProfilePanel = new javax.swing.JPanel();
    targetProfileLabel = new javax.swing.JLabel();
    selectTargetButton = new javax.swing.JButton();
    sourceTable = new workbench.gui.tools.TableSelectorPanel();
    targetTable = new workbench.gui.tools.TableSelectorPanel();
    targetHeader = new javax.swing.JLabel();
    sourceHeader = new javax.swing.JLabel();
    jSplitPane1 = new WbSplitPane();
    mapperPanel = new javax.swing.JPanel();
    optionsPanel = new javax.swing.JPanel();
    jPanel2 = new javax.swing.JPanel();
    sqlPanel = new javax.swing.JPanel();
    wherePanel = new javax.swing.JPanel();
    sqlEditorLabel = new javax.swing.JLabel();
    useQueryCbx = new javax.swing.JCheckBox();
    checkQueryButton = new javax.swing.JButton();
    updateOptionPanel = new javax.swing.JPanel();
    commitLabel = new javax.swing.JLabel();
    commitEvery = new javax.swing.JTextField();
    continueOnErrorCbx = new javax.swing.JCheckBox();
    deleteTargetCbx = new javax.swing.JCheckBox();
    dropTargetCbx = new javax.swing.JCheckBox();
    modeComboBox = new javax.swing.JComboBox();
    modeLabel = new javax.swing.JLabel();
    jPanel1 = new javax.swing.JPanel();
    jLabel1 = new javax.swing.JLabel();
    useBatchCheckBox = new javax.swing.JCheckBox();
    statusLabel = new javax.swing.JLabel();
    buttonPanel = new javax.swing.JPanel();
    jPanel3 = new javax.swing.JPanel();
    startButton = new WbButton();
    cancelButton = new javax.swing.JButton();
    jPanel4 = new javax.swing.JPanel();
    showLogButton = new javax.swing.JButton();
    showWbCommand = new javax.swing.JButton();
    jPanel5 = new javax.swing.JPanel();
    helpButton = new javax.swing.JButton();
    closeButton = new javax.swing.JButton();

    setLayout(new java.awt.GridBagLayout());

    sourceProfilePanel.setLayout(new java.awt.GridBagLayout());

    sourceProfileLabel.setBorder(new javax.swing.border.EtchedBorder());
    sourceProfileLabel.setMaximumSize(new java.awt.Dimension(32768, 24));
    sourceProfileLabel.setMinimumSize(new java.awt.Dimension(25, 24));
    sourceProfileLabel.setPreferredSize(new java.awt.Dimension(50, 24));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    sourceProfilePanel.add(sourceProfileLabel, gridBagConstraints);

    selectSourceButton.setText("...");
    selectSourceButton.setBorder(new javax.swing.border.EtchedBorder());
    selectSourceButton.setMaximumSize(new java.awt.Dimension(24, 24));
    selectSourceButton.setMinimumSize(new java.awt.Dimension(24, 24));
    selectSourceButton.setPreferredSize(new java.awt.Dimension(24, 24));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    sourceProfilePanel.add(selectSourceButton, gridBagConstraints);

    openFileButton.setIcon(ResourceMgr.getImage("Open"));
    openFileButton.setToolTipText(ResourceMgr.getString("Desc_DataPumperOpenFile"));
    openFileButton.setBorder(new javax.swing.border.EtchedBorder());
    openFileButton.setIconTextGap(0);
    openFileButton.setMaximumSize(new java.awt.Dimension(24, 24));
    openFileButton.setMinimumSize(new java.awt.Dimension(24, 24));
    openFileButton.setPreferredSize(new java.awt.Dimension(24, 24));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    sourceProfilePanel.add(openFileButton, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
    gridBagConstraints.weightx = 0.5;
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
    add(sourceProfilePanel, gridBagConstraints);

    targetProfilePanel.setLayout(new java.awt.GridBagLayout());

    targetProfileLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    targetProfileLabel.setBorder(new javax.swing.border.EtchedBorder());
    targetProfileLabel.setMaximumSize(new java.awt.Dimension(32768, 24));
    targetProfileLabel.setMinimumSize(new java.awt.Dimension(25, 24));
    targetProfileLabel.setPreferredSize(new java.awt.Dimension(0, 24));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    targetProfilePanel.add(targetProfileLabel, gridBagConstraints);

    selectTargetButton.setText("...");
    selectTargetButton.setBorder(new javax.swing.border.EtchedBorder());
    selectTargetButton.setMaximumSize(new java.awt.Dimension(24, 24));
    selectTargetButton.setMinimumSize(new java.awt.Dimension(24, 24));
    selectTargetButton.setPreferredSize(new java.awt.Dimension(24, 24));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    targetProfilePanel.add(selectTargetButton, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 0.5;
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
    add(targetProfilePanel, gridBagConstraints);

    sourceTable.setMaximumSize(new java.awt.Dimension(2147483647, 65));
    sourceTable.setMinimumSize(new java.awt.Dimension(25, 50));
    sourceTable.setPreferredSize(new java.awt.Dimension(25, 50));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 2, 6, 2);
    add(sourceTable, gridBagConstraints);

    targetTable.setMaximumSize(new java.awt.Dimension(2147483647, 65));
    targetTable.setMinimumSize(new java.awt.Dimension(25, 50));
    targetTable.setPreferredSize(new java.awt.Dimension(25, 50));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 4, 6, 2);
    add(targetTable, gridBagConstraints);

    targetHeader.setBackground(java.awt.Color.white);
    targetHeader.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    targetHeader.setText("<html><b>" + ResourceMgr.getString("LabelTargetConnection") + "</b></html>");
    targetHeader.setMaximumSize(new java.awt.Dimension(23768, 22));
    targetHeader.setMinimumSize(new java.awt.Dimension(25, 22));
    targetHeader.setPreferredSize(new java.awt.Dimension(25, 22));
    targetHeader.setOpaque(true);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 0.5;
    gridBagConstraints.insets = new java.awt.Insets(4, 2, 4, 2);
    add(targetHeader, gridBagConstraints);

    sourceHeader.setBackground(java.awt.Color.white);
    sourceHeader.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    sourceHeader.setText("<html><b>" + ResourceMgr.getString("LabelSourceConnection") + "</b></html>");
    sourceHeader.setMaximumSize(new java.awt.Dimension(32768, 22));
    sourceHeader.setMinimumSize(new java.awt.Dimension(25, 22));
    sourceHeader.setPreferredSize(new java.awt.Dimension(50, 22));
    sourceHeader.setOpaque(true);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 0.5;
    gridBagConstraints.insets = new java.awt.Insets(4, 2, 4, 2);
    add(sourceHeader, gridBagConstraints);

    jSplitPane1.setDividerLocation(100);
    jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
    jSplitPane1.setTopComponent(mapperPanel);

    optionsPanel.setLayout(new java.awt.GridBagLayout());

    jPanel2.setLayout(new java.awt.GridBagLayout());

    sqlPanel.setLayout(new java.awt.GridBagLayout());

    wherePanel.setLayout(new java.awt.BorderLayout());

    sqlEditorLabel.setText(ResourceMgr.getString("LabelDPAdditionalWhere"));
    wherePanel.add(sqlEditorLabel, java.awt.BorderLayout.NORTH);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.gridheight = 7;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 0, 4, 1);
    sqlPanel.add(wherePanel, gridBagConstraints);

    useQueryCbx.setText(ResourceMgr.getString("LabelDPUseSQLSource"));
    useQueryCbx.setToolTipText(ResourceMgr.getString("LabelDPUseSQLSource"));
    useQueryCbx.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    useQueryCbx.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 0.8;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 0, 2);
    sqlPanel.add(useQueryCbx, gridBagConstraints);

    checkQueryButton.setText(ResourceMgr.getString("LabelDPCheckQuery"));
    checkQueryButton.setToolTipText(ResourceMgr.getDescription("LabelDPCheckQuery"));
    checkQueryButton.setBorder(new javax.swing.border.EtchedBorder());
    checkQueryButton.setEnabled(false);
    checkQueryButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
    checkQueryButton.setMaximumSize(new java.awt.Dimension(200, 24));
    checkQueryButton.setMinimumSize(new java.awt.Dimension(120, 24));
    checkQueryButton.setPreferredSize(new java.awt.Dimension(130, 24));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 0.2;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
    sqlPanel.add(checkQueryButton, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 0.3;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
    jPanel2.add(sqlPanel, gridBagConstraints);

    updateOptionPanel.setLayout(new java.awt.GridBagLayout());

    commitLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    commitLabel.setText(ResourceMgr.getString("LabelDPCommitEvery"));
    commitLabel.setToolTipText(ResourceMgr.getDescription("LabelDPCommitEvery"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 8, 0, 0);
    updateOptionPanel.add(commitLabel, gridBagConstraints);

    commitEvery.setColumns(5);
    commitEvery.setText("\n");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
    updateOptionPanel.add(commitEvery, gridBagConstraints);

    continueOnErrorCbx.setText(ResourceMgr.getString("MsgDPContinueOnError"));
    continueOnErrorCbx.setToolTipText(ResourceMgr.getDescription("MsgDPContinueOnError"));
    continueOnErrorCbx.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
    updateOptionPanel.add(continueOnErrorCbx, gridBagConstraints);

    deleteTargetCbx.setText(ResourceMgr.getString("LabelDeleteTargetTable"));
    deleteTargetCbx.setToolTipText(ResourceMgr.getDescription("LabelDeleteTargetTable"));
    deleteTargetCbx.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(6, 5, 0, 0);
    updateOptionPanel.add(deleteTargetCbx, gridBagConstraints);

    dropTargetCbx.setText(ResourceMgr.getString("LabelDPDropTable"));
    dropTargetCbx.setToolTipText(ResourceMgr.getDescription("LabelDPDropTable"));
    dropTargetCbx.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    dropTargetCbx.setEnabled(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
    updateOptionPanel.add(dropTargetCbx, gridBagConstraints);

    modeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "insert", "update", "insert,update", "update,insert" }));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 0.1;
    gridBagConstraints.insets = new java.awt.Insets(3, 4, 0, 0);
    updateOptionPanel.add(modeComboBox, gridBagConstraints);

    modeLabel.setText(ResourceMgr.getString("LabelDPMode"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 8, 0, 0);
    updateOptionPanel.add(modeLabel, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.weighty = 1.0;
    updateOptionPanel.add(jPanel1, gridBagConstraints);

    jLabel1.setText(ResourceMgr.getString("LabelDPUpdateOptions"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(0, 7, 0, 5);
    updateOptionPanel.add(jLabel1, gridBagConstraints);

    useBatchCheckBox.setText(ResourceMgr.getString("LabelUseBatchUpdate"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
    updateOptionPanel.add(useBatchCheckBox, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 0.5;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
    jPanel2.add(updateOptionPanel, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    optionsPanel.add(jPanel2, gridBagConstraints);

    jSplitPane1.setRightComponent(optionsPanel);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
    add(jSplitPane1, gridBagConstraints);

    statusLabel.setBorder(new javax.swing.border.EtchedBorder());
    statusLabel.setMaximumSize(new java.awt.Dimension(32768, 24));
    statusLabel.setMinimumSize(new java.awt.Dimension(4, 24));
    statusLabel.setPreferredSize(new java.awt.Dimension(4, 24));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
    add(statusLabel, gridBagConstraints);

    buttonPanel.setLayout(new java.awt.GridBagLayout());

    jPanel3.setLayout(new java.awt.GridBagLayout());

    startButton.setText(ResourceMgr.getString("LabelStartDataPumper"));
    startButton.setEnabled(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
    jPanel3.add(startButton, gridBagConstraints);

    cancelButton.setText(ResourceMgr.getString("LabelCancelCopy"));
    cancelButton.setEnabled(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
    jPanel3.add(cancelButton, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    buttonPanel.add(jPanel3, gridBagConstraints);

    jPanel4.setLayout(new java.awt.GridBagLayout());

    showLogButton.setText(ResourceMgr.getString("LabelShowDataPumperLog"));
    showLogButton.setEnabled(false);
    jPanel4.add(showLogButton, new java.awt.GridBagConstraints());

    showWbCommand.setText(ResourceMgr.getString("LabelShowWbCopyCommand"));
    showWbCommand.setToolTipText(ResourceMgr.getDescription("LabelShowWbCopyCommand"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
    jPanel4.add(showWbCommand, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 0.4;
    buttonPanel.add(jPanel4, gridBagConstraints);

    jPanel5.setLayout(new java.awt.GridBagLayout());

    helpButton.setText(ResourceMgr.getString("LabelHelp"));
    helpButton.setToolTipText("");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    jPanel5.add(helpButton, gridBagConstraints);

    closeButton.setText(ResourceMgr.getString("LabelClose"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(0, 11, 0, 2);
    jPanel5.add(closeButton, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.weightx = 0.2;
    buttonPanel.add(jPanel5, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(9, 0, 8, 0);
    add(buttonPanel, gridBagConstraints);

  }
  // </editor-fold>//GEN-END:initComponents


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JPanel buttonPanel;
  private javax.swing.JButton cancelButton;
  private javax.swing.JButton checkQueryButton;
  private javax.swing.JButton closeButton;
  private javax.swing.JTextField commitEvery;
  private javax.swing.JLabel commitLabel;
  private javax.swing.JCheckBox continueOnErrorCbx;
  private javax.swing.JCheckBox deleteTargetCbx;
  private javax.swing.JCheckBox dropTargetCbx;
  private javax.swing.JButton helpButton;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JPanel jPanel2;
  private javax.swing.JPanel jPanel3;
  private javax.swing.JPanel jPanel4;
  private javax.swing.JPanel jPanel5;
  private javax.swing.JSplitPane jSplitPane1;
  private javax.swing.JPanel mapperPanel;
  private javax.swing.JComboBox modeComboBox;
  private javax.swing.JLabel modeLabel;
  private javax.swing.JButton openFileButton;
  private javax.swing.JPanel optionsPanel;
  private javax.swing.JButton selectSourceButton;
  private javax.swing.JButton selectTargetButton;
  private javax.swing.JButton showLogButton;
  private javax.swing.JButton showWbCommand;
  private javax.swing.JLabel sourceHeader;
  private javax.swing.JLabel sourceProfileLabel;
  private javax.swing.JPanel sourceProfilePanel;
  private workbench.gui.tools.TableSelectorPanel sourceTable;
  private javax.swing.JLabel sqlEditorLabel;
  private javax.swing.JPanel sqlPanel;
  private javax.swing.JButton startButton;
  private javax.swing.JLabel statusLabel;
  private javax.swing.JLabel targetHeader;
  private javax.swing.JLabel targetProfileLabel;
  private javax.swing.JPanel targetProfilePanel;
  private workbench.gui.tools.TableSelectorPanel targetTable;
  private javax.swing.JPanel updateOptionPanel;
  private javax.swing.JCheckBox useBatchCheckBox;
  private javax.swing.JCheckBox useQueryCbx;
  private javax.swing.JPanel wherePanel;
  // End of variables declaration//GEN-END:variables

	public void showWindow(MainWindow aParent)
	{
//		this.mainWindow = aParent;
		this.window  = new JFrame(ResourceMgr.getString("TxtWindowTitleDataPumper"))
		{
			public void setVisible(boolean visible)
			{
				if (!visible) saveSettings();
				super.setVisible(visible);
			}
		};

		this.window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.window.setIconImage(ResourceMgr.getImage("DataPumper").getImage());
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
		SwingUtilities.invokeLater(new Runnable()
		{
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
			this.targetProfileLabel.setIcon(ResourceMgr.getPicture("wait"));

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
			this.sourceProfileLabel.setIcon(ResourceMgr.getPicture("wait"));

			this.sourceTable.removeChangeListener();
			this.sourceConnection.disconnect();
			this.sourceTable.setConnection(null);
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
				this.useBatchCheckBox.setEnabled(this.supportsBatch);
			}
			else
			{
				this.useBatchCheckBox.setEnabled(false);
				this.useBatchCheckBox.setSelected(false);
			}
		}
		else
		{
			this.useBatchCheckBox.setEnabled(false);
			this.useBatchCheckBox.setSelected(false);
		}
	}
	private void showHelp()
	{
		HtmlViewer viewer = new HtmlViewer(this.window, "data-pumper.html");
		viewer.showDataPumperHelp();
	}
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
			this.showWbCommand();
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
		else if (e.getSource() == this.modeComboBox)
		{
			this.checkUseBatch();
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
		//this.checkQueryButton.setEnabled(useQuery);

		boolean isCopy = (this.fileImporter == null);
		this.sqlEditor.setEnabled(isCopy);
		this.checkQueryButton.setEnabled(isCopy && useQuery);
		
		this.useQueryCbx.setVisible(isCopy);
		this.useQueryCbx.setEnabled(isCopy);
		this.sqlEditor.setVisible(isCopy);
		this.checkQueryButton.setVisible(isCopy);
		this.sqlEditorLabel.setVisible(isCopy);
		//this.sqlPanel.setVisible(isCopy);
		
		//this.targetTable.allowNewTable(!useQuery && allowCreateTable);
		this.targetTable.allowNewTable(allowCreateTable);
		if (useQuery)
		{
			this.sqlEditorLabel.setText(ResourceMgr.getString("LabelDPQueryText"));
		}
		else
		{
			this.sqlEditorLabel.setText(ResourceMgr.getString("LabelDPAdditionalWhere"));
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
		String sql = this.sqlEditor.getText();
		if (sql != null && sql.length() > 0)
		{
			sql = SqlUtil.makeCleanSql(sql, false).toLowerCase();
			return sql.startsWith("select");
		}
		return false;
	}

	public void windowActivated(java.awt.event.WindowEvent e)
	{
	}

	public void windowClosed(java.awt.event.WindowEvent e)
	{
	}

	public void windowClosing(java.awt.event.WindowEvent e)
	{
		if (this.copyRunning) 
		{
			this.cancelCopy();
		}
		this.closeWindow();
	}

	public void closeWindow()
	{
		this.done();
		if (this.window != null)
		{
			this.window.removeWindowListener(this);
			this.window.dispose();
		}
	}

	public void activateWindow()
	{
		if (this.window != null)
		{
			this.window.setVisible(true);
			this.window.toFront();
		}
	}
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
			public void run()
			{
				disconnect();
				unregister();
			}
		};
		t.start();
	}

	private void unregister()
	{
		WbManager.getInstance().unregisterToolWindow(this);
	}


	public void windowDeactivated(java.awt.event.WindowEvent e)
	{
	}

	public void windowDeiconified(java.awt.event.WindowEvent e)
	{
	}

	public void windowIconified(java.awt.event.WindowEvent e)
	{
	}

	public void windowOpened(java.awt.event.WindowEvent e)
	{
	}

	/**
	 *	We have registered with both table selectors to be informed
	 *	when the user changes the selection. After each change (and
	 *	we don't actually care where it came from) the tables are
	 *	checked, and if both are present, we'll initialize the
	 *	ColumnMapper
	 */
	public void propertyChange(java.beans.PropertyChangeEvent evt)
	{
		TableIdentifier theTarget = this.targetTable.getSelectedTable();
		TableIdentifier source = this.sourceTable.getSelectedTable();

		if (evt.getSource() == this.sourceTable && source != null)
		{
			if (theTarget != null && theTarget.isNewTable())
			{
				this.targetTable.resetNewTableItem();
				theTarget = null;
			}
			if (theTarget == null)
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
					theTarget.setTable(name);
					this.targetTable.repaint();
				}
			}
		}

		if (this.hasSource() && theTarget != null)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
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
	private List getResultSetColumns()
	{
		if (this.sourceConnection == null) return null;
		String sql = this.sqlEditor.getText();

		List result = null;

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

	private List getKeyColumns()
	{
		ColumnMapper.MappingDefinition colMapping = this.columnMapper.getMapping();
		if (colMapping == null) return Collections.EMPTY_LIST;
		int count = colMapping.targetColumns.length;
		List keys = new ArrayList();

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
		try
		{
			this.initImporter();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		String sql = this.fileImporter.getWbCommand();
		EditWindow w = new EditWindow(this.window, ResourceMgr.getString("MsgWindowTitleDPScript"), sql, "workbench.datapumper.scriptwindow", true);
		w.setVisible(true);
		w.dispose();
		
	}
	private void showWbCommand()
	{
		if (this.fileImporter != null)
		{
			this.showImportCommand();
			return;
		}
		if (this.sourceProfile == null || this.targetProfile == null) return;
		if (!this.hasSource()) return;

		StringBuffer result = new StringBuffer(150);
		result.append(WbCopy.VERB + " -" + WbCopy.PARAM_SOURCEPROFILE + "=");
		String s = this.sourceProfile.getName();
		if (s.indexOf(' ') >-1) result.append('"');
		result.append(s);
		if (s.indexOf(' ') >-1) result.append('"');

		s = this.targetProfile.getName();
		result.append("\n     -" + WbCopy.PARAM_TARGETPROFILE + "=");
		if (s.indexOf(' ') >-1) result.append('"');
		result.append(s);
		if (s.indexOf(' ') >-1) result.append('"');

		TableIdentifier id = this.targetTable.getSelectedTable();
		if (targetProfile == null) return;

		if (id.isNewTable())
			s = id.getTableName();
		else
			s = id.getTableExpression();
		result.append("\n     -" + WbCopy.PARAM_TARGETTABLE + "=");
		result.append(s);

		if (id.isNewTable())
		{
			result.append("\n     -" + WbCopy.PARAM_CREATETARGET + "=true");
			if (this.dropTargetCbx.isSelected())
			{
				result.append("\n     -" + WbCopy.PARAM_DROPTARGET + "=true");
			}
		}

		ColumnMapper.MappingDefinition colMapping = this.columnMapper.getMapping();
		if (colMapping == null) return;
		int count = colMapping.targetColumns.length;

		if (this.useQueryCbx.isSelected())
		{
			String sql = this.sqlEditor.getText();
			result.append("\n     -" + WbCopy.PARAM_SOURCEQUERY + "=\"");
			result.append(sql);
			result.append('"');

			result.append("\n     -"+ WbCopy.PARAM_COLUMNS + "=\"");
			for (int i=0; i < count; i++)
			{
				if (i > 0) result.append(", ");
				result.append(colMapping.targetColumns[i].getColumnName());
			}
			result.append('"');
		}
		else
		{
			id = this.sourceTable.getSelectedTable();
			if (id == null) return;
			s = id.getTableExpression();
			result.append("\n     -" + WbCopy.PARAM_SOURCETABLE + "=");
			if (s.indexOf(' ') > -1) result.append('"');
			result.append(s);
			if (s.indexOf(' ') > -1) result.append('"');

			s = sqlEditor.getText();
			if (s != null && s.length() > 0)
			{
				result.append("\n     -" + WbCopy.PARAM_SOURCEWHERE + "=\"");
				result.append(s);
				result.append('"');
			}

			result.append("\n     -" + WbCopy.PARAM_COLUMNS + "=\"");
			for (int i=0; i < count; i++)
			{
				if (i > 0) result.append(", ");
				result.append(colMapping.sourceColumns[i].getColumnName());
				result.append("/");
				ColumnIdentifier col = colMapping.targetColumns[i];
				result.append(col.getColumnName());
			}
			result.append('"');
		}

		String mode = (String)this.modeComboBox.getSelectedItem();
		if (!"insert".equals(mode))
		{
			result.append("\n     -" + WbCopy.PARAM_MODE + "=" + mode);
			List keys = this.getKeyColumns();
			if (keys.size() > 0)
			{
				int keycount = keys.size();
				result.append("\n     -" + WbCopy.PARAM_KEYS + "=");
				for (int i=0; i < keycount; i++)
				{
					if (i > 0) result.append(",");
					result.append(keys.get(i).toString());
				}
			}
		}

		result.append("\n     -" + WbCopy.PARAM_DELETETARGET + "=");
		result.append(Boolean.toString(this.deleteTargetCbx.isSelected()));

		result.append("\n     -" + WbCopy.PARAM_CONTINUE + "=");
		result.append(Boolean.toString(this.continueOnErrorCbx.isSelected()));

		int commit = StringUtil.getIntValue(this.commitEvery.getText(), -1);
		if (commit > 0)
		{
			result.append("\n     -" + WbCopy.PARAM_COMMITEVERY + "=");
			result.append(commit);
		}

		if (this.useBatchCheckBox.isEnabled() && this.useBatchCheckBox.isSelected())
		{
			result.append("\n     -" + WbCopy.PARAM_USEBATCH + "=true");
		}

		result.append("\n;");

		EditWindow w = new EditWindow(this.window, ResourceMgr.getString("MsgWindowTitleDPScript"), result.toString(), "workbench.datapumper.scriptwindow", true);
		w.setVisible(true);
		w.dispose();
	}

	private void initColumnMapper()
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
			List sourceCols = null;
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
			this.dropTargetCbx.setEnabled(newTable);
			if (newTable)
			{
				this.columnMapper.defineColumns(sourceCols, sourceCols, false);
			}
			else
			{
				List targetCols = this.targetConnection.getMetadata().getTableColumns(target);
				boolean syncDataTypes = (this.fileImporter != null);
				this.columnMapper.defineColumns(sourceCols, targetCols, syncDataTypes);
			}

			this.columnMapper.setAllowSourceEditing(!useQuery && !newTable);
			this.startButton.setEnabled(true);
			this.showWbCommand.setEnabled(true);
		}
		catch (Exception e)
		{
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
			public void run()
			{
				doCancel();
			}
		};
		t.start();
	}
	
	private void doCancel()
	{
		if (copier != null) copier.cancel();
		EventQueue.invokeLater(new Runnable()
		{
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
		List cols = columnMapper.getMappingForImport();
		this.fileImporter.setTargetTable(this.targetTable.getSelectedTable());
		this.fileImporter.setImportColumns(cols);
	}
	
	private void startCopy()
	{
		if (this.targetConnection == null || (this.sourceConnection == null && this.fileImporter == null)) return;
		
		if (this.columnMapper == null) return;

		TableIdentifier ttable = this.targetTable.getSelectedTable();

		ColumnMapper.MappingDefinition colMapping = this.columnMapper.getMapping();

		if (!this.createCopier()) return;
		
		try
		{

			if (this.fileImporter != null)
			{
				this.initImporter();
				this.copier.copyFromFile(this.fileImporter.getProducer(), this.targetConnection, this.targetTable.getSelectedTable());
				int interval = DataImporter.estimateReportIntervalFromFileSize(this.fileImporter.getSourceFilename());
				this.copier.setReportInterval(interval);
			}
			else if (this.useQueryCbx.isSelected())
			{
				this.copier.copyFromQuery(this.sourceConnection, this.targetConnection, this.sqlEditor.getText(), ttable, colMapping.targetColumns);
			}
			else
			{
				boolean ignoreSelect = false;
				String where = null;
				TableIdentifier stable = this.sourceTable.getSelectedTable();
				if (this.isSelectQuery())
				{
					WbSwingUtilities.showErrorMessage(this, ResourceMgr.getString("MsgDPIgnoreSelect"));
					ignoreSelect = true;
				}
				if (!ignoreSelect) where = this.sqlEditor.getText();
				
				if (ttable.isNewTable())
				{
					boolean dropTable = this.dropTargetCbx.isSelected();
					Map mapping = new HashMap();
					int count = colMapping.sourceColumns.length;
					for (int i=0; i < count; i++)
					{
						mapping.put(colMapping.sourceColumns[i].getColumnName(), colMapping.targetColumns[i].getColumnName());
					}

					this.copier.copyFromTable(this.sourceConnection, this.targetConnection, stable, ttable, mapping, where, true, dropTable);
				}
				else
				{
					this.copier.copyFromTable(this.sourceConnection, this.targetConnection, stable, ttable, colMapping.sourceColumns, colMapping.targetColumns, where);
				}
			}
			this.copier.startBackgroundCopy();
			this.copyRunning = true;
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

	private boolean createCopier()
	{
		ColumnMapper.MappingDefinition colMapping = this.columnMapper.getMapping();
		if (colMapping == null) return false;
		
		this.copier = new DataCopier();
		this.copier.setDeleteTarget(this.deleteTargetCbx.isSelected());
		this.copier.setContinueOnError(this.continueOnErrorCbx.isSelected());
		String mode = (String)this.modeComboBox.getSelectedItem();
		List keys = this.getKeyColumns();
		
		this.copier.setKeyColumns(keys);
		if (mode.indexOf("update") > -1 && keys.size() == 0)
		{
			WbSwingUtilities.showErrorMessage(this, ResourceMgr.getString("ErrorDPNoKeyColumns"));
			return false;
		}
		if (keys.size() == colMapping.targetColumns.length && mode.indexOf("update") > -1)
		{
			WbSwingUtilities.showErrorMessage(this, ResourceMgr.getString("ErrorDPUpdateOnlyKeyColumns"));
			return false;
		}
		this.copier.setMode(mode);
		int commit = StringUtil.getIntValue(this.commitEvery.getText(), -1);
		this.copier.setCommitEvery(commit);
		this.copier.setUseBatch(this.useBatchCheckBox.isSelected());
		this.copier.setRowActionMonitor(this);
		this.copier.setReportInterval(10);
		return true;
	}
	
	public void setCurrentObject(String object, long currentRow, long total)
	{
		updateMonitor(currentRow);
	}

	public void setCurrentRow(long currentRow, long totalRows)
	{
		updateMonitor(currentRow);
	}

	private void updateMonitor(long currentRow)
	{
		if (currentRow == 1) this.updateWindowTitle();
		this.statusLabel.setText(this.copyMsg + " " + currentRow);
		this.statusLabel.repaint();
	}
	
	public void saveCurrentType(String type) {}
	public void restoreType(String type) {}
	public int getMonitorType() { return RowActionMonitor.MONITOR_PLAIN; }
	public void setMonitorType(int aType) {}

	public void jobFinished()
	{
		this.copyRunning = false;
		if (this.copier.isSuccess())
		{

			String msg = this.copier.getRowsInsertedMessage();
			String msg2 = this.copier.getRowsUpdatedMessage();
			StringBuffer copied = new StringBuffer(50);
			if (msg != null)
			{
				copied.append(msg);
				if (msg2 != null) copied.append(", " + msg2);
			}
			else if (msg2 != null && msg2.length() > 0)
			{
				copied.append(msg2);
			}
			else
			{
				long rows = this.copier.getAffectedRows();
				copied.append(rows);
				copied.append(" ");
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
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					showLog();
				}
			});
		}
	}

	private void showLog()
	{
		if (this.copier == null)
		{
			return;
		}

		String log = null;
		try
		{
			log = this.copier.getAllMessages();
		}
		catch (Exception e)
		{
			LogMgr.logError("DataPumper.showLog()", "Error when retrieving log information", e);
			log = ExceptionUtil.getDisplay(e);
		}

		EditWindow w = new EditWindow(this.window, ResourceMgr.getString("MsgWindowTitleDPLog"), log, "workbench.datapumper.logwindow");
		w.setVisible(true); // EditWindow is modal
		w.dispose();
	}
}
