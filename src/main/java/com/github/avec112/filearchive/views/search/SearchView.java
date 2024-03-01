package com.github.avec112.filearchive.views.search;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.avec112.filearchive.search.SearchService;
import com.github.avec112.filearchive.type.CustomDocument;
import com.github.avec112.filearchive.type.ProfileDocument;
import com.github.avec112.filearchive.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.*;
import java.util.List;
import java.util.Map;

@PageTitle("Search")
@Route(value = "search", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class SearchView extends VerticalLayout {
    private final SearchService searchService;
    private final TextField searchField;
    private final VerticalLayout resultLayout;

    private Div currentlySelectedSearchResult;
    private final Div iframeContainer;
    private final Element iframe;

    public SearchView(SearchService searchService) {
        this.searchService = searchService;
        setWidthFull();

        searchField = new TextField();
        searchField.setWidth("300px");
        searchField.focus();
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.addKeyPressListener(Key.ENTER, e -> searchEvent());

        resultLayout = new VerticalLayout();
        resultLayout.setWidthFull();
        resultLayout.setPadding(false);

        iframe = new Element("iframe");
        iframe.setAttribute("class", "iframe-A4");

        iframeContainer = new Div();
        iframeContainer.setVisible(false);
        iframeContainer.getElement().appendChild(iframe);

        add(
                searchField,
                new HorizontalLayout(
                        resultLayout,
                        iframeContainer
                )
        );
    }

    private void searchEvent() {
        resultLayout.removeAll();
        iframeContainer.setVisible(false);

        String searchTerm = searchField.getValue();

        try {
            SearchResponse<JsonNode> response = searchService.search(searchTerm);

            TotalHits total = response.hits().total();
            boolean isExactResult = total.relation() == TotalHitsRelation.Eq;

            if (isExactResult) {
//                    System.out.println("There are " + total.value() + " results");
                Notification.show("There are " + total.value() + " results");
            }

            List<Hit<JsonNode>> hits = response.hits().hits();

            resultLayout.add(new Span("Antall treff: " + hits.size()));

            ObjectMapper mapper = new ObjectMapper();

            for (Hit<JsonNode> hit: hits) {

                String indexName = hit.index();
                JsonNode source = hit.source();

                File file;
                String documentType;
                String documentTypeColor = ""; // "", success, error, contrast

                if ("custom".equals(indexName)) {
                    // The document is from the "custom" index, process as CustomDocument
                    CustomDocument customDocument = mapper.treeToValue(source, CustomDocument.class);
                    file = new File(customDocument.getFilePath());
                    documentType = "Custom";
                    documentTypeColor = "contrast";

                } else if ("profile".equals(indexName)) {
                    // The document is from the "profile" index, process as ProfileDocument
                    ProfileDocument profileDocument = mapper.treeToValue(source, ProfileDocument.class);
                    file = new File(profileDocument.getFilePath());
                    documentType = "Profile";
                    documentTypeColor = "";

                } else {
                    throw new NotImplementedException("There are noe support for index: " + indexName);
                }

                Map<String, List<String>> highlights = hit.highlight();
                StreamResource resource = new StreamResource(file.getName(), () -> {
                    return getPdfAsStream(file); // Should return an InputStream
                });

                // Register the resource with VaadinSession and get the URL
                String resourceURL = VaadinSession.getCurrent().getResourceRegistry()
                        .registerResource(resource).getResourceUri().toString();


                Anchor anchor = new Anchor(resource, file.getName());
                anchor.setTarget(AnchorTarget.BLANK);


                VerticalLayout highlightLayout = new VerticalLayout();
                highlightLayout.setPadding(false);
                highlightLayout.setSpacing(false);
                highlights.forEach((field, snippets) -> {
                    snippets.forEach(snippet -> highlightLayout.add(new Html("<div>" + snippet + "</div>")));
                });

                Span documentTypeSpan = new Span("DocumentType: ");
                documentTypeSpan.getStyle().setColor("#658770");
                Span documentTypeBadge = new Span(documentType);
                documentTypeBadge.getElement().getThemeList().add("badge primary small " + documentTypeColor);
                Span scoreSpan = new Span("Score: " + hit.score());
                scoreSpan.getStyle().setColor("#658770");
                Div resultDiv = createClickableDiv(resourceURL,
                        anchor,
                        highlightLayout,
                        new HorizontalLayout(
                                documentTypeSpan,
                                documentTypeBadge
                        ),
                        new Div(scoreSpan)
                );
                resultDiv.setWidthFull();
                resultLayout.add(resultDiv);

            }


        } catch (IOException ex) {
            Notification.show(ExceptionUtils.getRootCauseMessage(ex), 5000, Notification.Position.MIDDLE);
        }
    }

    private Div createClickableDiv(String resourceUrl, Component...components) {
        Div clickedDiv = new Div(components);
        clickedDiv.addClassNames("search-result-div", "hover-hand");
        clickedDiv.addClickListener(e -> onDivClick(clickedDiv, resourceUrl));
        return clickedDiv;
    }

    private void onDivClick(Div clickedDiv, String resourceURL) {
        if (currentlySelectedSearchResult != null) {
            currentlySelectedSearchResult.removeClassName("selected-style");
        }
        if (!clickedDiv.equals(currentlySelectedSearchResult)) {
            clickedDiv.addClassName("selected-style");
            currentlySelectedSearchResult = clickedDiv;
            iframeContainer.setVisible(true);
            iframe.setAttribute("src", resourceURL);
        } else {
            currentlySelectedSearchResult = null; // Allow deselecting the current selection by clicking it again
            iframeContainer.setVisible(false);
            iframe.removeAttribute("src");
        }
    }

    private InputStream getPdfAsStream(File file) {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
