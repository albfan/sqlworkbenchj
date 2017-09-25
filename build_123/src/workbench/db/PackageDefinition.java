/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PackageDefinition
  implements DbObject
{
  private String schema;
  private String packageName;
  private String remarks;

  public PackageDefinition(String schemaName, String pkgName)
  {
    schema = schemaName;
    packageName = pkgName;
  }

  @Override
  public String getCatalog()
  {
    // currently I don't know of any DBMS that supports packages AND catalogs
    return null;
  }

  @Override
  public String getSchema()
  {
    return schema;
  }

  @Override
  public String getObjectType()
  {
    return "PACKAGE";
  }

  @Override
  public String getObjectName()
  {
    return packageName;
  }

  @Override
  public void setName(String name)
  {
    packageName = name;
  }

  @Override
  public String getObjectName(WbConnection conn)
  {
    return SqlUtil.buildExpression(conn, this);
  }

  @Override
  public String getObjectExpression(WbConnection conn)
  {
    return SqlUtil.buildExpression(conn, this);
  }

  @Override
  public String getFullyQualifiedName(WbConnection conn)
  {
    return SqlUtil.fullyQualifiedName(conn, this);
  }

  @Override
  public CharSequence getSource(WbConnection con)
    throws SQLException
  {
    ProcedureReader reader = ReaderFactory.getProcedureReader(con.getMetadata());
    return reader.getPackageSource(getCatalog(), getSchema(), getObjectName());
  }

  @Override
  public String getObjectNameForDrop(WbConnection con)
  {
    return getFullyQualifiedName(con);
  }

  @Override
  public String getComment()
  {
    return remarks;
  }

  @Override
  public void setComment(String comment)
  {
    remarks = comment;
  }

  @Override
  public String getDropStatement(WbConnection con, boolean cascade)
  {
    return "DROP PACKAGE " + getFullyQualifiedName(con);
  }

  public List<ProcedureDefinition> getProcedures()
  {
    return Collections.emptyList();
  }

  @Override
  public boolean supportsGetSource()
  {
    return true;
  }

}
