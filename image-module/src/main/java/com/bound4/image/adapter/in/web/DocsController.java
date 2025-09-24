package com.bound4.image.adapter.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DocsController {
    
    @GetMapping("/docs")
    public String docs() {
        return "redirect:/docs/index.html";
    }
}