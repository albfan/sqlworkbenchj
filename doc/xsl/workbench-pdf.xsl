<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
>

  <!--  Original DocBook Stylesheet    -->
  <xsl:import href="../docbook/fo/docbook.xsl"/>
  <xsl:attribute-set name="section.title.level1.properties">
    <xsl:attribute name="break-before">page</xsl:attribute>
  </xsl:attribute-set>
  
	<xsl:attribute-set name="xref.properties">
	  <xsl:attribute name="color">
	    <xsl:choose>
	      <xsl:when test="self::link">blue</xsl:when>
	      <xsl:when test="self::ulink">blue</xsl:when>
	      <xsl:otherwise>inherit</xsl:otherwise>
	    </xsl:choose>
	  </xsl:attribute>
	  <xsl:attribute name="text-decoration">
	    <xsl:choose>
	      <xsl:when test="self::link">underline</xsl:when>
	      <xsl:when test="self::ulink">underline</xsl:when>
	      <xsl:otherwise>inherit</xsl:otherwise>
	    </xsl:choose>
	  </xsl:attribute>
	</xsl:attribute-set>
  
	<xsl:attribute-set name="root.properties">
		<xsl:attribute name="widows">6</xsl:attribute>
		<xsl:attribute name="orphans">6</xsl:attribute>
	</xsl:attribute-set>	
</xsl:stylesheet>


