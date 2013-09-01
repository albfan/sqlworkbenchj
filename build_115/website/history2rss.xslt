<?xml version="1.0" encoding="iso-8859-1"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:include href="../scripts/get_builds_from_history.xslt"/>

<xsl:output encoding="iso-8859-1"
          method="html"
          omit-xml-declaration="yes"
          indent="yes"
          standalone="yes"
          />

<xsl:template match="history">

<xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
<p>Changes from build <xsl:value-of select="$prev-build"/> to build <xsl:value-of select="$current-build"/></p>

<xsl:if test="$active-build = -1">

  <xsl:call-template name="process-entries">
		<xsl:with-param name="entries" select="release[1]/entry[@dev-build=$last-dev-build-nr]"/>
	</xsl:call-template>

</xsl:if>

<xsl:if test="$active-build != -1">

  <xsl:call-template name="process-entries">
		<xsl:with-param name="entries" select="release[1]/entry"/>
	</xsl:call-template>

</xsl:if>

<xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>

</xsl:template>

<xsl:template name="process-entries">
	<xsl:param name="entries"/>

 <xsl:if test="count($entries[@type='enh']) &gt; 0">
		<p>Enhancements</p>

		<ul>
		<xsl:for-each select="$entries[@type='enh']">
			<li>
				<xsl:value-of select="normalize-space(.)"/>
			</li>
		</xsl:for-each>
		</ul>
	</xsl:if>

 <xsl:if test="count($entries[@type='fix']) &gt; 0">
		<p>Bug Fixes</p>

		<ul>
		<xsl:for-each select="$entries[@type='fix']">
			<li>
				<xsl:value-of select="normalize-space(.)"/>
			</li>
		</xsl:for-each>
		</ul>
	</xsl:if>

</xsl:template>

</xsl:stylesheet>