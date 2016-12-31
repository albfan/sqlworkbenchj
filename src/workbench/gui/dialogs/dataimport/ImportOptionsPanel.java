/*
 * ImportOptionsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.gui.dialogs.dataimport;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import workbench.interfaces.EncodingSelector;
import workbench.interfaces.ValidatingComponent;
import workbench.resource.Settings;

import workbench.db.importer.ImportOptions;
import workbench.db.importer.ProducerFactory;
import workbench.db.importer.TextImportOptions;

import workbench.gui.components.DividerBorder;

import workbench.util.StringUtil;

/**
 *
 * @author  Thomas Kellerer
 *
 */
public class ImportOptionsPanel
	extends JPanel
	implements EncodingSelector, ActionListener, ValidatingComponent
{
	private JPanel typePanel;
	private CardLayout card;
	private JComboBox typeSelector;
	private GeneralImportOptionsPanel generalOptions;
	private TextOptionsPanel textOptions;
	private ProducerFactory.ImportType currentType = null;

	public ImportOptionsPanel()
	{
		super();
		setLayout(new BorderLayout(0,2));
		generalOptions = new GeneralImportOptionsPanel();

		JPanel generalContainer = new JPanel();
		generalContainer.setLayout(new BorderLayout(0,0));
		generalContainer.add(this.generalOptions, BorderLayout.CENTER);
		Border leftMargin = new EmptyBorder(0, 3, 0, 0);
		generalContainer.setBorder(leftMargin);

		JPanel selectorPanel = new JPanel(new BorderLayout(2, 2));
		Border b = new CompoundBorder(DividerBorder.BOTTOM_DIVIDER, new EmptyBorder(0, 0, 5, 0));
		selectorPanel.setBorder(b);
		typeSelector = new JComboBox();
		typeSelector.addItem("Text");
		typeSelector.addItem("XML");
		JLabel type = new JLabel("Type");
		selectorPanel.add(type, BorderLayout.WEST);
		selectorPanel.add(typeSelector, BorderLayout.CENTER);
		generalContainer.add(selectorPanel, BorderLayout.SOUTH);

		this.add(generalContainer, BorderLayout.NORTH);

		this.textOptions = new TextOptionsPanel();
		this.typePanel = new JPanel();
		typePanel.setBorder(leftMargin);
		this.card = new CardLayout();
		this.typePanel.setLayout(card);

		this.typePanel.add(this.textOptions, "text");
		this.typePanel.add(new JPanel(), "xml"); // no options for XML import

		this.add(typePanel, BorderLayout.CENTER);
		typeSelector.addActionListener(this);
	}

	public void allowImportTypeSelection(boolean flag)
	{

	}
	public void allowImportModeSelection(boolean flag)
	{
		this.generalOptions.setModeSelectorEnabled(flag);
	}

	public void saveSettings(String section)
	{
		this.generalOptions.saveSettings(section);
		this.textOptions.saveSettings(section + ".text");
		if (StringUtil.isBlank(section))
		{
			section = "import";
		}
		Settings.getInstance().setProperty("workbench." + section + ".type", this.currentType == null ? -1 : currentType.toInteger());
	}

	public void restoreSettings(String section)
	{
		if (StringUtil.isBlank(section))
		{
			section = "import";
		}
		this.generalOptions.restoreSettings(section);
		this.textOptions.restoreSettings(section + ".text");
		int type = Settings.getInstance().getIntProperty("workbench." + section + ".type", -1);
		this.setImportType(ProducerFactory.ImportType.valueOf(type));
	}

	/**
	 *	Sets the displayed options according to
	 *  DataExporter.EXPORT_XXXX types
	 */
	public void setImportType(ProducerFactory.ImportType type)
	{
		if (type == ProducerFactory.ImportType.Text)
		{
			setTypeText();
		}
		else if (type == ProducerFactory.ImportType.XML)
		{
			setTypeXml();
		}
	}

	public ProducerFactory.ImportType getImportType()
	{
		return this.currentType;
	}

	public void setTypeText()
	{
		this.card.show(this.typePanel, "text");
		this.currentType = ProducerFactory.ImportType.Text;
		typeSelector.setSelectedIndex(0);
	}

	public void setTypeXml()
	{
		this.card.show(this.typePanel, "xml");
		this.currentType = ProducerFactory.ImportType.XML;
		typeSelector.setSelectedIndex(1);
	}

	public ImportOptions getGeneralOptions()
	{
		return this.generalOptions;
	}

	public TextImportOptions getTextOptions()
	{
		return textOptions;
	}

	@Override
	public String getEncoding()
	{
		return generalOptions.getEncoding();
	}

	@Override
	public void setEncoding(String enc)
	{
		generalOptions.setEncoding(enc);
	}

	@Override
	public void actionPerformed(ActionEvent event)
	{
		if (event.getSource() == this.typeSelector)
		{
			String item = typeSelector.getSelectedItem().toString().toLowerCase();
			ProducerFactory.ImportType oldType = this.currentType;

			this.card.show(this.typePanel, item);

			if ("text".equals(item))
				this.currentType = ProducerFactory.ImportType.Text;
			else if ("xml".equals(item))
				this.currentType = ProducerFactory.ImportType.XML;

			if (oldType != currentType) firePropertyChange("exportType", oldType, this.currentType);
		}
	}

	@Override
	public boolean validateInput()
	{
		if (this.textOptions != null)
		{
			return generalOptions.validateInput();
		}
		return true;
	}

	@Override
	public void componentDisplayed()
	{
	}

  @Override
  public void componentWillBeClosed()
  {
		// nothing to do
  }

}
