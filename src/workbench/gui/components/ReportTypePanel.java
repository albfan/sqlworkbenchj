/*
 * ReportTypePanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import javax.swing.JRadioButton;
import javax.swing.JLabel;
import workbench.resource.ResourceMgr;
import java.awt.GridBagConstraints;
import javax.swing.ButtonGroup;
import java.awt.GridBagLayout;

public class ReportTypePanel
	extends EncodingPanel
{
	public static final int TYPE_WB_REPORT = 1;
	public static final int TYPE_DBDESIGNER = 2;

	private int reportType = TYPE_WB_REPORT;
	private JRadioButton wbReportType;
	private JRadioButton dbDesignerType;
	private ButtonGroup buttonGroup = new ButtonGroup();

	public ReportTypePanel()
	{
		this("UTF-8");
	}

	public ReportTypePanel(String encoding)
	{
		super(encoding);
		JLabel l =  new JLabel(ResourceMgr.getString("LblReportType"));

		GridBagLayout layout = (GridBagLayout)this.getLayout();
		GridBagConstraints c = layout.getConstraints(this.encodings);
    c.weightx = 0;
    c.weighty = 0;
    layout.setConstraints(this.encodings, c);

		c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 3;
		c.insets = new java.awt.Insets(10, 5, 5, 5);
    c.fill = java.awt.GridBagConstraints.HORIZONTAL;
    c.anchor = java.awt.GridBagConstraints.NORTHWEST;

		this.add(l, c);

		c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 4;
    c.fill = java.awt.GridBagConstraints.HORIZONTAL;
    c.anchor = java.awt.GridBagConstraints.NORTHWEST;

		wbReportType = new JRadioButton(ResourceMgr.getString("LblReportTypeWorkbench"));
		wbReportType.setSelected(true);
		this.add(wbReportType, c);

		c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 5;
    c.fill = java.awt.GridBagConstraints.HORIZONTAL;
    c.anchor = java.awt.GridBagConstraints.NORTHWEST;
    c.weightx = 1.0;
    c.weighty = 1.0;
		dbDesignerType = new JRadioButton(ResourceMgr.getString("LblReportTypeDbDesigner"));
		this.add(dbDesignerType, c);
		buttonGroup.add(this.wbReportType);
		buttonGroup.add(this.dbDesignerType);

	}

	public void setTypeWbReport()
	{
		this.setType(TYPE_WB_REPORT);
	}

	public void setTypeDbDesigner()
	{
		this.setType(TYPE_DBDESIGNER);
	}

	private void setType(int type)
	{
		if (type != TYPE_DBDESIGNER && type != TYPE_WB_REPORT)
			throw new IllegalArgumentException("Wrong type");

		this.reportType = type;
		if (this.reportType == TYPE_WB_REPORT)
		{
			this.wbReportType.setSelected(true);
		}
		else
		{
			this.dbDesignerType.setSelected(true);
		}
	}

	public boolean isWbReport()
	{
		return this.wbReportType.isSelected();
	}

	public boolean isDbDesigner()
	{
		return this.dbDesignerType.isSelected();
	}

}
