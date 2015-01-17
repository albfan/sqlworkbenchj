<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!--
  Cleanup oracle specific code in order to apply it for a PostgreSQL database.
  
  Specifically following issues are handled:
  - convert "sysdate" to "current_date" for default values
  - replace "nvl" to "coalesce" in column definitions
  
  Example Usage:
    /* export Oracle Schema */
	WbSchemaReport -file=oracle_schema.xml
				   -types='TABLE';
	
	/* call this XSLT to cleanup Oracle specific code */
	WbXslt -inputfile=oracle_schema.xml
	       -stylesheet=wbreport2pg_cleanupora.xslt
	       -xsltOutput=oracle_schema_cleaned.xml;

	/* convert Oracle schema definition to PostgreSQL schema definition */
	WbXslt -inputfile=oracle_schema_cleaned.xml
	       -stylesheet=wbreport2pg.xslt
	       -xsltOutput=pg_schema.sql
	       -xsltParameters="useJdbcTypes=true";
       
  Author: Franz Mayer
-->
	<xsl:output method="xml" indent="yes" />
  
	<xsl:variable name="quote"><xsl:text>"</xsl:text></xsl:variable>
	<xsl:variable name="newline"><xsl:text>&#10;</xsl:text></xsl:variable>
	<xsl:variable name="lower_letters">abcdefghijklmnopqrstuvwxyz</xsl:variable>
	<xsl:variable name="upper_letters">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>

	<xsl:template match="/">
		<xsl:value-of select="$newline" />
		<xsl:apply-templates />
	</xsl:template>

	<xsl:template match="@* | node()">
		<xsl:copy>
			<xsl:apply-templates select="@* | node()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template match="default-value">
		<xsl:choose>
			<xsl:when test="translate(text(), $upper_letters, $lower_letters) = 'sysdate'">
				<default-value>
					<xsl:text>current_date</xsl:text>
				</default-value>
			</xsl:when>
			<xsl:otherwise>
				<xsl:copy-of select="." />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="column">
		<xsl:variable name="col_name" select="translate(@name, $upper_letters, $lower_letters)" />
		<xsl:choose>
			<xsl:when test="contains($col_name, 'nvl(')">
				<xsl:element name="column">
					<xsl:attribute name="name">
						<xsl:call-template name="_replace_text">
						<xsl:with-param name="text" select="$col_name" />
						<xsl:with-param name="replace" select="'nvl('" />
						<xsl:with-param name="by" select="'coalesce('" />
					</xsl:call-template>
					</xsl:attribute>
				</xsl:element>
			</xsl:when>
			<xsl:otherwise>
				<xsl:copy-of select="." />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- just copied from wbreport2pg.xslt -->
	<xsl:template name="_replace_text">
		<xsl:param name="text" />
		<xsl:param name="replace" />
		<xsl:param name="by" />
		<xsl:choose>
			<xsl:when test="contains($text, $replace)">
				<xsl:value-of select="substring-before($text, $replace)" />
				<xsl:copy-of select="$by" />
				<xsl:call-template name="_replace_text">
					<xsl:with-param name="text" select="substring-after($text, $replace)" />
					<xsl:with-param name="replace" select="$replace" />
					<xsl:with-param name="by" select="$by" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$text" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
