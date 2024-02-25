package com.github.avec112.filearchive.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;

public class SearchView extends AppLayout {

    private Footer createFooter() {
        Footer layout = new Footer();

        return layout;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        re
}
