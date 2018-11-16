<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<xsl:stylesheet
    version="3.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:fn="http://www.w3.org/2005/xpath-functions"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:lines="https://mosher.mine.nu/xmlns/lines"
    xmlns:hier="https://mosher.mine.nu/xmlns/hier"
    xmlns:gedcom="https://mosher.mine.nu/xmlns/gedcom"
>
    <xsl:output method="xml" version="1.1" encoding="UTF-8"/>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="gedcom:node[./gedcom:node/@gedcom:tag='CONC' or ./gedcom:node/@gedcom:tag='CONT']">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>

            <xsl:element name="gedcom:value">
                <xsl:apply-templates select="gedcom:value/@*"/>
                <xsl:value-of select="gedcom:value/node()"/>
                <xsl:iterate select="gedcom:node[./@gedcom:tag='CONC' or ./@gedcom:tag='CONT']">
                    <xsl:if test="./@gedcom:tag='CONT'">
                        <xsl:value-of select="'&#x0A;'"/>
                    </xsl:if>
                    <xsl:value-of select="gedcom:value/node()"/>
                </xsl:iterate>
            </xsl:element>

            <xsl:apply-templates select="gedcom:node[not(./@gedcom:tag='CONC' or ./@gedcom:tag='CONT')]"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
