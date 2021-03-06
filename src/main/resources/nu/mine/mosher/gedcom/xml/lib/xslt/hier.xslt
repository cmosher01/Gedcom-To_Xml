<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!--
    Gedcom-To-Xml
    Converts GEDCOM file to XML format.

    Copyright © 2019–2020, by Christopher Alan Mosher, Shelton, Connecticut, USA, cmosher01@gmail.com .

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
-->
<xsl:stylesheet
    version="3.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:fn="http://www.w3.org/2005/xpath-functions"
    xmlns:lines="https://mosher.mine.nu/xmlns/lines"
    xmlns:hier="https://mosher.mine.nu/xmlns/hier"
>
    <xsl:output method="xml" version="1.1" encoding="UTF-8"/>

    <xsl:key
        name="child-by-parent"
        match="hier:node"
        use="fn:generate-id(preceding-sibling::*[xs:nonNegativeInteger(@hier:level) lt xs:nonNegativeInteger(current()/@hier:level)][1])"
    />

    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="hier:root">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="hier:node[xs:nonNegativeInteger(@hier:level) eq 0]"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="hier:node">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:element name="hier:value">
                <xsl:value-of select="."/>
            </xsl:element>
            <xsl:apply-templates select="fn:key('child-by-parent', fn:generate-id())"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
