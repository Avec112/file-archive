package com.github.avec112.filearchive.views.search;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import com.github.avec112.filearchive.search.SearchService;
import com.github.avec112.filearchive.type.CustomFile;
import com.github.avec112.filearchive.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import java.io.IOException;
import java.util.List;

@PageTitle("Search")
@Route(value = "search", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class SearchView extends VerticalLayout {
    private final SearchService searchService;
    private TextField searchField;
    private Button searchButton;
    private VerticalLayout resultLayout;

    public SearchView(SearchService searchService) {
        this.searchService = searchService;

        searchField = new TextField();
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());

        searchButton = new Button("Search", e -> {
            resultLayout.removeAll();

            String searchTerm = searchField.getValue();

            try {
                SearchResponse<CustomFile> response = searchService.search(searchTerm);

                TotalHits total = response.hits().total();
                boolean isExactResult = total.relation() == TotalHitsRelation.Eq;

                if (isExactResult) {
//                    System.out.println("There are " + total.value() + " results");
                    Notification.show("There are " + total.value() + " results");
                }
                //else {
//                    System.out.println("There are more than " + total.value() + " results");
                //}

                List<Hit<CustomFile>> hits = response.hits().hits();
                for (Hit<CustomFile> hit: hits) {
                    CustomFile customFile = hit.source();
                    Paragraph fileName = new Paragraph(customFile.getFileName());
                    fileName.getStyle().setFontWeight(Style.FontWeight.BOLD.name());
                    resultLayout.add(new Div(
                            new Hr(),
                            fileName,
                            new Paragraph(customFile.getContent())
                    ));

                }

            } catch (IOException ex) {
                Notification.show(ex.getMessage()); // TODO
            }
        });

        resultLayout = new VerticalLayout();


        add(
                new HorizontalLayout(
                    searchField,
                    searchButton
                ),
                resultLayout
        );
    }
}
