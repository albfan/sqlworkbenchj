/*
 * ConnectionProfile.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.db;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.postgres.PgPassReader;

import workbench.gui.profiles.ProfileKey;

import workbench.sql.DelimiterDefinition;

import workbench.util.CollectionUtil;
import workbench.util.FileDialogUtil;
import workbench.util.StringUtil;
import workbench.util.WbCipher;
import workbench.util.WbDesCipher;
import workbench.util.WbFile;

/**
 *	A class to store a connection definition including non-JDBC properties
 *  specific to the application.
 *	@author Thomas Kellerer
 */
public class ConnectionProfile
	implements PropertyChangeListener
{
	public static final String PROPERTY_PROFILE_GROUP = "profileGroup";
	private static final String CRYPT_PREFIX = "@*@";
	private String name;
	private String url;
	private String driverclass;
	private String username;
	private String temporaryUsername;
	private String password;
	private String driverName;
	private String group;
	private String icon;
	private boolean autocommit;
	private boolean storeCacheLocally;
	private boolean rollbackBeforeDisconnect;
	private boolean changed;
	private boolean groupChanged;
	private boolean isNew;
	private boolean usePgPass;
	private boolean storePassword = true;
	private boolean separateConnection;
	private Properties connectionProperties;
	private String workspaceFile;
	private boolean ignoreDropErrors;
	private boolean trimCharData;
	private boolean temporaryProfile;
	private boolean oracleSysDBA;
	private boolean detectOpenTransaction;

	private boolean readOnly;
	private boolean preventNoWhere;
	private boolean confirmUpdates;
	private boolean promptForUsername;

	private Integer defaultFetchSize;

	private boolean emptyStringIsNull;
	private boolean includeNullInInsert = true;
	private boolean removeComments;
	private boolean rememberExplorerSchema;
	private boolean hideWarnings;

	private String postConnectScript;
	private String preDisconnectScript;
	private String idleScript;
	private long idleTime = 0;
	private Color infoColor;
	private boolean copyPropsToSystem;
	private Integer connectionTimeout;

	private DelimiterDefinition alternateDelimiter;
	private ObjectNameFilter schemaFilter;
	private ObjectNameFilter catalogFilter;
	private String lastSettingsKey;
	private String macroFileName;
  private final Set<String> tags = CollectionUtil.caseInsensitiveSet();

	public ConnectionProfile()
	{
		this.isNew = true;
		this.changed = true;
		Settings.getInstance().addPropertyChangeListener(this, Settings.PROPERTY_ENCRYPT_PWD);
	}

	public ConnectionProfile(String profileName, String driverClass, String url, String userName, String pwd)
	{
		this();
		setUrl(url);
		setDriverclass(driverClass);
		setUsername(userName);
		setPassword(pwd);
		setName(profileName);
		reset();
	}

	public static ConnectionProfile createEmptyProfile()
	{
		ConnectionProfile cp = new ConnectionProfile();
		cp.setUseSeparateConnectionPerTab(true);
		cp.setStoreExplorerSchema(true);
		cp.setName(ResourceMgr.getString("TxtEmptyProfileName"));
		return cp;
	}

	private void syncSettingsKey()
	{
		if (lastSettingsKey == null) return;
		String newKey = getSettingsKey();
		if (newKey == null) return;

		Settings.getInstance().replacePartialKey("." + lastSettingsKey + ".", "."  + newKey + ".");
		this.lastSettingsKey = newKey;
	}

	public String getSettingsKey()
	{
		if (name == null || group == null) return null;
		Pattern p = Pattern.compile("[^0-9A-Za-z]+");
		Matcher gm = p.matcher(group);
		String cleanGroup = gm.replaceAll("").toLowerCase();

		Matcher nm = p.matcher(name);
		String cleanName = nm.replaceAll("").toLowerCase();
		String key = cleanGroup + "." + cleanName;
		return key;
	}

	public String getMacroFilename()
	{
		return macroFileName;
	}

	public WbFile getMacroFile()
	{
		if (StringUtil.isBlank(macroFileName)) return null;
		WbFile f = new WbFile(FileDialogUtil.replaceConfigDir(macroFileName));
		if (f.isAbsolute() && f.exists())
		{
			return f;
		}
		WbFile realFile = new WbFile(Settings.getInstance().getConfigDir(), macroFileName);
		if (realFile.exists() && realFile.isFile()) return realFile;
		return null;
	}

	public void setMacroFilename(String fname)
	{
		if (StringUtil.stringsAreNotEqual(fname, macroFileName))
		{
			this.macroFileName = fname;
			this.changed = true;
		}
	}

  public Set<String> getTags()
  {
    return Collections.unmodifiableSet(tags);
  }

  public String getTagList()
  {
    return StringUtil.listToString(tags, ',', false);
  }

  public void setTagList(String list)
  {
    List<String> tagList = StringUtil.stringToList(list, ",", true, true, false, false);
    changed = !tags.equals(tagList);
    tags.clear();
    tags.addAll(tagList);
  }

	public boolean getStoreCacheLocally()
	{
		return storeCacheLocally;
	}

	public void setStoreCacheLocally(boolean flag)
	{
		this.changed = (flag != storeCacheLocally);
		this.storeCacheLocally = flag;
	}

	public boolean getPromptForUsername()
	{
		return promptForUsername;
	}

	public void setPromptForUsername(boolean flag)
	{
		changed = (flag != promptForUsername);
		this.promptForUsername = flag;
	}

	public boolean getDetectOpenTransaction()
	{
		return detectOpenTransaction;
	}

	public void setDetectOpenTransaction(boolean flag)
	{
		changed = (flag != detectOpenTransaction);
		detectOpenTransaction = flag;
	}

	public boolean isTemporaryProfile()
	{
		return temporaryProfile;
	}

	public void setTemporaryProfile(boolean flag)
	{
		this.temporaryProfile = flag;
	}

	public ObjectNameFilter getCatalogFilter()
	{
		return catalogFilter;
	}

	public void setCatalogFilter(ObjectNameFilter filter)
	{
		if (catalogFilter == null && filter == null) return;
		if (filter == null)
		{
			changed = true;
		}
		else
		{
			changed = filter.isModified();
		}
		catalogFilter = filter;
	}

	public ObjectNameFilter getSchemaFilter()
	{
		return schemaFilter;
	}

	public void setSchemaFilter(ObjectNameFilter filter)
	{
		if (schemaFilter == null && filter == null) return;
		if (filter == null)
		{
			changed = true;
		}
		else
		{
			changed = filter.isModified();
		}
		schemaFilter = filter;
	}

	public Color getInfoDisplayColor()
	{
		return this.infoColor;
	}

	public boolean isHideWarnings()
	{
		return hideWarnings;
	}

	public void setHideWarnings(boolean flag)
	{
		this.changed = hideWarnings != flag;
		this.hideWarnings = flag;
	}

	public boolean getOracleSysDBA()
	{
		return oracleSysDBA;
	}

	public void setOracleSysDBA(boolean flag)
	{
		this.changed = oracleSysDBA != flag;
		this.oracleSysDBA = flag;
	}

	public int getConnectionTimeoutValue()
	{
		if (connectionTimeout == null) return 0;
		return connectionTimeout.intValue();
	}

	public Integer getConnectionTimeout()
	{
		return connectionTimeout;
	}

	public void setConnectionTimeout(Integer seconds)
	{
		int currentValue = (connectionTimeout == null ? Integer.MIN_VALUE : connectionTimeout.intValue());
		int newValue = (seconds == null ? Integer.MIN_VALUE : seconds.intValue());

		if (currentValue != newValue)
		{
			this.connectionTimeout = (newValue > 0 ? seconds : null);
			this.changed = true;
		}
	}

	public void setInfoDisplayColor(Color c)
	{
		if (this.infoColor == null && c == null) return;
		if (this.infoColor != null && c != null)
		{
			this.changed = !this.infoColor.equals(c);
		}
		else
		{
			this.changed = true;
		}
		this.infoColor = c;
	}

	public void setAlternateDelimiterString(String delim)
	{
		DelimiterDefinition def = new DelimiterDefinition(delim);
		if (!def.isStandard())
		{
			setAlternateDelimiter(def);
		}
	}

	public String getAlternateDelimiterString()
	{
		if (this.alternateDelimiter == null) return null;
		if (this.alternateDelimiter.isEmpty()) return null;
		return alternateDelimiter.getDelimiter();
	}

	public DelimiterDefinition getAlternateDelimiter()
	{
		if (this.alternateDelimiter == null) return null;
		if (this.alternateDelimiter.isEmpty()) return null;
		return this.alternateDelimiter;
	}

	public void setAlternateDelimiter(DelimiterDefinition def)
	{
		if (def == null && this.alternateDelimiter == null) return;

		// Do not accept a semicolon as the alternate delimiter
		if (def != null && def.isStandard()) return;

		if (!Objects.equals(def, this.alternateDelimiter) || (def != null && def.isChanged()))
		{
			this.alternateDelimiter = def;
			this.changed = true;
		}
	}

	public boolean getCopyExtendedPropsToSystem()
	{
		return this.copyPropsToSystem;
	}

	public void setCopyExtendedPropsToSystem(boolean flag)
	{
		if (flag != this.copyPropsToSystem) changed = true;
		this.copyPropsToSystem = flag;
	}

	public boolean isReadOnly()
	{
		return readOnly;
	}

	public void setReadOnly(boolean flag)
	{
		if (this.readOnly != flag) changed = true;
		this.readOnly = flag;
	}

	public void setPreventDMLWithoutWhere(boolean flag)
	{
		if (preventNoWhere != flag) changed = true;
		preventNoWhere = flag;
	}

	public boolean getPreventDMLWithoutWhere()
	{
		return preventNoWhere;
	}

	public boolean getTrimCharData()
	{
		return trimCharData;
	}

	public void setTrimCharData(boolean flag)
	{
		if (flag != trimCharData) changed = true;
		trimCharData = flag;
	}

	public boolean getStoreExplorerSchema()
	{
		return rememberExplorerSchema;
	}

	public void setStoreExplorerSchema(boolean value)
	{
		if (value != rememberExplorerSchema) changed = true;
		rememberExplorerSchema = value;
	}

	public String getGroup()
	{
		if (this.group == null) return ResourceMgr.getString("LblDefGroup");
		return this.group;
	}

	public void setGroup(String g)
	{
		if (StringUtil.equalString(this.group, g)) return;
		this.group = g;
		this.changed = true;
		this.groupChanged = true;
		syncSettingsKey();
	}

	public String getIcon()
	{
		return this.icon;
	}

	public void setIcon(String icon)
	{
		if (this.icon != null && StringUtil.equalString(this.icon, icon)) return;
		this.icon = icon;
		this.changed = true;
	}

	public boolean isProfileForKey(ProfileKey key)
	{
		ProfileKey myKey = getKey();
		return myKey.equals(key);
	}

	public ProfileKey getKey()
	{
		return new ProfileKey(this.getName(), this.getGroup());
	}

	/**
	 * This method is used for backward compatibility. Old profiles
	 * had this property and to be able to load XML files with
	 * old profiles the setter must still be there.
	 *
	 * @deprecated
	 * @param flag
	 */
	public void setDisableUpdateTableCheck(boolean flag) { }

	/**
	 * Return true if the application should use a separate connection
	 * per tab or if all SQL tabs including DbExplorer tabs and windows
	 * should share the same connection
	 */
	public boolean getUseSeparateConnectionPerTab()
	{
		return this.separateConnection;
	}

	public void setUseSeparateConnectionPerTab(boolean aFlag)
	{
		if (this.separateConnection != aFlag) this.changed = true;
		this.separateConnection = aFlag;
	}

	public void setIncludeNullInInsert(boolean flag)
	{
		if (this.includeNullInInsert != flag) this.changed = true;
		this.includeNullInInsert = flag;
	}

	/**
	 * Define how columns with a NULL value are treated when creating INSERT statements.
	 * If this is set to false, then any column with an a NULL value
	 * will not be included in an generated INSERT statement.
	 *
	 * @see workbench.storage.StatementFactory#createInsertStatement(workbench.storage.RowData, boolean, String, java.util.List)
	 */
	public boolean getIncludeNullInInsert()
	{
		return this.includeNullInInsert;
	}

	/**
	 * Define how empty strings (Strings with length == 0) are treated.
	 * If this is set to true, then they are treated as a NULL value, else an
	 * empty string is sent to the database during update and insert.
	 * @see #setIncludeNullInInsert(boolean)
	 */
	public void setEmptyStringIsNull(boolean flag)
	{
		if (this.emptyStringIsNull != flag) this.changed = true;
		this.emptyStringIsNull = flag;
	}

	public boolean getEmptyStringIsNull()
	{
		return this.emptyStringIsNull;
	}

	/**
	 * Define how comments inside SQL statements are handled.
	 * If this is set to true, then any comment (single line comments with --
	 * or multi-line comments using /* are removed from the statement
	 * before sending it to the database.
	 *
	 * @see workbench.sql.StatementRunner#runStatement(java.lang.String)
	 */
	public void setRemoveComments(boolean flag)
	{
		if (this.removeComments != flag) this.changed = true;
		this.removeComments = flag;
	}

	public boolean getRemoveComments()
	{
		return this.removeComments;
	}

	/**
	 * @deprecated Replaced by {@link #setUseSeparateConnectionPerTab(boolean)}
	 */
	public void setUseSeperateConnectionPerTab(boolean aFlag) { this.setUseSeparateConnectionPerTab(aFlag); }

	public boolean getRollbackBeforeDisconnect()
	{
		return this.rollbackBeforeDisconnect;
	}

	public void setRollbackBeforeDisconnect(boolean flag)
	{
		if (flag != this.rollbackBeforeDisconnect) this.changed = true;
		this.rollbackBeforeDisconnect = flag;
	}

	/**
	 *	Sets the current password. If the password
	 *	is not already encrypted, it will be encrypted
	 *
	 *	@see #getPassword()
	 *	@see workbench.util.WbCipher#encryptString(String)
	 */
	public final void setPassword(String pwd)
	{
		if (pwd == null)
		{
			if (this.password != null)
			{
				this.password = null;
				if (this.storePassword) this.changed = true;
			}
			return;
		}

		pwd = pwd.trim();

		// check encryption settings when reading the profiles...
		if (Settings.getInstance().getUseEncryption())
		{
			if (!this.isEncrypted(pwd))
			{
				pwd = this.encryptPassword(pwd);
			}
		}
		else
		{
			if (this.isEncrypted(pwd))
			{
				pwd = this.decryptPassword(pwd);
			}
		}

		if (!pwd.equals(this.password))
		{
			this.password = pwd;
			if (this.storePassword) this.changed = true;
		}
	}


	/**
	 *	Returns the encrypted version of the password.
	 *	This getter/setter pair is used when saving the profile
	 *	@see #decryptPassword(String)
	 */
	public String getPassword()
	{
		if (this.storePassword)
		{
			return this.password;
		}
		else
		{
			return null;
		}
	}

	public boolean usePgPass()
	{
		return usePgPass;
	}

	/**
	 * Set the password from a plain readable text
	 * @param aPassword
	 */
	public void setInputPassword(String aPassword)
	{
		this.setPassword(aPassword);
	}

	/**
	 *	Returns the plain text version of the
	 *	current password.
	 *
	 *	@see #encryptPassword(String)
	 */
	public String decryptPassword()
	{
		return this.decryptPassword(getPassword());
	}

	/**
	 *	Returns the plain text version of the given
	 *	password. This is not put into the getPassword()
	 *	method because the XMLEncode would write the
	 *	password in plain text into the XML file.
	 *
	 *	A method beginning with decrypt is not
	 *	regarded as a property and thus not written
	 *	to the XML file.
	 *
	 *	@param aPwd the encrypted password
	 */
	public String decryptPassword(String pwd)
	{
		if (StringUtil.isEmptyString(pwd)) return "";
		if (!isEncrypted(pwd))
		{
			return pwd;
		}
		else
		{
			WbCipher des = WbDesCipher.getInstance();
			return des.decryptString(pwd.substring(CRYPT_PREFIX.length()));
		}
	}

	private boolean isEncrypted(String aPwd)
	{
		return aPwd.startsWith(CRYPT_PREFIX);
	}

	public void setNew()
	{
		this.changed = true;
		this.isNew = true;
	}

	public boolean isNew()
	{
		return this.isNew;
	}

	public boolean isChanged()
	{
		return this.changed || this.isNew;
	}

	public boolean isGroupChanged()
	{
		return this.groupChanged;
	}

	/**
	 * Reset the changed and new flags.
	 * @see #isNew()
	 * @see #isChanged()
	 */
	public final void reset()
	{
		this.changed = false;
		this.groupChanged = false;
		this.isNew = false;
		if (this.alternateDelimiter != null) this.alternateDelimiter.resetChanged();
		if (this.schemaFilter != null) schemaFilter.resetModified();
		if (this.catalogFilter != null) catalogFilter.resetModified();
		this.lastSettingsKey = getSettingsKey();
	}

	private String encryptPassword(String aPwd)
	{
		if (Settings.getInstance().getUseEncryption() && !this.isEncrypted(aPwd))
		{
			WbCipher des = WbDesCipher.getInstance();
			aPwd = CRYPT_PREFIX + des.encryptString(aPwd);
		}
		return aPwd;
	}

	/**
	 *	Returns the name of the Profile
	 */
	@Override
	public String toString()
	{
		return this.name;
	}

	public String getIdString()
	{
		return getKey().toString() + " using: " + this.driverclass;
	}
	/**
	 * The hashCode is based on the profile key's hash code.
	 *
	 * @see #getKey()
	 * @see ProfileKey#hashCode()
	 * @return the hashcode for the profile key
	 */
	@Override
	public int hashCode()
	{
		return getKey().hashCode();
	}

	@Override
	public boolean equals(Object other)
	{
		try
		{
			ConnectionProfile prof = (ConnectionProfile)other;
			return this.getKey().equals(prof.getKey());
		}
		catch (ClassCastException e)
		{
			return false;
		}
	}

	public String getUrl()
	{
		return this.url;
	}

	public final void setUrl(String newUrl)
	{
		if (newUrl != null) newUrl = newUrl.trim();
		if (StringUtil.stringsAreNotEqual(newUrl, url)) changed = true;
		url = newUrl;
    usePgPass = PgPassReader.isPgUrl(url) && Settings.getInstance().usePgPassFile();
	}

	public String getDriverName()
	{
		return driverName;
	}

	public final void setDriverName(String name)
	{
		if (name != null && StringUtil.stringsAreNotEqual(name, this.driverName))
		{
			this.driverName = name.trim();
			this.changed = true;
		}
	}

	public String getDriverclass()
	{
		return this.driverclass;
	}

	public final void setDriverclass(String drvClass)
	{
		if (drvClass != null && StringUtil.stringsAreNotEqual(drvClass, driverclass))
		{
			changed = true;
			this.driverclass = drvClass.trim();
		}
	}

	public void setDriver(DbDriver driver)
	{
		if (driver != null)
		{
			setDriverName(driver.getName());
			setDriverclass(driver.getDriverClass());
		}
	}

	public String getUsername()
	{
		if (temporaryUsername != null) return temporaryUsername;
		return this.username;
	}

	private String getPgPassPassword()
	{
		if (usePgPass())
		{
			PgPassReader reader = new PgPassReader(url, getLoginUser());
      LogMgr.logDebug("ConnectionProfile.getPgPassPassword()", "Using password from " + PgPassReader.getPgPassFile());
			return reader.getPasswordFromFile();
		}
		return null;
	}

	public String getLoginPassword()
	{
		if (StringUtil.isNonEmpty(password))
		{
			return this.decryptPassword(password);
		}

		if (usePgPass() && !getStorePassword())
		{
			return getPgPassPassword();
		}
		return null;
	}

	public String getLoginUser()
	{
		String user = getUsername();
		if (usePgPass() && StringUtil.isEmptyString(user))
		{
			user = System.getenv("PGUSER");
			if (user == null)
			{
				user = System.getProperty("user.name");
			}
		}

    if (Settings.getInstance().replaceEnvVarsInProfile())
    {
      user = StringUtil.replaceProperties(user); // replace standard Java properties first
      user = StringUtil.replaceProperties(System.getenv(), user); // now replace system environment variables
    }

		return user;
	}

  public boolean needsPasswordPrompt()
  {
    return getLoginPassword() == null && getStorePassword() == false;
  }

	public void setTemporaryUsername(String tempName)
	{
		this.temporaryUsername = tempName;
	}

	public final void setUsername(String newName)
	{
		if (newName != null) newName = newName.trim();
		if (!StringUtil.equalString(newName, username) && !changed && !promptForUsername) changed = true;
		this.temporaryUsername = null;
		this.username = newName;
	}

	public boolean getAutocommit()
	{
		return this.autocommit;
	}

	public void setAutocommit(boolean aFlag)
	{
		if (aFlag != this.autocommit && !changed)
		{
			this.changed = true;
		}
		this.autocommit = aFlag;
	}

	public String getName()
	{
		return this.name;
	}

	public final void setName(String aName)
	{
		if (!changed && !StringUtil.equalString(name, aName)) changed = true;
		this.name = aName;
		syncSettingsKey();
	}

	public boolean getStorePassword()
	{
		return this.storePassword;
	}

	public void setStorePassword(boolean aFlag)
	{
		if (aFlag != this.storePassword && !this.changed)
		{
			this.changed = true;
		}
		this.storePassword = aFlag;
	}


	/**
	 * Returns a copy of this profile keeping it's modified state.
	 * isNew() and isChanged() of the copy will return the same values as this instance
	 *
	 * @return a copy of this profile
	 * @see #isNew()
	 * @see #isChanged()
	 */
	public ConnectionProfile createStatefulCopy()
	{
		ConnectionProfile result = createCopy();
		result.isNew = this.isNew;
		result.changed = this.changed;
		return result;
	}

	/**
	 * Returns a copy of this profile.
	 *
	 * The copy is marked as "new" and "changed", so isNew() and isChanged()
	 * will return true on the copy
	 *
	 * @return a copy of this profile
	 * @see #isNew()
	 * @see #isChanged()
	 */
	public ConnectionProfile createCopy()
	{
		ConnectionProfile result = new ConnectionProfile();
		result.setUrl(url);
		result.setAutocommit(autocommit);
		result.setDriverclass(driverclass);
		result.setConnectionTimeout(connectionTimeout);
		result.setDriverName(driverName);
		result.setName(name);
		result.setGroup(group);
		result.setIcon(icon);
		result.setPassword(getPassword());
		result.setOracleSysDBA(oracleSysDBA);
		result.setUsername(username);
		result.setWorkspaceFile(workspaceFile);
		result.setIgnoreDropErrors(ignoreDropErrors);
		result.setUseSeparateConnectionPerTab(separateConnection);
		result.setTrimCharData(trimCharData);
		result.setIncludeNullInInsert(includeNullInInsert);
		result.setEmptyStringIsNull(emptyStringIsNull);
		result.setRollbackBeforeDisconnect(rollbackBeforeDisconnect);
		result.setConfirmUpdates(confirmUpdates);
		result.setStorePassword(storePassword);
		result.setDefaultFetchSize(defaultFetchSize);
		result.setStoreExplorerSchema(rememberExplorerSchema);
		result.setIdleScript(idleScript);
		result.setIdleTime(idleTime);
		result.setPreDisconnectScript(preDisconnectScript);
		result.setPostConnectScript(postConnectScript);
		result.setInfoDisplayColor(infoColor);
		result.setReadOnly(readOnly);
		result.setAlternateDelimiter(alternateDelimiter == null ? null : alternateDelimiter.createCopy());
		result.setHideWarnings(hideWarnings);
		result.setCopyExtendedPropsToSystem(copyPropsToSystem);
		result.setRemoveComments(this.removeComments);
		result.setCatalogFilter(this.catalogFilter == null ? null : catalogFilter.createCopy());
		result.setSchemaFilter(this.schemaFilter == null ? null : schemaFilter.createCopy());
		result.setDetectOpenTransaction(this.detectOpenTransaction);
		result.setPreventDMLWithoutWhere(this.preventNoWhere);
		result.setPromptForUsername(this.promptForUsername);
		result.setStoreCacheLocally(this.storeCacheLocally);
		result.setMacroFilename(this.macroFileName);
    result.setTagList(getTagList());
		result.lastSettingsKey = this.lastSettingsKey;
		result.temporaryUsername = null;
		if (connectionProperties != null)
		{
			Enumeration keys = connectionProperties.propertyNames();
			result.connectionProperties = new Properties();

			while (keys.hasMoreElements())
			{
				String key = (String)keys.nextElement();
				String value = connectionProperties.getProperty(key);
				result.connectionProperties.put(key, value);
			}
		}
		return result;
	}

	public static Comparator<ConnectionProfile> getNameComparator()
	{
		return new Comparator<ConnectionProfile>()
		{
			@Override
			public int compare(ConnectionProfile o1, ConnectionProfile o2)
			{
				if (o1 == null && o2 == null) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return StringUtil.compareStrings(o1.name, o2.name, true);
			}
		};
	}

	public boolean getIgnoreDropErrors()
	{
		return this.ignoreDropErrors;
	}

	public void setIgnoreDropErrors(boolean aFlag)
	{
		changed = aFlag != this.ignoreDropErrors;
		this.ignoreDropErrors = aFlag;
	}

	public String getWorkspaceFile()
	{
		return this.workspaceFile;
	}

	public void setWorkspaceFile(String aWorkspaceFile)
	{
		this.workspaceFile = aWorkspaceFile;
		this.changed = true;
	}

	public void addConnectionProperty(String aKey, String aValue)
	{
		if (aKey == null) return;
		if (this.connectionProperties == null)
		{
			this.connectionProperties = new Properties();
		}
		this.connectionProperties.put(aKey, aValue);
		this.changed = true;
	}

	public Properties getConnectionProperties()
	{
		return this.connectionProperties;
	}

	public void setConnectionProperties(Properties props)
	{
		boolean wasDefined = (this.connectionProperties != null && this.connectionProperties.size() > 0);
		if (props != null)
		{
			if (props.isEmpty())
			{
				this.connectionProperties = null;
				if (wasDefined) this.changed = true;
			}
			else
			{
				this.connectionProperties = props;
				this.changed = true;
			}
		}
		else
		{
			this.connectionProperties = null;
			if (wasDefined) this.changed = true;
		}
	}

	public boolean getConfirmUpdates()
	{
		return confirmUpdates;
	}

	public void setConfirmUpdates(boolean flag)
	{
		if (flag != this.confirmUpdates) this.changed = true;
		this.confirmUpdates = flag;
	}

	@Override
	public void propertyChange(java.beans.PropertyChangeEvent evt)
	{
		if (Settings.PROPERTY_ENCRYPT_PWD.equals(evt.getPropertyName()))
		{
			String old = this.password;
			// calling setPassword will encrypt/decrypt the password
			// according to the current setting
			this.setPassword(old);
		}
	}

	public int getFetchSize()
	{
		if (this.defaultFetchSize == null) return -1;
		else return this.defaultFetchSize.intValue();
	}

	public Integer getDefaultFetchSize()
	{
		return defaultFetchSize;
	}

	public void setDefaultFetchSize(Integer fetchSize)
	{
		int currentValue = (defaultFetchSize == null ? Integer.MIN_VALUE : defaultFetchSize.intValue());
		int newValue = (fetchSize == null ? Integer.MIN_VALUE : fetchSize.intValue());

		if (currentValue != newValue)
		{
			this.defaultFetchSize = (newValue > 0 ? fetchSize : null);
			this.changed = true;
		}
	}

	public boolean hasConnectScript()
	{
		return StringUtil.isNonBlank(postConnectScript) ||
			StringUtil.isNonBlank(preDisconnectScript) ||
			(StringUtil.isNonBlank(idleScript) && idleTime > 0);
	}

	public String getPostConnectScript()
	{
		return postConnectScript;
	}


	public void setPostConnectScript(String script)
	{
		if (!StringUtil.equalStringOrEmpty(script, this.postConnectScript))
		{
			if (StringUtil.isBlank(script))
			{
				this.postConnectScript = null;
			}
			else
			{
				this.postConnectScript = script.trim();
			}
			this.changed = true;
		}
	}

	public String getPreDisconnectScript()
	{
		return preDisconnectScript;
	}

	public void setPreDisconnectScript(String script)
	{
		if (!StringUtil.equalStringOrEmpty(script, this.preDisconnectScript))
		{
			if (StringUtil.isBlank(script))
			{
				this.preDisconnectScript = null;
			}
			else
			{
				this.preDisconnectScript = script.trim();
			}
			this.changed = true;
		}
	}

	public long getIdleTime()
	{
		return this.idleTime;
	}

	public void setIdleTime(long time)
	{
		if (time != this.idleTime && !changed)
		{
			this.changed = true;
		}
		this.idleTime = time;
	}

	public String getIdleScript()
	{
		return idleScript;
	}

	public void setIdleScript(String script)
	{
		if (!StringUtil.equalStringOrEmpty(script, this.idleScript))
		{
			if (StringUtil.isBlank(script))
			{
				this.idleScript = null;
			}
			else
			{
				this.idleScript = script.trim();
			}
			this.changed = true;
		}
	}

	public static String makeFilename(String jdbcUrl, String userName)
	{
		Pattern invalidChars = Pattern.compile("[^a-zA-Z0-9$]+");
		Matcher urlMatcher = invalidChars.matcher(jdbcUrl);
		String url = urlMatcher.replaceAll("_");

		// remove the jdbc_ prefix, it is not needed
		url = url.substring(5);

		String user = "";
		if (StringUtil.isNonBlank(userName))
		{
			Matcher userMatcher = invalidChars.matcher(userName);
			user = userMatcher.replaceAll("_") + "@";
		}
		return user.toLowerCase() + url.toLowerCase();
	}

}
