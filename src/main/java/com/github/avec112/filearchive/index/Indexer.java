package com.github.avec112.filearchive.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.ingest.simulate.Document;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.github.avec112.filearchive.type.CustomFile;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.elasticsearch.client.RestClient;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

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

    public void indexDocuments(String directoryPath, String indexName) throws IOException, TikaException, SAXException {
        Tika tika = new Tika();

        File dir = new File(directoryPath);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".pdf"));


        if (files != null) {
            for (File file : files) {
                System.out.printf("Found file %s%n", file);
                String fileName = file.getName();
                String parsedText = tika.parseToString(file);

                CustomFile customFile = new CustomFile();
                customFile.setFileName(fileName);
                customFile.setContent(parsedText);

                indexDocument(customFile, indexName);

            }
        }
    }

    private void indexDocument(CustomFile customFile, String indexName) throws IOException {

        IndexResponse indexResponse = client.index(i -> i
                .index(indexName)
                .id(customFile.getFileName())
                .document(customFile)
        );

        Result result = indexResponse.result();
        System.out.printf("Result:%s%n", result.jsonValue());
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
            indexer.indexDocuments("pdf/", "archive");
        } catch (IOException | TikaException | SAXException e) {
            throw new RuntimeException(e);
        } finally {
            indexer.close();
        }
    }
}
