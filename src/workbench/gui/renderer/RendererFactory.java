/*
 * RendererFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
	
	private static TableCellRenderer createRenderer(String className)
	{
		TableCellRenderer rend = null;
		try
		{
			Class cls = Class.forName(className);
			rend = (TableCellRenderer)cls.newInstance();
		}
		catch (Exception e)
		{
			LogMgr.logError("RendererFactory.createRenderer()", "Error creating renderer", e);
			rend = new DefaultTableCellRenderer();
		}
		return rend;
	}	
	
	public static TableCellRenderer getSortHeaderRenderer()
	{
		return createRenderer("workbench.gui.components.SortHeaderRenderer");
	}
	
	public static TableCellRenderer getDateRenderer(String format)
	{
		TableCellRenderer rend = null;
		try
		{
			Class cls = Class.forName("workbench.gui.renderer.DateColumnRenderer");
			Class[] types = new Class[] { String.class };
			Constructor cons = cls.getConstructor(types);
			Object[] args = new Object[] { format };
			rend = (TableCellRenderer)cons.newInstance(args);
		}
		catch (Exception e)
		{
			LogMgr.logError("RendererFactory.getDateRenderer()", "Error creating renderer", e);
			return new DefaultTableCellRenderer();
		} 
		return rend;
	}

	public static TableCellRenderer getTooltipRenderer()
	{
		return createRenderer("workbench.gui.renderer.ToolTipRenderer");
	}
	
	public static TableCellRenderer getStringRenderer()
	{
		return createRenderer("workbench.gui.renderer.StringColumnRenderer");
	}
	
	public static TableCellRenderer getIntegerRenderer()
	{
		return createRenderer("workbench.gui.renderer.NumberColumnRenderer");
	}
	
	public static TableCellRenderer createNumberRenderer(int maxDigits, char sep)
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

	public static TableCellRenderer getSqlTypeRenderer()
	{
		return createRenderer("workbench.gui.renderer.SqlTypeRenderer");
	}
	
	public static TableCellRenderer getClobRenderer()
	{
		return createRenderer("workbench.gui.renderer.ClobColumnRenderer");
	}

	public static TableCellRenderer getBlobRenderer()
	{
		return createRenderer("workbench.gui.renderer.BlobColumnRenderer");
	}
	
}
