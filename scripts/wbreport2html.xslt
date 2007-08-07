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

<!-- <xsl:preserve-space elements="code"/> -->
<xsl:variable name="docTitle" select="'SQL Workbench/J - Schema Report'"/>

<xsl:template match="/">
  <html>
  <head>
    <style>
      .tableNameHeading {
           margin-top:2em;
           margin-bottom:1em;
           border-top: 2px solid gray;
           border-bottom: 1px solid #C0C0C0;
           background-color:#F5F5FF;
       }

      .tableComment{ background-color:#e4efff; margin-bottom:20px;}
      .tableDefinition { padding:2px; border-collapse:collapse; margin-top:1em;}

      .tdTableDefinition {
        padding-right:10px;
        vertical-align:top;
        border-bottom:1px solid #C0C0C0;
      }

      .tdTableHeading {
        padding:2px;
        font-weight:bold;
        vertical-align:top;
        border-bottom: 1px solid #C0C0C0;
        background-color: #d0dfff;
      }
      
      .subTitle {
        font-size:115%;
        font-variant:small-caps; 
        font-weight:bold;
      }
    </style>
  <title><xsl:value-of select="$docTitle"/></title>
  </head>
  <body>
    <xsl:call-template name="create-toc"/>
    <xsl:call-template name="table-definitions"/>
  </body>
  </html>
</xsl:template>

<xsl:template name="create-toc">
    <center><h2><xsl:value-of select="$docTitle"/></h2></center>
    <h3>List of tables</h3>
    <ul>
      <xsl:for-each select="/schema-report/table-def">
        <xsl:sort select="table-name"/>
        <li>
          <xsl:variable name="table" select="table-name"/>
          <a href="#{$table}"><xsl:value-of select="$table"/></a>
        </li>
      </xsl:for-each>
    </ul>
</xsl:template>


<xsl:template name="table-definitions">

  <xsl:for-each select="/schema-report/table-def">

    <xsl:sort select="table-name"/>
    <xsl:variable name="table" select="table-name"/>

    <div class="tableNameHeading">
      <a style="font-size:175%; font-weight:bold; font-family: monospace;" name="{$table}">
        <xsl:value-of select="$table"/>
      </a>
      <p>
      <xsl:value-of select="table-comment"/>
      </p>
    </div>

    <table class="tableDefinition">
      <tr>
        <td class="tdTableHeading" width="15%">Column</td>
        <td class="tdTableHeading" width="12%">Type</td>
        <td class="tdTableHeading" width="3%">PK</td>
        <td class="tdTableHeading" width="5%">Nullable</td>
        <td class="tdTableHeading">Comment</td>
      </tr>

      <xsl:for-each select="column-def">
        <xsl:sort select="dbms-position"/>
        <tr>
          <td class="tdTableDefinition"><xsl:value-of select="column-name"/></td>
          <td class="tdTableDefinition">
            <xsl:value-of select="dbms-data-type"/>
          </td>
          <td class="tdTableDefinition" nowrap="nowrap">
            <xsl:if test="primary-key='true'">
              <xsl:text>PK</xsl:text>
            </xsl:if>
          </td>
          <td class="tdTableDefinition" nowrap="nowrap">
            <xsl:if test="nullable='false'">
              <xsl:text>NOT NULL</xsl:text>
            </xsl:if>
          </td>
          <td class="tdTableDefinition">
            <xsl:value-of select="comment"/>
          </td>
        </tr>
      </xsl:for-each>
    </table>

		<xsl:if test="count(column-def/references) &gt; 0">
		    <p class="subTitle">Foreign keys</p>
        <ul>
        <xsl:for-each select="column-def">
          <xsl:variable name="column" select="column-name"/>
          <xsl:if test="references">
          	<xsl:variable name="targetTable" select="references/table-name"/>
          	<li><xsl:value-of select="$column"/> references <a href="#{$targetTable}"><xsl:value-of select="$targetTable"/></a> (<xsl:value-of select="references/column-name"/>)</li>
					</xsl:if>
        </xsl:for-each>
        </ul>
		</xsl:if>

  </xsl:for-each>

</xsl:template>

</xsl:transform>  
