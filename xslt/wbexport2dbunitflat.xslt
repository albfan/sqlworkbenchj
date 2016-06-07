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

<xsl:param name="nullString">[NULL]</xsl:param>

<xsl:variable name="newline"><xsl:text>&#10;</xsl:text></xsl:variable>
<xsl:variable name="lt" select="'&lt;'"/>
<xsl:variable name="gt" select="'&gt;'"/>

<xsl:template match="/">

  <xsl:variable name="tableName" select="/wb-export/table-def/table-name"/>

<dataset>

  <xsl:value-of select="$newline"/>

  <!-- Write the data rows -->
  <xsl:for-each select="/wb-export/data/row-data">
    <xsl:text>  </xsl:text><xsl:value-of select="$lt" disable-output-escaping="yes"/><xsl:value-of select="$tableName"/>

    <xsl:for-each select="column-data">

      <xsl:variable name="col-index" select="@index"/>
      <xsl:variable name="column" select="/wb-export/table-def/column-def[@index=$col-index]/column-name"/>

      <xsl:variable name="value">
        <xsl:choose>
          <xsl:when test="@null='true'">
            <xsl:value-of select="$nullString"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="."/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>

      <xsl:text> </xsl:text><xsl:value-of select="$column"/><xsl:text>="</xsl:text><xsl:value-of select="$value"/><xsl:text>" </xsl:text>
    </xsl:for-each>

    <xsl:text>/</xsl:text><xsl:value-of select="$gt" disable-output-escaping="yes"/>
    <xsl:value-of select="$newline"/>

  </xsl:for-each>

</dataset>

</xsl:template>

</xsl:stylesheet>

