<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet
     version="1.0" 
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>

<xsl:output 
  encoding="iso-8859-15" 
  method="text" 
  indent="no" 
  standalone="no"	
  omit-xml-declaration="no"
  doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
/>

<xsl:variable name="newline"><xsl:text>&#10;</xsl:text></xsl:variable>
<xsl:variable name="tab"><xsl:text>&#x09;</xsl:text></xsl:variable>

<xsl:template match="/">
	<!-- Write the header row -->
	<xsl:for-each select="/wb-export/table-def/column-def">
		<xsl:value-of select="column-name"/>
		<xsl:if test="position() &lt; last()">
			<xsl:value-of select="$tab"/>
		</xsl:if>
	</xsl:for-each>
	<xsl:value-of select="$newline"/>
	
	<!-- Write the data rows -->
	<xsl:for-each select="/wb-export/data/row-data">
		<xsl:for-each select="column-data">
			<xsl:value-of select="."/>
			<xsl:if test="position() &lt; last()"><xsl:value-of select="$tab"/>
			</xsl:if>
		</xsl:for-each>
		<xsl:value-of select="$newline"/>
	</xsl:for-each>
</xsl:template>

</xsl:stylesheet>
