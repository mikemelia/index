package com.mikemelia.student;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Slicer {

    public static final Pattern FIELD_CONTENTS = Pattern.compile("^\\s*field contents.*");
    public static final Pattern FIELD_PATH = Pattern.compile("^\\ss*field path.*");
    public static final Pattern TERM = Pattern.compile("^\\s*term.*");
    private final BufferedWriter[] writers;
    private final BufferedReader reader;
    private final List<String> term = new ArrayList();
    private BufferedWriter writer;
    private double percentage;
    private boolean processed_all_terms = false;
    private boolean processed_first_line = false;
    private int numberOfFiles;
    private int currentWriter = 0;

    public Slicer(String from, String to, int numberOfFiles) {
        this.numberOfFiles = numberOfFiles;
        this.reader = createReader(new File(from));
        this.writers = createWriter(to, numberOfFiles);
    }

    private BufferedReader createReader(File source) {
        try {
            return new BufferedReader(new FileReader(source));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("couldn't read source file " + source.getName(), e);
        }
    }

    private BufferedWriter[] createWriter(String destination, int numberOfFiles) {
        try {
            BufferedWriter writers[] = new BufferedWriter[numberOfFiles];
            for (int i = 0; i < numberOfFiles; i++) {
                writers[i] = new BufferedWriter(new FileWriter(new File(destination + i + ".pst")));
            }
            return writers;
        } catch (IOException e) {
            throw new RuntimeException("couldn't create writer on " + destination, e);
        }
    }

    public static void main(String[] args) {
        new Slicer("/Volumes/Mac/indexmedium/_c.pst", "/Volumes/Mac/postings/medium0.1/sliced", 3).slice(0.001);
    }

    private void slice(double percentage) {
        this.percentage = percentage;
        String line;
        try {
            while ((line = getNextLine()) != null &! processed_all_terms) {
                parse(line);
            }
        } finally {
            closeWriters();
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

    private void closeWriters() {
        try {
            for (int i = 0; i < numberOfFiles; i++) {
                writers[i].flush();
                writers[i].close();
            }
        } catch (IOException e) {
            throw new RuntimeException("couldn't close writer", e);
        }
    }

    private void parse(String line) {
        if (!processed_first_line && FIELD_CONTENTS.matcher(line).matches()) {
            System.out.println("contents for " + percentage);
            processed_first_line = true;
            return;
        }
        if (FIELD_PATH.matcher(line).matches()) {
            processed_all_terms = true;
            return;
        }
        if (TERM.matcher(line).matches()) {
            writer = writers[currentWriter];
            currentWriter = (++currentWriter) % numberOfFiles;
            writeTerm();
            createNewTerm(line);
            return;
        }
        addLine(line);
    }

    private void writeTerm() {
        double random = Math.random();
        if (random < (percentage)) {
           for (String line : term) {
                write(line);
            }
        } else {
//            System.out.println("Not writing " + random);
        }
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

    private void createNewTerm(String line) {
        term.clear();
        addLine(line);
    }

    private boolean addLine(String line) {
        return term.add(line);
    }

    private String getNextLine() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            throw new RuntimeException("couldn't read line", e);
        }
    }

}
