<?xml version="1.0" encoding="iso-8859-1"?>

<!--
   Convert the history.xml file to a standalone HTML that is included in
   the distribution. The generated HTML will contain the change history
   for the last 10 releases. The full history is maintained on the
   homepage
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output encoding="iso-8859-1"
          method="html"
          omit-xml-declaration="yes"
          indent="no"
          standalone="yes"
          doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
          doctype-system="http://www.w3.org/TR/html4/loose.dtd"
          />

  <xsl:include href="get_builds_from_history.xslt"/>

  <xsl:template match="history">

    <html>
    <head>
      <title>SQL Workbench/J Change Log</title>
      <style>

      body {
        font-size:13px;
        margin-left:5%;
        margin-right:5%;
      }

      h1 {
        text-align:center;
        font-size:20px;
        color: #666633;
        font-family: sans-serif;
        margin-top:0px;
        margin-bottom:25px;
      }

      h2 {
        color: #666633;
        background-color:#F0F1E3;
        font-family: sans-serif;
        font-size:16px;
        margin-top:0px;
        text-align:left;
        padding:5px;
      }

      h3 {
        color: #666633;
        font-family: sans-serif;
        font-size: 12px;
        text-align:left;
        margin-top: 0;
        margin-bottom: 10px;
        width: 100%;
        border-bottom: 1px;
        border-bottom-color:#D9D8B2;
        border-bottom-style: solid;
      }

      li {
        padding-bottom:8px;
      }

      </style>
    </head>
    <body>
      <h1 style="font-size:18px">SQL Workbench/J Change Log</h1>

      <p>
        This document only shows the release history for the last 10 releases.<br/>
        The full release history is available at the SQL Workbench/J <a href="http://www.sql-workbench.net/history.html">homepage</a>
      </p>
      <xsl:for-each select="/history/release[position() &lt; 10]">
        <xsl:variable name="display-build">
          <xsl:if test="position() = 1">
            <xsl:value-of select="$current-build"/>
          </xsl:if>
          <xsl:if test="position() > 1">
            <xsl:value-of select="@build"/>
          </xsl:if>
        </xsl:variable>

        <h2 class="build-nr">Build <xsl:value-of select="$display-build"/> (<xsl:value-of select="@date"/>)</h2>

        <xsl:if test="count(entry[@type='enh']) &gt; 0">
          <h3 class="history-entry">Enhancements</h3>
          <ul>
          <xsl:for-each select="entry[@type='enh']">
            <xsl:sort select="@dev-build" order="descending" data-type="number"/>
            <li><xsl:copy-of select="normalize-space(text())"/></li>
          </xsl:for-each>
          </ul>
        </xsl:if>

        <xsl:if test="count(entry[@type='fix']) &gt; 0">
          <h3 class="history-entry">Bug fixes</h3>
          <ul>
          <xsl:for-each select="entry[@type='fix']">
            <xsl:sort select="@dev-build" order="descending" data-type="number"/>
            <li><xsl:copy-of select="normalize-space(text())"/></li>
          </xsl:for-each>
          </ul>
        </xsl:if>

      </xsl:for-each>
    </body>
    </html>

  </xsl:template>

</xsl:stylesheet>
