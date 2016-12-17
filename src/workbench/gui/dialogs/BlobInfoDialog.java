/*
 * BlobInfoDialog.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.gui.dialogs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JDialog;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.EscAction;
import workbench.gui.components.BlobHandler;
import workbench.gui.components.EncodingDropDown;
import workbench.gui.components.FlatButton;

import workbench.util.ExceptionUtil;
import workbench.util.FileDialogUtil;
import workbench.util.StringUtil;
import workbench.util.ToolDefinition;

/**
 * A dialog to display information about a BLOB from a result set.
 *
 * The dialog offers the following features:
 * <ul>
 *	<li>Download the contents of the BLOB</li>
 *  <li>Upload a file to be stored in the BLOB column</li>
 *  <li>View the blob information as a hex dump</li>
 *  <li>View the BLOB information as an image</li>
 *  <li>Open the BLOB with an external tool</li>
 * </ul>
 * @author  Thomas Kellerer
 */
public class BlobInfoDialog
	extends JDialog
	implements ActionListener
{
	private Object blobValue;
	private BlobHandler handler;
	private EscAction escAction;
	private File uploadFile;
	private boolean hasTools = false;
	private boolean setToNull = false;

	public BlobInfoDialog(java.awt.Frame parent, boolean modal, boolean readOnly)
	{
		super(parent, modal);
		initComponents();
		handler = new BlobHandler();

		getRootPane().setDefaultButton(closeButton);
		escAction = new EscAction(this, this);

		String encoding = Settings.getInstance().getDefaultBlobTextEncoding();
		encodingPanel.setEncoding(encoding);
		List<ToolDefinition> tools = Settings.getInstance().getExternalTools(true, true);
		this.hasTools = (tools != null && tools.size() > 0);
		this.externalViewer.setEnabled(hasTools);
		this.externalTools.setEnabled(hasTools);
		if (hasTools)
		{
			DefaultComboBoxModel model = new DefaultComboBoxModel();
			String name = Settings.getInstance().getLastUsedBlobTool();
			int toSelect = -1;

			for (int i = 0; i < tools.size(); i++)
			{
				model.addElement(tools.get(i));
				if (StringUtil.equalString(name, tools.get(i).getName()))
				{
					toSelect = i;
				}
			}

			this.externalTools.setModel(model);
			if (toSelect != -1)
			{
				this.externalTools.setSelectedIndex(toSelect);
			}
		}
		uploadButton.setEnabled(!readOnly);
		setNullButton.setEnabled(!readOnly);
		WbSwingUtilities.center(this, parent);
	}

	public File getUploadedFile()
	{
		return uploadFile;
	}

	public boolean setToNull()
	{
		return setToNull;
	}

	public byte[] getNewValue()
	{
		return handler.getNewValue();
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == escAction)
		{
			closeWindow();
		}
	}

	private void closeWindow()
	{
		setVisible(false);
		dispose();
	}

	public void setBlobValue(Object value)
	{
		this.blobValue = value;
		String lbl = null;
		if (value instanceof File)
		{
			lbl = ResourceMgr.getString("LblFileSize");
		}
		else
		{
			lbl = ResourceMgr.getString("LblBlobSize");
		}
		long len = 0;

		if (value == null)
		{
			lbl += ": (null)";
		}
		else
		{
			len = handler.getBlobSize(blobValue);
			lbl = lbl + ": " + Long.toString(len) + " Byte";
		}
		infoLabel.setText(lbl);
		if (value instanceof File)
		{
			infoLabel.setToolTipText(value.toString());
		}
		else
		{
			infoLabel.setToolTipText(handler.getByteDisplay(len).toString());
		}
		// Show as text is always enabled to allow text editing
		// for null BLOBs as well.

		saveAsButton.setEnabled(len > 0);
		showImageButton.setEnabled(len > 0);
		showHexButton.setEnabled(len > 0);
		showAsTextButton.setEnabled(len > 0);
		externalViewer.setEnabled(len > 0 && hasTools);
	}

	private void openWithExternalViewer()
	{
		try
		{
			File f = File.createTempFile("wb$tmp_", ".data");
			f.deleteOnExit();
			BlobHandler.saveBlobToFile(this.blobValue, f.getAbsolutePath());
			ToolDefinition tool = (ToolDefinition)this.externalTools.getSelectedItem();
			tool.runApplication(f.getAbsolutePath());
			Settings.getInstance().setLastUsedBlobTool(tool.getName());
		}
		catch (Exception e)
		{
			LogMgr.logError("BlobInfoDialog.openWithExternalViewer()", "Error running external program", e);
			String msg = ExceptionUtil.getDisplay(e);
			WbSwingUtilities.showErrorMessage(this, msg);
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

    closeButton = new javax.swing.JButton();
    jPanel1 = new javax.swing.JPanel();
    infoLabel = new javax.swing.JLabel();
    showAsTextButton = new FlatButton();
    saveAsButton = new FlatButton();
    encodingPanel = new EncodingDropDown(null, false);
    showImageButton = new FlatButton();
    uploadButton = new FlatButton();
    showHexButton = new FlatButton();
    externalViewer = new FlatButton();
    externalTools = new javax.swing.JComboBox();
    setNullButton = new FlatButton();

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    setTitle(ResourceMgr.getString("TxtBlobInfo"));
    setResizable(false);
    addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosed(java.awt.event.WindowEvent evt) {
        formWindowClosed(evt);
      }
    });
    getContentPane().setLayout(new java.awt.GridBagLayout());

    closeButton.setText(ResourceMgr.getString("LblClose")); // NOI18N
    closeButton.addMouseListener(new java.awt.event.MouseAdapter() {
      public void mouseClicked(java.awt.event.MouseEvent evt) {
        closeButtonMouseClicked(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    gridBagConstraints.insets = new java.awt.Insets(12, 0, 10, 0);
    getContentPane().add(closeButton, gridBagConstraints);

    jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.lightGray));
    jPanel1.setLayout(new java.awt.GridBagLayout());

    infoLabel.setText("jLabel1");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(15, 10, 0, 8);
    jPanel1.add(infoLabel, gridBagConstraints);

    showAsTextButton.setText(ResourceMgr.getString("LblShowAsTxt")); // NOI18N
    showAsTextButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        showAsTextButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 0.5;
    gridBagConstraints.insets = new java.awt.Insets(9, 10, 0, 5);
    jPanel1.add(showAsTextButton, gridBagConstraints);

    saveAsButton.setText(ResourceMgr.getString("MnuTxtFileSaveAs")); // NOI18N
    saveAsButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        saveAsButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 0.5;
    gridBagConstraints.insets = new java.awt.Insets(9, 10, 2, 5);
    jPanel1.add(saveAsButton, gridBagConstraints);

    encodingPanel.setLabelVisible(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 0.5;
    gridBagConstraints.insets = new java.awt.Insets(9, 8, 0, 10);
    jPanel1.add(encodingPanel, gridBagConstraints);

    showImageButton.setText(ResourceMgr.getString("LblShowAsImg")); // NOI18N
    showImageButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        showImageButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 0.5;
    gridBagConstraints.insets = new java.awt.Insets(16, 10, 2, 5);
    jPanel1.add(showImageButton, gridBagConstraints);

    uploadButton.setText(ResourceMgr.getString("LblUploadFile")); // NOI18N
    uploadButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        uploadButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(9, 8, 2, 10);
    jPanel1.add(uploadButton, gridBagConstraints);

    showHexButton.setText(ResourceMgr.getString("LblShowAsHex")); // NOI18N
    showHexButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        showHexButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(16, 8, 2, 10);
    jPanel1.add(showHexButton, gridBagConstraints);

    externalViewer.setText(ResourceMgr.getString("LblExternalView")); // NOI18N
    externalViewer.setToolTipText(ResourceMgr.getString("d_LblExternalView")); // NOI18N
    externalViewer.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        externalViewerActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 0.5;
    gridBagConstraints.insets = new java.awt.Insets(7, 10, 11, 5);
    jPanel1.add(externalViewer, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 0.5;
    gridBagConstraints.insets = new java.awt.Insets(7, 8, 11, 10);
    jPanel1.add(externalTools, gridBagConstraints);

    setNullButton.setText(ResourceMgr.getString("LblSetNull")); // NOI18N
    setNullButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        setNullButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 8, 0, 10);
    jPanel1.add(setNullButton, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
    getContentPane().add(jPanel1, gridBagConstraints);

    pack();
  }// </editor-fold>//GEN-END:initComponents

	private void setNullButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_setNullButtonActionPerformed
	{//GEN-HEADEREND:event_setNullButtonActionPerformed
		this.setToNull = true;
		closeWindow();
	}//GEN-LAST:event_setNullButtonActionPerformed

	private void showHexButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_showHexButtonActionPerformed
	{//GEN-HEADEREND:event_showHexButtonActionPerformed
		try
		{
			byte[] data = handler.getBlobAsArray(this.blobValue);
			if (data == null)
			{
				WbSwingUtilities.showErrorMessageKey(this, "MsgBlobNotRetrieved");
				return;
			}
			HexViewer v = new HexViewer(this, ResourceMgr.getString("TxtBlobData"));
			v.setData(data);
			v.setVisible(true);
		}
		catch (Exception e)
		{
			LogMgr.logError("BlobInfoDialog.showHexButtonActionPerformed()", "Error showing hex data", e);
		}
	}//GEN-LAST:event_showHexButtonActionPerformed

	private void showAsTextButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_showAsTextButtonActionPerformed
	{//GEN-HEADEREND:event_showAsTextButtonActionPerformed
		if (handler.showBlobAsText(this, this.blobValue, encodingPanel.getEncoding()))
		{
			// if the blob has been edited, then clear the upload file in case
			// there was one.
			this.uploadFile = null;
		}
	}//GEN-LAST:event_showAsTextButtonActionPerformed

	private void saveAsButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_saveAsButtonActionPerformed
	{//GEN-HEADEREND:event_saveAsButtonActionPerformed
		long fileSize = -1;
		try
		{
			String file = FileDialogUtil.getBlobFile(this);
			if (file == null) return;

			fileSize = BlobHandler.saveBlobToFile(blobValue, file);
			String msg = ResourceMgr.getString("MsgBlobSaved");
			File f = new File(file);
			msg = StringUtil.replace(msg, "%filename%", f.getAbsolutePath());
			fileSize = f.length();
			msg = StringUtil.replace(msg, "%filesize%", Long.toString(fileSize));
			WbSwingUtilities.showMessage(this, msg);
		}
		catch (Exception ex)
		{
			LogMgr.logError("WbTable.saveBlobContent", "Error when writing data file", ex);
			String msg = ResourceMgr.getString("ErrBlobSaveError");
			msg += "\n" + ExceptionUtil.getDisplay(ex);
			WbSwingUtilities.showErrorMessage(this, msg);
		}
	}//GEN-LAST:event_saveAsButtonActionPerformed

	private void uploadButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_uploadButtonActionPerformed
	{//GEN-HEADEREND:event_uploadButtonActionPerformed
		String file = FileDialogUtil.getBlobFile(this, false);
		if (file != null)
		{
			this.uploadFile = new File(file);
		}
	}//GEN-LAST:event_uploadButtonActionPerformed

	private void showImageButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_showImageButtonActionPerformed
	{//GEN-HEADEREND:event_showImageButtonActionPerformed
		this.handler.showBlobAsImage(this, this.blobValue);
	}//GEN-LAST:event_showImageButtonActionPerformed

	private void externalViewerActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_externalViewerActionPerformed
	{//GEN-HEADEREND:event_externalViewerActionPerformed
		openWithExternalViewer();
	}//GEN-LAST:event_externalViewerActionPerformed

	private void formWindowClosed(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosed
	{//GEN-HEADEREND:event_formWindowClosed
		String encoding = encodingPanel.getEncoding();
		Settings.getInstance().setDefaultBlobTextEncoding(encoding);
	}//GEN-LAST:event_formWindowClosed

	private void closeButtonMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_closeButtonMouseClicked
	{//GEN-HEADEREND:event_closeButtonMouseClicked
		closeWindow();
	}//GEN-LAST:event_closeButtonMouseClicked

  // Variables declaration - do not modify//GEN-BEGIN:variables
  public javax.swing.JButton closeButton;
  public workbench.gui.components.EncodingDropDown encodingPanel;
  public javax.swing.JComboBox externalTools;
  public javax.swing.JButton externalViewer;
  public javax.swing.JLabel infoLabel;
  public javax.swing.JPanel jPanel1;
  public javax.swing.JButton saveAsButton;
  public javax.swing.JButton setNullButton;
  public javax.swing.JButton showAsTextButton;
  public javax.swing.JButton showHexButton;
  public javax.swing.JButton showImageButton;
  public javax.swing.JButton uploadButton;
  // End of variables declaration//GEN-END:variables

}
