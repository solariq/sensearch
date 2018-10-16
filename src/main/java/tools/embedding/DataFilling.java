package tools.embedding;

import java.io.*;
import java.util.HashMap;
import java.util.Scanner;

public class DataFilling {

    private static final int VEC_SIZE = 50;
    public static final String SER_FILENAME = "resources/hashmap.ser";

    private HashMap<String, double[]> hashMap;
    private Scanner scanner;

    public DataFilling(String filename) {
        try {
            scanner = new Scanner(new File(filename));
            hashMap = new HashMap<>();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void fill() {
        while (scanner.hasNext()) {
            String word = scanner.next();
            double[] coord = new double[VEC_SIZE];
            for (int i = 0; i < coord.length; i++) {
                coord[i] = Double.parseDouble(scanner.next());
            }
            hashMap.put(word, coord);
        }
    }

    public void save() {
        try(FileOutputStream fos = new FileOutputStream(SER_FILENAME);
            ObjectOutputStream oos = new ObjectOutputStream(fos)) {

            oos.writeObject(hashMap);

        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}