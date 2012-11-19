<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet
     version="1.0"
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>

<xsl:output
  encoding="iso-8859-15"
  method="xml"
  indent="no"
  standalone="yes"
  omit-xml-declaration="yes"
/>

<xsl:variable name="newline"><xsl:text>&#10;</xsl:text></xsl:variable>
<xsl:variable name="lt" select="'&lt;'"/>
<xsl:variable name="gt" select="'&gt;'"/>

<xsl:template match="/">

  <xsl:variable name="tableName" select="/wb-export/table-def/table-name"/>

<dataset>

  <xsl:value-of select="$newline"/>
  <xsl:text>  </xsl:text><table name="{$tableName}">
  <xsl:value-of select="$newline"/>

  <xsl:for-each select="/wb-export/table-def/column-def">
     <xsl:text>    </xsl:text><column><xsl:value-of select="column-name"/></column>
     <xsl:value-of select="$newline"/>
  </xsl:for-each>

  <!-- Write the data rows -->
  <xsl:for-each select="/wb-export/data/row-data">
    <xsl:text>    </xsl:text><row>
    <xsl:value-of select="$newline"/>
    <xsl:for-each select="column-data">

      <xsl:variable name="col-index" select="@index"/>
      <xsl:variable name="column" select="/wb-export/table-def/column-def[@index=$col-index]/column-name"/>

      <xsl:choose>
        <xsl:when test="@null='true'">
          <xsl:text>      </xsl:text><null/>
        </xsl:when>
      <xsl:otherwise>
        <xsl:text>      </xsl:text><value><xsl:value-of select="."/></value>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:value-of select="$newline"/>
    </xsl:for-each> <!-- column-data -->

    <xsl:text>    </xsl:text></row>
    <xsl:value-of select="$newline"/>

  </xsl:for-each> <!-- row-data -->
  <xsl:text>    </xsl:text></table>
  <xsl:value-of select="$newline"/>
</dataset>

</xsl:template>

</xsl:stylesheet>

