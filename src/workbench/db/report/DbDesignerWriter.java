/*
 * DbDesignerWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.report;

import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.importer.TableDependencySorter;
import workbench.gui.dbobjects.ProgressPanel;
import workbench.storage.RowActionMonitor;
import workbench.util.SqlUtil;

/**
 * @author support@sql-workbench.net
 */
public class DbDesignerWriter 
{
	private List<ReportTable> tables;
	private String modelName;
	private WbConnection dbConnection;
	private int objectId = 1000;
	private int columnId = 1;
	private Map<DataType, Integer> dataTypeIds;
	private Map<ReportTable, Integer> tableIds;
	private Map<ReportTable, Map<ReportColumn, Integer>> columnIds;
	private Map<String, TableRelation> relations = new HashMap<String, TableRelation>();
	private RowActionMonitor monitor;
	private ProgressPanel progressPanel;
	
	public DbDesignerWriter(WbConnection conn, List<TableIdentifier> tableList, String name)
		throws SQLException
	{
		this.dbConnection = conn;
		this.tables = new ArrayList<ReportTable>(tableList.size());
		TableDependencySorter sorter = new TableDependencySorter(conn);
		sorter.sortForInsert(tableList);
		tableIds = new HashMap<ReportTable, Integer>(tables.size());
		columnIds = new HashMap<ReportTable, Map<ReportColumn, Integer>>(tables.size() * 5);
		for (TableIdentifier tbl : tableList)
		{
			ReportTable rtable = new ReportTable(tbl, this.dbConnection, "", true, true, true, true, false);
			tables.add(rtable);
			getTableId(rtable);
		}
		this.modelName = name;
		for (ReportTable tbl : tables)
		{
			addRelations(tbl);
		}
	}
	
	public void setProgressPanel(ProgressPanel panel)
	{
		this.progressPanel = panel;
	}
	public void setMonitor(RowActionMonitor m)
	{
		this.monitor = m;
	}
	
	public int getTableCount()
	{
		return tables.size();
	}
	
	private int getNextColumnId()
	{
		return columnId++;
	}
	
	private int getNextObjectId()
	{
		return objectId++;
	}
	
	public void writeXml(Writer out)
		throws IOException, SQLException
	{
		writeHeader(out);
		writeDataTypes(out);
		out.write("  </SETTINGS>\n");
		out.write("  <METADATA>\n");
		writeTables(out);
		writeRelations(out);
		out.write("  </METADATA>\n");
		writeEnd(out);
	}
	
	protected void writeHeader(Writer out) 
		throws IOException
	{
		out.write("<?xml version=\"1.0\" standalone=\"yes\" ?>\n");
		out.write("<DBMODEL Version=\"4.0\">\n");
		out.write("  <SETTINGS>\n");
		out.write("    <GLOBALSETTINGS ModelName=\"" + modelName + "\" IDModel=\"0\" " +
			"IDVersion=\"0\" " +
			"VersionStr=\"1.0.0.0\" " +
			"UseVersionHistroy=\"1\" " +
			"AutoIncVersion=\"1\" " +
			"DatabaseType=\"MySQL\" " +
			"ZoomFac=\"100.00\" " +
			"XPos=\"0\" " +
			"YPos=\"0\" " +
			"DefaultDataType=\"1\" " +
			"DefaultTablePrefix=\"0\" " +
			"PageOrientation=\"1\" " +
			"PageFormat=\"A4 (210x297 mm, 8.26x11.7 inches)\" " +
			"TableNameInRefs=\"0\" " +
			"FKPrefix=\"FK\" " +
			"FKPostfix=\"\" " +
			"CreateFKRefDefIndex=\"0\" " +
			"DBQuoteCharacter=\"\"/>\n");
	}
	
	private void writeEnd(Writer out) 
		throws IOException
	{
		out.write("</DBMODEL>");
	}
	
	private void writeDataTypes(Writer out)
		throws IOException, SQLException
	{
		Set<DataType> types = new HashSet<DataType>();
		out.write("    <DATATYPEGROUPS>\n");
		out.write("      <DATATYPEGROUP Name=\"Numeric Types\" Icon=\"1\"/>\n");
		out.write("      <DATATYPEGROUP Name=\"String Types\" Icon=\"3\"/>\n");
		out.write("      <DATATYPEGROUP Name=\"Date and Time Types\" Icon=\"2\"/>\n");
		out.write("      <DATATYPEGROUP Name=\"Blob and Text Types\" Icon=\"4\" />\n");
		out.write("      <DATATYPEGROUP Name=\"Other\" Icon=\"3\"/>\n");
		out.write("    </DATATYPEGROUPS>\n");
		
		for (ReportTable tbl : tables)
		{
			for (ReportColumn c : tbl.getColumns())
			{
				DataType type = DataType.getDataType(c.getColumn());
				types.add(type);
				int jdbctype = c.getColumn().getDataType();
				
				if (SqlUtil.isNumberType(jdbctype))
				{
					type.setTypeGroupId(0);
				}
				else if (SqlUtil.isStringType(jdbctype))
				{
					type.setTypeGroupId(1);
				}
				else if (SqlUtil.isDateType(jdbctype))
				{
					type.setTypeGroupId(2);
				}
				else if (SqlUtil.isClobType(jdbctype) || SqlUtil.isBlobType(jdbctype))
				{
					type.setTypeGroupId(3);
				}
				else
				{
					type.setTypeGroupId(4);
				}
			}
		}
		
		out.write("    <DATATYPES>\n");
		int typeId = 1;
		this.dataTypeIds = new HashMap<DataType, Integer>();
		for (DataType type : types)
		{
			type.setId(typeId++);
			dataTypeIds.put(type, type.getId());
			int paramCount = type.getParameters().size();
			int paramRequired = (paramCount == 0 ? 0 : 1);
			boolean mapping = !type.getDbmsTypeName().equals(type.getTypeName());
			String mappingName = (mapping ? type.getDbmsTypeName() : "");
			
			out.write("      <DATATYPE ID=\"" + type.getId() + "\" " + 
				"IDGroup=\"" + type.getTypeGroupId() + "\" " +
				"TypeName=\"" + type.getTypeName() + "\" " +
				"ParamCount=\"" + paramCount + "\" " + 
				"OptionCount=\"0\" ParamRequired=\"" + paramRequired + "\" " + 
				"EditParamsAsString=\"0\" SynonymGroup=\"0\" PhysicalMapping=" + boolToInt(mapping) + " " +
				"PhysicalTypeName=\"" + mappingName + "\" >\n");
			if (paramCount > 0)
			{
				out.write("       	<PARAMS>\n");
				for (int i=0; i < paramCount; i++)
				{
					out.write("       	  <PARAM Name=\"" + type.getParameters().get(i) + "\" />\n");
				}
				out.write("       	</PARAMS>\n");
			}
			out.write("      </DATATYPE>\n");
		}
		out.write("    </DATATYPES>\n");
		out.write("    <COMMON_DATATYPES>\n");
		for (DataType type : types)
		{
			out.write("    <COMMON_DATATYPE ID=\"" + type.getId() + "\"/>\n");
		}		
		out.write("    </COMMON_DATATYPES>\n");
	}
	
	private void writeTables(Writer out)
		throws IOException
	{
		out.write("    <TABLES>\n");
		
		int tableOrder = 0;
		int x = 5;
		int y = 5;
		int tableRowCount = 0;
		
		int currentTable = 1;
		
		for (ReportTable tbl : tables)
		{
			
			String tableName = tbl.getTable().getTableName();
			if (this.monitor != null)
			{
				this.monitor.setCurrentObject(tableName, currentTable++, tables.size());
			}
			if (this.progressPanel != null)
			{
				this.progressPanel.setInfoText(tableName);
			}
				
			int id = getTableId(tbl);
			tableOrder ++;
			out.write("      <TABLE ID=\"" + id + "\" " +  
				"Tablename=\"" + tbl.getTable().getTableName() + "\" " +
				"XPos=\"" + x + "\" YPos=\"" + y + "\" " + 
				"TableType=\"0\" TablePrefix=\"0\" nmTable=\"0\" Temporary=\"0\" UseStandardInserts=\"0\" StandardInserts=\"\\n\" " + 
				"Comments=\"" + tbl.getTableComment() + "\" " + 
				"Collapsed=\"0\" " + 
				"IsLinkedObject=\"0\" IDLinkedModel=\"-1\" Obj_id_Linked=\"-1\" OrderPos=\"" + tableOrder + "\" " +
				">\n");
			out.write("        <COLUMNS>\n");
			int maxCols = 0;
			int maxNameLength = 0;
			
			List<ReportColumn> columns = tbl.getColumnsSorted();
			// first write all PK columns
			int pos = 1;
			for (ReportColumn c : columns)
			{
				if (!c.getColumn().isPkColumn()) continue;
				int colLength = writeColumn(tbl, c, pos++, out);
				pos ++;
				if (colLength > maxNameLength)
				{
					maxNameLength = colLength;
				}
			}
			
			pos = 1;
			// now write all FK columns
			for (ReportColumn c : columns)
			{
				if (c.getForeignKey() == null) continue;
				int colLength = writeColumn(tbl, c, pos++, out);
				if (colLength > maxNameLength)
				{
					maxNameLength = colLength;
				}
			}

			// now write the rest
			pos = 1;
			for (ReportColumn c : columns)
			{
				if (c.getColumn().isPkColumn() || c.getForeignKey() != null) continue;
				int colLength = writeColumn(tbl, c, pos++, out);
				if (colLength > maxNameLength)
				{
					maxNameLength = colLength;
				}
			}
			
			if (columns.size() > maxCols)
			{
				maxCols = columns.size();
			}
			if (tbl.getTable().getTableName().length() > maxNameLength)
			{
				maxNameLength = tbl.getTable().getTableName().length();
			}
			
			out.write("        </COLUMNS>\n");
			writeTableRelations(tbl, out);
			out.write("      </TABLE>\n"); 
			x += (maxNameLength * 10) + 10;
			tableRowCount ++;
			if (tableRowCount == 4)
			{
				x = 5;
				y += (maxCols * 30) + 40;
				maxCols = 0;
				tableRowCount = 0;
			}
		}
		out.write("    </TABLES>\n");
	}
	
	private int writeColumn(ReportTable tbl, ReportColumn c, int pos, Writer out)
		throws IOException
	{
		DataType type = DataType.getDataType(c.getColumn());

		int typeId = dataTypeIds.get(type);
		ColumnIdentifier col = c.getColumn();
		int paramCount = type.getParameters().size();
		String params = "";

		if (paramCount == 1)
		{
			params = "(" + col.getColumnSize() + ")";
		}
		else if (paramCount == 2)
		{
			params = "(" + col.getColumnSize() + "," + col.getDecimalDigits() + ")";
		}
		int colId = getColumnId(tbl, c);
		String isFk = boolToInt(c.getForeignKey() != null);
		out.write("          <COLUMN ID=\"" + colId + "\" " +
			"ColName=\"" + col.getColumnName() + "\" " + 
			"idDatatype=\"" + typeId + "\" " + 
			"DatatypeParams=\"" + params + "\" " +
			"Width=\"-1\" Prec=\"-1\" " + 
			"Pos=\"" + pos + "\" " + 
			"PrimaryKey=" + boolToInt(col.isPkColumn()) + " NotNull=" + boolToInt(!col.isNullable()) + " " + 
			"PrevColName=\"\" " +
			"AutoInc=\"0\" IsForeignKey=" + isFk + ""+
			">\n");

		out.write("          </COLUMN>\n");
		int colLength = col.getColumnName().length() + params.length() + type.getTypeName().length();
		return colLength;
	}
	private List<Integer> getStartRelations(ReportTable source)
	{
		int sourceId = getTableId(source);
		List<Integer> result = new ArrayList<Integer>();
		for (TableRelation rel : relations.values())
		{
			if (rel.getSourceTableId() == sourceId)
			{
				result.add(rel.getRelationId());
			}
		}
		return result;
	}
	
	private List<Integer> getEndRelations(ReportTable source)
	{
		int sourceId = getTableId(source);
		List<Integer> result = new ArrayList<Integer>();
		for (TableRelation rel : relations.values())
		{
			if (rel.getTargetTableId() == sourceId)
			{
				result.add(rel.getRelationId());
			}
		}
		return result;
	}

	private void writeTableRelations(ReportTable tbl, Writer out)
		throws IOException
	{
		List<Integer> start = getStartRelations(tbl);
		if (start.size() > 0)
		{
			out.write("        <RELATIONS_START>\n");
			for (Integer i : start)
			{
				out.write("          <RELATION_START ID=\"" + i + "\"/>\n");
			}
			out.write("        </RELATIONS_START>\n");
		}
		List<Integer> end = getEndRelations(tbl);
		if (end.size() > 0)
		{
			out.write("        <RELATIONS_END>\n");
			for (Integer i : end)
			{
				out.write("          <RELATION_END ID=\"" + i + "\"/>\n");
			}
			out.write("        </RELATIONS_END>\n");
		}
	}
	
	private void writeRelations(Writer out)
		throws IOException
	{
		out.write("    <RELATIONS>\n");
		int order = 1;
		for (TableRelation relation : relations.values())
		{
			out.write("      <RELATION ID=\"" + relation.getRelationId() + "\"");
			out.write(" RelationName=\"" + relation.getRelationName() + "\"");
			out.write(" Kind=\"" + relation.getRelationKind() +"\""); 
			out.write(" SrcTable=\""  +  relation.getTargetTableId() + "\"");
			out.write(" DestTable=\"" +  relation.getSourceTableId() + "\"");
			out.write(" FKFields=\"" + relation.getColumns() + "\"");
			out.write(" relDirection=\"4\"");
			out.write(" OptionalStart=\"0\" OptionalEnd=\"0\"");
			out.write(" Invisible=\"0\"");
			out.write(" CreateRefDef=\"1\"");
			out.write(" FKFieldsComments=\"\\n\"");
			out.write(" Comments=\"\"");
			out.write(" OrderPos=\"" + order + "\"");
			out.write(" FKRefDefIndex_Obj_id=\"-1\" Splitted=\"0\" IsLinkedObject=\"0\" IDLinkedModel=\"-1\" Obj_id_Linked=\"-1\"");
			out.write(" RefDef=\"Matching=0\\nOnDelete=" + relation.getDeleteAction() + "\\nOnUpdate=" + relation.getUpdateAction() + "\\n\"");
			out.write(" MidOffset=\"0\" CaptionOffsetX=\"0\" CaptionOffsetY=\"0\" StartIntervalOffsetX=\"0\" StartIntervalOffsetY=\"0\" EndIntervalOffsetX=\"0\" EndIntervalOffsetY=\"0\"");
			out.write("/>\n");
		}
		out.write("    </RELATIONS>\n");
	}
	
	private ReportTable findTable(TableIdentifier tableName)
	{
		for (ReportTable tbl : tables)
		{
			if (tbl.getTable().equals(tableName)) return tbl;
		}
		return null;
	}
	
	private String buildRelationKey(ReportTable source, ReportTable target)
	{
		return source.getTable().getTableName() + "$-$" + target.getTable().getTableName();
	}
	
	private void addRelations(ReportTable source)
	{
		ReportColumn[] cols = source.getColumns();
		for (ReportColumn col : cols)
		{
			ColumnReference ref = col.getForeignKey();
			if (ref != null)
			{
				String key = buildRelationKey(source, ref.getForeignTable());
				TableRelation rel = relations.get(key);
				TableIdentifier tbl = ref.getForeignTable().getTable();
				ReportTable rep = findTable(tbl);
				if (rel == null)
				{
					int sid = getTableId(source);
					int tid = getTableId(rep);
					int id = getNextObjectId();
					rel = new TableRelation(id, sid, tid, ref.getFkName());
					rel.setDeleteAction(ref.getDeleteRule());
					rel.setUpdateAction(ref.getUpdateRule());
					relations.put(key, rel);
				}
				ReportColumn tcol = rep.findColumn(ref.getForeignColumn());
				rel.addColumnReference(col.getColumn(), tcol.getColumn());
			}
		}
	}
	
	private int getTableId(ReportTable table)
	{
		Integer id = tableIds.get(table);
		if (id == null)
		{
			int tid = getNextObjectId();
			tableIds.put(table, tid);
			return tid;
		}
		return id.intValue();
	}
	
	private int getColumnId(ReportTable table, ReportColumn column)
	{
		Map<ReportColumn, Integer> idmap = columnIds.get(table);
		if (idmap == null)
		{
			idmap = new HashMap<ReportColumn, Integer>();
			columnIds.put(table, idmap);
		}
		int colid = -1;
		Integer id = idmap.get(column);
		
		if (id == null)
		{
			colid = getNextColumnId();
			idmap.put(column, colid);
		}
		else
		{
			colid = id.intValue();
		}
		return colid;
	}
	
	private String boolToInt(boolean flag)
	{
		if (flag) return "\"1\"";
		return "\"0\"";
	}
	
	public static void main(String args[])
	{
		Connection con = null;
		WbConnection wb = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		Statement stmt = null;
		try
		{
			Class.forName("org.h2.Driver");
			con = DriverManager.getConnection("jdbc:h2:c:/projects/turnierverwaltung/db/judo", "sa", "");
			wb = new WbConnection("test", con, null);
			SchemaReporter rep = new SchemaReporter(wb);
			TableIdentifier[] tables = new TableIdentifier[] { new TableIdentifier("EVENT"), new TableIdentifier("EVENT_TYPE") };
			rep.setTableList(tables);
			rep.setDbDesigner(true);
			rep.setOutputFilename("c:/temp/report/test.xml");
			rep.setIncludeTables(true);
			rep.writeXml();

			rep.setDbDesigner(false);
			rep.setOutputFilename("c:/temp/report/testwb.xml");
			rep.setIncludeTables(true);
			rep.writeXml();
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
		finally
		{
			try { stmt.executeUpdate("shutdown immediate"); } catch (Exception e) {}
//			System.out.println("Closing connection...");
			SqlUtil.closeAll(rs, stmt);
			try
			{
				con.close();
			}
			catch (Throwable th)
			{
			}
		}
		
		System.out.println("done.");
	}	
}
