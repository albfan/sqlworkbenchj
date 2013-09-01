<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:variable name="last-build" select="/history/release[2]/@build"/>
<xsl:variable name="active-build" select="/history/release[1]/@build"/>

<xsl:variable name="last-dev-build-nr">
	<xsl:for-each select="/history/release[1]/entry">
			<xsl:sort select="@dev-build" order="descending" data-type="number"/>
			<xsl:if test="position()=1">
					<xsl:value-of select="@dev-build" />
			</xsl:if>
	</xsl:for-each>
</xsl:variable>

<xsl:variable name="prev-dev-build-nr" select="$last-dev-build-nr - 1"/>

<xsl:variable name="current-build">
	<xsl:if test="$active-build = -1">
		<xsl:value-of select="concat($last-build,'.',$last-dev-build-nr)"/>
	</xsl:if>
	<xsl:if test="$active-build != -1">
		<xsl:value-of select="$active-build"/>
	</xsl:if>
</xsl:variable>

<xsl:variable name="prev-build">
	<xsl:if test="$active-build = -1">
		<xsl:value-of select="concat($last-build,'.',$prev-dev-build-nr)"/>
	</xsl:if>
	<xsl:if test="$active-build != -1">
		<xsl:value-of select="$last-build"/>
	</xsl:if>
</xsl:variable>

</xsl:stylesheet>