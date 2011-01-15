/*
 * ImportOptionsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import workbench.db.importer.ProducerFactory;
import workbench.gui.components.DividerBorder;
import workbench.interfaces.EncodingSelector;
import workbench.interfaces.ValidatingComponent;
import workbench.resource.Settings;
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
	private XmlOptionsPanel xmlOptions;
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

		this.xmlOptions = new XmlOptionsPanel();
		this.typePanel.add(this.xmlOptions, "xml");

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
		this.xmlOptions.saveSettings(section + ".xml");
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
		this.xmlOptions.restoreSettings(section + ".xml");
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

	public XmlImportOptions getXmlOptions()
	{
		return xmlOptions;
	}

	public String getEncoding()
	{
		return generalOptions.getEncoding();
	}

	public void setEncoding(String enc)
	{
		generalOptions.setEncoding(enc);
	}

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

	public boolean validateInput()
	{
		if (this.textOptions != null)
		{
			return generalOptions.validateInput();
		}
		return true;
	}

	public void componentDisplayed()
	{
	}

}
