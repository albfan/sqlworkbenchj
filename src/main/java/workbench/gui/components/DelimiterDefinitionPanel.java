/*
 * DelimiterDefinitionPanel.java
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
package workbench.gui.components;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import workbench.resource.ResourceMgr;
import workbench.sql.DelimiterDefinition;

/**
 *
 * @author Thomas Kellerer
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
	private boolean updating;

	public DelimiterDefinitionPanel()
	{
		super();
		initComponents();
		this.delimitTextField.setImmediateUpdate(true);
		this.singleLineCheckBox.setImmediateUpdate(true);
	}

	public void setDelimiter(DelimiterDefinition delim)
	{
		try
		{
			updating = true;

			delimitTextField.removePropertyChangeListener(PROP_DELIM, this);
			singleLineCheckBox.removePropertyChangeListener(PROP_SLD, this);

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
		finally
		{
			updating = false;
		}

		delimitTextField.addPropertyChangeListener(PROP_DELIM, this);
		singleLineCheckBox.addPropertyChangeListener(PROP_SLD, this);
	}

	public void setColumns(int columns)
	{
		delimitTextField.setColumns(columns);
	}

	public int getColumns()
	{
		return delimitTextField.getColumns();
	}

	public DelimiterDefinition getDelimiter()
	{
		return this.delimiter;
	}

	public JTextField getTextField()
	{
		return this.delimitTextField;
	}

	public JCheckBox getCheckBox()
	{
		return this.singleLineCheckBox;
	}

  private void initComponents()
  {


		delimitTextField = new StringPropertyEditor();
		singleLineCheckBox = new BooleanPropertyEditor();

		setLayout(new GridBagLayout());

		delimitTextField.setName("delimiter");
		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		gridBagConstraints.weightx = 0.2;
		add(delimitTextField, gridBagConstraints);

		singleLineCheckBox.setText(ResourceMgr.getString("LblDelimSingleLine"));
		singleLineCheckBox.setToolTipText(ResourceMgr.getDescription("LblDelimSingleLine"));
		singleLineCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		singleLineCheckBox.setMargin(new Insets(0, 0, 0, 0));
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new Insets(0, 6, 0, 0);
		add(singleLineCheckBox, gridBagConstraints);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (updating) return;
		if (evt.getSource() == this.delimitTextField || evt.getSource() == this.singleLineCheckBox)
		{
			if (evt.getPropertyName().equals(PROP_DELIM) || evt.getPropertyName().equals(PROP_SLD))
			{
				firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
			}
		}
	}

}
