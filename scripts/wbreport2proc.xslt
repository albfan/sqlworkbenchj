<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:redirect="http://xml.apache.org/xalan/redirect"
                extension-element-prefixes="redirect"
                >
  <xsl:output
    encoding="iso-8859-15"
    method="text"
    indent="no"
    omit-xml-declaration="yes"
    doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
  />
  <xsl:strip-space elements="*"/>
  <xsl:template match="/">
    <xsl:for-each select="/schema-report/proc-def">
      <xsl:variable name="proc" select="proc-name"/>
      <xsl:variable name="filename" select="concat($proc, '.sql')"/>
      <redirect:write file="$filename">
        <xsl:value-of select="proc-source"/>
      </redirect:write>
    </xsl:for-each>

  </xsl:template>

</xsl:stylesheet>
