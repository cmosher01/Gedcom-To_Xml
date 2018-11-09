/*

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.

    Copyright © 2018, by Christopher Alan Mosher, Shelton, Connecticut, USA.

    <https://mosher.mine.nu/>

 */

package nu.mine.mosher.gedcom;

import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.exception.InvalidLevel;
import nu.mine.mosher.mopper.ArgParser;
import org.owasp.encoder.Encode;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static nu.mine.mosher.logging.Jul.log;

// Created by Christopher Alan Mosher on 2018-11-03.

public class GedcomToXml implements Gedcom.Processor {
    public static final String DTD_FPI = "+//IDN mosher.mine.nu//DTD gedcom nodes 1.0//EN";
    public static final String DTD_URL = "https://mosher.mine.nu/dtd/gedcom/nodes.dtd";
    private final GedcomToXmlOptions options;
    private final BufferedWriter out;
    private final BufferedReader in;
    private int p = -1;

    public static void main(final String... args) throws InvalidLevel, IOException {
        log();

        final GedcomToXmlOptions options = new ArgParser<>(new GedcomToXmlOptions()).parse(args).verify();
        final GedcomToXml gedcomToXml = new GedcomToXml(options);
        if (options.nodes) {
            gedcomToXml.nodesOnlyMode();
        } else {
            new Gedcom(options, gedcomToXml).main();
        }
        System.out.flush();
        System.err.flush();
    }

    private static class NodeLine {
        public final int level;
        public final String value;
        public NodeLine(final int level, final String value) {
            this.level = level;
            this.value = value;
        }
    }

    private void nodesOnlyMode() throws IOException {
        try {
            outHead();
            outDoctype();
            this.out.write("<gedcom:nodes xmlns:gedcom=\"https://mosher.mine.nu/xmlns/gedcom\">\n");

            final LevelLineReader r = new LevelLineReader(this.in);
            int lineNumber = 1;
            for (final LevelLineReader.LevelLine line : r) {
                processLine(line, lineNumber++);
            }
            finish();

            this.out.write("</gedcom:nodes>\n");
            this.out.flush();
            this.in.close();
        } finally {
            try {
                this.out.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private void outDoctype() throws IOException {
        this.out.write(String.format("<!DOCTYPE gedcom:nodes PUBLIC \"%s\" \"%s\">\n", DTD_FPI, DTD_URL));
    }

    private void finish() throws IOException {
        final int pop = this.p + 1;
        for (int i = 0; i < pop; ++i) {
            log().warning("Adding closing gedcom:node at the end.");
            this.out.write("</gedcom:node>\n");
        }
    }

    private void processLine(final LevelLineReader.LevelLine line, int lineNumber) throws IOException {
        final int pop = this.p + 1 - line.level;
        if (pop < 0) {
            log().warning(String.format("Illegal level (%d) found at line %d. Generating nodes (without tags) to compensate.", line.level, lineNumber));
            for (int i = 0; i < this.p+1; ++i) {
                this.out.write("</gedcom:node>\n");
            }
            for (int i = 0; i < line.level; ++i) {
                this.out.write("<gedcom:node gedcom:line-number=\""+lineNumber+"\">\n");
                this.out.write("<gedcom:value/>\n");
            }
        } else {
            for (int i = 0; i < pop; ++i) {
                this.out.write("</gedcom:node>\n");
            }
        }
        this.p = line.level;
        this.out.write("<gedcom:node gedcom:line-number=\""+lineNumber+"\">\n");
        this.out.write("<gedcom:value>");
        this.out.write(cdata(line.value));
        this.out.write("</gedcom:value>\n");
    }

    private GedcomToXml(final GedcomToXmlOptions options) {
        this.options = options;
        final Charset csGedcom = (Objects.nonNull(options.encoding)) ? options.encoding : StandardCharsets.UTF_8;
        this.out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FileDescriptor.out), StandardCharsets.UTF_8));
        if (this.options.nodes) {
            this.in = new BufferedReader(new InputStreamReader(new FileInputStream(FileDescriptor.in), csGedcom));
        } else {
            this.in = null;
        }
    }

    @Override
    public boolean process(final GedcomTree tree) {
        try {
            new GedcomConcatenator(tree).concatenate();
            outHead();
            convertFrom(tree.getRoot(), 0);
            this.out.flush();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                this.out.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void outHead() throws IOException {
        if (!this.options.fragment) {
            this.out.write("<?xml version=\"1.1\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        }
    }

    private void convertFrom(final TreeNode<GedcomLine> node, final int indent) throws IOException {
        outNode(node, indent);
        for (final TreeNode<GedcomLine> child : node) {
            convertFrom(child, indent + 1);
        }
        closeNode(node, indent);
    }

    private void outNode(TreeNode<GedcomLine> node, int indent) throws IOException {
        final GedcomLine line = node.getObject();
        indent(indent);
        this.out.write("<" + tag(line));
        if (Objects.isNull(line)) {
            this.out.write(" xmlns:gedcom=\"https://mosher.mine.nu/xmlns/gedcom\"");
            this.out.write(" xmlns:xlink=\"http://www.w3.org/1999/xlink\"");
        } else {
            if (line.hasID()) {
                this.out.write(" xml:id=\"" + enc(line.getID()) + "\"");
            }
            if (line.isPointer()) {
                this.out.write(" xlink:href=\"#" + enc(line.getPointer()) + "\"");
            }
            final String value = line.getValue();
            if (!value.isEmpty() && asAttr(value)) {
                this.out.write(" gedcom:value=\"" + enc(value) + "\"");
            }
        }
        this.out.write(">");
        nl();
        if (Objects.nonNull(line)) {
            final String value = line.getValue();
            if (!value.isEmpty() && !asAttr(value)) {
                indent(indent + 1);
                this.out.write(this.options.escape ? enc(value) : cdata(value));
                nl();
            }
        }
    }

    private boolean asAttr(String value) {
        boolean small = value.length() < this.options.charsMin;
        boolean hasNewlines = value.contains("\n");
        return small && !hasNewlines;
    }

    private String cdata(String value) {
        return "<![CDATA[" + cdataEscape(value) + "]]>";
    }

    private String cdataEscape(String value) {
        return value.replace("]]>", "]]]]><![CDATA[>");
    }

    private void indent(int indent) throws IOException {
        if (this.options.pretty) {
            for (int i = 0; i < indent; ++i) {
                this.out.write("    ");
            }
        }
    }

    private void nl() throws IOException {
        if (this.options.pretty) {
            this.out.newLine();
        }
    }

    private void closeNode(final TreeNode<GedcomLine> node, int indent) throws IOException {
        final GedcomLine line = node.getObject();
        indent(indent);
        this.out.write("</" + tag(line) + ">");
        nl();
    }

    private String tag(final GedcomLine line) {
        return "gedcom:" + (Objects.isNull(line) ? "GED" : enc(line.getTagString()));
    }

    private static String enc(String value) {
        return Encode.forXml(value);
    }
}
