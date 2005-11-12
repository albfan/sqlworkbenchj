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
    <xsl:for-each select="/schema-diff/modify-table">
      
      <xsl:variable name="table" select="@name"/>
      
      <xsl:apply-templates select="add-column">
        <xsl:with-param name="table" select="$table"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="remove-column">
        <xsl:with-param name="table" select="$table"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="modify-column">
        <xsl:with-param name="table" select="$table"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="remove-primary-key">
        <xsl:with-param name="table" select="$table"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="add-primary-key">
        <xsl:with-param name="table" select="$table"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="add-index">
        <xsl:with-param name="table" select="$table"/>
      </xsl:apply-templates>

  </xsl:for-each>

  <xsl:for-each select="/schema-diff/create-view">
	  <xsl:apply-templates select="view-def"/>
  </xsl:for-each>
  <xsl:for-each select="/schema-diff/update-view">
	  <xsl:apply-templates select="view-def"/>
  </xsl:for-each>
  
</xsl:template>

<xsl:template match="rename">
<!-- Ignore table rename -->
</xsl:template>

<xsl:template match="add-index">
</xsl:template>

<xsl:template match="drop-index">
</xsl:template>

<xsl:template match="add-column">
<xsl:param name="table"/> 
<xsl:variable name="column" select="column-def/column-name"/>
<xsl:variable name="nullable" select="column-def/nullable"/>
ALTER TABLE <xsl:value-of select="$table"/> ADD <xsl:value-of select="$column"/><xsl:text> </xsl:text><xsl:value-of select="column-def/dbms-data-type"/><xsl:if test="$nullable = 'false'">  NOT NULL</xsl:if>;
</xsl:template>

<!-- Process the modify-column part -->
<xsl:template match="modify-column">
<xsl:param name="table"/> 
<xsl:variable name="column" select="@name"/>
<xsl:if test="string-length(dbms-data-type) &gt; 0">
ALTER TABLE <xsl:value-of select="$table"/> MODIFY <xsl:value-of select="$column"/><xsl:text> </xsl:text><xsl:value-of select="dbms-data-type"/>;
</xsl:if>
<xsl:if test="nullable = 'true'">
ALTER TABLE <xsl:value-of select="$table"/> MODIFY <xsl:value-of select="$column"/> NULL;
</xsl:if>
<xsl:if test="nullable = 'false'">
ALTER TABLE <xsl:value-of select="$table"/> MODIFY <xsl:value-of select="$column"/> NOT NULL;
</xsl:if>
<xsl:if test="string-length(default-value) &gt; 0">
ALTER TABLE <xsl:value-of select="$table"/> MODIFY <xsl:value-of select="$column"/> DEFAULT <xsl:value-of select="default-value"/>;  
</xsl:if>
<xsl:if test="string-length(comment) &gt; 0">
COMMENT ON COLUMN <xsl:value-of select="$table"/>.<xsl:value-of select="$column"/> IS '<xsl:value-of select="comment"/>';  
</xsl:if>
</xsl:template>

<!-- Add primary keys -->
<xsl:template match="add-primary-key">
<xsl:param name="table"/> 
ALTER TABLE <xsl:value-of select="$table"/> 
ADD CONSTRAINT <xsl:value-of select="@name"/><xsl:text> </xsl:text>
PRIMARY KEY (
  <xsl:for-each select="column-name">
    <xsl:copy-of select="."/><xsl:if test="position() &lt; last()"><xsl:text>, 
  </xsl:text></xsl:if>
  </xsl:for-each>
);  
</xsl:template>

<!-- Remove primary keys -->
<xsl:template match="remove-primary-key">
<xsl:param name="table"/> 
ALTER TABLE <xsl:value-of select="$table"/> DROP PRIMARY KEY;
</xsl:template>

<!-- re-create a view -->
<xsl:template match="view-def">
CREATE OR REPLACE VIEW <xsl:value-of select="view-name"/>
(
  <xsl:for-each select="column-def">
  	<xsl:sort select="dbms-position"/>
    <xsl:copy-of select="column-name"/><xsl:if test="position() &lt; last()"><xsl:text>, 
  </xsl:text></xsl:if>
  </xsl:for-each>
)
AS 
<xsl:copy-of select="view-source"/>
</xsl:template>

</xsl:stylesheet>
