/*
 * ImportFileOptionsPanel.java
 *
 * Created on October 30, 2002, 1:41 PM
 */

package workbench.gui.components;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import javax.swing.JRadioButton;
import javax.swing.event.ChangeListener;
import workbench.gui.components.DividerBorder;
import workbench.resource.ResourceMgr;



/**
 *
 * @author  workbench@kellerer.org
 */
public class ExportOptionsPanel
	extends JPanel
	implements ActionListener
{

	private JRadioButton typeSql;
	private JRadioButton typeText;
	private JCheckBox createTableOption;
	private JCheckBox includeHeadersOption;
	
	public ExportOptionsPanel()
	{
		this.setLayout(new GridBagLayout());
		ButtonGroup type = new ButtonGroup();
		
		JLabel label = new JLabel(ResourceMgr.getString("LabelExportTypeDesc"));
		this.typeSql = new JRadioButton(ResourceMgr.getString("LabelExportTypeSql"));
		
		this.typeText = new JRadioButton(ResourceMgr.getString("LabelExportTypeText"));
		
		this.typeSql.addActionListener(this);
		this.typeText.addActionListener(this);

		type.add(typeSql);
		type.add(typeText);
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(0, 5, 0, 0);
		this.add(label, gbc);
		
		gbc.gridy++;
		gbc.insets = new Insets(0, 0, 0, 0);
		this.add(typeSql, gbc);
		
		this.createTableOption = new JCheckBox(ResourceMgr.getString("LabelExportIncludeCreateTable"));
		gbc.gridy++;
		this.add(this.createTableOption, gbc);

		gbc.gridy++;
		this.add(this.typeText, gbc);
		
		this.includeHeadersOption = new JCheckBox(ResourceMgr.getString("LabelExportIncludeHeaders"));
		gbc.gridy++;
		this.add(this.includeHeadersOption, gbc);
		
		gbc.gridy++;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		JPanel dummy = new JPanel();
		this.add(dummy, gbc);

		this.typeSql.setSelected(true);
		this.includeHeadersOption.setSelected(false);
		this.includeHeadersOption.setEnabled(false);
		
	}
	
	public boolean isTypeSql()
	{
		return this.typeSql.isSelected();
	}
	
	public boolean isTypeText()
	{
		return this.typeText.isSelected();
	}
	
	public boolean getIncludeTextHeader()
	{
		return this.includeHeadersOption.isSelected();
	}
	
	public boolean getCreateTable()
	{
		return this.createTableOption.isSelected();
	}
	
	public void actionPerformed(java.awt.event.ActionEvent e)
	{
		this.includeHeadersOption.setEnabled(this.typeText.isSelected());
		this.createTableOption.setEnabled(this.typeSql.isSelected());
	}	

}
