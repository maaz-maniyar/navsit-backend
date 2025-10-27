package com.wie.NavSIT.controller;


import com.wie.NavSIT.service.NavigationService;
import com.wie.NavSIT.service.GraphLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/navigation")
@CrossOrigin(origins = "*")
public class NavigationController {


    @Autowired
    private NavigationService navigationService;


    @Autowired
    private GraphLoader loader;


    @PostMapping("/path")
    public Map<String, Object> getPath(@RequestBody Map<String, String> req) {
        String from = req.get("source");
        String to = req.get("destination");
        List<String> path = navigationService.shortestPath(from, to);
        Map<String, Object> res = new HashMap<>();
        if (path == null) {
            res.put("error", "No path found");
            return res;
        }
        res.put("path", path);
        res.put("coordinates", navigationService.pathToCoordinates(path));
        res.put("nextNode", path.size() > 1 ? path.get(1) : path.get(0));
        res.put("nextCoordinates", loader.getGraph().nodes.get(res.get("nextNode")));
        return res;
    }


    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody Map<String, Object> req) {
        double lat = ((Number) req.get("latitude")).doubleValue();
        double lon = ((Number) req.get("longitude")).doubleValue();
        List<String> path = (List<String>) req.get("path");
        return navigationService.computeNextNodeFromPosition(lat, lon, path);
    }
}