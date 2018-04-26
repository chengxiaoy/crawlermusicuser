package org.chengy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("/status")
@Controller
@CrossOrigin("*")
public class CrawlerStatusController {

    @RequestMapping("/get")
    @ResponseBody
    public String getStatus() {
        return "app running";
    }

}
