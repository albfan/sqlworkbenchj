/*
 * Workbench2Designer.java
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

/**
 * @author tstill
 */
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import workbench.log.LogMgr;

public class Workbench2Designer
{
	// Global value so it can be ref'd by the tree-adapter
	private static Document source;
	private static Document destination;

	//object model

	private static TreeMap tables = new TreeMap();
	private static TreeMap relations = new TreeMap();
	private static TreeMap dataTypes =new TreeMap();
	private static GlobalSettings globalSettings;
	private static final TreeMap globalSQLDataTypes = Workbench2Designer.initGlobalSQLDataTypes();

	private static TreeMap initGlobalSQLDataTypes()
	{
		TreeMap ret = new TreeMap();
		ret.put("INTEGER",new SQLDataType("INTEGER", 0,"","INTEGER",new String[]{"length"}));
		ret.put("FLOAT",new SQLDataType("FLOAT",1,"","FLOAT",new String[]{"precision"}));
		ret.put("#FLOAT",new SQLDataType("#FLOAT", 0,"","FLOAT",new String[]{"length","decimals"}));
		ret.put("DOUBLE",new SQLDataType("DOUBLE", 0,"","DOUBLE",new String[]{"length","decimals"}));
		ret.put("REAL",new SQLDataType("REAL", 0,"","REAL",new String[]{"length","decimals"}));
		ret.put("DECIMAL",new SQLDataType("DECIMAL", 0,"","DECIMAL",new String[]{"length","decimals"}));
		ret.put("NUMBER",new SQLDataType("NUMBER", 1,"NUMBER","NUMERIC",new String[]{"length","decimals"}));
		ret.put("DATE",new SQLDataType("DATE",0, "DATE","DATE"));
		ret.put("DATETIME",new SQLDataType("DATETIME",0,"DATE","DATETIME"));
		ret.put("TIMESTAMP",new SQLDataType("TIMESTAMP",0,"DATE","TIMESTAMP"));
		ret.put("VARCHAR2",new SQLDataType("VARCHAR2",1,"VARCHAR2","VARCHAR",new String[]{"length"}));
		ret.put("VARCHAR",new SQLDataType("VARCHAR",0,"","VARCHAR",new String[]{"length"}));
		ret.put("BOOL",new SQLDataType("BOOL",0,"NUMBER(1)","BOOL"));
		ret.put("LONG",new SQLDataType("LONG",0,"LONG","CLOB"));
		ret.put("OTHER",new SQLDataType("OTHER",0,"OBJECT","OTHER"));
		return ret;
	}
	private static Workbench2Designer.IDCounter idCounter=new Workbench2Designer.IDCounter();

	//DBD transformation ralated fields
	private static DBDIDReference dbdIDReference;

	//GUI
	private static final int dbdCanvasWidth=1000;
	private static final int dbdCanvasHeight=4000;
	private static final boolean cascadeIfNecc=true;
	private final static int dbdXOffset=100;
	private final static int dbdYOffset=20;
	private final static int dbdTableWidth=300;
	private final static int dbdTableHeight=100;
	private static final Workbench2Designer.GUIPositioner dbdTablePositioner =
	new Workbench2Designer.GUIPositioner(dbdCanvasHeight,dbdCanvasWidth,dbdTableHeight,dbdTableWidth,dbdXOffset,dbdYOffset,cascadeIfNecc);
	//so called metadata
	private static final LinkedList dbdRelationKinds = Workbench2Designer.initDBDRelationKinds();
	private static LinkedList initDBDRelationKinds()
	{
		LinkedList ret = new LinkedList();
		ret.add(0,"1:1");
		ret.add(1,"1:n");
		ret.add(2,"n:n");
		return ret;
	}
	private static final LinkedList dbdOnUpdate = Workbench2Designer.initDBDOnUpdate();
	private static LinkedList initDBDOnUpdate()
	{
		LinkedList ret = new LinkedList();
		ret.add(0,"RESTRICT");
		ret.add(1,"CASCADE");
		ret.add(2,"SET NULL");
		ret.add(3,"NO ACTION");
		ret.add(4,"SET DEFAULT");

		return ret;
	}
	private static final LinkedList dbdOnDelete = Workbench2Designer.initDBDOnDelete();
	private static LinkedList initDBDOnDelete()
	{
		LinkedList ret = new LinkedList();
		ret.add(0,"RESTRICT");
		ret.add(1,"CASCADE");
		ret.add(2,"SET NULL");
		ret.add(3,"NO ACTION");
		ret.add(4,"SET DEFAULT");

		return ret;
	}
	private static final TreeMap dbdDataTypeGroupRef = Workbench2Designer.initDBDDataTypeGroupRef();
	private static TreeMap initDBDDataTypeGroupRef()
	{
		TreeMap ret = new TreeMap();
		ret.put("INTEGER","Numeric Types");
		ret.put("FLOAT","Numeric Types");
		ret.put("#FLOAT","Numeric Types");
		ret.put("DOUBLE","Numeric Types");
		ret.put("REAL","Numeric Types");
		ret.put("DECIMAL","Numeric Types");
		ret.put("NUMBER","Numeric Types");
		ret.put("DATE","Date and Time Types");
		ret.put("DATETIME","Date and Time Types");
		ret.put("TIMESTAMP","Date and Time Types");
		ret.put("VARCHAR2","String Types");
		ret.put("VARCHAR","String Types");
		ret.put("BOOL","Numeric Types");
		ret.put("LONG","Numeric Types");
		return ret;
	}
	private static final LinkedList dbdDataTypeGroups = Workbench2Designer.initDBDDataTypeGroups();
	private static LinkedList initDBDDataTypeGroups()
	{

		LinkedList ret = new LinkedList();
		ret.add(0,"Numeric Types");
		ret.add(1,"Date and Time Types");
		ret.add(2,"String Types");
		ret.add(3,"Blob and Text Types");
		ret.add(4,"User defined Types");
		ret.add(5,"Geographic Types");
		return ret;
	}
	private static final String[] dbdRegionColors = Workbench2Designer.initDBDRegionColors();
	private static String[] initDBDRegionColors()
	{
		String[] ret=
		{
			"Yellow=#FEFDED",
			"Green=#EAFFE5",
			"Cyan=#ECFDFF",
			"Blue=#F0F1FE",
			"Magenta=#FFEBFA"
		};
		return ret;
	}

	public Workbench2Designer(File f)
		throws SAXException,ParserConfigurationException,IOException
	{
		InputSource in = new InputSource(new BufferedInputStream(new FileInputStream(f)));
		this.init(in);
	}

	public Workbench2Designer(Reader in)
		throws SAXException,ParserConfigurationException,IOException
	{
		this.init(new InputSource(in));
	}

	public Workbench2Designer(InputStream in)
		throws SAXException,ParserConfigurationException,IOException
	{
		this.init(new InputSource(in));
	}

	private void init(InputSource in)
		throws SAXException,ParserConfigurationException,IOException
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//factory.setNamespaceAware(true);
		//factory.setValidating(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Workbench2Designer.source = builder.parse(in);
		Workbench2Designer.destination = builder.newDocument();
	}

	public void transformWorkbench2Designer()
		throws DOMException,Workbench2Designer.MalformedSourceException
	{
		this.wbReadGlobalSettings();
		this.wbReadTables();
		Workbench2Designer.dbdIDReference=new DBDIDReference();
		Workbench2Designer.dbdIDReference.createDBDIDRef();

		Element dbdSettings = this.dbdCreateSettings();
		Element dbdTables = this.dbdCreateTables();
		Element dbdRelations = this.dbdCreateRelations();
		Element dbdNotes = destination.createElement("NOTES");
		Element dbdImages = destination.createElement("IMAGES");
		Element dbdPluginData = destination.createElement("PLUGINDATA");
		Element dbdPluginDataRec = destination.createElement("PLUGINDATARECORDS");
		Element dbdQueryData = destination.createElement("QUERYDATA");
		Element dbdQueryDataRec = destination.createElement("QUERYRECORDS");
		Element dbdLinkedMod = destination.createElement("LINKEDMODELS");
		Element dbdMetaData = destination.createElement("METADATA");
		Element dbdRoot = destination.createElement("DBMODEL");

		destination.appendChild(dbdRoot);
		dbdRoot.setAttribute("version", "4.0");
		dbdRoot.appendChild(dbdSettings);
		dbdRoot.appendChild(dbdMetaData);
		dbdRoot.appendChild(dbdPluginData);
		dbdRoot.appendChild(dbdQueryData);
		dbdPluginData.appendChild(dbdPluginDataRec);
		dbdQueryData.appendChild(dbdQueryDataRec);
		dbdMetaData.appendChild(dbdTables);
		dbdMetaData.appendChild(dbdRelations);
		dbdMetaData.appendChild(dbdImages);
		dbdMetaData.appendChild(dbdNotes);

		//actually not needed..
		this.dbdAppendRelations();
		//needed...
		this.dbdAppendIndices();
	}

	public void writeOutputFile(File outputFile)
		throws IOException,TransformerException,TransformerConfigurationException
	{
		// Use a Transformer for output
		TransformerFactory tFactory =
		TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();

		DOMSource domsource = new DOMSource(Workbench2Designer.destination);

		StreamResult result = new StreamResult(outputFile);
		transformer.transform(domsource, result);
	}

	public String stripNewLines(String s)
	{
		String t = s.trim();
		int x = t.indexOf("\n");
		if (x >= 0) t = t.substring(0, x);
		return t;
	}

	public String getNodeTypeName(int index)
	{
		// An array of names for DOM node-types
		// (Array indexes = nodeType() values.)
		final String[] typeName =
		{
			"none",
			"Element",
			"Attr",
			"Text",
			"CDATA",
			"EntityRef",
			"Entity",
			"ProcInstr",
			"Comment",
			"Document",
			"DocType",
			"DocFragment",
			"Notation",
		};
		return typeName[index];
	}

	public String getTextFromElement(Node node)
	{
		String type = getNodeTypeName(node.getNodeType());
		if(!type.equals("Element"))return "";
		Node firstChild = node.getFirstChild();
		String text;
		if(firstChild==null)
		{
			text="";
		}
		else
		{
			text = (getNodeTypeName(firstChild.getNodeType()).equals("Text"))?
				this.stripNewLines(firstChild.getNodeValue()):"";
		}
		return text;
	}

	public String getTextByTagName(String tagname,Document document)
	{
		NodeList nodelist;
		Node node;
		String ret;
		try
		{
			nodelist = document.getElementsByTagName(tagname);
			node = nodelist.item(0);
			ret = this.getTextFromElement(node);
		}
		catch(java.lang.NullPointerException e)
		{ret="";}
		return ret;
	}

	public void setAttribute(String name,String value, Element element)
	{
		if (value==null)value="";
		if (name!=null)element.setAttribute(name,value);
	}

	public void setAttributes(TreeMap attributes, Element element)
	{
		Iterator it = attributes.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry entry = (Map.Entry)it.next();
			String name = (String)entry.getKey();
			if (name == null) continue;
			Object value = entry.getValue();
			if (value==null)
			{
				element.setAttribute(name, "");
			}
			else if (value instanceof String)
			{
				if (name!=null)element.setAttribute(name, (String)value);
			}
			else
			{
				LogMgr.logError("Workbench2Designer.setAttributes()", "Not a String for key=" + name + ", got " + value.getClass().getName() + " instead!", null);
			}
		}

	}

	public void forceEndTag(Element node)
	{
		if(!node.hasChildNodes())
		{
			node.setAttribute("xml:space","preserve");
			Node forceEndTag= destination.createTextNode(" ");
			node.appendChild(forceEndTag);
		}
	}

	public Element dbdCreateTables()
		throws DOMException,Workbench2Designer.MalformedSourceException
	{
		Set tblNames =  Workbench2Designer.tables.keySet();
		Iterator it = tblNames.iterator();
		Element tablesElm = destination.createElement("TABLES");
		int orderPos=0;
		while(it.hasNext())
		{
			orderPos++;

			Table table = (Table) Workbench2Designer.tables.get(it.next());
			Element tableElm = destination.createElement("TABLE");
			String id= dbdIDReference.getTableDBDID(table.getName());

			Workbench2Designer.dbdTablePositioner.setNewPosition();
			int xPos=Workbench2Designer.dbdTablePositioner.getCurrentXPos();
			int yPos=Workbench2Designer.dbdTablePositioner.getCurrentYPos();

			TreeMap attributes= new TreeMap();
			attributes.put("ID",id);
			attributes.put("Comments",table.getComment());
			attributes.put("Tablename",table.getName());
			attributes.put("Collapsed","0");
			attributes.put("IDLinkedModel","-1");
			attributes.put("IsLinkedObject","0");
			attributes.put("Obj_id_Linked","-1");
			attributes.put("OrderPos",orderPos+"");
			attributes.put("PrevTableName","");
			attributes.put("StandardInserts","\n");
			attributes.put("TableOptions","");
			attributes.put("TablePrefix","0");
			attributes.put("TableType","0");
			attributes.put("UseStandardInserts","0");
			attributes.put("XPos",xPos+"");
			attributes.put("YPos",yPos+"");
			attributes.put("nmTable","0");

			this.setAttributes(attributes,tableElm);

			tablesElm.appendChild((Node)tableElm);
			Element columnsElm=this.dbdCreateColumns(table);
			tableElm.appendChild((Node)columnsElm);
		}
		return tablesElm;
	}

	public Element dbdCreateColumns(Table table)
		throws DOMException,Workbench2Designer.MalformedSourceException
	{
		Element columnsElm = destination.createElement("COLUMNS");

		TreeMap columns = table.getColumns();
		Set colNames = columns.keySet();
		Iterator iit = colNames.iterator();

		while(iit.hasNext())
		{
			try
			{
				Column column = (Column) columns.get(iit.next());
				Element columnElm = destination.createElement("COLUMN");
				SQLDataTypeStatement dataTypeStatement=column.getDataTypeStatement();
				String idDataType=Workbench2Designer.dbdIDReference.getDataTypeDBDID(dataTypeStatement.datatype.index);
				int notNull =(column.isNullable())?0:1;
				int foreignKey = (column.isForeignKey())?1:0;
				int primaryKey = (column.isPrimaryKey())?1:0;

				TreeMap attributes= new TreeMap();
				String name=column.getName();
				String id = Workbench2Designer.dbdIDReference.getColumnDBDID(table.name, name);

				attributes.put("ID",id);
				attributes.put("Comments",column.getComment());
				attributes.put("ColName",name);
				attributes.put("DatatypeParams",dataTypeStatement.paramString);
				attributes.put("DefaultValue",column.getDefaultValue());
				attributes.put("IsForeignKey",""+foreignKey);
				attributes.put("NotNull",""+notNull);
				attributes.put("Pos",column.position+"");
				attributes.put("Prec","-1");
				attributes.put("PrevColName","");
				attributes.put("PrimaryKey",""+primaryKey);
				attributes.put("Width","-1");
				attributes.put("idDatatype",idDataType);
				attributes.put("AutoInc","0");

				this.setAttributes(attributes, columnElm);

				columnsElm.appendChild((Node)columnElm);
				Element optionsElm = destination.createElement("OPTIONSELECTED");
				this.forceEndTag(optionsElm);
				columnElm.appendChild((Node)optionsElm);
			}
			catch(java.lang.NullPointerException ne)
			{
				throw(new Workbench2Designer.MalformedSourceException("Invalid table declaration ",ne));
			}
		}
		return columnsElm;
	}

	public Element dbdCreateRelations()
		throws DOMException,Workbench2Designer.MalformedSourceException
	{
		Iterator it = Workbench2Designer.relations.entrySet().iterator();
		Element relationsElm = destination.createElement("RELATIONS");
		while(it.hasNext())
		{
			Map.Entry entry = (Map.Entry)it.next();
			Relation relation = (Relation) entry.getValue();
			if (relation == null) continue;
			
			try
			{
				Table src = (Table) Workbench2Designer.tables.get(relation.getSrcTable());
				
				// ignore missing tables
				if (src == null) 
				{
					LogMgr.logWarning("Workbench2DbDesigner.dbdCreateRelations()", "Source table " + relation.getSrcTable() + "for relation " + relation.getRelName() + " not found in WB table list!");
					it.remove();
					continue;
				}

				//String relSource = Workbench2Designer.dbdIDReference.getTableDBDID(src.getName());
				Table dest = (Table) Workbench2Designer.tables.get(relation.getDestTable());

				if (dest == null) 
				{
					LogMgr.logWarning("Workbench2DbDesigner.dbdCreateRelations()", "Destination table " + relation.getDestTable() + "for relation " + relation.getRelName() + " not found in WB table list!");
					it.remove();
					continue;
				}

				String relDestination = Workbench2Designer.dbdIDReference.getTableDBDID(dest.getName());
				if (relDestination == null)
				{
					LogMgr.logWarning("Workbench2DbDesigner.dbdCreateRelations()", "Destination string for relation " + relation.getRelName() + " not found in WB table list!");
					it.remove();
					continue;
				}
				String kind;

				String[] cols=relation.getConstraintStatement().split("\\\\n");
				cols = cols[0].split(relation.getOperator());
				Column sc = (Column)src.getColumns().get(cols[0]);
				Column dc = (Column)dest.getColumns().get(cols[1]);
				String t1=(sc.isPrimaryKey()&&!(src.getPrimaryKeys().size()>1))?"1":"n";
				String t2=(dc.isPrimaryKey()&&!(dest.getPrimaryKeys().size()>1))?"1":"n";
				kind = Workbench2Designer.dbdRelationKinds.indexOf(t1+":"+t2)+"";


				String onDelete = (!(relation.getOnDelete().equals("")&&relation.getOnDelete()==null))?
					"OnDelete="+Workbench2Designer.dbdOnDelete.indexOf(relation.getOnDelete())+"\\n":"";
				String onUpdate = (!(relation.getOnUpdate().equals("")&&relation.getOnUpdate()==null))?
					"OnUpdate="+Workbench2Designer.dbdOnUpdate.indexOf(relation.getOnUpdate())+"\\n":"";

				Element relationElm = Workbench2Designer.destination.createElement("RELATION");

				TreeMap attributes= new TreeMap();
				String id=Workbench2Designer.dbdIDReference.getRelationDBDID(relation.getID());

				String srcId = Workbench2Designer.dbdIDReference.getTableDBDID(src.getName());
				String destId = Workbench2Designer.dbdIDReference.getTableDBDID(dest.getName());
				attributes.put("ID",id);
				attributes.put("DestTable",destId);
				attributes.put("SrcTable",srcId);
				attributes.put("RelationName",relation.getRelName());
				attributes.put("RefDef","Matching=0\\n"+onDelete+onUpdate);
				attributes.put("CaptionOffsetX","0");
				attributes.put("CaptionOffsetY","0");
				attributes.put("Comments","");
				attributes.put("CreateRefDef","1");
				attributes.put("EndIntervalOffsetX","0");
				attributes.put("EndIntervalOffsetY","0");
				attributes.put("FKFields",relation.getConstraintStatement());
				attributes.put("FKFieldsComments","\\n");
				attributes.put("FKRefDefIndex_Obj_id","-1");
				attributes.put("IDLinkedModel","-1");
				attributes.put("Invisible","0");
				attributes.put("IsLinkedObject","0");
				attributes.put("Kind",kind);
				attributes.put("MidOffset","35");
				attributes.put("Obj_id_Linked","-1");
				attributes.put("OptionalEnd","0");
				attributes.put("OptionalStart","0");
				attributes.put("OrderPos","0");
				attributes.put("Splitted","0");
				attributes.put("StartIntervalOffsetX","0");
				attributes.put("StartIntervalOffsetY","");
				attributes.put("relDirection","4");

				this.setAttributes(attributes, relationElm);

				relationsElm.appendChild((Node)relationElm);
			}
			catch(java.lang.NullPointerException ne)
			{
				//throw(new Workbench2Designer.MalformedSourceException("Invalid relation - destination/source missing or not existing",ne));
				LogMgr.logError("Workbench2Designer.dbdCreateRelations", "Error when adding relation for " + relation, ne);
			}
		}//while
		return relationsElm;

	}

	public Element dbdCreateDatatypeGroups()
		throws DOMException
	{

		Iterator it = Workbench2Designer.dbdDataTypeGroups.listIterator();
		Element dataTypeGroupsElm = destination.createElement("DATATYPEGROUPS");
		int i=0;
		while(it.hasNext())
		{
			i++;
			String dataTypeGroup = (String) it.next();

			Element dataTypeGroupElm = destination.createElement("DATATYPEGROUP");

			dataTypeGroupElm.setAttribute("Name",dataTypeGroup);
			dataTypeGroupElm.setAttribute("Icon",i+"");


			dataTypeGroupsElm.appendChild((Node)dataTypeGroupElm);


		}
		return dataTypeGroupsElm;

	}

	public Element dbdCreateDatatypes()
		throws DOMException,Workbench2Designer.MalformedSourceException
	{
		Set dtpKeys = Workbench2Designer.globalSQLDataTypes.keySet();
		Iterator it = dtpKeys.iterator();
		Element datatypesElm = destination.createElement("DATATYPES");

		while(it.hasNext())
		{
			try
			{
				String text=(String)it.next();
				SQLDataType datatype = (SQLDataType) Workbench2Designer.globalSQLDataTypes.get(text);
				String group=(String)Workbench2Designer.dbdDataTypeGroupRef.get(datatype.index);
				String idGroup= Workbench2Designer.dbdDataTypeGroups.indexOf(group)+"";
				Element datatypeElm = destination.createElement("DATATYPE");
				String id = Workbench2Designer.dbdIDReference.getDataTypeDBDID(datatype.index);
				TreeMap attributes= new TreeMap();

				attributes.put("ID",id);
				attributes.put("Description","");
				attributes.put("IDGroup",idGroup);
				attributes.put("OptionCount","0");
				attributes.put("ParamCount",datatype.paramCount+"");
				attributes.put("ParamRequired",datatype.paramRequired+"");
				attributes.put("PhysicalMapping","0");
				attributes.put("PhysicalTypeName",datatype.alias);
				attributes.put("SynonymGroup","0");
				attributes.put("TypeName",datatype.name);
				attributes.put("EditParamsAsString","0");

				this.setAttributes(attributes, datatypeElm);

				datatypesElm.appendChild((Node)datatypeElm);
				if (datatype.paramCount!=0)
				{
					Element optionsElm = destination.createElement("OPTIONS");
					Element optionElm = destination.createElement("OPTION");
					attributes = new TreeMap();
					attributes.put("Default","0");
					attributes.put("Name","ZEROFILL");
					this.setAttributes(attributes, optionElm);
					Element paramsElm = destination.createElement("PARAMS");
					if(datatype.params!=null)
					{
						for(int i=0;i<datatype.params.length;i++)
						{
							Element paramElm = destination.createElement("PARAM");
							this.setAttribute("Name", datatype.params[i], paramElm);
							paramsElm.appendChild(paramElm);
						}
					}
					datatypeElm.appendChild(paramsElm);
					optionsElm.appendChild(optionElm);
					datatypeElm.appendChild(optionsElm);
				}
				else
				{
					forceEndTag(datatypeElm);
				}
			}
			catch(java.lang.NullPointerException ne)
			{
				throw(new Workbench2Designer.MalformedSourceException("datatype not found in dbd datatype-groups",ne));
			}
		}
		return datatypesElm;

	}

	public Element dbdCreateCommonDatatypes()
		throws DOMException
	{

		Element dbdCommonDataTypes=destination.createElement("COMMON_DATATYPES");
		Iterator it= Workbench2Designer.dataTypes.keySet().iterator();
		Element dbdCommonDataType;
		while(it.hasNext())
		{
			String dataTypeIndexName=(String)it.next();
			String id = dbdIDReference.getDataTypeDBDID(dataTypeIndexName);
			dbdCommonDataType=destination.createElement("COMMON_DATATYPE");
			dbdCommonDataType.setAttribute("ID", id);
			dbdCommonDataTypes.appendChild(dbdCommonDataType);
		}
		return dbdCommonDataTypes;
	}

	public Element dbdCreateGlobalSettings()
		throws DOMException
	{

		Element globalsettingsElm = destination.createElement("GLOBALSETTINGS");
		TreeMap attributes= new TreeMap();
		attributes.put("ActivateRefDefForNewRelations","1");
		attributes.put("AutoIncVersion","1");
		attributes.put("CanvasHeight",Workbench2Designer.dbdCanvasHeight+"");
		attributes.put("CanvasWidth",Workbench2Designer.dbdCanvasWidth+"");
		attributes.put("Comments",Workbench2Designer.globalSettings.comments);
		attributes.put("CreateFKRefDefIndex","0");
		attributes.put("CreateSQLforLinkedObjects","0");
		attributes.put("DBQuoteCharacter","\"");
		attributes.put("DatabaseType","MySql");
		attributes.put("DefModelFont","Tahoma");
		attributes.put("DefQueryDBConn","");
		attributes.put("DefSaveDBConn","");
		attributes.put("DefSyncDBConn","");
		attributes.put("DefaultDataType","5");
		attributes.put("DefaultTablePrefix","0");
		attributes.put("DefaultTableType","0");
		attributes.put("Description","");
		attributes.put("FKPostfix","");
		attributes.put("FKPrefix","");
		attributes.put("HPageCount","3.005135730007337");
		attributes.put("IDModel","0");
		attributes.put("IDVersion","0");
		attributes.put("ModelName",Workbench2Designer.globalSettings.modelName);
		attributes.put("PageAspectRatio","1.418318380972251");
		attributes.put("PageFormat","A4 (210x297 mm, 8.26x11.7 inches)");
		attributes.put("PageOrientation","1");
		attributes.put("PositionGridX","20");
		attributes.put("PositionGridY","20");
		attributes.put("Printer","");
		attributes.put("SelectedPages","0");
		attributes.put("TableNameInRefs","1");
		attributes.put("UsePositionGrid","0");
		attributes.put("UseVersionHistroy","1");
		attributes.put("VersionStr","1.0.0.0");
		attributes.put("XPos","0");
		attributes.put("YPos","0");
		attributes.put("ZoomFac","100.00");

		this.setAttributes(attributes, globalsettingsElm);

		return globalsettingsElm;

	}

	public Element dbdCreateSettings()
		throws DOMException,Workbench2Designer.MalformedSourceException
	{

		Element dbdSettings = destination.createElement("SETTINGS");

		Element dbdGlobalSettings = this.dbdCreateGlobalSettings();
		Element dbdDataTypeGroups = this.dbdCreateDatatypeGroups();
		Element dbdDataTypes = this.dbdCreateDatatypes();
		Element dbdCommonDataTypes = this.dbdCreateCommonDatatypes();

		Element dbdTablePrefixes = destination.createElement("TABLEPREFIXES");
		Element dbdTablePrefix = destination.createElement("TABLEPREFIX");
		dbdTablePrefix.setAttribute("Name","Default (no prefix)");
		dbdTablePrefixes.appendChild(dbdTablePrefix);

		Element dbdRegionColors = destination.createElement("REGIONCOLORS");
		Element dbdRegionColor;
		for(int i=0;i<Workbench2Designer.dbdRegionColors.length;i++)
		{
			dbdRegionColor = destination.createElement("REGIONCOLOR");
			dbdRegionColor.setAttribute("Color", Workbench2Designer.dbdRegionColors[i]);
			dbdRegionColors.appendChild(dbdRegionColor);
		}

		Element dbdPositionMarkers = destination.createElement("POSITIONMARKERS");
		Element dbdPositionMarker = destination.createElement("POSITIONMARKER");
		dbdPositionMarker.setAttribute("X","0");
		dbdPositionMarker.setAttribute("Y","0");
		dbdPositionMarker.setAttribute("ZoomFac","-1.0");
		dbdPositionMarkers.appendChild(dbdPositionMarker);

		dbdSettings.appendChild(dbdGlobalSettings);
		dbdSettings.appendChild(dbdDataTypeGroups);
		dbdSettings.appendChild(dbdDataTypes);
		dbdSettings.appendChild(dbdCommonDataTypes);
		dbdSettings.appendChild(dbdTablePrefixes);
		dbdSettings.appendChild(dbdRegionColors);
		dbdSettings.appendChild(dbdPositionMarkers);

		return dbdSettings;
	}

	public void dbdAppendRelations()
		throws DOMException
	{

		NodeList dbdTables=destination.getElementsByTagName("TABLE");

		for(int i=0;i<dbdTables.getLength();i++)
		{
			NamedNodeMap attributes = dbdTables.item(i).getAttributes();
			String tablename=attributes.getNamedItem("Tablename").getNodeValue();
			Table table=(Table)Workbench2Designer.tables.get(tablename);

			Element dbdRelationsStart=destination.createElement("RELATIONS_START");
			Element dbdRelationsEnd=destination.createElement("RELATIONS_END");
			Element dbdRelationEnd;
			Element dbdRelationStart;

			Iterator iit = table.getRelations().keySet().iterator();
			while(iit.hasNext())
			{
				Relation relation = (Relation)Workbench2Designer.relations.get(iit.next());
				String id=(String)Workbench2Designer.dbdIDReference.getRelationDBDID(relation.getID());
				if(table.getName().equals(relation.getDestTable()))
				{
					dbdRelationEnd=destination.createElement("RELATION_END");
					dbdRelationEnd.setAttribute("ID", id);
					dbdRelationsEnd.appendChild(dbdRelationEnd);
				}
				if(table.getName().equals(relation.getSrcTable()))
				{
					dbdRelationStart=destination.createElement("RELATION_START");
					dbdRelationStart.setAttribute("ID", id);
					dbdRelationsStart.appendChild(dbdRelationStart);
				}
			}
			this.forceEndTag(dbdRelationsStart);
			dbdTables.item(i).appendChild(dbdRelationsStart);
			this.forceEndTag(dbdRelationsEnd);
			dbdTables.item(i).appendChild(dbdRelationsEnd);
		}
	}

	public void dbdAppendIndices()
		throws DOMException
	{
		NodeList dbdTables=destination.getElementsByTagName("TABLE");

		for(int i=0;i<dbdTables.getLength();i++)
		{
			NamedNodeMap attr = dbdTables.item(i).getAttributes();
			String tablename=attr.getNamedItem("Tablename").getNodeValue();
			Table table=(Table)Workbench2Designer.tables.get(tablename);
			Element dbdIndices=destination.createElement("INDICES");
			TreeMap indices=table.getIndices();
			Iterator it= indices.keySet().iterator();
			while(it.hasNext())
			{
				Index index = (Index)indices.get(it.next());
				if(index.isPrimaryKey())continue;
				Element dbdIndex=destination.createElement("INDEX");

				TreeMap attributes = new TreeMap();
				attributes.put("IndexName",index.getName());
				attributes.put("IndexKind", "0");
				attributes.put("FKRefDef_Obj_id", "-1");
				attributes.put("ID", dbdIDReference.getIndexDBDID(index.getID()));
				this.setAttributes(attributes,dbdIndex);

				Element dbdIndexCols=destination.createElement("INDEXCOLUMNS");

				Iterator iit = index.getColumns().iterator();
				while(iit.hasNext())
				{
					Element dbdIndexCol=destination.createElement("INDEXCOLUMN");
					dbdIndexCols.appendChild(dbdIndexCol);

					String colName=(String)iit.next();
					String idColumn=Workbench2Designer.dbdIDReference.getColumnDBDID(table.getName(),colName);
					attributes = new TreeMap();
					attributes.put("LengthParam", "0");
					attributes.put("idColumn", idColumn);
					this.setAttributes(attributes,dbdIndexCol);

				}
				dbdIndices.appendChild(dbdIndex);
				dbdIndex.appendChild(dbdIndexCols);
				this.forceEndTag(dbdIndexCols);
			}
			this.forceEndTag(dbdIndices);
			dbdTables.item(i).appendChild(dbdIndices);
		}

	}

	public void wbReadGlobalSettings()
	{
		String databaseType =  this.getTextByTagName("database-product-name",source);
		String comments = "created: "+ this.getTextByTagName("created",source);
		String modelName="";
		Workbench2Designer.globalSettings= new GlobalSettings(comments,databaseType,modelName);
	}

	public void wbReadTables()
	{
		NodeList tbls = source.getElementsByTagName("table-def");
		for(int i=0;i<tbls.getLength();i++)
		{
			Node tbl = tbls.item(i);
			Table table = new Table();
			NodeList childs = tbl.getChildNodes();

			for(int ii=0;ii<childs.getLength();ii++)
			{
				Node node = childs.item(ii);
				String name =node.getNodeName();
				String text = this.getTextFromElement(node);

				if (name.equals("table-name"))
				{
					table.setName(text);
					continue;
				}
				if (name.equals("table-comment"))
				{
					table.setComment(text);
					continue;
				}
				if (name.equals("table-schema"))
				{
					table.setScheme(text);
					continue;
				}
				if (name.equals("column-def"))
				{
					wbReadColumn(table,node.getChildNodes());
					continue;
				}
				if (name.equals("index-def"))
				{
					wbReadIndex(table,node.getChildNodes());
					continue;
				}

			}
			if(!(table.getName()==null&&table.getName().equals("")))
			{
				Workbench2Designer.tables.put(table.getName(),table);
			}
		}
	}

	public void wbReadIndex(Table table,NodeList indexdata)
	{
		Index newIndex = new Index();
		for(int i=0;i<indexdata.getLength();i++)
		{
			Node node = indexdata.item(i);
//			String type = getNodeTypeName(node.getNodeType());
			String name = node.getNodeName();
			String text = this.getTextFromElement(node);

			if (name.equals("name"))
			{
				newIndex.setName(text);
				continue;
			}
			if (name.equals("unique"))
			{
				if (text.equals("true"))
					newIndex.setUnique(true);
				continue;
			}
			if (name.equals("primary-key"))
			{
				if (text.equals("true"))
					newIndex.setPrimaryKey(true);
				continue;
			}
			if (name.equals("index-expression"))
			{
				String[] cols = text.split(", ");
				for (int ii=0;ii < cols.length;ii++)
				{
					newIndex.addColumn(cols[ii]);
				}
				continue;
			}
		}
		newIndex.setID(Workbench2Designer.idCounter.getNewID()+"");
		table.setIndex(newIndex);
	}

	public void wbReadColumn(Table table,NodeList coldata)
	{
		Column newCol = new Column();
		for(int i=0;i<coldata.getLength();i++)
		{

			Node node = coldata.item(i);
//			String type = getNodeTypeName(node.getNodeType());
			String name =node.getNodeName();
			String text = this.getTextFromElement(node);

			if (name.equals("column-name"))
			{
				newCol.setName(text);
				continue;
			}
			if (name.equals("dbms-data-type"))
			{
				SQLDataTypeStatement dataTypeStatement = new SQLDataTypeStatement(text);
				newCol.setDataTypeStatement(dataTypeStatement);
				continue;
			}
			if (name.equals("primary-key"))
			{
				if (text.equals("true"))
					newCol.setPrimaryKey();
				table.addPrimaryKey(newCol);
				continue;
			}
			if (name.equals("nullable"))
			{
				if (text.equals("true"))
					newCol.setNullable();
				continue;
			}
			if (name.equals("default-value"))
			{
				newCol.setDefaultValue(text);
				continue;
			}
			if (name.equals("dbms-position"))
			{
				Integer pos =(text!=null&&!text.equals(""))? new Integer(text):new Integer(0);
				newCol.setPosition(pos.intValue());
				continue;
			}

		}
		for(int i=0;i<coldata.getLength();i++)
		{
			String colName = coldata.item(i).getNodeName();
			if (colName.equals("references"))
			{
				newCol.setForeignKey();
				wbReadRelations(newCol.getName(),table.getName(),coldata.item(i).getChildNodes());
			}
		}
		if(!(newCol.getName()==null||newCol.getName().equals("")))
		{
			SQLDataType datatype=newCol.getDataTypeStatement().datatype;
			Workbench2Designer.dataTypes.put(datatype.index,"");
			table.addColumn(newCol);
		}
	}

	public void wbReadRelations(String colname,String tablename,NodeList relcontent)
	{
		Relation newRel=new Relation();
		for(int i=0;i<relcontent.getLength();i++)
		{

			Node node = relcontent.item(i);
			String name =node.getNodeName();
			String text = this.getTextFromElement(node);

			if (name.equals("table-name"))
			{
				newRel = new Relation();
				newRel.setOperator("=");
				newRel.setID(Workbench2Designer.idCounter.getNewID());
				newRel.setDestTable(tablename);
				newRel.setSrcTable(text);
				String id = newRel.getID();
				Workbench2Designer.relations.put(id, newRel);
			}
			if (name.equals("column-name"))
			{
				newRel.addConstraint(text,colname);
			}
			if (name.equals("constraint-name"))
			{
				newRel.setRelName(text);
			}
			if (name.equals("update-rule"))
			{
				newRel.setOnUpdate(text);
			}
			if (name.equals("delete-rule"))
			{
				newRel.setOnDelete(text);
			}
		}
	}

	private static class GUIPositioner
	{
		private int xPos=0;
		private int yPos=0;
		private int canvasHeight;
		private int canvasWidth;
		private int elementHeight;
		private int elementWidth;
		private int elementXOffset;
		private int elementYOffset;
		private boolean cascadeIfNecc;

		private GUIPositioner(int canvasHeight,int canvasWidth,int elementHeight, int elementWidth, int elementXOffset,
		int elementYOffset, boolean cascadeIfNecc)
		{
			this.canvasHeight=canvasHeight;
			this.canvasWidth=canvasWidth;
			this.elementHeight=elementHeight;
			this.elementWidth=elementWidth;
			this.elementYOffset=elementYOffset;
			this.elementXOffset=elementXOffset;
			this.cascadeIfNecc=cascadeIfNecc;
		}
		public int getCurrentXPos()
		{
			return this.xPos;
		}
		public int getCurrentYPos()
		{
			return this.yPos;
		}
		public void setNewPosition()
		{
			boolean endReachedX=(this.xPos+this.elementWidth-this.elementXOffset>=this.canvasWidth)?true:false;
			boolean endReachedY=(this.yPos+this.elementHeight-this.elementYOffset>=this.canvasHeight)?true:false;
			if(endReachedX&&!endReachedY)
			{
				this.xPos=0;
				this.yPos=this.yPos+this.elementYOffset;
			}
			if(!endReachedX)
			{
				this.xPos=this.xPos+this.elementXOffset;
				this.yPos=this.yPos+20;
			}
			if(endReachedX&&endReachedY&&cascadeIfNecc)
			{
				this.elementXOffset=this.elementXOffset/2;
				this.elementYOffset=this.elementYOffset/2;
				this.xPos=this.elementXOffset;
				this.yPos=this.elementYOffset;
			}
			if(endReachedX&&endReachedY&&!cascadeIfNecc)
			{
				this.xPos=0;
				this.yPos=0;
			}
		}
	}

	private static class IDCounter
	{
		private int idCounter =0;

		public String getNewID()
		{
			this.idCounter=this.idCounter+1;
			return this.idCounter+"";
		}

	}

	private static class DBDIDReference
	{

		private int idCounter =0;
		private TreeMap dataTypeReference= new TreeMap();
		private TreeMap tableReference= new TreeMap();
		private TreeMap relationsReference = new TreeMap();
		private TreeMap columnsReference = new TreeMap();
		private TreeMap indicesReference = new TreeMap();
		private TreeMap extraReference = new TreeMap();


		private String getNewID()
		{
			this.idCounter=this.idCounter+1;
			return this.idCounter+"";
		}

		public void createDBDIDRef()
		{
			Iterator it;
			it = Workbench2Designer.globalSQLDataTypes.keySet().iterator();
			while(it.hasNext())
			{
				this.dataTypeReference.put(it.next(),this.getNewID());
			}
			it = Workbench2Designer.tables.keySet().iterator();
			//this.idCounter=999;
			while(it.hasNext())
			{
				String tableName=(String)it.next();
				this.tableReference.put(tableName,this.getNewID());
				Table table =(Table) Workbench2Designer.tables.get(tableName);
				Iterator iit = table.getColumns().keySet().iterator();
				while(iit.hasNext())
				{
					String columnName=(String)iit.next();
					this.columnsReference.put(tableName+"/"+columnName,this.getNewID());
				}
				iit = table.getIndices().keySet().iterator();
				while(iit.hasNext())
				{
					String id=(String)iit.next();
					this.indicesReference.put(id,this.getNewID());
				}

			}
			it = Workbench2Designer.relations.keySet().iterator();
			while(it.hasNext())
			{
				this.relationsReference.put(it.next(),this.getNewID());
			}
		}

		public String getTableDBDID(String tableName)
		{
			return (String) this.tableReference.get(tableName);
		}
		public String getColumnDBDID(String tableName, String columnName)
		{
			return (String) this.columnsReference.get(tableName+"/"+columnName);
		}

		public String getIndexDBDID( String id)
		{
			return (String) this.indicesReference.get(id);
		}
		public String getRelationDBDID(String relationID)
		{
			return (String)this.relationsReference.get(relationID);
		}
		public String getDataTypeDBDID(String dataTypeIndexName)
		{
			return (String) this.dataTypeReference.get(dataTypeIndexName);
		}
		public String getExtraDBDID(String name)
		{
			return (String) this.extraReference.get(name);
		}

	}

	private static class GlobalSettings
	{
		private String comments;
		private String databaseType;
		private String modelName;
		GlobalSettings(String comments,String databaseType,String modelName)
		{
			this.comments=comments;
			this.databaseType=databaseType;
			this.modelName=modelName;
		}
	}

	public static class SQLDataTypeStatement
	{

		private String[] params;
		private String paramString="";
//		private Object[] ret;
		private String name;
		private SQLDataType datatype;
		private int paramCount;

		private SQLDataTypeStatement(String typeStatement)
		{

			if (typeStatement.indexOf("(")!=-1)
			{
				int start = typeStatement.indexOf("(");
				int end = typeStatement.indexOf(")");
				this.paramString=typeStatement.substring(start+1,end);
				this.params=this.paramString.split(",");
				this.paramString="("+this.paramString+")";
				this.name=typeStatement.substring(0,start);
			}
			else
			{
				this.name=typeStatement;
			}
			this.paramCount =(params==null)?0:params.length;
			if(this.name.equals("FLOAT"))
			{
				if (this.paramCount==1)
				{
					this.datatype=(SQLDataType)Workbench2Designer.globalSQLDataTypes.get("FLOAT");
				}
				else
				{
					this.datatype=(SQLDataType)Workbench2Designer.globalSQLDataTypes.get("#FLOAT");
				}
			}
			else
			{
				this.datatype=(SQLDataType)Workbench2Designer.globalSQLDataTypes.get(this.name);
			}
			
			if (this.datatype == null)
			{
				this.datatype = (SQLDataType)Workbench2Designer.globalSQLDataTypes.get("OTHER");
			}
		}
		
		public SQLDataType getSQLDataType()
		{
			return this.datatype;
		}
	}

	private static class SQLDataType
	{
		private String[] params;
		private int paramCount;
		private int paramRequired;
		private String index;
		private String name;
		private String alias;
		SQLDataType(String index, int paramRequired, String alias, String name, String[] params)
		{
			this.params=params;
			this.paramCount=this.params.length;
			this.index=index;
			this.name=name;
			this.alias=alias;
			this.paramRequired= paramRequired;
		}
		SQLDataType(String index, int paramRequired, String alias, String name)
		{
			this.params=null;
			this.paramCount=0;
			this.index=index;
			this.name=name;
			this.alias=alias;
			this.paramRequired=0;
		}
	}

	public static class Table
	{

		private String name;
		private String comment;
		private String scheme;
		private TreeMap relations = new TreeMap();
		private TreeMap columns = new TreeMap();
		private TreeMap indices = new TreeMap();
		private TreeMap primaryKeys = new TreeMap();

		public TreeMap getPrimaryKeys()
		{
			return this.primaryKeys;
		}
		public void addPrimaryKey(Column pk)
		{
			this.primaryKeys.put(pk.getName(),pk);
		}
		public String getName()
		{
			return this.name;
		}
		public void setName(String n)
		{
			this.name = n;
		}
		public String getComment()
		{
			return this.comment;
		}
		public void setComment(String c)
		{
			this.comment = c;
		}
		public String getScheme()
		{
			return this.scheme;
		}
		public void setScheme(String s)
		{
			this.scheme = s;
		}
		public void setRelation(Relation r)
		{
			String id = r.getID();
			Workbench2Designer.relations.put(id,r);
		}
		public TreeMap getRelations()
		{
			return Workbench2Designer.relations;
		}
		public TreeMap getIndices()
		{
			return this.indices;
		}
		public void setIndex(Index index)
		{
			String id = index.getID();
			this.indices.put(id,index);
		}
		public void addColumn(Column c)
		{
			this.columns.put(c.getName(),c);
		}
		public TreeMap getColumns()
		{
			return this.columns;
		}
	}

	public static class Column
	{
		private String name;
		private String comment="";
		private SQLDataTypeStatement dataTypeStatement;
		private String table;
		private boolean primaryKey=false;
		private String defaultValue="";
		private int position;
		private boolean nullable=false;
		private boolean foreignKey=false;

		public String getTable()
		{
			return this.table;
		}
		public void setTable(String tbl)
		{
			this.table = tbl;
		}
		public String getName()
		{
			return this.name;
		}
		public void setName(String n)
		{
			this.name = n;
		}
		public String getComment()
		{
			return this.comment;
		}
		public void setComment(String c)
		{
			this.comment = c;
		}
		public String getDefaultValue()
		{
			return this.comment;
		}
		public void setDefaultValue(String dV)
		{
			this.defaultValue = dV;
		}
		public int getPosition()
		{
			return this.position;
		}
		public void setPosition(int p)
		{
			this.position = p;
		}
		public void setDataTypeStatement(SQLDataTypeStatement statement)
		{
			this.dataTypeStatement=statement;
		}
		public SQLDataTypeStatement getDataTypeStatement()
		{
			return this.dataTypeStatement;
		}
		public boolean isForeignKey()
		{
			return this.foreignKey;
		}
		public void setForeignKey()
		{
			this.foreignKey = true;
		}
		public boolean isPrimaryKey()
		{
			return this.primaryKey;
		}
		public void setPrimaryKey()
		{
			this.primaryKey = true;
		}
		public boolean isNullable()
		{
			return this.nullable;
		}
		public void setNullable()
		{
			this.nullable = true;
		}
	}

	public static class Relation
	{
		private String destTable;
		private String srcTable;
		private String operator;
		private TreeSet constraints = new TreeSet();
		private String onUpdate;
		private String onDelete;
		private String id;
		private int relDirection;
		private String relName;

		public void setOperator(String operator)
		{
			this.operator=operator;
		}
		public String getOperator()
		{
			return this.operator;
		}
		public String getDestTable()
		{
			return this.destTable;
		}
		public void setDestTable(String dT)
		{
			this.destTable = dT;
		}
		public String getSrcTable()
		{
			return this.srcTable;
		}
		public void setSrcTable(String sT)
		{
			this.srcTable = sT;
		}
		public TreeSet getConstraints()
		{
			return this.constraints;
		}
		public String getConstraintStatement()
		{
			String ret="";
			Iterator it = this.constraints.iterator();
			while(it.hasNext())
			{
				ret=ret+it.next()+"\\n";
			}
			return ret;
		}
		void addConstraint(String f1,String f2)
		{
			this.constraints.add(f1+this.operator+f2);
		}
		public String getOnUpdate()
		{
			return this.onUpdate;
		}
		public void setOnUpdate(String upd)
		{
			this.onUpdate = upd;
		}
		public String getOnDelete()
		{
			return this.onDelete;
		}
		public void setOnDelete(String del)
		{
			this.onDelete = del;
		}
		public String getID()
		{
			return this.id;
		}
		public void setID(String iD)
		{
			this.id = iD;
		}
		public int getRelDirection()
		{
			return this.relDirection;
		}
		public void setRelDirection(int rD)
		{
			this.relDirection = rD;
		}
		public String getRelName()
		{
			return this.relName;
		}
		public void setRelName(String rN)
		{
			this.relName = rN;
		}
	}

	public static class Index
	{
		private String id;
		private String name;
		private LinkedList columns=new LinkedList();
		private boolean unique=false;
		private boolean primaryKey=false;


		String getID()
		{
			return this.id;
		}
		void setID(String iD)
		{
			this.id = iD;
		}
		public void setName(String name)
		{
			this.name=name;
		}
		public String getName()
		{
			return this.name;
		}
		public void addColumn(String colName)
		{
			this.columns.add(colName);
		}
		public LinkedList getColumns()
		{
			return this.columns;
		}
		public void setUnique(boolean unique)
		{
			this.unique=unique;
		}
		public boolean isUnique()
		{
			return  this.unique;
		}
		public void setPrimaryKey(boolean flag)
		{
			this.primaryKey=flag;
		}
		public boolean isPrimaryKey()
		{
			return  this.primaryKey;
		}
	}

	public static class MalformedSourceException extends Exception
	{
		MalformedSourceException(String msg, Throwable thrw)
		{
			super(msg, thrw);
		}
	}


}
