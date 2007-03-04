<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet
     version="1.0" 
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>

<xsl:output 
  encoding="iso-8859-15" 
  method="text" 
  indent="no" 
  standalone="yes"  
  omit-xml-declaration="yes"
  doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
/>

<xsl:strip-space elements="*"/>

<xsl:template match="/">
	<xsl:for-each select="/schema-diff/add-table/table-def/column-def[primary-key='true']">
		<xsl:copy-of select="column-name"/>,
	</xsl:for-each>
</xsl:template>

</xsl:stylesheet>