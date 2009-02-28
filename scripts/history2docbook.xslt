<?xml version="1.0" encoding="iso-8859-1"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:output encoding="iso-8859-1"
          method="xml"
          omit-xml-declaration="yes"
          indent="yes"
          standalone="yes"/>

  <xsl:template match="history">

    <section id="history">
      <title>Change log</title>

			<xsl:variable name="prev-build" select="release[2]/@build"/>
			<xsl:variable name="current-build">
				<xsl:if test="release[1]/@build = -1">
					<xsl:value-of select="concat($prev-build,'.',release[1]/entry[1]/@dev-build)"/>
				</xsl:if>
				<xsl:if test="release[1]/@build != -1">
					<xsl:value-of select="release[1]/@build"/>
				</xsl:if>
			</xsl:variable>
			
			<para>
				Changes from build <xsl:value-of select="$prev-build"/> to build <xsl:value-of select="$current-build"/>
			</para>

			<xsl:if test="count(release[1]/entry[@type='enh']) &gt; 0">
				<bridgehead renderas='sect3'>Enhancements</bridgehead>

				<itemizedlist spacing="normal">
					<xsl:for-each select="release[1]/entry[@type='enh']">
							<listitem><xsl:copy-of select="description/text()"/></listitem>
					</xsl:for-each>
				</itemizedlist>
			</xsl:if>

			<xsl:if test="count(release[1]/entry[@type='fix']) &gt; 0">
				<bridgehead renderas='sect3'>Bug fixes</bridgehead>

				<itemizedlist spacing="normal">
					<xsl:for-each select="release[1]/entry[@type='fix']">
							<listitem><xsl:copy-of select="description/text()"/></listitem>
					</xsl:for-each>
				</itemizedlist>
			</xsl:if>

			<para>
				The full release history is available at the SQL Workbench/J <ulink url="http://www.sql-workbench.net/history.html">homepage</ulink>
			</para>
			
    </section>

  </xsl:template>

</xsl:stylesheet>