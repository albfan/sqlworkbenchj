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
        background-color: rgb(240,240,240);
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
      
      .subTitle {
        font-size:110%;
        font-variant:small-caps; 
      }
    </style>
  <title><xsl:call-template name="write-name"/></title>
  </head>
  <body>
    <xsl:call-template name="create-toc"/>
    <xsl:call-template name="table-definitions"/>
  </body>
  </html>
</xsl:template>

<xsl:template name="write-name">
  <xsl:variable name="title" select="//schema-report/report-title"/>
  <xsl:choose>
    <xsl:when test="string-length($title) &gt; 0">
      <xsl:value-of select="$title"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="'SQL Workbench/J - Schema Report'"/>
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
    <xsl:variable name="letterCount" select="count(/schema-report/table-def[translate(substring(table-name,1,1), $upper, $lower) = $letter])"/>
    <xsl:if test="$letterCount &gt; 0">
      <a style="font-size:150%;font-weight:bold" name="toc_{$letter}">
        <xsl:value-of select="translate($letter, $lower, $upper)"/>
      </a>
      <ul>
      <xsl:for-each select="/schema-report/table-def[translate(substring(table-name,1,1), $upper, $lower) = $letter]">
        <xsl:sort select="table-name"/>
        <li>
          <xsl:variable name="table" select="table-name"/>
          <a href="#{$table}"><xsl:value-of select="$table"/></a>
        </li>
      </xsl:for-each>
      </ul>
    </xsl:if>
</xsl:template>

<xsl:template name="table-definitions">

  <xsl:for-each select="/schema-report/table-def">

    <xsl:sort select="table-name"/>
    <xsl:variable name="table" select="table-name"/>

    <div class="tableNameHeading">
      <a style="font-size:175%; font-weight:bold; font-family: monospace;" name="{$table}">
        <xsl:value-of select="$table"/>
      </a>
      <xsl:if test="string-length(table-comment) &gt; 0">
        <p>
        <xsl:value-of select="table-comment"/>
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
		    <p class="subTitle">References</p>
        <ul>
        <xsl:for-each select="column-def">
          <xsl:variable name="column" select="column-name"/>
          <xsl:if test="references">
          	<xsl:variable name="targetTable" select="references/table-name"/>
          	<li><a href="#{$targetTable}"><xsl:value-of select="$targetTable"/></a> (<xsl:value-of select="references/column-name"/>) through <xsl:value-of select="$column"/></li>
					</xsl:if>
        </xsl:for-each>
        </ul>
		</xsl:if>
		
		<xsl:if test="count(//references[table-name=$table]) &gt; 0">
		    <p class="subTitle">Referenced by</p>
		    <ul>
        <xsl:for-each select="//references[table-name=$table]">
          <xsl:variable name="ftable" select="parent::*/parent::*/child::table-name"/>
          <xsl:if test="string-length($ftable) &gt; 0">
            <li><a href="#{$ftable}"><xsl:value-of select="$ftable"/></a></li>
          </xsl:if>
        </xsl:for-each>
        </ul>
		</xsl:if>

  </xsl:for-each>

</xsl:template>

</xsl:transform>  
