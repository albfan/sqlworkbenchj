/*
 * RendererFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.renderer;

import java.lang.reflect.Constructor;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import workbench.log.LogMgr;

/**
 * A factory for TableCellRenderers. Classes are created using
 * Class.forName() to avoid unnecessary class loading during startup.
 * This is used from within WbTable
 *
 * @see workbench.gui.components.WbTable#initDefaultRenderers()
 *
 * @author support@sql-workbench.net
 */
public class RendererFactory
{
	public static TableCellRenderer getSortHeaderRenderer()
	{
		try
		{
			Class cls = Class.forName("workbench.gui.components.SortHeaderRenderer");
			TableCellRenderer rend = (TableCellRenderer)cls.newInstance();
			return rend;
		}
		catch (Exception e)
		{
			LogMgr.logError("RendererFactory.getSortHeaderRenderer()", "Error creating renderer", e);
			return new DefaultTableCellRenderer();
		}
	}
	
	public static TableCellRenderer getDateRenderer(String format)
	{
		try
		{
			Class cls = Class.forName("workbench.gui.renderer.DateColumnRenderer");
			Class[] types = new Class[] { String.class };
			Constructor cons = cls.getConstructor(types);
			Object[] args = new Object[] { format };
			TableCellRenderer rend = (TableCellRenderer)cons.newInstance(args);
			return rend;
		}
		catch (Exception e)
		{
			LogMgr.logError("RendererFactory.getDateRenderer()", "Error creating renderer", e);
			return new DefaultTableCellRenderer();
		}
	}

	public static TableCellRenderer getTooltipRenderer()
	{
		try
		{
			Class cls = Class.forName("workbench.gui.renderer.ToolTipRenderer");
			TableCellRenderer rend = (TableCellRenderer)cls.newInstance();
			return rend;
		}
		catch (Exception e)
		{
			LogMgr.logError("RendererFactory.getTooltipRenderer()", "Error creating renderer", e);
			return new DefaultTableCellRenderer();
		}
	}
	
	public static TableCellRenderer getStringRenderer()
	{
		try
		{
			Class cls = Class.forName("workbench.gui.renderer.StringColumnRenderer");
			TableCellRenderer rend = (TableCellRenderer)cls.newInstance();
			return rend;
		}
		catch (Exception e)
		{
			LogMgr.logError("RendererFactory.getStringRenderer()", "Error creating renderer", e);
			return new DefaultTableCellRenderer();
		}
	}
	
	public static TableCellRenderer getIntegerRenderer()
	{
		try
		{
			Class cls = Class.forName("workbench.gui.renderer.NumberColumnRenderer");
			TableCellRenderer rend = (TableCellRenderer)cls.newInstance();
			return rend;
		}
		catch (Exception e)
		{
			LogMgr.logError("RendererFactory.getIntegerRenderer()", "Error creating renderer", e);
			return new DefaultTableCellRenderer();
		}
	}
	
	public static TableCellRenderer getNumberRenderer(int maxDigits, char sep)
	{
		try
		{
			Class cls = Class.forName("workbench.gui.renderer.NumberColumnRenderer");
			Class[] types = new Class[] { int.class, char.class };
			Constructor cons = cls.getConstructor(types);
			Object[] args = new Object[] { new Integer(maxDigits), new Character(sep) };
			TableCellRenderer rend = (TableCellRenderer)cons.newInstance(args);
			return rend;
		}
		catch (Exception e)
		{
			LogMgr.logError("RendererFactory.getNumberRenderer()", "Error creating renderer", e);
			return new DefaultTableCellRenderer();
		}
	}

	public static TableCellRenderer getClobRenderer()
	{
		try
		{
			Class cls = Class.forName("workbench.gui.renderer.ClobColumnRenderer");
			TableCellRenderer rend = (TableCellRenderer)cls.newInstance();
			return rend;
		}
		catch (Exception e)
		{
			LogMgr.logError("RendererFactory.getClobRenderer()", "Error creating renderer", e);
			return new DefaultTableCellRenderer();
		}
	}
	
}
