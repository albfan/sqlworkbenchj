<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:transform 
     version="1.0" 
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output 
  encoding="iso-8859-15" 
  method="html" 
  indent="yes"
  standalone="yes"
  omit-xml-declaration="yes"
  doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
/>

<xsl:strip-space elements="*"/>

<xsl:template match="/">
  <html>
  <head>
    <style>
    
      .ulToc {
          list-style-type:none;
      } 
      
      .liToc {
          width:40em;
          margin:2px 0 0 0;
          padding:0 5px 0 0;
          float:left;
      }    
      
      .tableNameHeading {
           margin-top:2em;
           margin-bottom:1em;
           border-top: 2px solid gray;
           border-bottom: 1px solid #C0C0C0;
           background-color:#F5F5FF;
       }

      .tableComment { 
        background-color:#e4efff; margin-bottom:20px;
      }
      
      .tableDefinition { 
        padding:2px; border-collapse:collapse; margin-top:1em;
      }

      .tdTableDefinition {
        padding-right:10px;
        vertical-align:top;
        border-bottom:1px solid #C0C0C0;
      }

      .tdColName {
        width:20em;
      }
      
      .tdDataType {
        width:10em;
      }

      .tdPkFlag {
        width:4em;
      }

      .tdNullFlag {
        width:9em;
      }

      .tdTableHeading {
        padding:2px;
        font-weight:bold;
        vertical-align:top;
        border-bottom: 1px solid #C0C0C0;
        background-color: rgb(240,240,240);
      }
      
      .subTitle {
        font-size:110%;
        font-variant:small-caps; 
      }
    </style>
    
  <title><xsl:call-template name="write-name"/></title>
  </head>
  <body>
    <xsl:call-template name="create-toc"/>
    <div style="clear:both"/>
    <xsl:call-template name="table-definitions"/>
  </body>
  </html>
</xsl:template>

<xsl:template name="write-name">
  <xsl:variable name="title" select="//architect-project/project-name"/>
  <xsl:choose>
    <xsl:when test="string-length($title) &gt; 0">
      <xsl:value-of select="$title"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="'Power*Architect Datamodel'"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="create-toc">
    <center><h2><xsl:call-template name="write-name"/></h2></center>
    <h3><xsl:text>List of tables</xsl:text></h3>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'a'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'b'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'c'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'d'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'e'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'f'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'g'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'h'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'i'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'j'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'k'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'m'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'n'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'o'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'p'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'q'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'r'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'s'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'t'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'u'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'v'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'w'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'x'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'y'"/>
    </xsl:call-template>
    <xsl:call-template name="create-toc-single-letter">
      <xsl:with-param name="letter" select="'z'"/>
    </xsl:call-template>
</xsl:template>

<xsl:template name="create-toc-single-letter">
    <xsl:param name="letter"/>
    <xsl:variable name="lower">abcdefghijklmnopqrstuvwxyz</xsl:variable>
    <xsl:variable name="upper">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>
    <xsl:variable name="letterCount" select="count(/architect-project/target-database/table[translate(substring(@name,1,1), $upper, $lower) = $letter])"/>
    <xsl:if test="$letterCount &gt; 0">
      <a style="font-size:150%;font-weight:bold" name="toc_{$letter}">
        <xsl:value-of select="translate($letter, $lower, $upper)"/>
      </a>
      <ul>
      <xsl:for-each select="/architect-project/target-database/table[translate(substring(@name,1,1), $upper, $lower) = $letter]">
        <xsl:sort select="@name"/>
        <li>
          <xsl:variable name="table" select="@name"/>
          <a href="#{$table}"><xsl:value-of select="$table"/></a>
        </li>
      </xsl:for-each>
      </ul>
    </xsl:if>
</xsl:template>

<xsl:template name="table-definitions">

  <xsl:for-each select="/architect-project/target-database/table">

    <xsl:sort select="@name"/>
    <xsl:variable name="table" select="@name"/>
    <xsl:variable name="table-id" select="@id"/>

    <div class="tableNameHeading">
      <a style="font-size:175%; font-weight:bold; font-family: monospace;" name="{$table}">
        <xsl:value-of select="$table"/>
      </a>
      <xsl:if test="string-length(@remarks) &gt; 0">
        <p>
        <xsl:value-of select="@remarks"/>
        </p>
      </xsl:if>
    </div>

    <table class="tableDefinition" width="100%">
      <tr>
        <td class="tdTableHeading tdColName"><xsl:text>Column</xsl:text></td>
        <td class="tdTableHeading tdDataType"><xsl:text>Type</xsl:text></td>
        <td class="tdTableHeading tdPkFlag"><xsl:text>PK</xsl:text></td>
        <td class="tdTableHeading tdNullFlag"><xsl:text>Nullable</xsl:text></td>
        <td class="tdTableHeading"><xsl:text>Comment</xsl:text></td>
      </tr>

      <xsl:for-each select="folder//column">
        <xsl:sort select="@id"/>
        <tr>
          <td class="tdTableDefinition"><xsl:value-of select="@name"/></td>
          <td class="tdTableDefinition">
            <xsl:call-template name="write-data-type">
              <xsl:with-param name="type-id" select="@type"/>
              <xsl:with-param name="precision" select="@precision"/>
              <xsl:with-param name="scale" select="@scale"/>
            </xsl:call-template>
           </td>
          <td class="tdTableDefinition" nowrap="nowrap">
            <xsl:if test="string-length(@primaryKeySeq) &gt; 0">
              <xsl:text>PK</xsl:text>
            </xsl:if>
          </td>
          <td class="tdTableDefinition" nowrap="nowrap">
            <xsl:if test="@nullable='0'">
              <xsl:text>NOT NULL</xsl:text>
            </xsl:if>
          </td>
          <td class="tdTableDefinition">
            <xsl:value-of select="@remarks"/>
          </td>
        </tr>
      </xsl:for-each>
    </table>

		<xsl:if test="count(/architect-project/target-database/relationships/relationship[@fk-table-ref=$table-id]) &gt; 0">
		    <p class="subTitle"><xsl:text>References</xsl:text></p>
        <ul>
        <xsl:for-each select="/architect-project/target-database/relationships/relationship[@fk-table-ref=$table-id]">
        	<xsl:variable name="pk-id" select="@pk-table-ref"/>
        	<xsl:variable name="targetTable" select="/architect-project/target-database/table[@id=$pk-id]/@name"/>
        	<li>
            <a href="#{$targetTable}"><xsl:value-of select="$targetTable"/></a><xsl:text> through (</xsl:text>
          	<xsl:for-each select="column-mapping">
              <xsl:variable name="fk-col-id" select="@fk-column-ref"/>
              <xsl:variable name="fk-col-name" select="//column[@id=$fk-col-id]/@name"/>
              <xsl:value-of select="$fk-col-name"/>
              <xsl:if test="position() &lt; last()"><xsl:text>, </xsl:text></xsl:if>
          	</xsl:for-each><xsl:text>)</xsl:text>
          </li>
        </xsl:for-each>
        </ul>
		</xsl:if>

		<xsl:if test="count(/architect-project/target-database/relationships/relationship[@pk-table-ref=$table-id]) &gt; 0">
		    <p class="subTitle"><xsl:text>Referenced By</xsl:text></p>
        <ul>
        <xsl:for-each select="/architect-project/target-database/relationships/relationship[@pk-table-ref=$table-id]">
        	<xsl:variable name="fk-id" select="@fk-table-ref"/>
        	<xsl:variable name="targetTable" select="/architect-project/target-database/table[@id=$fk-id]/@name"/>
        	<li><a href="#{$targetTable}"><xsl:value-of select="$targetTable"/></a><xsl:text> referencing (</xsl:text>
        	<xsl:for-each select="column-mapping">
            <xsl:variable name="pk-col-id" select="@pk-column-ref"/>
            <xsl:variable name="pk-col-name" select="//column[@id=$pk-col-id]/@name"/>
            <xsl:value-of select="$pk-col-name"/>
            <xsl:if test="position() &lt; last()"><xsl:text>, </xsl:text></xsl:if>
        	</xsl:for-each><xsl:text>)</xsl:text>
          </li>
        </xsl:for-each>
        </ul>
		</xsl:if>
    
  </xsl:for-each>

</xsl:template>

<xsl:template name="write-data-type">
  <xsl:param name="type-id"/>
  <xsl:param name="precision"/>
  <xsl:param name="scale"/>
  <xsl:choose>
    <xsl:when test="$type-id = 2005">
      <xsl:text>CLOB</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 2011">
      <xsl:text>NCLOB</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 2004">
      <xsl:text>BLOB</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = -3">
      <xsl:text>VARBINARY</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = -4">
      <xsl:text>LONGVARBINARY</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = -1">
      <xsl:text>LONGVARCHAR</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 93">
      <xsl:text>TIMESTAMP</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 92">
      <xsl:text>TIME</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 91">
      <xsl:text>DATE</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 1">
      <xsl:text>CHAR</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = -15">
      <xsl:text>NCHAR</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 4">
      <xsl:text>INTEGER</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 5">
      <xsl:text>SMALLINT</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 8">
      <xsl:text>DOUBLE</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 7">
      <xsl:text>REAL</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 6">
      <xsl:text>FLOAT</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 16">
      <xsl:text>BOOLEAN</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = -7">
      <xsl:text>BIT</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 2">
      <xsl:text>NUMERIC(</xsl:text><xsl:value-of select="$precision"/><xsl:text>,</xsl:text><xsl:value-of select="$scale"/><xsl:text>)</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 3">
      <xsl:text>DECIMAL(</xsl:text><xsl:value-of select="$precision"/><xsl:text>,</xsl:text><xsl:value-of select="$scale"/><xsl:text>)</xsl:text>
    </xsl:when>
    <xsl:when test="$type-id = 12">
      <xsl:text>VARCHAR(</xsl:text><xsl:value-of select="$precision"/><xsl:text>)</xsl:text>
    </xsl:when>
    <xsl:otherwise>
        <xsl:text>[</xsl:text><xsl:value-of select="$type-id"/><xsl:text>]</xsl:text>
    </xsl:otherwise>  
  </xsl:choose>
</xsl:template>

</xsl:transform>  
