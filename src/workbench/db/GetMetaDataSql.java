/*
 * Created on 8. August 2002, 19:37
 */
package workbench.db;

import java.util.ArrayList;
import java.util.HashMap;
import workbench.util.WbPersistence;

/**
 *
 * @author  workbench@kellerer.org
 */
public class GetMetaDataSql
{
	
	private String baseSql;
	private String schema;
	private String schemaField;
	private String catalog;
	private String catalogField;
	private String objectName;
	private String objectNameField;
	private String orderBy;
	private boolean useUpperCase;
	private boolean useLowerCase;
	
	private boolean isProcedureCall;
	private boolean argumentsNeedParanthesis;
	private int schemaArgumentPos;
	private int catalogArgumentPos;
	private int objectNameArgumentPos;
	
	public GetMetaDataSql()
	{
	}
	
	public String getSql()
	{
		if (this.isProcedureCall)
			return this.getProcedureCallSql();
		else
			return this.getSelectSql();
	}

	private String getSelectSql()
	{
		StringBuffer sql = new StringBuffer(baseSql);
		if (baseSql.toLowerCase().indexOf("where") == -1)
		{
			sql.append(" WHERE ");
		}
		else
		{
			sql.append(" AND ");
		}	
		if (schema != null && schemaField != null)
		{
			sql.append(schemaField + " = '" + schema + "'");
		}
		if (catalog != null && catalogField != null)
		{
			sql.append(" AND " + catalogField + " = '" + catalog + "'");
		}
		if (objectName != null && objectNameField != null)
		{
			sql.append(" AND " + objectNameField + " = '" + objectName + "'");
		}
		if (this.orderBy != null)
		{
			sql.append(" " + this.orderBy);
		}
		return sql.toString();
	}

	private String getProcedureCallSql()
	{
		StringBuffer sql = new StringBuffer(this.baseSql);
		if (this.argumentsNeedParanthesis)
		{
			sql.append(" (");
		}
		sql.append(' ');
		for (int i=1; i < 4; i ++)
		{
			if (schemaArgumentPos == i && this.schema != null)
			{
				if (i > 1) sql.append(',');
				sql.append(this.schema);
			}
			else if (catalogArgumentPos == i && this.catalog != null)
			{
				if (i > 1) sql.append(',');
				sql.append(this.catalog);
			}
			else if (this.objectNameArgumentPos == i && this.objectName != null)
			{
				if (i > 1) sql.append(',');
				sql.append(this.objectName);
			}
		}		
		if (this.argumentsNeedParanthesis)
		{
			sql.append(")");
		}
		return sql.toString();
	}
	/** Getter for property baseSql.
	 * @return Value of property baseSql.
	 *
	 */
	public String getBaseSql()
	{
		return baseSql;
	}
	
	/** Setter for property baseSql.
	 * @param baseSql New value of property baseSql.
	 *
	 */
	public void setBaseSql(String baseSql)
	{
		this.baseSql = baseSql;
	}
	
	/** Getter for property schema.
	 * @return Value of property schema.
	 *
	 */
	public String getSchema()
	{
		return schema;
	}
	
	/** Setter for property schema.
	 * @param schema New value of property schema.
	 *
	 */
	public void setSchema(String schema)
	{
		if (schema == null) 
		{
			this.schema = null;
		}
		else
		{
			if (this.useLowerCase)
			{
				this.schema = schema.toLowerCase();
			}
			else if (this.useUpperCase)
			{
				this.schema = schema.toUpperCase();
			}
			else
			{
				this.schema = schema;
			}
		}
	}
	
	/** Getter for property catalog.
	 * @return Value of property catalog.
	 *
	 */
	public String getCatalog()
	{
		return catalog;
	}
	
	/** Setter for property catalog.
	 * @param catalog New value of property catalog.
	 *
	 */
	public void setCatalog(String catalog)
	{
		if (catalog == null) 
		{
			this.catalog = null;
		}
		else
		{
			if (this.useLowerCase)
			{
				this.catalog = catalog.toLowerCase();
			}
			else if (this.useUpperCase)
			{
				this.catalog = catalog.toUpperCase();
			}
			else
			{
				this.catalog = catalog;
			}
		}
	}
	
	/** Getter for property objectName.
	 * @return Value of property objectName.
	 *
	 */
	public String getObjectName()
	{
		return objectName;
	}
	
	/** Setter for property objectName.
	 * @param objectName New value of property objectName.
	 *
	 */
	public void setObjectName(String objectName)
	{
		if (objectName == null) 
		{
			this.objectName = null;
		}
		else
		{
			if (this.useLowerCase)
			{
				this.objectName = objectName.toLowerCase();
			}
			else if (this.useUpperCase)
			{
				this.objectName = objectName.toUpperCase();
			}
			else
			{
				this.objectName = objectName;
			}
		}
	}
	
	/** Getter for property schemaField.
	 * @return Value of property schemaField.
	 *
	 */
	public String getSchemaField()
	{
		return schemaField;
	}
	
	/** Setter for property schemaField.
	 * @param schemaField New value of property schemaField.
	 *
	 */
	public void setSchemaField(String schemaField)
	{
		this.schemaField = schemaField;
	}
	
	/** Getter for property catalogField.
	 * @return Value of property catalogField.
	 *
	 */
	public String getCatalogField()
	{
		return catalogField;
	}
	
	/** Setter for property catalogField.
	 * @param catalogField New value of property catalogField.
	 *
	 */
	public void setCatalogField(String catalogField)
	{
		this.catalogField = catalogField;
	}
	
	/** Getter for property objectNameField.
	 * @return Value of property objectNameField.
	 *
	 */
	public String getObjectNameField()
	{
		return objectNameField;
	}
	
	/** Setter for property objectNameField.
	 * @param objectNameField New value of property objectNameField.
	 *
	 */
	public void setObjectNameField(String objectNameField)
	{
		this.objectNameField = objectNameField;
	}
	
	/** Getter for property orderBy.
	 * @return Value of property orderBy.
	 *
	 */
	public String getOrderBy()
	{
		return orderBy;
	}
	
	/** Setter for property orderBy.
	 * @param orderBy New value of property orderBy.
	 *
	 */
	public void setOrderBy(String orderBy)
	{
		this.orderBy = orderBy;
	}
	

	/** Getter for property useUpperCase.
	 * @return Value of property useUpperCase.
	 *
	 */
	public boolean isUseUpperCase()
	{
		return useUpperCase;
	}
	
	/** Setter for property useUpperCase.
	 * @param useUpperCase New value of property useUpperCase.
	 *
	 */
	public void setUseUpperCase(boolean useUpperCase)
	{
		this.useUpperCase = useUpperCase;
	}
	
	/** Getter for property useLowerCase.
	 * @return Value of property useLowerCase.
	 *
	 */
	public boolean isUseLowerCase()
	{
		return useLowerCase;
	}
	
	/** Setter for property useLowerCase.
	 * @param useLowerCase New value of property useLowerCase.
	 *
	 */
	public void setUseLowerCase(boolean useLowerCase)
	{
		this.useLowerCase = useLowerCase;
	}
	/** Getter for property isProcedureCall.
	 * @return Value of property isProcedureCall.
	 *
	 */
	public boolean isIsProcedureCall()
	{
		return isProcedureCall;
	}
	
	/** Setter for property isProcedureCall.
	 * @param isProcedureCall New value of property isProcedureCall.
	 *
	 */
	public void setIsProcedureCall(boolean isProcedureCall)
	{
		this.isProcedureCall = isProcedureCall;
	}

	/** Getter for property schemaArgumentPos.
	 * @return Value of property schemaArgumentPos.
	 *
	 */
	public int getSchemaArgumentPos()
	{
		return schemaArgumentPos;
	}
	
	/** Setter for property schemaArgumentPos.
	 * @param schemaArgumentPos New value of property schemaArgumentPos.
	 *
	 */
	public void setSchemaArgumentPos(int schemaArgumentPos)
	{
		this.schemaArgumentPos = schemaArgumentPos;
	}
	
	/** Getter for property catalogArgumentPos.
	 * @return Value of property catalogArgumentPos.
	 *
	 */
	public int getCatalogArgumentPos()
	{
		return catalogArgumentPos;
	}
	
	/** Setter for property catalogArgumentPos.
	 * @param catalogArgumentPos New value of property catalogArgumentPos.
	 *
	 */
	public void setCatalogArgumentPos(int catalogArgumentPos)
	{
		this.catalogArgumentPos = catalogArgumentPos;
	}
	
	/** Getter for property objectNameArgumentPos.
	 * @return Value of property objectNameArgumentPos.
	 *
	 */
	public int getObjectNameArgumentPos()
	{
		return objectNameArgumentPos;
	}
	
	/** Setter for property objectNameArgumentPos.
	 * @param objectNameArgumentPos New value of property objectNameArgumentPos.
	 *
	 */
	public void setObjectNameArgumentPos(int objectNameArgumentPos)
	{
		this.objectNameArgumentPos = objectNameArgumentPos;
	}
	
	/** Getter for property argumentsNeedParanthesis.
	 * @return Value of property argumentsNeedParanthesis.
	 *
	 */
	public boolean isArgumentsNeedParanthesis()
	{
		return argumentsNeedParanthesis;
	}
	
	/** Setter for property argumentsNeedParanthesis.
	 * @param argumentsNeedParanthesis New value of property argumentsNeedParanthesis.
	 *
	 */
	public void setArgumentsNeedParanthesis(boolean argumentsNeedParanthesis)
	{
		this.argumentsNeedParanthesis = argumentsNeedParanthesis;
	}

	public static void createDefaultStatements()
	{
		System.out.println("Generating default statements...");
		GetMetaDataSql oracleProc = new GetMetaDataSql();
		oracleProc.setUseUpperCase(true);
		oracleProc.setBaseSql("SELECT text FROM all_source");
		oracleProc.setObjectNameField("name");
		oracleProc.setCatalogField(null);
		oracleProc.setSchemaField("owner");
		oracleProc.setOrderBy("ORDER BY line");
		
		
		GetMetaDataSql mssProc = new GetMetaDataSql();
		mssProc.setBaseSql("exec sp_helptext");
		mssProc.setArgumentsNeedParanthesis(false);
		mssProc.setSchemaArgumentPos(0);
		mssProc.setCatalogArgumentPos(0);
		mssProc.setObjectNameArgumentPos(1);
		mssProc.setIsProcedureCall(true);
		
		HashMap procStatements = new HashMap();
		procStatements.put("Oracle", oracleProc);
		procStatements.put("Microsoft SQL Server", mssProc);
		WbPersistence.writeObject(procStatements, "ProcSourceStatements.xml");
		
		GetMetaDataSql listOraTrigs = new GetMetaDataSql();
		listOraTrigs.setUseUpperCase(true);
		listOraTrigs.setBaseSql("SELECT trigger_name, trigger_type, triggering_event as trigger_event FROM all_triggers");
		listOraTrigs.setCatalogField(null);
		listOraTrigs.setObjectNameField("table_name");
		listOraTrigs.setSchemaField("owner");
		listOraTrigs.setOrderBy("ORDER BY trigger_name");
		
		HashMap trgStatements = new HashMap();
		trgStatements.put("Oracle", listOraTrigs);
		WbPersistence.writeObject(trgStatements, "ListTriggersStatements.xml");

		GetMetaDataSql oraTrigSrc = new GetMetaDataSql();
		oraTrigSrc.setUseUpperCase(true);
		oraTrigSrc.setBaseSql("SELECT 'CREATE OR REPLACE TRIGGER '|| description, trigger_body FROM all_triggers");
		oraTrigSrc.setCatalogField(null);
		oraTrigSrc.setObjectNameField("trigger_name");
		oraTrigSrc.setSchemaField("owner");
		
		HashMap trgSrcStatements = new HashMap();
		trgStatements.put("Oracle", oraTrigSrc);
		WbPersistence.writeObject(trgStatements, "TriggerSourceStatements.xml");
		System.out.println("Done.");
	}
	
	public static void main(String args[])
	{
		createDefaultStatements();
	}
	
}
