package com.ajaia.docs.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The Angular app is served from inside the same jar. Its routes are handled in
 * the browser, so any path that is not an API call and does not look like a file
 * is handed back to index.html and the router takes over from there.
 */
@Controller
public class SpaForwardController {

    @GetMapping({"/", "/{path:(?!api$)[^\\.]*}", "/{path:(?!api$)[^\\.]*}/{child:[^\\.]*}"})
    public String forwardToApp() {
        return "forward:/index.html";
    }
}
