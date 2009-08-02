/*
 * ReportTableGrants.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.report;

import java.util.Collection;
import java.util.Collections;
import workbench.db.TableGrant;
import workbench.db.TableGrantReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.util.StrBuffer;

/**
 * Generate XML report information about table grants.
 *
 * @see workbench.db.TableGrantReader#getTableGrants(workbench.db.WbConnection, workbench.db.TableIdentifier)
 * @author support@sql-workbench.net
 */
public class ReportTableGrants
{
	public static final String TAG_GRANT = "grant";
	public static final String TAG_GRANT_GRANTEE = "grantee";
	public static final String TAG_GRANT_PRIV = "privilege";
	public static final String TAG_GRANT_GRANTABLE = "grantable";
	private Collection<TableGrant> grants;

	public ReportTableGrants(WbConnection con, TableIdentifier tbl)
	{
		TableGrantReader reader = new TableGrantReader();
		grants = reader.getTableGrants(con, tbl);
	}

	public ReportTableGrants(Collection<TableGrant> tableGrants)
	{
		this.grants = tableGrants;
	}

	public void appendXml(StrBuffer result, StrBuffer indent)
	{
		if (grants.size() == 0) return;

		TagWriter tagWriter = new TagWriter();

		StrBuffer indent1 = new StrBuffer(indent);
		indent1.append("  ");

		for (TableGrant grant : grants)
		{
			tagWriter.appendOpenTag(result, indent, TAG_GRANT);
			result.append('\n');
			tagWriter.appendTag(result, indent1, TAG_GRANT_PRIV, grant.getPrivilege());
			tagWriter.appendTag(result, indent1, TAG_GRANT_GRANTEE, grant.getGrantee());
			tagWriter.appendTag(result, indent1, TAG_GRANT_GRANTABLE, grant.isGrantable());
			tagWriter.appendCloseTag(result, indent, TAG_GRANT);
		}
	}

	public Collection<TableGrant> getGrants()
	{
		return Collections.unmodifiableCollection(grants);
	}

}

