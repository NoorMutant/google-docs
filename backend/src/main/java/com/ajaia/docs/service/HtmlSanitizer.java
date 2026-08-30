package com.ajaia.docs.service;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * The editor is a contenteditable area, so whatever the browser sends is
 * untrusted. Everything outside the formatting the toolbar can produce is
 * stripped before the content reaches the database.
 */
@Component
public class HtmlSanitizer {

    private static final Safelist ALLOWED = Safelist.none()
            .addTags("p", "br", "div", "span",
                    "b", "strong", "i", "em", "u",
                    "h1", "h2", "h3",
                    "ul", "ol", "li",
                    "blockquote", "code", "pre", "a")
            .addAttributes("a", "href", "title")
            .addProtocols("a", "href", "http", "https", "mailto");

    public String clean(String html) {
        if (html == null) {
            return "";
        }
        return Jsoup.clean(html, ALLOWED);
    }
}
