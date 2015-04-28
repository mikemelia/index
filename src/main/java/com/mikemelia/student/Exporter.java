package com.mikemelia.student;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Exporter {

    public static final Pattern TERM = Pattern.compile("^\\s+term.*");
    public static final Pattern FREQ = Pattern.compile("^\\s+freq\\s+(\\d+)");
    public static final Pattern DOC = Pattern.compile("^\\s+doc\\s+(\\d+)");
    public static final Pattern POS = Pattern.compile("^\\s+pos\\s+(\\d+)");
    public static final String SOURCE = "/Volumes/Flash/index01/_1n.pst";
    public static final String DEST = "/Volumes/Flash/thesis/data/notermordoc.txt";
    public static final String MAX_LONG = Long.toUnsignedString(-1);
    private final BufferedWriter writer;
    private final BufferedReader reader;
    private long currentTerm = 0L;
    private long lastDoc = 0L;
    private long lastPos = 0L;

    public Exporter(String from, String to) {
        this.reader = createReader(new File(from));
        this.writer = createWriter(new File(to));
    }

    private BufferedReader createReader(File source) {
        try {
            return new BufferedReader(new FileReader(source));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("couldn't read source file " + source.getName(), e);
        }
    }

    private BufferedWriter createWriter(File destination) {
        try {
            return new BufferedWriter(new FileWriter(destination));
        } catch (IOException e) {
            throw new RuntimeException("couldn't create writer on " + destination.getName(), e);
        }
    }

    public static void main(String[] args) {
        new Exporter(SOURCE, DEST).export();
    }

    private void export() {
        String line;
        try {
            while ((line = getNextLine()) != null) {
                parse(line);
            }
        } finally {
            closeWriter();
            closeReader();
        }

    }

    private void closeReader() {
        try {
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException("couldn't close reader", e);
        }
    }

    private void closeWriter() {
        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("couldn't close writer", e);
        }
    }

    private void parse(String line) {
        if (TERM.matcher(line).matches()) {
            write(String.format("%s", -1));
            lastDoc = 0;
            return;
        }
        Matcher docMatcher = DOC.matcher(line);
        if (docMatcher.matches()) {
//            long doc = Long.parseLong(docMatcher.group(1));
//            write(String.format("%d", nonNegative("Doc", doc, lastDoc)));
//            lastDoc = doc;
            lastPos = 0;
            return;
        }
        Matcher freqMatcher = FREQ.matcher(line);
        if (freqMatcher.matches()) {
            write(String.format("%d", Long.valueOf(freqMatcher.group(1))));
            return;
        }
        Matcher posMatcher = POS.matcher(line);
        if (posMatcher.matches()) {
            long pos = Long.parseLong(posMatcher.group(1));
            write(String.format("%d", nonNegative("Pos", pos, lastPos)));
            lastPos = pos;
        }
    }

    private long nonNegative(String type, long now, long previous) {
        long l = now - previous;
        if (l < 0) throw new RuntimeException(String.format("%s %d - %d", type, now, previous));
        return l;
    }

    private void write(String line) {
        try {
            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't write line " + line, e);
        }
    }

    private String getNextLine() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            throw new RuntimeException("couldn't read line", e);
        }
    }

}
