/*
 * MacroManager.java
 *
 * Created on July 5, 2003, 10:55 PM
 */

package workbench.sql;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import workbench.WbManager;
import workbench.log.LogMgr;
import workbench.util.WbPersistence;

/**
 *
 * @author  thomas
 */
public class MacroManager
{
	private static MacroManager instance = new MacroManager();
	private HashMap macros;
	private boolean modified = false;
	
	public static MacroManager getInstance()
	{
		return instance;
	}

	public String getMacroText(String aKey)
	{
		if (aKey == null) return null;
		this.checkMacros();
		return (String)this.macros.get(aKey.toLowerCase());
	}
	
	
	public void removeMacro(String aKey)
	{
		if (aKey == null) return;
		this.checkMacros();
		this.macros.remove(aKey);
		this.modified = true;
	}
	
	public List getMacroList()
	{
		this.checkMacros();
		Set keys = this.macros.keySet();
		ArrayList result = new ArrayList(keys);
		return result;
	}
	
	public String[] getMacroNames()
	{
		this.checkMacros();
		Set keys = this.macros.keySet();
		String[] result = new String[keys.size()];
		Iterator itr = keys.iterator();
		for (int i=0; itr.hasNext(); i++)
		{
			result[i] = (String)itr.next();
		}
		return result;
	}
	 
	public void setMacro(String aKey, String aText)
	{
		if (aKey == null || aKey.trim().length() == 0) return;
		this.checkMacros();
		this.macros.put(aKey.toLowerCase(), aText);
		this.modified = true;
	}

	public boolean isModified() { return this.modified; }
	
	public void clearAll()
	{
		if (this.macros != null)
		{
			this.macros.clear();
		}
		else
		{
			this.macros = new HashMap();
		}
		this.modified = true;
	}
	
	public void saveMacros()
	{
		if (this.macros != null && this.macros.size() > 0)
		{
			WbPersistence.writeObject(this.macros, this.getMacroFile().getAbsolutePath());
			this.modified = false;
		}
	}

	private MacroManager()
	{
	}

	private void checkMacros()
	{
		if (this.macros == null) this.loadMacros();
	}
	private File getMacroFile()
	{
		String configDir = WbManager.getSettings().getConfigDir();
		File f = new File(configDir, "WbMacros.xml");
		return f;
	}
	
	private void loadMacros()
	{
		try
		{
			Object o = WbPersistence.readObject(this.getMacroFile().getAbsolutePath());
			if (o instanceof HashMap)
			{
				this.macros = (HashMap)o;
			}
			else
			{
				this.macros = new HashMap(20);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("MacroManager.loadMacros()", "Error loading macro file", e);
		}
		this.modified = false;
	}
	

}

