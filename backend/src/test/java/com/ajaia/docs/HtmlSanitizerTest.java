package com.ajaia.docs;

import com.ajaia.docs.service.HtmlSanitizer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlSanitizerTest {

    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    @Test
    void keepsTheFormattingTheToolbarProduces() {
        String html = "<h1>Title</h1><p><strong>bold</strong> <em>italic</em> <u>underline</u></p>"
                + "<ul><li>one</li></ul><ol><li>two</li></ol>";

        assertThat(sanitizer.clean(html)).isEqualToIgnoringWhitespace(html);
    }

    @Test
    void removesScriptsAndEventHandlers() {
        String cleaned = sanitizer.clean("<p onclick=\"steal()\">hi</p><script>alert(1)</script>");

        assertThat(cleaned).doesNotContain("script").doesNotContain("onclick");
        assertThat(cleaned).contains("hi");
    }

    @Test
    void nullContentBecomesAnEmptyString() {
        assertThat(sanitizer.clean(null)).isEmpty();
    }
}
