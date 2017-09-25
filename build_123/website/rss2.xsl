<?xml version="1.0"?>
<!--
  Title: RSS 2.0 XSL Template
  Author: Rich Manalang (http://manalang.com)
  Description: This sample XSLT will convert any valid RSS 2.0 feed to HTML.
-->
<xsl:stylesheet version="1.0"
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 xmlns:wfw="http://wellformedweb.org/CommentAPI/">
 	<xsl:output method="html"/>
	<xsl:template match="/">
		<style>
			<xsl:comment>
			.syndication-content-area {
			}
			.syndication-title {
				font-size: 1.1em;
				font-weight: bold;
			}
			.syndication-description {
				font-size: .9em;
				margin: 0 0 10px 0;
			}
			.syndication-list {
				font-size: .8em;
				margin:0 0 0 20px;
			}
			.syndication-list-item {
				margin: 0 0 5px 0;
			}
			.syndication-list-item a,
			.syndication-list-item a:link {
				color: blue;
			}
			.syndication-list-item a:active,
			.syndication-list-item a:hover {
				color: red;
			}
			.syndication-list-item a:visited {
				color: black;
				text-decoration: none;
			}
			.syndication-list-item-date {
				font-size: .8em;
			}
			.syndication-list-item-description {
				font-size: .9em;
			}
			</xsl:comment>
		</style>
		<xsl:apply-templates select="/rss/channel"/>
	</xsl:template>
	<xsl:template match="/rss/channel">
		<div class="syndication-content-area">
			<div class="syndication-title">
				<xsl:value-of select="title"/>
			</div>
			<div class="syndication-description">
				<xsl:value-of select="description"/>
			</div>
			<ul class="syndication-list">
				<xsl:apply-templates select="item"/>
			</ul>
		</div>
	</xsl:template>
	<xsl:template match="/rss/channel/item">
		<li class="syndication-list-item">
			<a href="{link}" title="{description}">
				<xsl:value-of select="title"/>
			</a>
			<span class="syndication-list-item-date">
		 		(<xsl:value-of select="pubDate"/>)
			</span>
			<div class="syndication-list-item-description">
				<xsl:value-of select="description" disable-output-escaping="yes" />
			</div>
		</li>
	</xsl:template>
</xsl:stylesheet>