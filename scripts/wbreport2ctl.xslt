<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:redirect="http://xml.apache.org/xalan/redirect"
                extension-element-prefixes="redirect"
                >
<xsl:output encoding="iso-8859-15" method="text" indent="no" omit-xml-declaration="yes" doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"/>
  <xsl:param name="wb-basedir">.</xsl:param>
  <xsl:strip-space elements="*"/>
  <xsl:variable name="newline"><xsl:text>&#10;</xsl:text></xsl:variable>
  <xsl:template match="/">
    <xsl:for-each select="/schema-report/table-def">
      <xsl:variable name="table" select="table-name"/>
      <xsl:variable name="filename" select="concat($wb-basedir, '/', $table, '.ctl')"/>
      <redirect:write file="{$filename}">OPTIONS (skip=1)
LOAD DATA CHARACTERSET UTF8
TRUNCATE
INTO TABLE <xsl:value-of select="$table"/>
FIELDS TERMINATED BY '\t'
    TRAILING NULLCOLS
(
<xsl:for-each select="column-def">
  <xsl:sort select="dbms-position" data-type="number"/>
  <xsl:variable name="dbtype" select="dbms-data-type"/>
  <xsl:variable name="coltype">
    <xsl:choose>
      <xsl:when test="$dbtype = 'DATE'">  DATE "YYYY-MM-DD HH24:MI:SS"</xsl:when>
      <xsl:otherwise></xsl:otherwise>
    </xsl:choose>
    <xsl:if test="(position() &lt; last())">,<xsl:value-of select="$newline"/></xsl:if>
  </xsl:variable>
<xsl:text>   </xsl:text><xsl:value-of select="column-name"/><xsl:value-of select="$coltype"/>
</xsl:for-each>
)
      </redirect:write>
    </xsl:for-each>

  </xsl:template>

</xsl:stylesheet>  
