/*
 * DriverEditorPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.gui.profiles;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import workbench.interfaces.Validator;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.DbDriver;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ClassFinderGUI;
import workbench.gui.components.ExtensionFileFilter;
import workbench.gui.components.FlatButton;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.components.WbFileChooser;

import workbench.util.ClassFinder;
import workbench.util.WbFile;

/**
 *
 * @author  Thomas Kellerer
 */
public class DriverEditorPanel
	extends JPanel
	implements DocumentListener, ListSelectionListener, ActionListener
{
	private DbDriver currentDriver;
	private Validator validator;
	private GridBagConstraints defaultErrorConstraints;
	private JLabel errorLabel;
	private String lastDir;
	private final String lastDirProperty = "workbench.drivers.lastlibdir";

	public DriverEditorPanel()
	{
		super();
		initComponents();
    defaultErrorConstraints = new GridBagConstraints();
		defaultErrorConstraints.gridx = 0;
		defaultErrorConstraints.gridy = 0;
		defaultErrorConstraints.gridwidth = GridBagConstraints.REMAINDER;
		defaultErrorConstraints.fill = GridBagConstraints.HORIZONTAL;
		defaultErrorConstraints.ipadx = 0;
		defaultErrorConstraints.ipady = 0;
		defaultErrorConstraints.anchor = java.awt.GridBagConstraints.WEST;
		defaultErrorConstraints.insets = new java.awt.Insets(15, 8, 0, 3);

		errorLabel = new JLabel(ResourceMgr.getString("ErrDrvNameNotUnique"));
		Border b = new CompoundBorder(new LineBorder(Color.RED.brighter(), 1), new EmptyBorder(3, 5, 3, 5));
		errorLabel.setBorder(b);
		errorLabel.setFont(errorLabel.getFont().deriveFont(Font.BOLD));
		errorLabel.setBackground(new Color(255, 255, 220));
		errorLabel.setOpaque(true);

		tfName.getDocument().addDocumentListener(this);
		libList.getSelectionModel().addListSelectionListener(this);
		btnUp.addActionListener(this);
		btnDown.addActionListener(this);
		btnRemove.addActionListener(this);
		btnAdd.addActionListener(this);
		lastDir = Settings.getInstance().getProperty(lastDirProperty, null);
		WbSwingUtilities.setMinimumSize(statusLabel, 1, 20);
		checkButtons();
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		int[] indexes = libList.getSelectedIndices();

		int selectedIndex = -1;
		if (indexes.length == 1)
		{
			selectedIndex = indexes[0];
		}

		DefaultListModel model = (DefaultListModel)libList.getModel();
		int count = model.getSize();

		if (e.getSource() == btnRemove && indexes.length > 0)
		{
			removeSelected();
		}
		else if (e.getSource() == btnUp && selectedIndex > 0)
		{
			swap(selectedIndex, selectedIndex - 1);
		}
		else if (e.getSource() == btnDown && selectedIndex > -1 && selectedIndex < count - 1)
		{
			swap(selectedIndex, selectedIndex + 1);
		}
		else if (e.getSource() == btnAdd)
		{
			selectFile();
		}
	}

	private void removeSelected()
	{
		int[] indexes = libList.getSelectedIndices();
		if (indexes.length == 0) return;

		Arrays.sort(indexes);
		DefaultListModel model = (DefaultListModel)libList.getModel();
		for (int i=indexes.length - 1; i >= 0; i --)
		{
			model.remove(indexes[i]);
		}
	}

	private void swap(int firstIndex, int secondIndex)
	{
		DefaultListModel model = (DefaultListModel)libList.getModel();
		Object first = model.get(firstIndex);
		Object second = model.get(secondIndex);
		model.set(firstIndex, second);
		model.set(secondIndex, first);
		libList.setSelectedIndex(secondIndex);
	}

	private void selectFile()
	{
		DefaultListModel model = (DefaultListModel)libList.getModel();
		JFileChooser jf = new WbFileChooser();
		jf.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		jf.setMultiSelectionEnabled(true);
		if (this.lastDir != null)
		{
			jf.setCurrentDirectory(new File(this.lastDir));
		}
		jf.setFileFilter(ExtensionFileFilter.getJarFileFilter());
		int answer = jf.showOpenDialog(SwingUtilities.getWindowAncestor(this));
		if (answer == JFileChooser.APPROVE_OPTION)
		{
			File[] selectedFiles = jf.getSelectedFiles();
			removeSelected();
			for (File f : selectedFiles)
			{
				WbFile wbf = new WbFile(f);
				model.addElement(new LibraryElement(wbf));
			}
			lastDir = selectedFiles[0].getParent();
			Settings.getInstance().setProperty(lastDirProperty, lastDir);
			selectClass();
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		checkButtons();
	}

	private void checkButtons()
	{
		int selectedIndex = libList.getSelectedIndex();
		int count = libList.getModel().getSize();
		btnRemove.setEnabled(selectedIndex > -1);
		btnUp.setEnabled(selectedIndex > 0);
		btnDown.setEnabled(selectedIndex > -1 && selectedIndex < count - 1);
		btnAdd.setEnabled(!tfClassName.getText().trim().equals("sun.jdbc.odbc.JdbcOdbcDriver"));
	}

	public void setValidator(Validator nameValidator)
	{
		this.validator = nameValidator;
	}

	private List<String> getLibraries()
	{
		int size = libList.getModel().getSize();
		List<String> result = new ArrayList<String>(size);
		for (int i=0; i < size; i++)
		{
			LibraryElement lib = (LibraryElement)libList.getModel().getElementAt(i);
			result.add(lib.getPath());
		}
		return result;
	}

	protected void selectClass()
	{
		ClassFinder finder = new ClassFinder(Driver.class);

		// Ignore deprecated drivers
		List<String> classes = Settings.getInstance().getListProperty("workbench.db.drivers.deprecated", false);
		finder.setExcludedClasses(classes);

		List<String> libs = getLibraries();

		ClassFinderGUI gui = new ClassFinderGUI(finder, tfClassName, statusLabel);
		gui.setStatusBarKey("TxtSearchingDriver");
		gui.setWindowTitleKey("TxtSelectDriver");
		gui.setClassPath(libs);
		gui.startCheck();
	}

	public boolean validateName()
	{
		boolean valid = false;
		if (validator.isValid(tfName.getText()))
		{
			this.remove(errorLabel);
			valid = true;
		}
		else
		{
			this.add(errorLabel, defaultErrorConstraints);
		}
		this.doLayout();
		this.validate();
		return valid;
	}

	@Override
	public void insertUpdate(DocumentEvent e)
	{
		validateName();
	}

	@Override
	public void removeUpdate(DocumentEvent e)
	{
		validateName();
	}

	@Override
	public void changedUpdate(DocumentEvent e)
	{
		validateName();
	}

	public String getCurrentName()
	{
		return tfName.getText().trim();
	}

	public void setDriver(DbDriver driver)
	{
		this.currentDriver = driver;
		this.tfName.setText(driver.getName());
		this.tfClassName.setText(driver.getDriverClass());
		List<String> libraryList = driver.getLibraryList();
		DefaultListModel model = new DefaultListModel();
		for (String lib : libraryList)
		{
			model.addElement(new LibraryElement(lib));
		}
		libList.setModel(model);
		libList.getSelectionModel().clearSelection();
		checkButtons();
		this.tfSampleUrl.setText(driver.getSampleUrl());
		this.detectDriverButton.setEnabled(libList.getModel().getSize() > 0);
	}

	public void updateDriver()
	{
		this.currentDriver.setName(tfName.getText().trim());
		this.currentDriver.setDriverClass(tfClassName.getText().trim());
		this.currentDriver.setLibraryList(getLibraries());
		this.currentDriver.setSampleUrl(tfSampleUrl.getText());
	}

	public DbDriver getDriver()
	{
		this.updateDriver();
		return this.currentDriver;
	}

	public void reset()
	{
		this.currentDriver = null;
		this.tfName.setText("");
		this.tfClassName.setText("");
		this.libList.setModel(new DefaultListModel());
		this.tfSampleUrl.setText("");
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

    lblName = new javax.swing.JLabel();
    tfName = new javax.swing.JTextField();
    lblClassName = new javax.swing.JLabel();
    tfClassName = new javax.swing.JTextField();
    lblLibrary = new javax.swing.JLabel();
    lblSample = new javax.swing.JLabel();
    tfSampleUrl = new javax.swing.JTextField();
    statusLabel = new javax.swing.JLabel();
    detectDriverButton = new FlatButton();
    jPanel2 = new javax.swing.JPanel();
    jScrollPane1 = new javax.swing.JScrollPane();
    libList = new javax.swing.JList();
    btnAdd = new javax.swing.JButton();
    btnRemove = new javax.swing.JButton();
    btnUp = new javax.swing.JButton();
    btnDown = new javax.swing.JButton();

    setLayout(new java.awt.GridBagLayout());

    lblName.setText(ResourceMgr.getString("LblDriverName")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(11, 10, 0, 7);
    add(lblName, gridBagConstraints);

    tfName.setHorizontalAlignment(javax.swing.JTextField.LEFT);
    tfName.setMinimumSize(new java.awt.Dimension(50, 20));
    tfName.addMouseListener(new TextComponentMouseListener());
    tfName.addFocusListener(new java.awt.event.FocusAdapter()
    {
      public void focusLost(java.awt.event.FocusEvent evt)
      {
        DriverEditorPanel.this.focusLost(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(11, 3, 0, 3);
    add(tfName, gridBagConstraints);

    lblClassName.setText(ResourceMgr.getString("LblDriverClass")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(6, 10, 0, 7);
    add(lblClassName, gridBagConstraints);

    tfClassName.setColumns(10);
    tfClassName.setHorizontalAlignment(javax.swing.JTextField.LEFT);
    tfClassName.addMouseListener(new TextComponentMouseListener());
    tfClassName.addFocusListener(new java.awt.event.FocusAdapter()
    {
      public void focusLost(java.awt.event.FocusEvent evt)
      {
        DriverEditorPanel.this.focusLost(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(6, 3, 0, 3);
    add(tfClassName, gridBagConstraints);

    lblLibrary.setText(ResourceMgr.getString("LblDriverLibrary")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(6, 10, 0, 7);
    add(lblLibrary, gridBagConstraints);

    lblSample.setText(ResourceMgr.getString("LblSampleUrl")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(6, 10, 0, 7);
    add(lblSample, gridBagConstraints);

    tfSampleUrl.setColumns(10);
    tfSampleUrl.setHorizontalAlignment(javax.swing.JTextField.LEFT);
    tfSampleUrl.addMouseListener(new TextComponentMouseListener());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(6, 3, 0, 3);
    add(tfSampleUrl, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(14, 6, 0, 7);
    add(statusLabel, gridBagConstraints);

    detectDriverButton.setIcon(ResourceMgr.getImage("magnifier.png"));
    detectDriverButton.setToolTipText(ResourceMgr.getString("MsgDetectDriver")); // NOI18N
    detectDriverButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
    detectDriverButton.setMaximumSize(new java.awt.Dimension(22, 22));
    detectDriverButton.setMinimumSize(new java.awt.Dimension(22, 22));
    detectDriverButton.setPreferredSize(new java.awt.Dimension(22, 22));
    detectDriverButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        detectDriverButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 4);
    add(detectDriverButton, gridBagConstraints);

    jPanel2.setLayout(new java.awt.GridBagLayout());

    libList.setVerifyInputWhenFocusTarget(false);
    libList.setVisibleRowCount(4);
    jScrollPane1.setViewportView(libList);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridheight = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanel2.add(jScrollPane1, gridBagConstraints);

    btnAdd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/workbench/resource/images/Open16.gif"))); // NOI18N
    btnAdd.setToolTipText(ResourceMgr.getString("d_LblDriverLibrary")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 8);
    jPanel2.add(btnAdd, gridBagConstraints);

    btnRemove.setIcon(new javax.swing.ImageIcon(getClass().getResource("/workbench/resource/images/Remove16.gif"))); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(4, 8, 0, 8);
    jPanel2.add(btnRemove, gridBagConstraints);

    btnUp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/workbench/resource/images/Up16.gif"))); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(16, 8, 0, 8);
    jPanel2.add(btnUp, gridBagConstraints);

    btnDown.setIcon(new javax.swing.ImageIcon(getClass().getResource("/workbench/resource/images/Down16.gif"))); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(4, 8, 0, 8);
    jPanel2.add(btnDown, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(6, 3, 3, 0);
    add(jPanel2, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents

	private void focusLost(java.awt.event.FocusEvent evt)//GEN-FIRST:event_focusLost
	{//GEN-HEADEREND:event_focusLost
		if (validateName())
		{
			this.currentDriver.setName(tfName.getText().trim());
		}
	}//GEN-LAST:event_focusLost

	private void detectDriverButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_detectDriverButtonActionPerformed
	{//GEN-HEADEREND:event_detectDriverButtonActionPerformed
		selectClass();
	}//GEN-LAST:event_detectDriverButtonActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton btnAdd;
  private javax.swing.JButton btnDown;
  private javax.swing.JButton btnRemove;
  private javax.swing.JButton btnUp;
  private javax.swing.JButton detectDriverButton;
  private javax.swing.JPanel jPanel2;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JLabel lblClassName;
  private javax.swing.JLabel lblLibrary;
  private javax.swing.JLabel lblName;
  private javax.swing.JLabel lblSample;
  private javax.swing.JList libList;
  private javax.swing.JLabel statusLabel;
  private javax.swing.JTextField tfClassName;
  private javax.swing.JTextField tfName;
  private javax.swing.JTextField tfSampleUrl;
  // End of variables declaration//GEN-END:variables
}
