/*
 * ObjectDropperUI.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.importer.TableDependencySorter;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.EditWindow;
import workbench.gui.components.NoSelectionModel;
import workbench.gui.components.WbButton;
import workbench.interfaces.ObjectDropper;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.RowActionMonitor;
import workbench.util.ExceptionUtil;
import workbench.util.WbThread;

/**
 *
 * @author  Thomas Kellerer
 */
public class ObjectDropperUI
	extends JPanel
	implements RowActionMonitor, WindowListener
{

	protected JDialog dialog;
	protected boolean cancelled;
	protected boolean running;
	protected ObjectDropper dropper;
	private Thread checkThread;
	private Thread dropThread;

	public ObjectDropperUI(ObjectDropper drop)
	{
		super();
		dropper = drop;
		initComponents();
		if (!dropper.supportsFKSorting())
		{
			checkFKButton.setEnabled(false);
			addMissingTables.setEnabled(false);
			addMissingTables.setSelected(false);
			optionPanel.remove(checkPanel);
			this.remove(statusLabel);
		}
	}

	protected void doDrop()
	{
		if (this.running)
		{
			return;
		}

		try
		{
			this.running = true;
			this.cancelled = false;
			this.dropper.dropObjects();
		}
		catch (Throwable ex)
		{
			final String msg = ex.getMessage();
			WbSwingUtilities.showErrorMessage(dialog, msg);
		}
		finally
		{
			this.running = false;
			this.dropThread = null;
		}

		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				if (cancelled)
				{
					dropButton.setEnabled(true);
				}
				else
				{
					dialog.setVisible(false);
					dialog.dispose();
					dialog = null;
				}
			}
		});
	}

	public boolean dialogWasCancelled()
	{
		return this.cancelled;
	}

	protected void initDisplay()
	{
		List<? extends DbObject> objects = this.dropper.getObjects();
		int numNames = objects.size();

		String[] display = new String[numNames];
		for (int i = 0; i < numNames; i++)
		{
			display[i] = objects.get(i).getObjectType() + " " + objects.get(i).getObjectNameForDrop(dropper.getConnection());
		}
		this.objectList.setListData(display);

		if (!dropper.supportsCascade())
		{
			this.optionPanel.remove(this.checkBoxCascadeConstraints);
			this.checkBoxCascadeConstraints.setSelected(false);
		}
	}

	protected void showScript()
	{
		this.dropper.setCascade(checkBoxCascadeConstraints.isSelected());
		CharSequence script = dropper.getScript();
		final EditWindow w = new EditWindow(this.dialog, ResourceMgr.getString("TxtWindowTitleGeneratedScript"), script.toString(), "workbench.objectdropper.scriptwindow", true);
		w.setVisible(true);
		w.dispose();
	}

	public void showDialog(Frame aParent)
	{
		initDisplay();

		this.dialog = new JDialog(aParent, ResourceMgr.getString("TxtDropObjectsTitle"), true);
		try
		{
			this.dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			this.dialog.addWindowListener(this);
			this.dialog.getContentPane().add(this);
			this.dialog.pack();
			if (this.dialog.getWidth() < 200)
			{
				this.dialog.setSize(200, this.dialog.getHeight());
			}
			WbSwingUtilities.center(this.dialog, aParent);
			this.cancelled = true;
			this.dialog.setVisible(true);
		}
		finally
		{
			if (this.dialog != null)
			{
				this.dialog.dispose();
				this.dialog = null;
			}
		}
	}

	@Override
	public void windowOpened(WindowEvent e)
	{
	}

	@Override
	public void windowClosing(WindowEvent e)
	{
		cancel();
		dialog.setVisible(false);
		dialog.dispose();
		dialog = null;
	}

	@Override
	public void windowClosed(WindowEvent e)
	{
	}

	@Override
	public void windowIconified(WindowEvent e)
	{
	}

	@Override
	public void windowDeiconified(WindowEvent e)
	{
	}

	@Override
	public void windowActivated(WindowEvent e)
	{
	}

	@Override
	public void windowDeactivated(WindowEvent e)
	{
	}

	private void cancel()
	{
		if (!this.running) return;

		this.cancelled = true;
		try
		{
			statusLabel.setText(ResourceMgr.getString("MsgCancelling"));
			dropper.cancel();
		}
		catch (Exception e)
		{
			LogMgr.logError("ObjectDropperUI", "Error when cancelling drop", e);
		}
		finally
		{
			statusLabel.setText("");
		}

		if (dropThread != null)
		{
			try
			{
				dropThread.interrupt();
				dropThread.join(1500);
			}
			catch (Exception e)
			{
				// ignore
			}
		}
		dropButton.setEnabled(true);
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

    mainPanel = new javax.swing.JPanel();
    jScrollPane1 = new javax.swing.JScrollPane();
    objectList = new javax.swing.JList();
    optionPanel = new javax.swing.JPanel();
    checkPanel = new javax.swing.JPanel();
    checkFKButton = new javax.swing.JButton();
    addMissingTables = new javax.swing.JCheckBox();
    showScriptButton = new javax.swing.JButton();
    checkBoxCascadeConstraints = new javax.swing.JCheckBox();
    jPanel1 = new javax.swing.JPanel();
    buttonPanel = new javax.swing.JPanel();
    dropButton = new WbButton();
    cancelButton = new WbButton();
    statusLabel = new javax.swing.JLabel();

    setLayout(new java.awt.GridBagLayout());

    mainPanel.setLayout(new java.awt.BorderLayout(0, 2));

    objectList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
    objectList.setSelectionModel(new NoSelectionModel());
    jScrollPane1.setViewportView(objectList);

    mainPanel.add(jScrollPane1, java.awt.BorderLayout.CENTER);

    optionPanel.setLayout(new java.awt.GridBagLayout());

    checkPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));

    checkFKButton.setText(ResourceMgr.getString("LblCheckFKDeps"));
    checkFKButton.setToolTipText(ResourceMgr.getDescription("LblCheckFKDeps"));
    checkFKButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        checkFKButtonActionPerformed(evt);
      }
    });
    checkPanel.add(checkFKButton);

    addMissingTables.setSelected(true);
    addMissingTables.setText(ResourceMgr.getString("LblIncFkTables"));
    addMissingTables.setToolTipText(ResourceMgr.getDescription("LblIncFkTables"));
    addMissingTables.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 0, 0));
    checkPanel.add(addMissingTables);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 6, 0);
    optionPanel.add(checkPanel, gridBagConstraints);

    showScriptButton.setText(ResourceMgr.getString("LblShowScript"));
    showScriptButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        showScriptButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 3, 0);
    optionPanel.add(showScriptButton, gridBagConstraints);

    checkBoxCascadeConstraints.setText(ResourceMgr.getString("LblCascadeConstraints"));
    checkBoxCascadeConstraints.setToolTipText(ResourceMgr.getDescription("LblCascadeConstraints"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 3, 3, 0);
    optionPanel.add(checkBoxCascadeConstraints, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    optionPanel.add(jPanel1, gridBagConstraints);

    mainPanel.add(optionPanel, java.awt.BorderLayout.SOUTH);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    add(mainPanel, gridBagConstraints);

    buttonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

    dropButton.setText(ResourceMgr.getString("LblDrop"));
    dropButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        dropButtonActionPerformed(evt);
      }
    });
    buttonPanel.add(dropButton);

    cancelButton.setText(ResourceMgr.getString("LblCancel"));
    cancelButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        cancelButtonActionPerformed(evt);
      }
    });
    buttonPanel.add(cancelButton);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
    gridBagConstraints.weightx = 1.0;
    add(buttonPanel, gridBagConstraints);

    statusLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
    statusLabel.setMaximumSize(new java.awt.Dimension(32768, 24));
    statusLabel.setMinimumSize(new java.awt.Dimension(150, 24));
    statusLabel.setPreferredSize(new java.awt.Dimension(150, 24));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    add(statusLabel, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents

	private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancelButtonActionPerformed
	{//GEN-HEADEREND:event_cancelButtonActionPerformed
		this.cancelled = true;
		if (this.running)
		{
			cancel();
		}
		else
		{
			this.dialog.setVisible(false);
			this.dialog.dispose();
			this.dialog = null;
		}
	}//GEN-LAST:event_cancelButtonActionPerformed

	private void dropButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_dropButtonActionPerformed
	{//GEN-HEADEREND:event_dropButtonActionPerformed
		if (this.running)
		{
			return;
		}
		this.dropButton.setEnabled(false);
		this.dropper.setCascade(checkBoxCascadeConstraints.isSelected());
		this.dropper.setRowActionMonitor(this);

		dropThread = new WbThread("DropThread")
		{

			@Override
			public void run()
			{
				doDrop();
			}
		};
		dropThread.start();
	}//GEN-LAST:event_dropButtonActionPerformed

private void showScriptButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showScriptButtonActionPerformed
	showScript();
}//GEN-LAST:event_showScriptButtonActionPerformed

	private void fkCheckFinished(final List<TableIdentifier> tables)
	{
		this.checkThread = null;
		EventQueue.invokeLater(new Runnable()
		{

			@Override
			public void run()
			{
				statusLabel.setText("");
				dropper.setObjects(tables);
				initDisplay();
				dropButton.setEnabled(true);
				showScriptButton.setEnabled(true);
				cancelButton.setEnabled(true);
				WbSwingUtilities.showDefaultCursor(dialog);
			}
		});
	}

private void checkFKButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkFKButtonActionPerformed

	final WbConnection conn = dropper.getConnection();
	if (conn == null || conn.isBusy())
	{
		return;
	}

	this.dropButton.setEnabled(false);
	this.cancelButton.setEnabled(false);
	showScriptButton.setEnabled(false);
	this.statusLabel.setText(ResourceMgr.getString("MsgFkDeps"));

	WbSwingUtilities.showWaitCursor(dialog);

	this.checkThread = new WbThread("FKCheck")
	{
		@Override
		public void run()
		{
			List<TableIdentifier> sorted = null;
			try
			{
				conn.setBusy(true);
				TableDependencySorter sorter = new TableDependencySorter(conn);

				// The tableDependencySorter will only accept TableIdentifier objects
				// not DbObjects, so I need to create a new list.
				// The list should not contain only TableIdentifiers anyway, otherwise
				// the ObjectDropper wouldn't (or shouldn't) support FK checking
				List<TableIdentifier> tables = new ArrayList<TableIdentifier>();
				for (DbObject dbo : dropper.getObjects())
				{
					if (dbo instanceof TableIdentifier)
					{
						tables.add((TableIdentifier) dbo);
					}
				}
				sorted = sorter.sortForDelete(tables, addMissingTables.isSelected());
			}
			catch (Exception e)
			{
				LogMgr.logError("TableDeleterUI.checkFK()", "Error checking FK dependencies", e);
				WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(e));
				sorted = null;
			}
			finally
			{
				conn.setBusy(false);
				fkCheckFinished(sorted);
			}
		}
	};
	checkThread.start();
}//GEN-LAST:event_checkFKButtonActionPerformed
  // Variables declaration - do not modify//GEN-BEGIN:variables
  protected javax.swing.JCheckBox addMissingTables;
  protected javax.swing.JPanel buttonPanel;
  protected javax.swing.JButton cancelButton;
  protected javax.swing.JCheckBox checkBoxCascadeConstraints;
  protected javax.swing.JButton checkFKButton;
  protected javax.swing.JPanel checkPanel;
  protected javax.swing.JButton dropButton;
  protected javax.swing.JPanel jPanel1;
  protected javax.swing.JScrollPane jScrollPane1;
  protected javax.swing.JPanel mainPanel;
  protected javax.swing.JList objectList;
  protected javax.swing.JPanel optionPanel;
  protected javax.swing.JButton showScriptButton;
  protected javax.swing.JLabel statusLabel;
  // End of variables declaration//GEN-END:variables

	@Override
	public void setMonitorType(int aType)
	{
	}

	@Override
	public int getMonitorType()
	{
		return RowActionMonitor.MONITOR_PLAIN;
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
	public void setCurrentObject(String object, long number, long totalObjects)
	{
		String lbl = ResourceMgr.getFormattedString("LblDropping", object);
		statusLabel.setText(lbl);
	}

	@Override
	public void setCurrentRow(long currentRow, long totalRows)
	{
	}

	@Override
	public void jobFinished()
	{
	}

}
