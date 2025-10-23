package com.wie.NavSIT.controller;

import com.wie.NavSIT.model.CampusGraph;
import com.wie.NavSIT.service.GraphLoader;
import com.wie.NavSIT.service.PathFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CampusController {

    @Autowired
    private GraphLoader loader;

    // Optional: get all nodes
    @GetMapping("/nodes")
    public Map<String, double[]> getNodes() {
        return loader.getGraph().nodes;
    }

    // Navigate from one node to another
    @PostMapping("/navigate")
    public List<double[]> navigate(@RequestBody Map<String, String> request) {
        String from = request.get("from");
        String to = request.get("to");
        return PathFinder.findPath(loader.getGraph(), from, to);
    }
}
