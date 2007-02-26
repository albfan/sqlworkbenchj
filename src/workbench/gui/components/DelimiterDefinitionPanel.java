/*
 * DelimiterDefinitionPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.JPanel;
import workbench.interfaces.SimplePropertyEditor;
import workbench.resource.ResourceMgr;
import workbench.sql.DelimiterDefinition;

/**
 *
 * @author support@sql-workbench.net
 */
public class DelimiterDefinitionPanel 
	extends JPanel
	implements PropertyChangeListener
{
	private DelimiterDefinition delimiter;
  private StringPropertyEditor delimitTextField;
  private BooleanPropertyEditor singleLineCheckBox;
	public static final String PROP_DELIM = "delimiter";
	public static final String PROP_SLD = "singleLine";
	
	public DelimiterDefinitionPanel()
	{
		initComponents();
		this.delimitTextField.setImmediateUpdate(true);
		this.singleLineCheckBox.setImmediateUpdate(true);
	}

	public void setDelimiter(DelimiterDefinition delim)
	{
		if (delim != null)
		{
			this.delimiter = delim;
		}
		else
		{
			this.delimiter = new DelimiterDefinition();
		}
		
		this.delimitTextField.setSourceObject(null, PROP_DELIM);
		this.singleLineCheckBox.setSourceObject(null, PROP_SLD);
		
		this.delimitTextField.setText(this.delimiter.getDelimiter());
		this.singleLineCheckBox.setSelected(this.delimiter.isSingleLine());
		
		this.delimitTextField.setSourceObject(delimiter, PROP_DELIM);
		this.singleLineCheckBox.setSourceObject(delimiter, PROP_SLD);
	}

	public DelimiterDefinition getDelimiter()
	{
		return this.delimiter;
	}
	
	public javax.swing.JTextField getTextField()
	{
		return this.delimitTextField;
	}
	
	public javax.swing.JCheckBox getCheckBox()
	{
		return this.singleLineCheckBox;
	}
	
  private void initComponents()
  {
    java.awt.GridBagConstraints gridBagConstraints;

    delimitTextField = new StringPropertyEditor();
    singleLineCheckBox = new BooleanPropertyEditor();
		delimitTextField.addPropertyChangeListener(PROP_DELIM, this);
		singleLineCheckBox.addPropertyChangeListener(PROP_SLD, this);
		
    setLayout(new java.awt.GridBagLayout());

    delimitTextField.setMinimumSize(new java.awt.Dimension(72, 20));
    delimitTextField.setName("delimiter");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 0.5;
    add(delimitTextField, gridBagConstraints);

    singleLineCheckBox.setText(ResourceMgr.getString("LblDelimSingleLine"));
    singleLineCheckBox.setToolTipText(ResourceMgr.getDescription("LblDelimSingleLine"));
    singleLineCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    singleLineCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 0.5;
    gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
    add(singleLineCheckBox, gridBagConstraints);
  }

	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getSource() == this.delimitTextField || evt.getSource() == this.singleLineCheckBox)
		{
			if (evt.getPropertyName().equals(PROP_DELIM) || evt.getPropertyName().equals(PROP_SLD))
			{
				firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
			}
		}
	}

}
