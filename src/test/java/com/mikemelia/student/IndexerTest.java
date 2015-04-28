package com.mikemelia.student;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotEquals;

public class IndexerTest {

    public static final String IDENTIFIER = "01";

    @Test
    public void itShouldFindTheTerm() {
        Indexer indexer = new Indexer();
        indexer.index(new File(Indexer.GUTENBERG_TXT), IDENTIFIER);
        int i = indexer.query("hello", IDENTIFIER);
        assertNotEquals(0, i);
        System.out.println(i);
    }

    @Test
    public void sh() {
        System.out.println(String.format("Number is %s", Integer.toBinaryString(~7)));
        System.out.println(String.format("Number is %s", Integer.toBinaryString(~(248 << 24))));
    }

}
