/*
 * Created on 3. Dezember 2002, 23:31
 */
package workbench.db.oracle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import workbench.db.TableIdentifier;

/**
 *
 * @author  workbench@kellerer.org
 */
public class SynonymReader
{
	public static TableIdentifier getSynonymTable(Connection con, String anOwner, String aSynonym)
		throws SQLException
	{
		StringBuffer sql = new StringBuffer(200);
		sql.append("SELECT synonym_name, table_owner, table_name FROM all_synonyms ");
		sql.append(" WHERE synonym_name = ? AND owner = ?");
		
		PreparedStatement stmt = con.prepareStatement(sql.toString());
		stmt.setString(1, aSynonym);
		stmt.setString(2, anOwner);
		
		ResultSet rs = stmt.executeQuery();
		String table = null;
		String owner = null;
		TableIdentifier result = null;
		if (rs.next())
		{
			owner = rs.getString(2);
			table = rs.getString(3);
			result = new TableIdentifier(null, owner, table);
		}
		rs.close();
		stmt.close();
		return result;
	}
	
	public static String getSynonymSource(Connection con, String anOwner, String aSynonym)
		throws SQLException
	{
		TableIdentifier id = getSynonymTable(con, anOwner, aSynonym);
		StringBuffer result = new StringBuffer(200);
		result.append("CREATE SYNONYM ");
		result.append(aSynonym);
		result.append("\n       FOR ");
		result.append(id.getTableExpression());
		result.append(';');
		return result.toString();
	}
	
}


