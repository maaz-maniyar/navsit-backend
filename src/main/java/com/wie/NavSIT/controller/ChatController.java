package com.wie.NavSIT.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wie.NavSIT.model.CampusGraph;
import com.wie.NavSIT.service.GraphLoader;
import com.wie.NavSIT.service.PathFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins="*")
public class ChatController {

    @Autowired
    private GraphLoader loader;

    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    // Store user's current navigation state
    private final Map<String, List<String>> activePaths = new HashMap<>();

    /**
     * Handles any message from the frontend chatbot.
     * Message is sent to Python NLP model to extract intent & entity.
     */
    @PostMapping
    public Map<String, Object> handleMessage(@RequestBody Map<String, Object> request) {
        String userMessage = (String) request.get("message");
        Double userLat = (Double) request.get("latitude");
        Double userLon = (Double) request.get("longitude");

        // Step 1: Send message to Python NLP service
        String pythonUrl = "https://mocknlp-production.up.railway.app/parse";
        Map<String, String> payload = Map.of("message", userMessage);
        JsonNode response = restTemplate.postForObject(pythonUrl, payload, JsonNode.class);

        String intent = response.get("intent").asText();
        String entity = response.has("entity") ? response.get("entity").asText() : null;

        CampusGraph graph = loader.getGraph();
        Map<String, Object> result = new HashMap<>();

        // Step 2: Handle navigation requests
        if ("navigation".equalsIgnoreCase(intent) && entity != null) {
            String currentNode = graph.findNearestNode(userLat, userLon);
            List<String> path = PathFinder.findPathByName(graph, currentNode, entity);

            if (path != null && !path.isEmpty()) {
                activePaths.put("user", path);
                result.put("reply", "Navigating to " + entity + "...");
                result.put("nextNode", path.get(0));
            } else {
                result.put("reply", "Sorry, I couldn't find a route to " + entity + ".");
            }
        }
        // Step 3: If not navigation, forward NLP response directly
        else {
            String answer = response.has("answer") ? response.get("answer").asText() : "I'm not sure how to respond.";
            result.put("reply", answer);
        }

        return result;
    }

    /**
     * Updates user's next navigation node as they move.
     */
    @PostMapping("/update-node")
    public Map<String, Object> updateNode(@RequestBody Map<String, Object> position) {
        double lat = (double) position.get("latitude");
        double lon = (double) position.get("longitude");

        CampusGraph graph = loader.getGraph();
        String currentNode = graph.findNearestNode(lat, lon);
        List<String> path = activePaths.get("user");

        Map<String, Object> result = new HashMap<>();
        if (path != null && path.contains(currentNode)) {
            int index = path.indexOf(currentNode);
            if (index < path.size() - 1) {
                result.put("nextNode", path.get(index + 1));
            } else {
                result.put("nextNode", null);
                result.put("reply", "You’ve reached your destination!");
            }
        } else {
            result.put("nextNode", currentNode);
        }

        return result;
    }
}
