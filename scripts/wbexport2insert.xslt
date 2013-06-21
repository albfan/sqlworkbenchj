<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet
     version="1.0"
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>

<xsl:output
  encoding="iso-8859-15"
  method="text"
  indent="yes"
  standalone="yes"
  omit-xml-declaration="yes"
/>

<xsl:variable name="newline"><xsl:text>&#10;</xsl:text></xsl:variable>
<xsl:variable name="tab"><xsl:text>&#x09;</xsl:text></xsl:variable>

<xsl:template match="/">
    <xsl:variable name="table" select="/wb-export/table-def/table-name"/>

    <xsl:for-each select="/wb-export/data/row-data">
    <xsl:text>insert into </xsl:text><xsl:value-of select="$table"/><xsl:text> (</xsl:text>

    <xsl:for-each select="/wb-export/table-def/column-def">
       <xsl:sort select="@index"/>
       <xsl:value-of select="column-name"/>
       <xsl:if test="position() &lt; last()"><xsl:text>,</xsl:text></xsl:if>
    </xsl:for-each>
    <xsl:text>)</xsl:text>
    <xsl:value-of select="$newline"/>
    <xsl:text>values</xsl:text>
    <xsl:value-of select="$newline"/>
    <xsl:text>(</xsl:text>
    <xsl:value-of select="$newline"/>

    <xsl:for-each select="column-data">

      <xsl:variable name="col-index" select="@index"/>
      <xsl:variable name="column" select="/wb-export/table-def/column-def[@index=$col-index]/column-name"/>
      <xsl:variable name="type-name" select="/wb-export/table-def/column-def[@index=$col-index]/java-sql-type-name"/>

      <xsl:variable name="value">
        <xsl:choose>
          <xsl:when test="@null='true'">
            <xsl:value-of select="'NULL'"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="."/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:text>  </xsl:text>
      <xsl:choose>
        <xsl:when test="($type-name = 'VARCHAR') or ($type-name = 'CHAR') or ($type-name = 'NCHAR') or ($type-name = 'NVARCHAR') or ($type-name = 'CLOB') or ($type-name = 'NCLOB')">
          <xsl:text>'</xsl:text><xsl:value-of select="$value"/><xsl:text>'</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$value"/>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:if test="position() &lt; last()"><xsl:text>,</xsl:text></xsl:if>
      <xsl:value-of select="$newline"/>
    </xsl:for-each>
    <xsl:text>);</xsl:text>

    <xsl:value-of select="$newline"/>
  </xsl:for-each>

</xsl:template>

</xsl:stylesheet>
