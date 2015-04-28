package com.mikemelia.student;


import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class Indexer {

    public static final Pattern PATTERN = Pattern.compile(".*\\.txt");
    public static final String GUTENBERG_TXT = "/Volumes/Flash/thesis/unzipped";
    public static final Version VERSION = Version.LUCENE_4_9;
    public static final String NAME = "contents";
    private final double percentage = 1.0;

    public static final String INDEX_PATH = "/Volumes/Flash/index";
    private Directory directory;
    private final StandardAnalyzer analyzer;

    public Indexer() {
        analyzer = new StandardAnalyzer(Version.LUCENE_4_9);
    }

    private Directory createDirectory(File indexDirectory) {
        try {
            File index = indexDirectory;
            return new SimpleFSDirectory(index);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create index", e);
        }
    }

    private File createOrDeleteDirectory(String identifier) {
        System.out.println("PROCESSING " + identifier);
        File index = directoryFrom(identifier);
        if (!index.exists()) {
            index.mkdir();
        } else {
            for (File file : index.listFiles()) {
                file.delete();
            }
        }
        return index;
    }

    private File directoryFrom(String identifier) {
        return new File(INDEX_PATH + identifier);
    }

    public int query(String text, String identifier) {
        this.directory = createDirectory(directoryFrom(identifier));
        TopDocs topDocs = getTopDocs(text, 100);
        return topDocs.totalHits;
    }

    private Query createQuery(String text) {
        try {
            return new QueryParser(VERSION, NAME, analyzer).parse(text);
        } catch (ParseException e) {
            throw new RuntimeException(String.format("Couldn't parse : %s", text));
        }
    }

    private TopDocs getTopDocs(String text, int n) {
        try {
            return new IndexSearcher(DirectoryReader.open(directory)).search(createQuery(text), n);
        } catch (IOException e) {
            throw new RuntimeException("Can't create a reader", e);
        }
    }

    public static void main(String[] args) {
        new Indexer().index(new File(GUTENBERG_TXT), "01");
    }


    public void index(File location, String identifier) {
        directory = createDirectory(createOrDeleteDirectory(identifier));
        IndexWriter writer = createWriter();
        List<File> files = getFiles(location);
        int counter = 1;
        for (File file : files) {
            System.out.println(String.format("processing %s - %d", file.getName(), counter++));
            if (Math.random() < percentage) addDocument(file, writer);
        }
        mergeSegments(writer);
        try {
            writer.close(true);
        } catch (IOException e) {
            throw new RuntimeException("couldn't close writer", e);
        }
    }

    private void mergeSegments(IndexWriter writer) {
        try {
            writer.forceMerge(1);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't merge to 1 segment", e);
        }
    }

    private void addDocument(File file, IndexWriter writer) {
        Document document = new Document();
        try {
            String path = file.getCanonicalPath();
            document.add(new StringField("path", path, Field.Store.YES));

            Reader reader = new FileReader(file);
            document.add(new TextField(NAME, reader));

            writer.addDocument(document);
        } catch (IOException e) {
            throw new RuntimeException("couldn't index doc " + file.getName(), e);
        }
    }

    private IndexWriter createWriter() {
        IndexWriterConfig config = new IndexWriterConfig(VERSION, analyzer);
        config.setCodec(new SimpleTextCodec());
        config.setUseCompoundFile(false);
        LogDocMergePolicy mergePolicy = new LogDocMergePolicy();
        config.setMergePolicy(mergePolicy);
        try {
            return new IndexWriter(directory, config);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create writer", e);
        }
    }

    private List<File> getFiles(File location) {
        File[] files = location.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return isTextFile(name);
            }
        });
        List<File> fileList = Arrays.asList(files);
        Collections.shuffle(fileList);
        return fileList;
    }

    private boolean isTextFile(String name) {
        return PATTERN.matcher(name).matches();
    }
}
