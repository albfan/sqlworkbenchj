/*
 * DataPumper.java
 *
 * Created on December 20, 2003, 7:05 PM
 */

package workbench.gui.tools;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeListener;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import workbench.WbManager;
import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionProfile;
import workbench.db.DataCopier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.importer.DataImporter;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.DividerBorder;
import workbench.gui.components.EditWindow;
import workbench.gui.components.WbButton;
import workbench.gui.components.WbSplitPane;
import workbench.gui.help.HtmlViewer;
import workbench.gui.profiles.ProfileSelectionDialog;
import workbench.gui.sql.EditorPanel;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.wbcommands.WbCopy;
import workbench.storage.RowActionMonitor;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  workbench@kellerer.org
 */
public class DataPumper 
	extends JPanel
	implements ActionListener, WindowListener, PropertyChangeListener, RowActionMonitor
{
	private ConnectionProfile source;
	private ConnectionProfile target;
	private WbConnection sourceConnection;
	private WbConnection targetConnection;
	
	private JFrame window;
	private ColumnMapper columnMapper;
	private final String copyMsg = ResourceMgr.getString("MsgCopyingRow");
	private boolean copyRunning = false;
	private DataCopier copier;
	private StringBuffer copyLog;
	private EditorPanel sqlEditor;
	private MainWindow mainWindow;
	boolean allowCreateTable = "true".equals(WbManager.getSettings().getProperty("workbench.datapumper.allowcreate", "false"));

	/** Creates new form DataPumper */
	public DataPumper(ConnectionProfile source, ConnectionProfile target)
	{
		this.source = source;
		this.target = target;
		initComponents();
		this.selectSourceButton.addActionListener(this);
		this.selectTargetButton.addActionListener(this);
		this.closeButton.addActionListener(this);
		this.updateDisplay();
		this.startButton.addActionListener(this);
		this.cancelButton.addActionListener(this);
		this.showLogButton.addActionListener(this);
		this.helpButton.addActionListener(this);
		this.columnMapper = new ColumnMapper();
		this.mapperPanel.setLayout(new BorderLayout());
		this.mapperPanel.add(this.columnMapper, BorderLayout.CENTER);

		this.separatorPanel1.setBorder(new DividerBorder(DividerBorder.MIDDLE));
		this.separatorPanel2.setBorder(new DividerBorder(DividerBorder.MIDDLE));
		this.checkQueryButton.addActionListener(this);
		this.showWbCommand.addActionListener(this);
		this.useQueryCbx.addActionListener(this);
		this.sqlEditor = EditorPanel.createSqlEditor();
		this.wherePanel.add(this.sqlEditor);
		this.showWbCommand.setEnabled(false);
		
		if (!this.allowCreateTable)
		{
			this.dropTargetCbx.setVisible(this.allowCreateTable);
			//this.remove(this.dropTargetCbx);
			GridBagLayout grid = (GridBagLayout)this.buttonPanel.getLayout();
			grid.removeLayoutComponent(this.dropTargetCbx);
			this.buttonPanel.remove(this.dropTargetCbx);
			
			GridBagConstraints cons = grid.getConstraints(this.commitEvery);
			cons.gridy --;
			grid.setConstraints(this.commitEvery, cons);
			
			cons = grid.getConstraints(this.commitLabel);
			cons.gridy--;
			grid.setConstraints(this.commitLabel, cons);
			//grid.layoutContainer(this);
		}
	}
	
	public void saveSettings()
	{
		Settings s = WbManager.getSettings();
		if (this.source != null)
		{
			s.setProperty("workbench.datapumper.source", "lastprofile", this.source.getName());
		}
		if (this.target != null)
		{
			s.setProperty("workbench.datapumper.target", "lastprofile", this.target.getName());
		}
		s.setProperty("workbench.datapumper", "divider", jSplitPane1.getDividerLocation());
		s.setProperty("workbench.datapumper.target", "deletetable", Boolean.toString(this.deleteTargetCbx.isSelected()));
		s.setProperty("workbench.datapumper", "continue", Boolean.toString(this.continueOnErrorCbx.isSelected()));
		s.setProperty("workbench.datapumper", "commitevery", this.commitEvery.getText());
		s.setProperty("workbench.datapumper", "usequery", Boolean.toString(this.useQueryCbx.isSelected()));
		s.setProperty("workbench.datapumper", "droptable", Boolean.toString(this.dropTargetCbx.isSelected()));
		String where = this.sqlEditor.getText();
		if (where != null && where.length() > 0)
		{
			s.setProperty("workbench.datapumper", "where", where);
		}
		else
		{
			s.setProperty("workbench.datapumper", "where", "");
		}
		s.storeWindowSize(this.window, "workbench.datapumper.window");
		s.storeWindowPosition(this.window, "workbench.datapumper.window");
	}
	
	public void restoreSettings()
	{
		Settings s = WbManager.getSettings();
		boolean delete = "true".equals(s.getProperty("workbench.datapumper.target", "deletetable", "false"));
		boolean cont = "true".equals(s.getProperty("workbench.datapumper", "continue", "false"));
		boolean drop = "true".equals(s.getProperty("workbench.datapumper", "droptable", "false"));
		this.deleteTargetCbx.setSelected(delete);
		this.continueOnErrorCbx.setSelected(cont);
		this.dropTargetCbx.setSelected(drop);
		if (!s.restoreWindowSize(this.window, "workbench.datapumper.window"))
		{
			this.window.setSize(640,480);
		}

		int commit = s.getIntProperty("workbench.datapumper", "commitevery", 0);
		if (commit > 0)
		{
			this.commitEvery.setText(Integer.toString(commit));
		}
		String where = s.getProperty("workbench.datapumper", "where", "");
		if (where != null && where.length() > 0)
		{
			this.sqlEditor.setText(where);
		}
		int loc = s.getIntProperty("workbench.datapumper", "divider", -1);
		if (loc == -1)
		{
			loc = (int)this.jSplitPane1.getHeight() / 2;
			if (loc < 10) loc = 100;
		}
		this.jSplitPane1.setDividerLocation(loc);
		boolean useQuery = "true".equals(s.getProperty("workbench.datapumper", "usequery", "false"));
		this.useQueryCbx.setSelected(useQuery);
		// initialize the depending controls for the usage of a SQL query
		this.checkType();
	}

	private void updateTargetDisplay()
	{
		String label = ResourceMgr.getString("LabelDPTargetProfile");
		if (this.target != null)
		{
			this.targetProfileLabel.setText(label + ": " + this.target.getName());
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
		if (this.source != null)
		{
			this.sourceProfileLabel.setText(label + ": " + this.source.getName());
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
		if (this.target != null && this.source != null && this.window != null)
		{
			String title = ResourceMgr.getString("TxtWindowTitleDataPumper");
			title = title + " [" + this.source.getName() + " -> " + this.target.getName() + "]";
			if (this.copier != null && this.copyRunning)
			{
				title = "» " + title; 
			}
			this.window.setTitle(title);
		}
	}
	
	private void checkConnections()
	{
		this.connectSource();
		this.connectTarget();
	}
	
	private void connectSource()
	{
		if (this.source == null) return;
		
		String label = ResourceMgr.getString("MsgConnectingTo") + " " + this.source.getName() + " ...";
		this.sourceProfileLabel.setIcon(ResourceMgr.getPicture("wait"));
		this.sourceProfileLabel.setText(label);
		Thread t = new Thread()
		{
			public void run()
			{
				doConnectSource();
			}
		};
		t.setName("DataPumper source connection");
		t.setDaemon(true);
		t.start();
	}
	
	private void doConnectSource()
	{
		this.disconnectSource();
		try
		{
			this.sourceConnection = WbManager.getInstance().getConnectionMgr().getConnection(this.source, "Dp-Source");
		}
		catch (Exception e)
		{
			LogMgr.logError("DataPumper.doConnectSource()", "Error when connecting to profile: " + this.source.getName(), e);
			String msg = ResourceMgr.getString("ErrorConnectionError") + "\n" + e.getMessage();
			this.source = null;
			WbManager.getInstance().showErrorMessage(this, msg);
		}
		finally
		{
			this.sourceProfileLabel.setIcon(null);
			this.updateSourceDisplay();
		}
		
		if (this.sourceConnection != null)
		{
			this.sourceTable.setChangeListener(this, "source-table");
			
		  Thread t = new Thread()
		  {
			  public void run()
			  {
				  sourceTable.setConnection(sourceConnection);
			  }
		  };
		  t.setName("Retrieve source tables");
		  t.setDaemon(true);
		  t.start();
		}
	}
	
	private void connectTarget()
	{
		if (this.target == null) return;
		
		String label = ResourceMgr.getString("MsgConnectingTo") + " " + this.target.getName() + " ...";
		this.targetProfileLabel.setText(label);
		this.targetProfileLabel.setIcon(ResourceMgr.getPicture("wait"));
		Thread t = new Thread()
		{
			public void run()
			{
				doConnectTarget();
			}
		};
		t.setName("DataPumper target connection");
		t.setDaemon(true);
		t.start();
	}
	
	private void doConnectTarget()
	{
		this.disconnectTarget();
		
		try
		{
			this.targetConnection = WbManager.getInstance().getConnectionMgr().getConnection(this.target, "Dp-Target");
		}
		catch (Exception e)
		{
			LogMgr.logError("DataPumper.doConnectSource()", "Error when connecting to profile: " + this.target.getName(), e);
			String msg = ResourceMgr.getString("ErrorConnectionError") + "\n" + e.getMessage();
			this.target = null;
			WbManager.getInstance().showErrorMessage(this, msg);
		}
		finally
		{
			this.targetProfileLabel.setIcon(null);
			this.updateTargetDisplay();
		}
		
		if (this.targetConnection != null)
		{
			this.targetTable.setChangeListener(this, "target-table");
			
		  Thread t = new Thread()
		  {
			  public void run()
			  {
				  targetTable.setConnection(targetConnection);
			  }
		  };
		  t.setName("Retrieve target tables");
		  t.setDaemon(true);
		  t.start();
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
  private void initComponents()//GEN-BEGIN:initComponents
  {
    java.awt.GridBagConstraints gridBagConstraints;

    sourceProfilePanel = new javax.swing.JPanel();
    sourceProfileLabel = new javax.swing.JLabel();
    selectSourceButton = new javax.swing.JButton();
    targetProfilePanel = new javax.swing.JPanel();
    targetProfileLabel = new javax.swing.JLabel();
    selectTargetButton = new javax.swing.JButton();
    sourceTable = new workbench.gui.tools.TableSelectorPanel();
    targetTable = new workbench.gui.tools.TableSelectorPanel();
    targetHeader = new javax.swing.JLabel();
    sourceHeader = new javax.swing.JLabel();
    closeButton = new javax.swing.JButton();
    jSplitPane1 = new WbSplitPane();
    mapperPanel = new javax.swing.JPanel();
    buttonPanel = new javax.swing.JPanel();
    startButton = new WbButton();
    deleteTargetCbx = new javax.swing.JCheckBox();
    showLogButton = new javax.swing.JButton();
    continueOnErrorCbx = new javax.swing.JCheckBox();
    separatorPanel1 = new javax.swing.JPanel();
    wherePanel = new javax.swing.JPanel();
    sqlEditorLabel = new javax.swing.JLabel();
    separatorPanel2 = new javax.swing.JPanel();
    commitEvery = new javax.swing.JTextField();
    commitLabel = new javax.swing.JLabel();
    cancelButton = new javax.swing.JButton();
    checkQueryButton = new javax.swing.JButton();
    useQueryCbx = new javax.swing.JCheckBox();
    showWbCommand = new javax.swing.JButton();
    dropTargetCbx = new javax.swing.JCheckBox();
    helpButton = new javax.swing.JButton();
    statusLabel = new javax.swing.JLabel();

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

    closeButton.setText(ResourceMgr.getString("LabelClose"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
    gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 4);
    add(closeButton, gridBagConstraints);

    jSplitPane1.setDividerLocation(100);
    jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
    jSplitPane1.setTopComponent(mapperPanel);

    buttonPanel.setLayout(new java.awt.GridBagLayout());

    startButton.setText(ResourceMgr.getString("LabelStartDataPumper"));
    startButton.setEnabled(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 7;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 1);
    buttonPanel.add(startButton, gridBagConstraints);

    deleteTargetCbx.setText(ResourceMgr.getString("LabelDeleteTargetTable"));
    deleteTargetCbx.setToolTipText(ResourceMgr.getDescription("LabelDeleteTargetTable"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
    buttonPanel.add(deleteTargetCbx, gridBagConstraints);

    showLogButton.setText(ResourceMgr.getString("LabelShowDataPumperLog"));
    showLogButton.setEnabled(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 7;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 1);
    buttonPanel.add(showLogButton, gridBagConstraints);

    continueOnErrorCbx.setText(ResourceMgr.getString("MsgDPContinueOnError"));
    continueOnErrorCbx.setToolTipText(ResourceMgr.getDescription("MsgDPContinueOnError"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    buttonPanel.add(continueOnErrorCbx, gridBagConstraints);

    separatorPanel1.setMaximumSize(new java.awt.Dimension(32767, 10));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
    buttonPanel.add(separatorPanel1, gridBagConstraints);

    wherePanel.setLayout(new java.awt.BorderLayout());

    sqlEditorLabel.setText(ResourceMgr.getString("LabelDPAdditionalWhere"));
    wherePanel.add(sqlEditorLabel, java.awt.BorderLayout.NORTH);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.gridheight = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 0, 4, 1);
    buttonPanel.add(wherePanel, gridBagConstraints);

    separatorPanel2.setMaximumSize(new java.awt.Dimension(32767, 10));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 6;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
    buttonPanel.add(separatorPanel2, gridBagConstraints);

    commitEvery.setColumns(5);
    commitEvery.setText("\n");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
    buttonPanel.add(commitEvery, gridBagConstraints);

    commitLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    commitLabel.setText(ResourceMgr.getString("LabelDPCommitEvery"));
    commitLabel.setToolTipText(ResourceMgr.getDescription("LabelDPCommitEvery"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 0, 0);
    buttonPanel.add(commitLabel, gridBagConstraints);

    cancelButton.setText(ResourceMgr.getString("LabelCancelCopy"));
    cancelButton.setEnabled(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 7;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 1);
    buttonPanel.add(cancelButton, gridBagConstraints);

    checkQueryButton.setText(ResourceMgr.getString("LabelDPCheckQuery"));
    checkQueryButton.setToolTipText(ResourceMgr.getDescription("LabelDPCheckQuery"));
    checkQueryButton.setBorder(new javax.swing.border.EtchedBorder());
    checkQueryButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
    checkQueryButton.setMaximumSize(new java.awt.Dimension(200, 24));
    checkQueryButton.setMinimumSize(new java.awt.Dimension(120, 24));
    checkQueryButton.setPreferredSize(new java.awt.Dimension(130, 24));
    checkQueryButton.setEnabled(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 0.2;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
    buttonPanel.add(checkQueryButton, gridBagConstraints);

    useQueryCbx.setText(ResourceMgr.getString("LabelDPUseSQLSource"));
    useQueryCbx.setToolTipText(ResourceMgr.getString("LabelDPUseSQLSource"));
    useQueryCbx.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    useQueryCbx.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 0.8;
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
    buttonPanel.add(useQueryCbx, gridBagConstraints);

    showWbCommand.setText(ResourceMgr.getString("LabelShowWbCopyCommand"));
    showWbCommand.setToolTipText(ResourceMgr.getDescription("LabelShowWbCopyCommand"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 7;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 4, 1);
    buttonPanel.add(showWbCommand, gridBagConstraints);

    dropTargetCbx.setText(ResourceMgr.getString("LabelDPDropTable"));
    dropTargetCbx.setToolTipText(ResourceMgr.getDescription("LabelDPDropTable"));
    dropTargetCbx.setEnabled(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    buttonPanel.add(dropTargetCbx, gridBagConstraints);

    helpButton.setText(ResourceMgr.getString("LabelHelp"));
    helpButton.setToolTipText("");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 7;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 4, 1);
    buttonPanel.add(helpButton, gridBagConstraints);

    jSplitPane1.setBottomComponent(buttonPanel);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
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
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
    add(statusLabel, gridBagConstraints);

  }//GEN-END:initComponents
	
	
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
  private javax.swing.JSplitPane jSplitPane1;
  private javax.swing.JPanel mapperPanel;
  private javax.swing.JButton selectSourceButton;
  private javax.swing.JButton selectTargetButton;
  private javax.swing.JPanel separatorPanel1;
  private javax.swing.JPanel separatorPanel2;
  private javax.swing.JButton showLogButton;
  private javax.swing.JButton showWbCommand;
  private javax.swing.JLabel sourceHeader;
  private javax.swing.JLabel sourceProfileLabel;
  private javax.swing.JPanel sourceProfilePanel;
  private workbench.gui.tools.TableSelectorPanel sourceTable;
  private javax.swing.JLabel sqlEditorLabel;
  private javax.swing.JButton startButton;
  private javax.swing.JLabel statusLabel;
  private javax.swing.JLabel targetHeader;
  private javax.swing.JLabel targetProfileLabel;
  private javax.swing.JPanel targetProfilePanel;
  private workbench.gui.tools.TableSelectorPanel targetTable;
  private javax.swing.JCheckBox useQueryCbx;
  private javax.swing.JPanel wherePanel;
  // End of variables declaration//GEN-END:variables

	public void showWindow(MainWindow aParent)
	{
		this.mainWindow = aParent;
		this.window  = new JFrame(ResourceMgr.getString("TxtWindowTitleDataPumper"))
		{
			public void hide()
			{
				saveSettings();
				super.hide();
			}
		};
		
		this.window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.window.setIconImage(ResourceMgr.getImage("DataPumper").getImage());
		this.window.getContentPane().add(this);
		this.restoreSettings();
		this.window.addWindowListener(this);
		WbManager.getInstance().registerToolWindow(this.window);
		
		if (aParent == null)
		{
			if (!WbManager.getSettings().restoreWindowPosition(this.window, "workbench.datapumper.window"))
			{
				WbSwingUtilities.center(this.window, null);
			}
		}
		else
		{
			WbSwingUtilities.center(this.window, aParent);
		}
		this.window .show();
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
			this.updateSourceDisplay();
			this.sourceProfileLabel.setIcon(null);
		}
	}
	
	private void selectTargetConnection()
	{
		this.target = this.selectConnection("workbench.datapumper.target.lastprofile");
		this.connectTarget();
	}
	
	private void selectSourceConnection()
	{
		this.source = this.selectConnection("workbench.datapumper.source.lastprofile");
		this.connectSource();
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
				WbManager.getSettings().setProperty(lastProfileKey, prof.getName());
			}
			dialog.setVisible(false);
			dialog.dispose();
		}
		catch (Throwable th)
		{
			LogMgr.logError("MainWindow.selectConnection()", "Error during connect", th);
			prof = null;
		}
		return prof;
	}
	
	private void showHelp()
	{
		HtmlViewer viewer = new HtmlViewer(this.window, "data-pumper.html");
		viewer.showDataPumperHelp();
		//WbManager.getInstance().getHelpViewer().showDataPumperHelp();
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
	}

	/**
	 *	Check the controls depending on the state of the useQuery CheckBox
	 */
	private void checkType()
	{
		boolean useQuery = this.useQueryCbx.isSelected();
		this.sourceTable.setEnabled(!useQuery);
		this.checkQueryButton.setEnabled(useQuery);
	
		this.targetTable.allowNewTable(!useQuery && allowCreateTable);
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
	
	public void done()
	{
		this.saveSettings();

		this.source = null;
		this.target = null;
		this.columnMapper.resetData();
		this.columnMapper = null;

		Thread t = new Thread()
		{
			public void run()
			{
				disconnectSource();
				disconnectTarget();
				disconnectDone();
			}
		};
		t.setName("DataPumper disconnect thread");
		t.setDaemon(true);
		t.start();
	}
	
	private void disconnectDone()
	{
		WbManager.getInstance().unregisterToolWindow(this.window);
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
		TableIdentifier target = this.targetTable.getSelectedTable();
		
		if (target != null && target.isNewTable())
		{
			
			String name = target.getTable();
			if (name == null) 
			{
				TableIdentifier source = this.sourceTable.getSelectedTable();
				String def = null;
				if (source != null)
					def = source.getTable();
				
				name = WbSwingUtilities.getUserInput(this, ResourceMgr.getString("TxtEnterNewTableName"), def);
				if (name != null) 
				{ 
					target.setTable(name);
					this.targetTable.repaint();
				}
			}
		}
		
		if (this.hasSource() && target != null)
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
		else
		{
			return (this.sourceTable.getSelectedTable() != null);
		}
	}
	private List getResultSetColumns()
	{
		if (this.sourceConnection == null) return null;
		String sql = this.sqlEditor.getText();
		
		ResultSet rs = null;
		Statement stmt = null;
		ArrayList result = null;
		
		try
		{
			stmt = this.sourceConnection.createStatement();
			stmt.setMaxRows(1);
			rs = stmt.executeQuery(sql);
			ResultSetMetaData meta = rs.getMetaData();
			int count = meta.getColumnCount();
			result = new ArrayList(count);
			for (int i=0; i < count; i++)
			{
				String name = meta.getColumnName(i + 1);
				if (name == null) name = meta.getColumnLabel(i + 1);
				if (name == null) continue;
				
				int type = meta.getColumnType(i + 1);
				ColumnIdentifier col = new ColumnIdentifier(name, type);
				result.add(col);
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("DataPumper", "Error when retrieving ResultSet definition for source SQL", e);
			WbManager.getInstance().showErrorMessage(this, e.getMessage());
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
			try { stmt.close(); } catch (Throwable th) {}
		}
		return result;
	}
	
	private void resetColumnMapper()
	{
		this.columnMapper.resetData();
	}

	private void showWbCommand()
	{
		if (this.source == null || this.target == null) return;
		if (!this.hasSource()) return;
		
		StringBuffer result = new StringBuffer(150);
		result.append("COPY -" + WbCopy.PARAM_SOURCEPROFILE + "=");
		String s = this.source.getName();
		if (s.indexOf(' ') >-1) result.append('"');
		result.append(s);
		if (s.indexOf(' ') >-1) result.append('"');
		
		s = this.target.getName();
		result.append("\n     -" + WbCopy.PARAM_TARGETPROFILE + "=");
		if (s.indexOf(' ') >-1) result.append('"');
		result.append(s);
		if (s.indexOf(' ') >-1) result.append('"');

		TableIdentifier id = this.targetTable.getSelectedTable();
		if (target == null) return;
		
		if (id.isNewTable())
			s = id.getTable();
		else
			s = id.getTableExpression();
		result.append("\n     -" + WbCopy.PARAM_TARGETTABLE + "=");
		result.append(s);
		
		/*
		if (id.isNewTable())
		{
			result.append("\n     -" + WbCopy.PARAM_CREATETARGET + "=true");
			if (this.dropTargetCbx.isSelected())
			{
				result.append("\n     -" + WbCopy.PARAM_DROPTARGET + "=true");
			}
		}
		*/
		
		ColumnMapper.MappingDefinition colMapping = this.columnMapper.getMapping();
		if (colMapping == null) return;
		int count = colMapping.targetColumns.length;
		
		if (this.useQueryCbx.isSelected())
		{
			String sql = SqlUtil.makeCleanSql(this.sqlEditor.getText(), false);
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
				result.append(SqlUtil.makeCleanSql(s, false));
				result.append('"');
			}
			
			result.append("\n     -" + WbCopy.PARAM_COLUMNS + "=\"");
			for (int i=0; i < count; i++)
			{
				if (i > 0) result.append(", ");
				result.append(colMapping.sourceColumns[i].getColumnName());
				result.append("/");
				result.append(colMapping.targetColumns[i].getColumnName());
			}
			result.append('"');
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
		result.append(";");
		
		EditWindow w = new EditWindow(this.window, ResourceMgr.getString("MsgWindowTitleDPScript"), result.toString(), "workbench.datapumper.scriptwindow");
		w.show();
		w.dispose();
	}
	
	private void initColumnMapper()
	{
		if (this.sourceConnection == null || this.targetConnection == null || !this.hasSource()) 
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
				this.columnMapper.defineColumns(sourceCols, sourceCols);
			}
			else
			{
				List targetCols = this.targetConnection.getMetadata().getTableColumns(target);
				this.columnMapper.defineColumns(sourceCols, targetCols);
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
		if (this.copier == null) return;
		this.statusLabel.setText(ResourceMgr.getString("MsgCancellingCopy"));
		this.copier.cancel();
		this.cancelButton.setEnabled(false);
		this.startButton.setEnabled(true);
		this.copyRunning = false;
		this.updateWindowTitle();
		this.statusLabel.setText(ResourceMgr.getString("MsgCopyCancelled"));
	}
	
	private void startCopy()
	{
		if (this.targetConnection == null || this.sourceConnection == null) return;
		if (this.columnMapper == null) return;
		
		TableIdentifier stable = null;
		TableIdentifier ttable = this.targetTable.getSelectedTable();
		
		boolean ignoreSelect = false;
		
		if (!this.useQueryCbx.isSelected())
		{
			stable = this.sourceTable.getSelectedTable();
			if (this.isSelectQuery())
			{
				WbManager.getInstance().showErrorMessage(this, ResourceMgr.getString("MsgDPIgnoreSelect"));
				ignoreSelect = true;
			}
		}
		//if (stable == null || ttable == null) return;
		
		ColumnMapper.MappingDefinition colMapping = this.columnMapper.getMapping();
		if (colMapping == null) return;
		
		try
		{
			this.copier = new DataCopier();
			this.copier.setDeleteTarget(this.deleteTargetCbx.isSelected());
			this.copier.setContinueOnError(this.continueOnErrorCbx.isSelected());
			int commit = StringUtil.getIntValue(this.commitEvery.getText(), -1);
			this.copier.setCommitEvery(commit);
			if (this.useQueryCbx.isSelected())
			{
				this.copier.copyFromQuery(this.sourceConnection, this.targetConnection, this.sqlEditor.getText(), ttable, colMapping.targetColumns);
			}
			else
			{
				String where = null;
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
					this.copier.setSourceTableWhere(where);
					this.copier.copyFromTable(this.sourceConnection, this.targetConnection, stable, ttable, colMapping.sourceColumns, colMapping.targetColumns);
				}
			}			
			this.copier.setRowActionMonitor(this);
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
	
	public void setCurrentRow(int currentRow, int totalRows)
	{
		if (currentRow == 1) this.updateWindowTitle();
		this.statusLabel.setText(this.copyMsg + " " + currentRow);
	}
	
	public void setMonitorType(int aType)
	{
	}
	
	public void jobFinished()
	{
		this.copyRunning = false;
		if (this.copier.isSuccess())
		{
			String copied = this.copier.getAffectedRow() + " " + ResourceMgr.getString("MsgCopyNumRows");
			if (this.copier.hasWarnings())
			{
				this.statusLabel.setText(ResourceMgr.getString("MsgCopyFinishedWithWarning") + " (" + copied + ")");
			}
			else
			{
				this.statusLabel.setText(ResourceMgr.getString("MsgCopyFinishedWithSuccess") + " (" + copied + ")");
			}
		}
		else
		{
			this.statusLabel.setText(ResourceMgr.getString("MsgCopyFinishedWithErrors"));
		}
		
		this.startButton.setEnabled(true);
		if (this.copier.hasWarnings() || !this.copier.isSuccess())
		{
			this.showLogButton.setEnabled(true);
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
		this.updateWindowTitle();
	}

	private void showLog()
	{
		if (this.copier == null)
		{
			return;
		}
		StringBuffer log = new StringBuffer(250);
		
		String errmsg = this.copier.getErrorMessage();
		if (errmsg != null) log.append(errmsg);
		
		
		String s = this.copier.getMessages();
		log.append(s);
		if (s != null) log.append("\n");

		String[] msg = this.copier.getImportWarnings();
		int count = msg.length;
		for (int i=0; i < count; i++)
		{
			log.append(msg[i]);
			log.append("\n");
		}
		msg = this.copier.getImportErrors();
		count = msg.length;
		if (count > 0) log.append("\n");
		for (int i=0; i < count; i++)
		{
			log.append(msg[i]);
			log.append("\n");
		}
		EditWindow w = new EditWindow(this.window, ResourceMgr.getString("MsgWindowTitleDPLog"), log.toString(), "workbench.datapumper.logwindow");
		w.show();
		w.dispose();
	}
}
