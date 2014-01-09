package better.cli.utils;

import java.io.PrintStream;

public class PrintUtils {

    public static void printTable(String[][] table) {
        printTable(System.out, table);
    }

    public static void printTable(PrintStream os, String[][] table) {
        // Find out what the maximum number of columns is in any row
        int maxColumns = 0;
        for (int i = 0; i < table.length; i++) {
            maxColumns = Math.max(table[i].length, maxColumns);
        }

        // Find the maximum length of a string in each column
        int[] lengths = new int[maxColumns];
        for (int i = 0; i < table.length; i++) {
            for (int j = 0; j < table[i].length; j++) {
                lengths[j] = Math.max(table[i][j].length(), lengths[j]);
            }
        }

        // Generate a format string for each column
        String[] formats = new String[lengths.length];
        for (int i = 0; i < lengths.length; i++) {
            formats[i] = "%-" + lengths[i] + "s" + (i + 1 == lengths.length ? "\n" : " ");
        }

        // Print 'em out
        for (int i = 0; i < table.length; i++) {
            for (int j = 0; j < table[i].length; j++) {
                os.printf(formats[j], table[i][j]);
            }
        }
    }

}
