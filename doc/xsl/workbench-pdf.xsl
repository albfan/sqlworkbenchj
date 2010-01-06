<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
>

  <!--  Original DocBook Stylesheet    -->
  <xsl:import href="../../etc/docbook/fo/docbook.xsl"/>
  <xsl:param name="paper.type" select="'A4'"/>

  <xsl:template match="processing-instruction('pagebreak')">
     <fo:block break-after='page'/>
   </xsl:template>
 
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

  <xsl:attribute-set name="graphical.admonition.properties">
    <xsl:attribute name="space-before.optimum">0.3em</xsl:attribute>
    <xsl:attribute name="space-before.minimum">0.1em</xsl:attribute>
    <xsl:attribute name="space-before.maximum">0.5em</xsl:attribute>
    <xsl:attribute name="space-after.optimum">0.3em</xsl:attribute>
    <xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
    <xsl:attribute name="space-after.maximum">0.5em</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="root.properties">
    <xsl:attribute name="widows">6</xsl:attribute>
    <xsl:attribute name="orphans">6</xsl:attribute>
  </xsl:attribute-set>
  
  <xsl:attribute-set name="section.title.level1.properties">
    <xsl:attribute name="break-before">page</xsl:attribute>
    <xsl:attribute name="font-size">
      <xsl:value-of select="$body.font.master * 1.5"></xsl:value-of>
      <xsl:text>pt</xsl:text>
    </xsl:attribute>
  </xsl:attribute-set>
  
  <xsl:attribute-set name="section.title.level2.properties">
    <xsl:attribute name="space-before.minimum">2em</xsl:attribute>
    <xsl:attribute name="space-before.optimum">2.5em</xsl:attribute>
    <xsl:attribute name="font-size">
      <xsl:value-of select="$body.font.master * 1.25"></xsl:value-of>
      <xsl:text>pt</xsl:text>
    </xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="section.title.level3.properties">
    <xsl:attribute name="space-before.minimum">1.5em</xsl:attribute>
    <xsl:attribute name="space-before.optimum">2em</xsl:attribute>
    <xsl:attribute name="font-size">
      <xsl:value-of select="$body.font.master * 1.10"></xsl:value-of>
      <xsl:text>pt</xsl:text>
    </xsl:attribute>
  </xsl:attribute-set>
  
  
  <!--
  <xsl:attribute-set name="toc.line.properties">
    <xsl:variable name="current-level">
      <xsl:value-of select="count($toc-context::*) * 10"/>
    </xsl:variable>

    <xsl:attribute name="font-size"><xsl:value-of select="$current-level"/>pt</xsl:attribute>
    
    <xsl:attribute name="space-after">1cm</xsl:attribute>
  
  </xsl:attribute-set>

  <xsl:template name="toc.line">
    <xsl:param name="toc-context" select="NOTANODE"/>

    <xsl:variable name="id">
      <xsl:call-template name="object.id"/>
    </xsl:variable>

    <xsl:variable name="label">
      <xsl:apply-templates select="." mode="label.markup"/>
    </xsl:variable>

    <fo:block xsl:use-attribute-sets="toc.line.properties">
      <fo:inline keep-with-next.within-line="always">
        <fo:basic-link internal-destination="{$id}">
          <xsl:if test="$label != ''">
            <xsl:copy-of select="$label"/>
            <xsl:value-of select="$autotoc.label.separator"/>
          </xsl:if>
          <xsl:apply-templates select="." mode="titleabbrev.markup"/>
        </fo:basic-link>
      </fo:inline>
      <fo:inline keep-together.within-line="always">
        <xsl:text> </xsl:text>
        <fo:leader leader-pattern="dots"
                 leader-pattern-width="3pt"
                 leader-alignment="reference-area"
                 keep-with-next.within-line="always"/>
        <xsl:text> </xsl:text>
        <fo:basic-link internal-destination="{$id}">
          <fo:page-number-citation ref-id="{$id}"/>
        </fo:basic-link>
      </fo:inline>
    </fo:block>
  </xsl:template>
  -->
</xsl:stylesheet>
