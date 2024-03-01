package com.github.avec112.filearchive.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Service;

import java.io.IOException;

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

    public SearchResponse<JsonNode> search(String searchTerm) throws IOException {

        return client.search(s -> s
                        .index("custom", "profile") // Specify both indices
                        .query(query -> query
                                .bool(boolQuery -> boolQuery
                                        .should(should -> should
                                                .wildcard(wildcardQuery -> wildcardQuery
                                                        .field("fileName") // Relevant for "custom"
                                                        .value(searchTerm)
                                                )
                                        )
                                        .should(should -> should
                                                .wildcard(wildcardQuery -> wildcardQuery
                                                        .field("content") // Relevant for both "custom" and "profile"
                                                        .value(searchTerm)
                                                )
                                        )
                                        // Assuming "title", "ingress", and "filePath" are only relevant for "profile"
                                        .should(should -> should
                                                .wildcard(wildcardQuery -> wildcardQuery
                                                        .field("title")
                                                        .value(searchTerm)
                                                )
                                        )
                                        .should(should -> should
                                                .wildcard(wildcardQuery -> wildcardQuery
                                                        .field("ingress")
                                                        .value(searchTerm)
                                                )
                                        )
                                        .should(should -> should
                                                .wildcard(wildcardQuery -> wildcardQuery
                                                        .field("filePath")
                                                        .value(searchTerm)
                                                )
                                        )
                                )
                        )
                        .highlight(highlight -> highlight
                                .fields("fileName", fieldHighlight -> fieldHighlight) // Adjust according to common fields
                                .fields("ingress", fieldHighlight -> fieldHighlight) // Adjust according to common fields
                                .fields("content", fieldHighlight -> fieldHighlight) // Adjust according to common fields
                                .preTags("<strong style='color: red;'>")
                                .postTags("</strong>")
                        ),
                JsonNode.class // Using Object as a generic type; consider a common interface or base class
        );
    }



    public void close() {
        try {
            restClient.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    public static void main(String[] args) {
//        SearchService searcher = new SearchService();
//        try {
//            SearchResponse<CustomDocument> response = searcher.search("Samantha ");
//
//            TotalHits total = response.hits().total();
//            boolean isExactResult = total.relation() == TotalHitsRelation.Eq;
//
//            if (isExactResult) {
//                System.out.println("There are " + total.value() + " results");
//            } else {
//                System.out.println("There are more than " + total.value() + " results");
//            }
//
//            List<Hit<CustomDocument>> hits = response.hits().hits();
//            for (Hit<CustomDocument> hit: hits) {
//                CustomDocument customFile = hit.source();
//                System.out.println("Found product " + customFile + ", score " + hit.score());
//            }
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        } finally {
//            searcher.close();
//        }
//    }

}
