<?xml version="1.0" encoding="iso-8859-1"?>
<!DOCTYPE xslt [
    <!ENTITY space "&#32;">
    <!ENTITY nbsp "&#160;">
    <!ENTITY raquo "&#187;">
    <!ENTITY copy "&#169;">
    <!ENTITY reg "&#174;">
]>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:redirect="org.apache.xalan.xslt.extensions.Redirect"
                extension-element-prefixes="redirect"
                >

<xsl:output encoding="iso-8859-1"
          method="html"
          omit-xml-declaration="yes"
          indent="no"
          standalone="yes"
          doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
          doctype-system="http://www.w3.org/TR/html4/loose.dtd"
          />


<xsl:variable name="siteName" select="/site/@name"/>
<xsl:param name="buildNumber"/>
<xsl:param name="buildDate"/>
<xsl:param name="devBuildDate"/>
<xsl:param name="devBuildNumber"/>
<xsl:param name="currentDate"/>
<xsl:param name="includeDev" select="'0'"/>

<!-- Generate the table of content which will be displayed for each
  page in the left hand navigation
-->
<xsl:variable name="toc">
  <xsl:for-each select="/site/page">
    <xsl:variable name="pageTitle" select="@title"/>
        <xsl:variable name="notoc" select="@notoc"/>

        <xsl:if test="not($notoc = 'true')">

        <!-- Generate a link inside the "regular" website i.e. inside the source XML -->
        <xsl:if test="@name">
            <xsl:variable name="pageName" select="@name"/>
            <xsl:variable name="fileName">
              <xsl:value-of select="concat($pageName,'.html')"/>
            </xsl:variable>
            <A href="{$fileName}"><xsl:value-of select="$pageTitle"/></A><br/>
            </xsl:if>

            <!-- A link that should be included in the toc, but points to a different "site" -->
            <xsl:if test="@link">
              <xsl:variable name="linkTarget" select="@link"/>
              <a target="_blank" href="{$linkTarget}"><xsl:value-of select="$pageTitle"/></a><br/>
            </xsl:if>

        </xsl:if>
  </xsl:for-each>
</xsl:variable>

<!-- entry point template to select the whole site document -->
<xsl:template match="site">

    <xsl:for-each select="page">
        <xsl:if test="@name">

            <xsl:variable name="pageName" select="@name"/>
            <xsl:variable name="pageTitle" select="@title"/>
    
            <xsl:variable name="filename">
                <xsl:value-of select="concat($pageName,'.html')"/>
            </xsl:variable>
    
            <redirect:write select="$filename">
                <xsl:call-template name="main">
                  <xsl:with-param name="pageTitle" select="$pageTitle"/>
                </xsl:call-template>
            </redirect:write>

        </xsl:if>
    </xsl:for-each>

</xsl:template>

<xsl:template name="main">

  <xsl:param name="pageTitle"/>
  <xsl:param name="imageName"/>
  <xsl:param name="imageTitle"/>
  <xsl:param name="subTitle"/>
    
    <html>
    <head>
        <title><xsl:value-of select="$siteName"/>&nbsp;-&nbsp;<xsl:value-of select="@title"/>
          <xsl:if test="$imageTitle">
            <xsl:value-of select="$imageTitle"/>
          </xsl:if>
        </title>
		<meta http-equiv="Pragma" CONTENT="no-cache"/>
		<meta http-equiv="Expires" content="-1"/>
		<meta name="description" content="A free DBMS-independent SQL query tool and front-end"/>
		<meta name="keywords" lang="en" content="sql,query,tool,analyzer,jdbc,database,isql,viewer,frontend,java,dbms,oracle,postgres,firebirdsql,hsql,hsqldb,sqlserver,sqlplus,replacement,import,export,convert,insert,blob,xml,etl,migrate,compare,diff"/>
	    <meta name="date">
	    	<xsl:attribute name="content"><xsl:value-of select="$currentDate"/></xsl:attribute>
	    </meta>
		<meta name="robots" content="follow"/>
		<link href="wb.css" rel="stylesheet" type="text/css"></link>
    </head>
    <body>
    <table style="margin-right:10px;margin-left:5px;" border="0" cellpadding="0" cellspacing="0">
        <tr>
        	<!--
            <td align="left" valign="top">
                <a target="_blank" href="http://www.nosoftwarepatents.com"><img src="http://www.nosoftwarepatents.com/banners/90x40_1.jpg" alt="No software patents"/></a>
            </td>
            -->
            <td align="center" width="95%">
              <xsl:if test="title">
                <p class="pageTitle"><xsl:value-of select="title"/></p>
              </xsl:if>
              <xsl:if test="$pageTitle and not(title)">
                <p class="pageTitle"><xsl:value-of select="$pageTitle"/></p>
              </xsl:if>
              <xsl:if test="subtitle">
                <p class="subTitle"><xsl:value-of select="subtitle"/></p>
              </xsl:if>
              <xsl:if test="$subTitle">
                <p class="subTitle"><xsl:value-of select="$subTitle"/></p>
              </xsl:if>
              <xsl:if test="not(subtitle) and not($subTitle)">
                <p class="subTitle">&nbsp;</p>
              </xsl:if>
            </td>
            <td align="right" valign="top">
                <a target="_blank" href="http://www.netbeans.org"><img src="created-with-nb-2.gif" alt="Created with NetBeans"/></a>
            </td>
        </tr>
    </table>
    <table style="margin:0px" border="0" cellpadding="0" cellspacing="0">
        <tr>
            <td style="vertical-align:top">
                <table class="tocTable" border="0" cellpadding="0" cellspacing="0" margin="0">
                    <tr><td class="toc" style="background-color:#E0E0E0"><b>Content</b></td></tr>
                    <tr><td class="toc">
                        <xsl:copy-of select="$toc"/>
                    </td></tr>
                </table>
            </td>
            <xsl:if test="$imageName">
                <td class="image">
                    <img src="{$imageName}"/>
                </td>
            </xsl:if>
            <xsl:if test="not(imageName)">
                  <td class="content">
                    <xsl:apply-templates select="content"/>
                </td>
            </xsl:if>
        </tr>
    </table>

    </body>
    </html>
</xsl:template>

<xsl:template match="table">
   <table bgcolor="#CCCCCC" cellspacing="1" cellpadding="4" border="0" width="100%">
        <xsl:for-each select="tr">
            <tr>
                <xsl:if test="th">
                    <xsl:attribute name="class">theme</xsl:attribute>
                </xsl:if>
                <xsl:if test="td">
                    <xsl:if test="position() mod 2 = 0">
                        <xsl:attribute name="bgcolor">#FFFFFF</xsl:attribute>
                    </xsl:if>
                    <xsl:if test="position() mod 2 = 1">
                        <xsl:attribute name="bgcolor">#E7E7E7</xsl:attribute>
                    </xsl:if>
                </xsl:if>
                <xsl:for-each select="td">
                    <td><xsl:apply-templates/></td>
                </xsl:for-each>
                <xsl:for-each select="th">
                    <th width="100" align="left" nowrap="" class="small">
                    <span class="themebody"><xsl:apply-templates/></span>
                    </th>
                </xsl:for-each>
            </tr>
        </xsl:for-each>
    </table>
</xsl:template>

<xsl:template match="@*">
    <xsl:copy-of select="."/>
</xsl:template>

<xsl:template match="source-link">
<a href="WorkbenchSrc-Build{$buildNumber}.zip">Source code</a>
</xsl:template>

<xsl:template match="zip-link">
<!-- <a href="workbench.zip">ZIP File</a>-->
<a href="Workbench-Build{$buildNumber}.zip">Download stable release</a>

</xsl:template>

<!--
<xsl:template match="tar-link">
<a href="Workbench-Build{$buildNumber}.tar.gz">TAR File</a>
</xsl:template>
-->

<xsl:template match="build-number">
  <xsl:value-of select="$buildNumber"/>
</xsl:template>

<xsl:template match="build-date">
  <xsl:value-of select="$buildDate"/>
</xsl:template>

<xsl:template match="dev-build">
  <xsl:if test="$includeDev = 1">
        <h3 style="margin-top:20px">Development build</h3>
        <p>A development build is generated during development between stable builds while testing and implementing new features.
            In general these builds are pretty stable. But it can happen that some things are broken.
            Basically I'm using these builds myself on a daily basis, and it should be pretty safe to use them.<br/><br/>
            Bugfixes will show up in these builds first. <a href="dev-history.txt" target="_blank">Change history</a>
        </p>
        <p>The development build is not a full release. It contains only the Workbench.jar binary. The updated
        help is contained in that file, but no PDF or HTML help files are available. The source code
        for the development build is also available.
        </p>
      <ul>
        <li><a href="Workbench-Build{$devBuildNumber}.zip">Download development build</a> (<xsl:value-of select="$devBuildNumber"/>,&nbsp;<xsl:value-of select="$devBuildDate"/>)</li>
        <li><a href="WorkbenchSrc-Build{$devBuildNumber}.zip">Source code</a></li>
       </ul>
    </xsl:if>
</xsl:template>
    
<xsl:template match="dev-build-info">
  <xsl:if test="$includeDev = 1">
        <br/>Current development build: <span style="font-weight:bold"><a href="downloads.html"><xsl:value-of select="$devBuildNumber"/> (<xsl:value-of select="$devBuildDate"/>)</a></span><br/>
    </xsl:if>
</xsl:template>

<xsl:template match="image-link">
  <xsl:variable name="imageName" select="@name"/>
  <xsl:variable name="imageTitle" select="@title"/>
  <xsl:variable name="imagePreviewFile" select="concat(translate(@name,'.','_'),'.html')"/>
  <a href="{$imagePreviewFile}"><xsl:value-of select="."/></a>
    <redirect:write select="$imagePreviewFile">
        <xsl:call-template name="main">
          <xsl:with-param name="imageName" select="$imageName"/>
          <xsl:with-param name="pageTitle" select="'SQL Workbench/J'"/>
          <xsl:with-param name="subTitle" select="$imageTitle"/>
          <xsl:with-param name="imageTitle" select="$imageTitle"/>
        </xsl:call-template>
    </redirect:write>
</xsl:template>

<xsl:template match="mail-to">
<xsl:text disable-output-escaping="yes"><![CDATA[<a href=mailto:&#115;&#117;&#112;&#112;&#111;&#114;&#116;&#64;&#115;&#113;&#108;&#45;&#119;&#111;&#114;&#107;&#98;&#101;&#110;&#99;&#104;&#46;&#110;&#101;&#116;>&#115;&#117;&#112;&#112;&#111;&#114;&#116;&#64;&#115;&#113;&#108;&#45;&#119;&#111;&#114;&#107;&#98;&#101;&#110;&#99;&#104;&#46;&#110;&#101;&#116;</a>]]></xsl:text>
</xsl:template>
    
<!-- <xsl:template match="content"><xsl:value-of select="."/></xsl:template> -->
<xsl:template match="img"><xsl:copy><xsl:apply-templates select="@*"/><xsl:apply-templates/></xsl:copy></xsl:template>
<xsl:template match="ul"><xsl:copy><xsl:apply-templates select="@*"/><xsl:apply-templates/></xsl:copy></xsl:template>
<xsl:template match="ol"><xsl:copy><xsl:apply-templates select="@*"/><xsl:apply-templates/></xsl:copy></xsl:template>
<xsl:template match="li"><xsl:copy><xsl:apply-templates select="@*"/><xsl:apply-templates/></xsl:copy></xsl:template>
<xsl:template match="p"><xsl:copy><xsl:apply-templates select="@*"/><xsl:apply-templates/></xsl:copy></xsl:template>
<xsl:template match="b"><xsl:copy><xsl:apply-templates select="@*"/><xsl:apply-templates/></xsl:copy></xsl:template>
<xsl:template match="i"><xsl:copy><xsl:apply-templates select="@*"/><xsl:apply-templates/></xsl:copy></xsl:template>
<xsl:template match="br"><xsl:copy><xsl:apply-templates select="@*"/><xsl:apply-templates/></xsl:copy></xsl:template>
<xsl:template match="span"><xsl:copy><xsl:apply-templates select="@*"/><xsl:apply-templates/></xsl:copy></xsl:template>
<xsl:template match="a"><xsl:copy><xsl:apply-templates select="@*"/><xsl:apply-templates/></xsl:copy></xsl:template>
<xsl:template match="tt"><xsl:copy><xsl:apply-templates select="@*"/><xsl:apply-templates/></xsl:copy></xsl:template>
<xsl:template match="h1"><xsl:copy><xsl:apply-templates select="@*"/><xsl:apply-templates/></xsl:copy></xsl:template>
<xsl:template match="h2"><xsl:copy><xsl:apply-templates select="@*"/><xsl:apply-templates/></xsl:copy></xsl:template>
<xsl:template match="h3"><xsl:copy><xsl:apply-templates select="@*"/><xsl:apply-templates/></xsl:copy></xsl:template>
<xsl:template match="div"><xsl:copy><xsl:apply-templates select="@*"/><xsl:apply-templates/></xsl:copy></xsl:template>
<xsl:template match="span"><xsl:copy><xsl:apply-templates select="@*"/><xsl:apply-templates/></xsl:copy></xsl:template>

</xsl:stylesheet>