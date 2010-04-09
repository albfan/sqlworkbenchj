/*
 * SummaryBar.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import workbench.gui.sql.DwStatusBar;

/**
 *
 * @author Thomas Kellerer
 */
public class SummaryBar
	extends JPanel
{

	public SummaryBar(JLabel infoLabel)
	{
		super(new BorderLayout());
		setMinimumSize(new Dimension(80, DwStatusBar.BAR_HEIGHT));
		Border b = new CompoundBorder(new EmptyBorder(1, 0, 1, 0), BorderFactory.createEtchedBorder());
		setBorder(b);
		infoLabel.setBorder(new EmptyBorder(2, 4, 2, 2));
		add(infoLabel, BorderLayout.CENTER);
	}

}
