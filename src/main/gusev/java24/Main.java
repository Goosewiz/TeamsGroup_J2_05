package main.gusev.java24;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opencsv.bean.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

public class Main {
    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
        Scanner sc = new Scanner(System.in);
        HttpClient client = HttpClient.newBuilder().build();
        Path downloadDir = Path.of(System.getProperty("user.home"), "MOEX securities");
        if (Files.notExists(downloadDir)){
            Files.createDirectory(downloadDir);
        }
        List<CompletableFuture<HttpResponse<String>>> results = new ArrayList<>();
        while (true){
            System.out.println("Введите название компании для поиска");
            String search = sc.nextLine();
            String path = "https://iss.moex.com/iss/securities.json?q=" + search + "&is_trading=1";
            if (search.equals("exit"))
                break;
            try{
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(path))
                        .build();
                CompletableFuture<HttpResponse<String>> result = client.sendAsync(request,
                        HttpResponse.BodyHandlers.ofString());
                results.add(result);
                System.out.println("Начата загрузка информации по " + search);
                result.thenAccept((response) -> {
                    if (response.statusCode() == 200 && !response.body().equals(null)){
                        System.out.println("Информация найдена по " + search);
                        String body = response.body();
                        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                        InfoWrapper fileInfo = null;
                        try {
                            fileInfo = mapper.reader(InfoWrapper.class)
                                    .readValue(body);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        String[] columns = fileInfo.getInfoModel();
                        List<Integer> positions = new ArrayList<>();
                        for (int i = 0; i < columns.length; i++) {
                            if (columns[i].equals("secid") || columns[i].equals("shortname") || columns[i].equals("regnumber") || columns[i].equals("name")
                                    || columns[i].equals("emitent_title") || columns[i].equals("emitent_inn") || columns[i].equals("emitent_okpo")) {
                                positions.add(i);
                            }
                        }
                        String[][] answer = fileInfo.getAnswer();
                        List<CSV> fileCSV = new ArrayList<>();
                        for (int i = 0; i < answer.length; i++) {
                            CSV temp = new CSV(answer[i][positions.get(0)], answer[i][positions.get(1)], answer[i][positions.get(2)],
                                    answer[i][positions.get(3)], answer[i][positions.get(4)], answer[i][positions.get(5)], answer[i][positions.get(6)]);
                            fileCSV.add(temp);
                        }
                        String fileName = downloadDir.toString() + "\\" + search + ".csv";
                        if (fileCSV.size() == 0){
                            System.out.println(search + " - некорректное имя компании");
                        }
                        else{
                            try (Writer writer = new FileWriter(fileName)){
                                if (fileCSV.size() == 0){
                                    throw new IOException(search);
                                }
                                StatefulBeanToCsv beanToCsv = new StatefulBeanToCsvBuilder<CSV>(writer).withSeparator(';')
                                        .build();
                                beanToCsv.write(fileCSV);
                            } catch (IOException | CsvRequiredFieldEmptyException | CsvDataTypeMismatchException e) {
                                System.out.println(e.getMessage());
                            }
                        }
                    } else{
                        System.err.println("Ошибка поиска информации по заданной компании, ошибка " + response.statusCode());
                    }
                })
                        .exceptionally((e)->{
                            System.err.println("Ошибка " + e.getMessage());
                            return null;
                        });
            } catch(URISyntaxException e){
                System.err.println("Некорректное название компании" + e.getMessage());
            }
        }
        while(results.stream().anyMatch((cf) -> !cf.isDone()));
    }
}
