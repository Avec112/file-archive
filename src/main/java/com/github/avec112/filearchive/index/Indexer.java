package com.github.avec112.filearchive.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.github.avec112.filearchive.parser.ProfileDocumentParser;
import com.github.avec112.filearchive.type.CustomDocument;
import com.github.avec112.filearchive.type.ProfileDocument;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.elasticsearch.client.RestClient;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Indexer {

    private final RestClient restClient;
    private final ElasticsearchClient client;

    public Indexer() {
        restClient = RestClient
                .builder(HttpHost.create("http://localhost:9200"))
                .setDefaultHeaders(new Header[] {
                        // Bearer is base64(username:password)
                        new BasicHeader("Authorization", "Basic ZWxhc3RpYzpjaGFuZ2VtZQ==")
                })
                .build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        client = new ElasticsearchClient(transport);
    }

    public void indexDocuments(String directoryPath) throws IOException, TikaException, SAXException {

        List<Path> pdfFiles = Files.walk(Paths.get(directoryPath))
                .filter(p -> p.toString().endsWith(".pdf"))
                .toList();

        for (Path path : pdfFiles) {
            // document type profile
            if(path.toString().contains("profile")) {
                indexProfileDocument(path);
            } else {
                indexOtherDocument(path);
            }
        }

    }

    private void indexProfileDocument(Path path) throws TikaException, IOException, SAXException {

        ProfileDocumentParser parser = new ProfileDocumentParser();

        ProfileDocument document = parser.parseDocument(path);

        indexDocument(path, document, "profile");
    }


    private void indexOtherDocument(Path path) throws IOException, TikaException {

        Tika tika = new Tika();
        File file = path.toFile();
        String fileName = file.getName();
        String parsedText = tika.parseToString(file);

        CustomDocument customDocument = new CustomDocument();
        customDocument.setFileName(fileName);
        customDocument.setContent(parsedText);
        customDocument.setFilePath(file.getPath());

        indexDocument(path, customDocument, "custom");
    }

    private void indexDocument(Path path, Object document, String index) throws IOException {

        // Check if the index exists, create if not
        BooleanResponse exists = client.indices().exists(e -> e.index(index));
        if (!exists.value()) {
            CreateIndexResponse createIndexResponse = client.indices().create(c -> c.index(index));
            // Handle create index response if needed
            if (createIndexResponse.acknowledged()) {
                System.out.println("Index created: " + index);
            } else {
                System.out.println("Failed to create index: " + index);
            }
        }

        IndexResponse response = client.index(i -> i
                .index(index)
                .document(document)
        );

        Result result = response.result();
        System.out.printf("%s: %s%n", result.jsonValue(), path);
    }

    public void close() {
        try {
            restClient.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        Indexer indexer = new Indexer();
        try {
            indexer.indexDocuments("pdf/");
        } catch (IOException | TikaException | SAXException e) {
            throw new RuntimeException(e);
        } finally {
            indexer.close();
        }
    }
}
