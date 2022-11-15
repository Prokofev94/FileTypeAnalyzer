package analyzer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    static List<String[]> allPatterns = new ArrayList<>();


    public static void main(String[] args) throws InterruptedException {
        File file = new File(args[0]);
        File patternsFile = new File(args[1]);
        getPatterns(patternsFile);
        search(file);
    }

    static void getPatterns(File file) {
        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNext()) {
                String line = sc.nextLine();
                allPatterns.add(line.split(";"));
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        }
    }

    static int[] getPrefixFunction(String str) {
        int[] array = new int[str.length()];
        for (int i = 1; i <= str.length(); i++) {
            String sub = str.substring(0, i);
            String prefix = sub.substring(0, sub.length() - 1);
            String suffix = sub.substring(1);
            while (suffix.length() > 0) {
                if (suffix.equals(prefix)) {
                    break;
                }
                prefix = prefix.substring(0, prefix.length() - 1);
                suffix = suffix.substring(1);
            }
            array[i - 1] = suffix.length();
        }
        return array;
    }
    static String getMostPriority(List<String[]> patterns) {
        int highestPriority = Integer.MIN_VALUE;
        int index = 0;
        for (int i = 0; i < patterns.size(); i++) {
            int priority = Integer.parseInt(patterns.get(i)[0]);
            if (priority > highestPriority) {
                highestPriority = priority;
                index = i;
            }
        }
        return patterns.get(index)[2].replaceAll("\"", "");
    }

    static void search(File file) throws InterruptedException {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                search(f);
            }
        } else {
            String fileName = file.getName();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                List<String[]> matchesPatterns = new ArrayList<>();
                for (String[] feature : allPatterns) {
                    String pattern = feature[1].replaceAll("\"", "");
                    int[] prefixFunction = getPrefixFunction(pattern);
                    boolean matches = false;
                    StringBuilder fileFragment = new StringBuilder();
                    try (FileInputStream fileInputStream = new FileInputStream(file)) {
                        int byteRead;
                        while ((byteRead = fileInputStream.read()) != -1) {
                            while (fileFragment.length() < pattern.length()) {
                                fileFragment.append((char) byteRead);
                                byteRead = fileInputStream.read();
                            }
                            do {
                                for (int i = 0; i < pattern.length() && pattern.length() == fileFragment.length(); i++) {
                                    matches = true;
                                    if (pattern.charAt(i) != fileFragment.charAt(i)) {
                                        matches = false;
                                        int shift = i;
                                        if (i > 0) {
                                            shift -= prefixFunction[i - 1];
                                        }
                                        shift = shift == 0 ? 1 : shift;
                                        fileFragment = new StringBuilder(fileFragment.substring(shift));
                                        for (int j = 0; byteRead != -1 && j < shift; j++) {
                                            fileFragment.append((char) byteRead);
                                            byteRead = fileInputStream.read();
                                        }
                                        i = -1;
                                    }
                                }
                            } while (!matches && byteRead != -1);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (matches) {
                        matchesPatterns.add(feature);
                    }
                }
                System.out.print(fileName + ": ");
                if (matchesPatterns.size() > 0) {
                    System.out.println(getMostPriority(matchesPatterns));
                } else {
                    System.out.println("Unknown file type");
                }
            });
            executor.shutdown();
            executor.awaitTermination(100, TimeUnit.MILLISECONDS);
        }
    }
}