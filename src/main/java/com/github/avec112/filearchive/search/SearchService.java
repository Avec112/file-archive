package com.github.avec112.filearchive.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.github.avec112.filearchive.type.CustomFile;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class SearchService {

    private final RestClient restClient;
    private final ElasticsearchClient client;

    public SearchService() {
        restClient = RestClient
                .builder(HttpHost.create("http://localhost:9200"))
                .setDefaultHeaders(new Header[] {
                        // Basic is base64(elastic:changeme)
                        new BasicHeader("Authorization", "Basic ZWxhc3RpYzpjaGFuZ2VtZQ==")
                })
                .build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        client = new ElasticsearchClient(transport);
    }

    public SearchResponse<CustomFile> search(String searchTerm) throws IOException {

        return client.search(s -> s
                .index("archive")
                .query(q -> q
                        .match(t -> t
                                .field("fileName")
                                .field("content")
//                                .fuzziness("auto")
                                .query(searchTerm)
                        )
                ),
                CustomFile.class
        );
    }



    public void close() {
        try {
            restClient.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        SearchService searcher = new SearchService();
        try {
            SearchResponse<CustomFile> response = searcher.search("Samantha ");

            TotalHits total = response.hits().total();
            boolean isExactResult = total.relation() == TotalHitsRelation.Eq;

            if (isExactResult) {
                System.out.println("There are " + total.value() + " results");
            } else {
                System.out.println("There are more than " + total.value() + " results");
            }

            List<Hit<CustomFile>> hits = response.hits().hits();
            for (Hit<CustomFile> hit: hits) {
                CustomFile customFile = hit.source();
                System.out.println("Found product " + customFile + ", score " + hit.score());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            searcher.close();
        }
    }

}
