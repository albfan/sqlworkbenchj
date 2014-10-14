<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:transform
     version="1.0"
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>
<xsl:output
  encoding="iso-8859-15"
  method="html"
  indent="yes"
  omit-xml-declaration="yes"
  doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
/>

<xsl:variable name="ref-schema" select="concat('User=', /schema-diff/reference-connection/database-user, ' url=', /schema-diff/reference-connection/jdbc-url)"/>
<xsl:variable name="target-schema" select="concat('User=', /schema-diff/target-connection/database-user, ' url=', /schema-diff/target-connection/jdbc-url)"/>

<xsl:template match="/">
  <html>
  <head>
    <title>SQL Workbench/J - Schema Diff</title>
    <style type="text/css">
    
      body {
        font-size:12px;
      }
    
      h1 {
        font-size:18px;
      }

      h3 {
        border-bottom-width:2px;
        border-bottom-style:solid;
        border-bottom-color:gray;
        font-size:16px;
      }

      table {
        font-size:12px;
      }

      .tableDefinition {
        padding:2px; 
        border-collapse:collapse; 
        margin-top:1em;
        border-width:1px;
        border-style:solid;
        border-color:black;
      }

      td {
        border-width:1px;
        border-style:solid;
        border-color:gray;
        padding-left:5px;
        padding-right:5px;
        padding-bottom:2px;
        padding-top:2px;
      }
      
      .tdTableHeading {
        border-width:1px;
        border-style:solid;
        border-color:gray;
        padding:5px;
        font-weight:bold;
        vertical-align:top;
        background-color: rgb(240,240,240);
      }

    </style>
    <script type="text/javascript">
      <![CDATA[
        function toggleElement(elementId)
        {
          element = document.getElementById(elementId);
          style = element.style.display;

          var newstyle = "block";
          if (style == "block")
          {
             newstyle = "none";
          }
          element.style.display=newstyle;
        }
      ]]>
    </script>
  </head>
  <body>
    <h2>SQL Workbench/J - Schema diff</h2>
    <xsl:text>Reference schema: </xsl:text><xsl:value-of select="$ref-schema"/><br/>
    <xsl:text>Target schema: </xsl:text><xsl:value-of select="$target-schema"/><br/>

    <xsl:variable name="add-tbl-count" select="count(/schema-diff/add-table)"/>
    <xsl:variable name="col-drop-count" select="count(/schema-diff/modify-table/drop-column)"/>

    <ul>
      <li><a href="#missing-columns">Missing columns</a></li>
      <xsl:if test="$col-drop-count &gt; 0"><li><a href="#drop-columns">Columns to drop</a></li></xsl:if>
      <xsl:if test="$add-tbl-count &gt; 0"><li><a href="#missing-tables">Missing tables</a></li></xsl:if>
      <li><a href="#drop-tables">Tables to drop</a></li>
    </ul>
    
    <h3 id="missing-columns">Missing columns</h3>
    <xsl:call-template name="add-columns"/>

    <xsl:if test="$add-tbl-count &gt; 0">
      <h3 id="missing-tables">Missing tables </h3>
      <xsl:call-template name="add-tables"/>
    </xsl:if>

    <xsl:if test="$col-drop-count &gt; 0">
      <h3 id="drop-columns">Columns to drop</h3>
      <xsl:call-template name="drop-columns"/>
    </xsl:if>

    <h3 id="drop-tables">Tables to drop</h3>
    <xsl:call-template name="drop-tables"/>
    
  </body>
  </html>
</xsl:template>

<xsl:template name="add-columns">

    <table class="tableDefinition">
      <tr>
        <th class="tdTableHeading"><xsl:text>Table name</xsl:text></th>
        <th class="tdTableHeading"><xsl:text>Column</xsl:text></th>
        <th class="tdTableHeading"><xsl:text>Type</xsl:text></th>
        <th class="tdTableHeading"><xsl:text>Default value</xsl:text></th>
        <th class="tdTableHeading"><xsl:text>Nullable</xsl:text></th>
      </tr>
    
    <xsl:for-each select="/schema-diff/modify-table/add-column/column-def">
      <xsl:sort select="table-name"/>
      <xsl:variable name="table" select="../../@name"/>
      
      <tr>
        <td><xsl:value-of select="$table"/></td>
        <td><xsl:value-of select="column-name"/></td>
        <td><xsl:value-of select="dbms-data-type"/></td>
        <td><xsl:value-of select="default-value"/></td>
        <td><xsl:value-of select="nullable"/></td>
      </tr>
        
    </xsl:for-each>
    
  </table>
    
</xsl:template>

<xsl:template name="add-tables">

  <div id="missing-tables">
   
    <xsl:for-each select="/schema-diff/add-table">
      <xsl:sort select="table-name"/>
      
      <xsl:variable name="table" select="@name"/>
      
      <h4><a name="{$table}" href="javascript:toggleElement('add_{$table}_det')"><xsl:value-of select="$table"/></a></h4>
      <xsl:for-each select="table-def">
        <div id="add_{$table}_det" style="display:none">
        <xsl:call-template name="table-definition"/>
        </div>
      </xsl:for-each>
    </xsl:for-each>
    
  </div>
    
</xsl:template>


<xsl:template name="table-definition">

    <table class="tableDefinition">
      <tr>
        <th class="tdTableHeading"><xsl:text>Column</xsl:text></th>
        <th class="tdTableHeading"><xsl:text>Type</xsl:text></th>
        <th class="tdTableHeading"><xsl:text>Default value</xsl:text></th>
        <th class="tdTableHeading"><xsl:text>Nullable</xsl:text></th>
      </tr>
    
    <xsl:for-each select="column-def">
      <xsl:sort select="column-name"/>
      <tr>
        <td><xsl:value-of select="column-name"/></td>
        <td><xsl:value-of select="dbms-data-type"/></td>
        <td><xsl:value-of select="default-value"/></td>
        <td><xsl:value-of select="nullable"/></td>
      </tr>
    </xsl:for-each>
  </table>
</xsl:template>

<xsl:template name="drop-columns">

    <table class="tableDefinition">
      <tr>
        <td class="tdTableHeading"><xsl:text>Table name</xsl:text></td>
        <td class="tdTableHeading"><xsl:text>Column</xsl:text></td>
        <td class="tdTableHeading"><xsl:text>Type</xsl:text></td>
      </tr>
    
    <xsl:for-each select="/schema-diff/modify-table/drop-column/column-def">
      <xsl:sort select="table-name"/>
      <xsl:variable name="table" select="../../@name"/>
      
      <tr>
        <td><xsl:value-of select="$table"/></td>
        <td><xsl:value-of select="column-name"/></td>
        <td><xsl:value-of select="dbms-data-type"/></td>
      </tr>
        
    </xsl:for-each>
    
  </table>
    
</xsl:template>

<xsl:template name="drop-tables">

    <xsl:for-each select="/schema-diff/drop-table/table-name">
      <xsl:sort select="."/>
      <ul>
        <li>
          <xsl:value-of select="."/>
        </li>
      </ul>
    </xsl:for-each>

</xsl:template>

</xsl:transform>
