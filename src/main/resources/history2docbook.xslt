<?xml version="1.0" encoding="iso-8859-1"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:import href="get_builds_from_history.xslt"/>

<xsl:output encoding="iso-8859-1"
          method="xml"
          omit-xml-declaration="yes"
          indent="yes"
          standalone="yes"/>

  <xsl:template match="history">

    <section id="history">
      <title>Change log</title>
      
      <para>
        Changes from build <xsl:value-of select="$last-build"/> to build <xsl:value-of select="$current-build"/>
      </para>

      <xsl:if test="count(release[1]/entry[@type='enh']) &gt; 0">
        <bridgehead renderas='sect3'>Enhancements</bridgehead>

        <itemizedlist spacing="normal">
          <xsl:for-each select="release[1]/entry[@type='enh']">
            <xsl:sort select="@dev-build" order="descending" data-type="number"/>
            <listitem><xsl:copy-of select="text()"/></listitem>
          </xsl:for-each>
        </itemizedlist>
      </xsl:if>

      <xsl:if test="count(release[1]/entry[@type='fix']) &gt; 0">
        <bridgehead renderas='sect3'>Bug fixes</bridgehead>

        <itemizedlist spacing="normal">
          <xsl:for-each select="release[1]/entry[@type='fix']">
            <xsl:sort select="@dev-build" order="descending" data-type="number"/>
            <listitem><xsl:copy-of select="text()"/></listitem>
          </xsl:for-each>
        </itemizedlist>
      </xsl:if>

      <para>
        The full release history is available at the SQL Workbench/J <ulink url="http://www.sql-workbench.net/history.html">homepage</ulink>
      </para>
      
    </section>

  </xsl:template>

</xsl:stylesheet>