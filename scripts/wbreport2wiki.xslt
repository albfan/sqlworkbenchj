<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:transform 
     version="1.0" 
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>
<xsl:output 
  encoding="iso-8859-15" 
  method="text" 
  indent="no" 
  omit-xml-declaration="yes"
  doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
/>

<xsl:strip-space elements="*"/>

<!-- <xsl:preserve-space elements="code"/> -->
<xsl:variable name="docTitle" select="'SQL Workbench/J - Schema Report'"/>

<xsl:template match="/">
    <xsl:call-template name="table-definitions"/>
</xsl:template>

<xsl:template name="table-definitions">
  <xsl:variable name="newline"><xsl:text>&#10;</xsl:text></xsl:variable>
  
  <xsl:for-each select="/schema-report/table-def">
    <xsl:sort select="table-name"/>
    
    <xsl:variable name="table" select="table-name"/>
    
    <xsl:text>h5. </xsl:text><xsl:value-of select="$table"/>
    <xsl:value-of select="$newline"/>
    
    <xsl:value-of select="table-comment"/>
    <xsl:value-of select="$newline"/>
    <xsl:text>||Column||Type||PK||Nullable||Comment||</xsl:text>
    <xsl:value-of select="$newline"/>
      
    <xsl:for-each select="column-def">
      <xsl:sort select="dbms-position"/>
      <xsl:text>|</xsl:text>
      <xsl:value-of select="column-name"/>
      <xsl:text>|</xsl:text>
      <xsl:value-of select="dbms-data-type"/>
      <xsl:text>|</xsl:text>
      <xsl:choose>
        <xsl:when test="primary-key='true'">
          <xsl:text>PK</xsl:text>
        </xsl:when>
        <xsl:otherwise>
            <xsl:text> </xsl:text> <!-- Create a space between the pipes, otherwise Wiki won't recognize the table column -->
        </xsl:otherwise>
      </xsl:choose>
      <xsl:text>|</xsl:text>
      <xsl:choose>
        <xsl:when test="nullable='true'">
          <xsl:text>NULL</xsl:text>
        </xsl:when>
        <xsl:otherwise>
            <xsl:text>NOT NULL</xsl:text> <!-- Create a space between the pipes, otherwise Wiki won't recognize the table column -->
        </xsl:otherwise>
      </xsl:choose>
      <xsl:text>|</xsl:text>
      <xsl:value-of select="comment"/>
      <xsl:text>|</xsl:text>
      <xsl:value-of select="$newline"/>
      
    </xsl:for-each>
    
    <xsl:value-of select="$newline"/>
    
  </xsl:for-each>

</xsl:template>

</xsl:transform>  
