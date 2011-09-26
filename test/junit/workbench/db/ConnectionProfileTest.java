/*
 * ConnectionProfileTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import workbench.WbTestCase;
import workbench.sql.DelimiterDefinition;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Thomas Kellerer
 */
public class ConnectionProfileTest
	extends WbTestCase
{
	public ConnectionProfileTest()
	{
		super("ConnectionProfileTest");
	}

	@Test
	public void testCreateCopy()
		throws Exception
	{
		ConnectionProfile old = new ConnectionProfile();
		old.setAlternateDelimiter(new DelimiterDefinition("/", true));
		old.setAutocommit(false);
		old.setConfirmUpdates(true);
		old.setDriverName("Postgres");
		old.setConnectionTimeout(42);
		old.setEmptyStringIsNull(true);
		old.setUseSeparateConnectionPerTab(true);
		old.setDetectOpenTransaction(true);
		old.setIgnoreDropErrors(true);
		old.setStoreExplorerSchema(true);
		old.setName("First");
		old.setStorePassword(true);
		old.setCopyExtendedPropsToSystem(true);
		old.setIncludeNullInInsert(true);
		old.setIdleTime(42);
		old.setTrimCharData(true);
		old.setIdleScript("select 12 from dual");
		old.setPostConnectScript("drop database");
		old.setPreDisconnectScript("shutdown abort");
		old.setUrl("jdbc:some:database");
		old.setHideWarnings(true);
		old.setRemoveComments(true);
		ObjectNameFilter filter = new ObjectNameFilter();
		filter.addExpression("^pg_toast.*");
		filter.resetModified();
		old.setCatalogFilter(filter);

		ConnectionProfile copy = old.createCopy();
		assertFalse(copy.getAutocommit());
		assertTrue(copy.getConfirmUpdates());
		assertTrue(copy.getDetectOpenTransaction());
		assertEquals("Postgres", copy.getDriverName());
		assertEquals("First", copy.getName());
		assertTrue(copy.getStorePassword());
		assertTrue(copy.getUseSeparateConnectionPerTab());
		assertTrue(copy.getStoreExplorerSchema());
		assertTrue(copy.getIgnoreDropErrors());
		assertTrue(copy.getTrimCharData());
		assertTrue(copy.getIncludeNullInInsert());
		assertTrue(copy.getRemoveComments());
		assertNull(copy.getSchemaFilter());
		assertNotNull(copy.getCatalogFilter());
		assertEquals(1, copy.getCatalogFilter().getSize());
		assertEquals(42, copy.getIdleTime());
		assertEquals(filter, copy.getCatalogFilter());
		assertEquals(Integer.valueOf(42), copy.getConnectionTimeout());

		assertEquals("select 12 from dual", old.getIdleScript());
		assertEquals("jdbc:some:database", copy.getUrl());
		assertTrue(copy.isHideWarnings());

		assertEquals("drop database", old.getPostConnectScript());
		assertEquals("shutdown abort", old.getPreDisconnectScript());

		DelimiterDefinition delim = copy.getAlternateDelimiter();
		assertNotNull(delim);
		assertEquals("/", delim.getDelimiter());
		assertTrue(delim.isSingleLine());
		assertTrue(copy.getCopyExtendedPropsToSystem());

		old.setAlternateDelimiter(null);
		copy = old.createCopy();
		assertNull(copy.getAlternateDelimiter());
	}

	@Test
	public void testSessionProps()
	{
		ConnectionProfile profile = new ConnectionProfile();
		profile.setName("ProfileTest");
		profile.setAlternateDelimiter(new DelimiterDefinition("/", true));
		profile.setAutocommit(false);
		profile.setDriverName("Postgres");
		profile.reset();

		assertFalse(profile.readOnlySession());
		assertFalse(profile.confirmUpdatesInSession());

		profile.setSessionReadOnly(true);
		assertTrue(profile.readOnlySession());
		assertFalse(profile.isReadOnly());

		profile.setSessionConfirmUpdate(true);
		assertFalse(profile.isReadOnly());
		assertFalse(profile.readOnlySession());
		assertTrue(profile.confirmUpdatesInSession());
		assertFalse(profile.getConfirmUpdates());
	}

	@Test
	public void testProps()
		throws Exception
	{
		ConnectionProfile profile = new ConnectionProfile();
		profile.setName("ProfileTest");
		profile.setAlternateDelimiter(new DelimiterDefinition("/", true));
		profile.setAutocommit(false);
		profile.setConfirmUpdates(true);
		profile.setDriverName("Postgres");
		profile.reset();

		Properties props = new Properties();
		props.setProperty("remarksReporting", "true");
		profile.setConnectionProperties(props);
		assertTrue(profile.isChanged());
		profile.setCopyExtendedPropsToSystem(true);
		assertTrue(profile.isChanged());

		profile.setAutocommit(true);
		profile.setConfirmUpdates(false);
		assertTrue(profile.isChanged());

		profile.setAutocommit(true);
		profile.setConfirmUpdates(false);
		assertTrue(profile.isChanged());

		profile.setUrl("jdbc:postgres:local");
		assertTrue(profile.isChanged());

		profile.setUrl("jdbc:postgres:local");
		assertTrue(profile.isChanged());

		profile.setHideWarnings(false);
		profile.reset();
		profile.setHideWarnings(true);
		assertTrue(profile.isChanged());

		profile.reset();
		// Changing to a new URL has to be reflected
		profile.setUrl("jdbc:postgres:local;someProp=myValue");
		assertTrue(profile.isChanged());

		profile.setInputPassword("welcome");
		profile.setStorePassword(true);
		profile.reset();

		// check if changing the password sets the changed flag
		profile.setInputPassword("secret");
		assertTrue(profile.isChanged());

		profile.setStorePassword(false);
		profile.reset();
		profile.setInputPassword("welcome");
		// password are not saved, changing the password should not mark the profile
		// as changed
		assertFalse(profile.isChanged());

		profile.setEmptyStringIsNull(false);
		profile.reset();
		profile.setEmptyStringIsNull(true);
		assertTrue(profile.isChanged());
		profile.setEmptyStringIsNull(true);
		assertTrue(profile.isChanged());

		profile.setUseSeparateConnectionPerTab(false);
		profile.reset();
		profile.setUseSeparateConnectionPerTab(true);
		assertTrue(profile.isChanged());
		profile.setUseSeparateConnectionPerTab(true);
		assertTrue(profile.isChanged());

		profile.setStoreExplorerSchema(false);
		profile.reset();
		profile.setStoreExplorerSchema(true);
		assertTrue(profile.isChanged());
		profile.setStoreExplorerSchema(true);
		assertTrue(profile.isChanged());

		profile.reset();
		profile.setDriverName("Postgres 8.3");
		assertTrue(profile.isChanged());

		profile.reset();
		profile.setName("NewName");
		assertTrue(profile.isChanged());

		profile.setTrimCharData(false);
		profile.reset();
		profile.setTrimCharData(true);
		assertTrue(profile.isChanged());

		profile.setIgnoreDropErrors(false);
		profile.reset();
		profile.setIgnoreDropErrors(true);
		assertTrue(profile.isChanged());

		profile.setRollbackBeforeDisconnect(false);
		profile.reset();
		profile.setRollbackBeforeDisconnect(true);
		assertTrue(profile.isChanged());

		profile.setReadOnly(false);
		profile.reset();
		profile.setReadOnly(true);
		assertTrue(profile.isChanged());

		profile.reset();
		DelimiterDefinition def = new DelimiterDefinition("GO", true);
		profile.setAlternateDelimiter(def);
		assertTrue(profile.isChanged());

		profile.reset();
		profile.setInfoDisplayColor(Color.MAGENTA);
		assertTrue(profile.isChanged());

		profile.reset();
		profile.setWorkspaceFile("Arthur.wksp");
		assertTrue(profile.isChanged());

		profile.reset();
		profile.setDefaultFetchSize(4242);
		assertTrue(profile.isChanged());

		profile.setConnectionTimeout(1);
		profile.reset();
		profile.setConnectionTimeout(42);
		assertTrue(profile.isChanged());
	}

	@Test
	public void testFindInList()
	{
		ConnectionProfile profile = new ConnectionProfile();
		profile.setAlternateDelimiter(new DelimiterDefinition("/", true));
		profile.setAutocommit(false);
		profile.setDriverName("Postgres");
		profile.setEmptyStringIsNull(true);
		profile.setIgnoreDropErrors(true);
		profile.setName("First");
		profile.setGroup("Primary");
		profile.setStorePassword(true);

		List<ConnectionProfile> profiles = new ArrayList<ConnectionProfile>();
		profiles.add(profile);

		ConnectionProfile profile2 = new ConnectionProfile();
		profile2.setAutocommit(false);
		profile2.setDriverName("PostgreSQL");
		profile2.setEmptyStringIsNull(false);
		profile2.setIgnoreDropErrors(false);
		profile2.setName("First");
		profile2.setGroup("Primary");
		profile2.setStorePassword(false);

		profiles.remove(profile2);
		profiles.add(profile2);
		assertEquals(1, profiles.size());
	}
}
