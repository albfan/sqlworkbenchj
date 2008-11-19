<?xml version="1.0" encoding="iso-8859-1"?>
<!DOCTYPE xslt [
    <!ENTITY space "&#32;">
    <!ENTITY nbsp "&#160;">
    <!ENTITY raquo "&#187;">
    <!ENTITY copy "&#169;">
    <!ENTITY reg "&#174;">
]>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:redirect="http://xml.apache.org/xalan/redirect"
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
	<xsl:param name="filedir"/>
	<xsl:param name="buildNumber"/>
	<xsl:param name="buildDate"/>
	<xsl:param name="devBuildDate"/>
	<xsl:param name="devBuildNumber"/>
	<xsl:param name="currentDate"/>
	<xsl:param name="includeDev" select="'0'"/>
	<xsl:variable name="fdir">
		<xsl:value-of select="concat($filedir, '\')"/>
	</xsl:variable>


<!-- 
  Generate the table of content which will be displayed for each
  page in the left hand navigation
-->
<!-- <xsl:variable name="toc"> -->
	<xsl:template name="make-toc">
		<xsl:param name="currentPage"/>

		<div id="navigation">

			<ul class="toc">
				<xsl:for-each select="/site/page[@toc='main']">
					<xsl:call-template name="toc-entry">
						<xsl:with-param name="currentPage" select="$currentPage"/>
					</xsl:call-template>
				</xsl:for-each>
				<li class="toclist">
					<a class="list" href="manual/workbench-manual.html">Manual</a>
				</li>
			</ul>
			
			<ul class="toc" id="google">
				<li class="toclist">
					<a class="list" href="http://groups.google.com/group/sql-workbench">Forum@Google
						<img src="images/outlink.gif" title="out" border="0"/>
					</a>
				</li>
			</ul>

			<ul class="toc" id="smalltext">
				<li class="toclist">
					<a class="list" href="wb_news.xml">RSS Feed</a>
				</li>
				
				<xsl:for-each select="/site/page[@toc='small']">
					<xsl:call-template name="toc-entry"/>
				</xsl:for-each>
			</ul>

			<div id="nblink">
				<a target="_blank" href="http://www.netbeans.org">
					<img border="0" style="margin-top:50px;" src="created-with-nb-2.gif"/>
				</a>
			</div>
		</div>

	</xsl:template>
<!-- </xsl:variable> -->

	<xsl:template name="toc-entry">
		<xsl:param name="currentPage"/>
		
		<xsl:variable name="pageName" select="@name"/>
		<xsl:variable name="pageTitle" select="@title"/>
		<xsl:variable name="notoc" select="@notoc"/>

		<xsl:variable name="ref">
			<xsl:if test="@name">
				<xsl:value-of select="concat(@name,'.html')"/>
			</xsl:if>
			<xsl:if test="@link">
				<xsl:value-of select="@link"/>
			</xsl:if>
		</xsl:variable>

		<li class="toclist">
			<xsl:if test="$currentPage = $pageName">
				<xsl:attribute name="id">
					<xsl:value-of select="'current'"/>
				</xsl:attribute>
			</xsl:if>

			<A class="list" href="{$ref}">
				<xsl:if test="$currentPage = $pageName">
					<xsl:attribute name="id">
						<xsl:value-of select="'active'"/>
					</xsl:attribute>
				</xsl:if>
				<xsl:value-of select="$pageTitle"/>
			</A>
		</li>
	</xsl:template>

<!-- entry point template to select the whole site document -->
	<xsl:template match="site">

		<xsl:for-each select="page">
			<xsl:if test="@name">

				<xsl:variable name="pageName" select="@name"/>
				<xsl:variable name="pageTitle" select="@title"/>
    
				<xsl:variable name="fname">
					<xsl:value-of select="concat($pageName,'.html')"/>
				</xsl:variable>

				<xsl:variable name="filename">
					<xsl:value-of select="concat($fdir, $fname)"/>
				</xsl:variable>

				<redirect:write file="{$filename}">
					<xsl:call-template name="main">
						<xsl:with-param name="pageTitle" select="$pageTitle"/>
						<xsl:with-param name="pageName" select="@name"/>
					</xsl:call-template>
				</redirect:write>

			</xsl:if>
		</xsl:for-each>

	</xsl:template>

	<xsl:template name="main">
		<xsl:param name="pageName"/>
		<xsl:param name="pageTitle"/>
		<xsl:param name="imageName"/>
		<xsl:param name="imageTitle"/>
		<xsl:param name="subTitle"/>
		
		<html>
			<head>
				<title>
					<xsl:value-of select="$siteName"/>&nbsp;-&nbsp;
					<xsl:value-of select="@title"/>
				</title>
				<meta http-equiv="Pragma" CONTENT="no-cache"/>
				<meta http-equiv="Expires" content="-1"/>
				<link rel="SHORTCUT ICON" href="favicon.ico"/>
				<meta name="description" content="A free DBMS-independent SQL query tool and front-end"/>
				<meta name="keywords" lang="en" content="sql,query,tool,analyzer,jdbc,database,isql,viewer,frontend,java,dbms,oracle,postgres,h2database,h2,firebirdsql,hsql,hsqldb,sqlserver,sqlplus,replacement,import,export,unload,convert,insert,blob,clob,xml,etl,migrate,compare,diff,structure,table"/>
				<meta name="date">
					<xsl:attribute name="content">
						<xsl:value-of select="$currentDate"/>
					</xsl:attribute>
				</meta>
				<meta name="robots" content="follow"/>
				<link href="wb.css" rel="stylesheet" type="text/css"></link>
			</head>
			<body>
		
				<a target="new" href="http://www.mgm-tp.com/home/index.html" title="mgm technology partners GmbH">
					<div id="mgm"></div>
				</a>
				<div id="top"></div>
				<a href="index.html" title="home">
					<div id="head"></div>
				</a>
		
				<div id="left">
					<xsl:call-template name="make-toc">
						<xsl:with-param name="currentPage" select="$pageName"/>
					</xsl:call-template>

				</div>
				<div id="main">
					<xsl:if test="$imageName">
						<img src="{$imageName}"/>
					</xsl:if>
					<xsl:if test="not($imageName)">
						<div class="content">
							<xsl:apply-templates select="content"/>
						</div>
					</xsl:if>
				</div>
			</body>
		</html>
	</xsl:template>

	<xsl:template match="@*">
		<xsl:copy-of select="."/>
	</xsl:template>

	<xsl:template match="source-link">
		<a href="WorkbenchSrc-Build{$buildNumber}.zip">Source code</a>
	</xsl:template>

	<xsl:template match="zip-link">
		<a href="Workbench-Build{$buildNumber}.zip">Download stable release</a>
	</xsl:template>

	<xsl:template match="mac-link">
		<a href="Workbench-Build{$buildNumber}-Mac.tgz">Download for Mac</a>
	</xsl:template>

	<xsl:template match="build-number">
		<xsl:value-of select="$buildNumber"/>
	</xsl:template>

	<xsl:template match="build-date">
		<xsl:value-of select="$buildDate"/>
	</xsl:template>

	<xsl:template match="dev-build">
		<xsl:if test="$includeDev = 1">
			<h3 style="margin-top:20px">Development build</h3>
			<p>A development build is generated while testing and implementing new features for the next stable build.
            In general these builds are pretty stable and I am using them on a daily basis myself. 
				<br/>
				<br/>
            Bugfixes will show up in these builds first.
				<a href="dev-history.txt" target="_blank">Change history</a>
			</p>
			<ul>
				<li>
					<a href="Workbench-Build{$devBuildNumber}.zip">Download development build</a> (
					<xsl:value-of select="$devBuildNumber"/>,&nbsp;
					<xsl:value-of select="$devBuildDate"/>)
				</li>
				<li>
					<a href="WorkbenchSrc-Build{$devBuildNumber}.zip">Source code</a>
				</li>
			</ul>
		</xsl:if>
	</xsl:template>
    
	<xsl:template match="dev-build-info">
		<xsl:if test="$includeDev = 1">
			<br/>Current development build:
			<span style="font-weight:bold">
				<a href="downloads.html">
					<xsl:value-of select="$devBuildNumber"/> (
					<xsl:value-of select="$devBuildDate"/>)
				</a>
			</span>
			<br/>
		</xsl:if>
	</xsl:template>

	<xsl:template match="image-link">
		<xsl:variable name="imageName" select="@name"/>
		<xsl:variable name="imageTitle" select="@title"/>
		<xsl:variable name="fname" select="concat(translate(@name,'.','_'),'.html')"/>
		<xsl:variable name="imgFile" select="concat($fdir, $fname)"/>
		<a href="{$fname}">
			<xsl:value-of select="."/>
		</a>
		<redirect:write file="{$imgFile}">
			<xsl:call-template name="main">
				<xsl:with-param name="imageName" select="$imageName"/>
				<xsl:with-param name="pageTitle" select="'SQL Workbench/J'"/>
				<xsl:with-param name="subTitle" select="$imageTitle"/>
				<xsl:with-param name="imageTitle" select="$imageTitle"/>
			</xsl:call-template>
		</redirect:write>
	</xsl:template>

	<xsl:template match="mail-to">
		<xsl:text disable-output-escaping="yes"><![CDATA[<a href=mailto:&#115;&#117;&#112;&#112;&#111;&#114;&#116;&#64;&#115;&#113;&#108;&#45;&#119;&#111;&#114;&#107;&#98;&#101;&#110;&#99;&#104;&#46;&#110;&#101;&#116;>&#115;&#117;&#112;&#112;&#111;&#114;&#116;&#64;&#115;&#113;&#108;&#45;&#119;&#111;&#114;&#107;&#98;&#101;&#110;&#99;&#104;&#46;&#110;&#101;&#116;</a>]]>
		</xsl:text>
	</xsl:template>
    
<!-- <xsl:template match="content"><xsl:value-of select="."/></xsl:template> -->
	<xsl:template match="img">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="ul">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="ol">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="li">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="p">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="b">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="i">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="br">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="span">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="a">
		<xsl:variable name="ref" select="@href"/>
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates/>
		</xsl:copy>
		<xsl:if test="substring($ref,1,4) = 'http'">
			<img src="images/outlink.gif" title="out" border="0"/>
		</xsl:if>
	</xsl:template>
	<xsl:template match="tt">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="h1">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="h2">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="h3">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="div">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="span">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>