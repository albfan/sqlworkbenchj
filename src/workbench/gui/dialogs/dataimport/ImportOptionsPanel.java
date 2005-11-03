/*
 * ImportOptionsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
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
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import workbench.db.importer.ProducerFactory;
import workbench.gui.components.ColumnSelectorPanel;
import workbench.gui.components.DividerBorder;
import workbench.interfaces.EncodingSelector;
import workbench.resource.Settings;
import workbench.storage.ResultInfo;

/**
 *
 * @author  support@sql-workbench.net
 *
 */
public class ImportOptionsPanel
	extends JPanel
	implements EncodingSelector, ActionListener
{
	private JPanel typePanel;
	private CardLayout card;
	private JComboBox typeSelector;
	private GeneralImportOptionsPanel generalOptions;
	private TextOptionsPanel textOptions;
	private XmlOptionsPanel xmlOptions;
	private int currentType = -1;
	
	public ImportOptionsPanel()
	{
		super();
		this.setLayout(new BorderLayout());
		this.generalOptions = new GeneralImportOptionsPanel();
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(this.generalOptions, BorderLayout.CENTER);
		
		JPanel s = new JPanel(new BorderLayout(2, 2));
		Border b = new CompoundBorder(new DividerBorder(DividerBorder.BOTTOM), new EmptyBorder(0, 0, 5, 0));
		s.setBorder(b);
		typeSelector = new JComboBox();
		typeSelector.addItem("Text");
		typeSelector.addItem("XML");
		JLabel type = new JLabel("Type");
		s.add(type, BorderLayout.WEST);
		s.add(typeSelector, BorderLayout.CENTER);
		p.add(s, BorderLayout.SOUTH);
		
		this.add(p, BorderLayout.NORTH);
		
		this.textOptions = new TextOptionsPanel();
		this.typePanel = new JPanel();
		this.card = new CardLayout();
		this.typePanel.setLayout(card);
		this.typePanel.add(this.textOptions, "text");
		
		this.xmlOptions = new XmlOptionsPanel();
		this.typePanel.add(this.xmlOptions, "xml");
		
		this.add(typePanel, BorderLayout.CENTER);
		typeSelector.addActionListener(this);
	}
	
	public void allowImportModeSelection(boolean flag)
	{
		this.generalOptions.setModeSelectorEnabled(flag);
	}
	
	public void saveSettings()
	{
		this.generalOptions.saveSettings();
		this.textOptions.saveSettings();
		this.xmlOptions.saveSettings();
		Settings.getInstance().setProperty("workbench.import.type", Integer.toString(this.currentType));
	}
	
	public void restoreSettings()
	{
		this.generalOptions.restoreSettings();
		this.textOptions.restoreSettings();
		this.xmlOptions.restoreSettings();
		int type = Settings.getInstance().getIntProperty("workbench.import.type", -1);
		this.setImportType(type);
	}
	
	/**
	 *	Sets the displayed options according to 
	 *  DataExporter.EXPRT_XXXX types
	 */
	public void setImportType(int type)
	{
		switch (type)
		{
			case ProducerFactory.IMPORT_TEXT:
				setTypeText();
				break;
			case ProducerFactory.IMPORT_XML:
				setTypeXml();
				break;
		}
	}

	public int getImportType()
	{
		return this.currentType;
	}
	
	public void setTypeText()
	{
		this.card.show(this.typePanel, "text");
		this.currentType = ProducerFactory.IMPORT_TEXT;
		typeSelector.setSelectedIndex(0);
	}
	
	public void setTypeXml()
	{
		this.card.show(this.typePanel, "xml");
		this.currentType = ProducerFactory.IMPORT_XML;
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
			int oldType = this.currentType;

			this.card.show(this.typePanel, item);

			if ("text".equals(item))
				this.currentType = ProducerFactory.IMPORT_TEXT;
			else if ("xml".equals(item))
				this.currentType = ProducerFactory.IMPORT_XML;
			firePropertyChange("exportType", oldType, this.currentType);
		}
	}
	
}
