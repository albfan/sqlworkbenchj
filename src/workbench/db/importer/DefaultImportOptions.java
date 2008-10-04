/*
 * DefaultImportOptions.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import workbench.gui.dialogs.dataimport.ImportOptions;
import workbench.resource.Settings;

/**
 * @author support@sql-workbench.net
 */
public class DefaultImportOptions
	implements ImportOptions
{
	
	public String getEncoding() {  return "UTF-8"; }
	public String getDateFormat() {	return Settings.getInstance().getDefaultDateFormat(); }
	public String getTimestampFormat() { return Settings.getInstance().getDefaultTimestampFormat(); }

	public void setEncoding(String enc) {	}
	public void setDateFormat(String format) { }
	public void setTimestampFormat(String format) {	}
	public void setMode(String mode) { }
	public String getMode() { return "insert"; }
}
