package main.java.com.example.fileIO.read;

import javax.sound.midi.Soundbank;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Reader {
    private static final Path filePath = Paths.get("C:\\Users\\ROG\\Desktop\\csv.txt");


    public static void read() {
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败~！", e);
        }
    }


    public static String read2() throws IOException {
        byte[] fileBytes = Files.readAllBytes(Path.of("C:\\Users\\ROG\\Desktop\\csv.txt"));
        return new String(fileBytes, StandardCharsets.UTF_8);
    }




    public static void main(String[] args) throws IOException {
//        read();
        String content = read2();
        System.out.println(content);
    }
}
