package org.chengy.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/home")
@Controller
@CrossOrigin("*")
public class HomeController {

    @RequestMapping("/")
    public String getHome(ModelMap map) {

        return "/home";
    }

}
