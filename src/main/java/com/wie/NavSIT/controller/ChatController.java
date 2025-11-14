package com.wie.NavSIT.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.wie.NavSIT.model.CampusGraph;
import com.wie.NavSIT.service.GraphLoader;
import com.wie.NavSIT.service.NavigationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private GraphLoader loader;

    @Autowired
    private NavigationService navigationService;

    private final RestTemplate restTemplate = new RestTemplate();

    // Per-user active paths (simple in-memory map)
    private final Map<String, List<String>> activePaths = new HashMap<>();

    @PostMapping
    public Map<String, Object> handleMessage(@RequestBody Map<String, Object> request) {
        String userMessage = (String) request.get("message");
        Double userLat = request.get("latitude") != null ? ((Number) request.get("latitude")).doubleValue() : null;
        Double userLon = request.get("longitude") != null ? ((Number) request.get("longitude")).doubleValue() : null;

        String pythonUrl = "https://mocknlp-production.up.railway.app/parse";

        Map<String, String> payload = Map.of("message", userMessage);
        JsonNode response = restTemplate.postForObject(pythonUrl, payload, JsonNode.class);

        String intent = response.has("intent") ? response.get("intent").asText() : "unknown";
        String entity = null;
        if (response.has("entity")) {
            if (response.get("entity").isArray() && response.get("entity").size() > 0) {
                entity = response.get("entity").get(0).asText();
            } else {
                entity = response.get("entity").asText();
            }
        }
        String answer = response.has("response") ? response.get("response").asText() : "I'm not sure how to respond.";

        CampusGraph graph = loader.getGraph();
        Map<String, Object> result = new HashMap<>();

        if ((intent.equalsIgnoreCase("navigation") || intent.equalsIgnoreCase("navigation_request"))
                && entity != null && userLat != null && userLon != null) {
            String currentNode = graph.findNearestNode(userLat, userLon);
            List<String> path = navigationService.shortestPath(currentNode, entity);

            if (path != null && !path.isEmpty()) {
                activePaths.put("user", path);

                Map<String, Object> nextNodeData = navigationService.computeNextNodeFromPosition(userLat, userLon, path);
                String nextNode = (String) nextNodeData.get("nextNode");

                result.put("reply", "Navigating to " + entity + "...");
                result.put("nextNode", nextNode);
                result.put("path", path);
                result.put("coordinates", navigationService.pathToCoordinates(path));
            } else {
                result.put("reply", "Sorry, I couldn't find a route to " + entity + ".");
            }
        } else {
            result.put("reply", answer);
        }

        return result;
    }

    @PostMapping("/update-node")
    public Map<String, Object> updateNode(@RequestBody Map<String, Object> position) {
        double lat = ((Number) position.get("latitude")).doubleValue();
        double lon = ((Number) position.get("longitude")).doubleValue();

        List<String> path = activePaths.get("user");
        return navigationService.computeNextNodeFromPosition(lat, lon, path);
    }
}
