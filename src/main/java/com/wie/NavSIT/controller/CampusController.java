package com.wie.NavSIT.controller;


import com.wie.NavSIT.service.GraphLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.Map;


@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CampusController {


    @Autowired
    private GraphLoader loader;


    @GetMapping("/nodes")
    public Map<String, double[]> getNodes() {
        return loader.getGraph().nodes;
    }
}