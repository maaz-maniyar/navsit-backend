package com.wie.NavSIT.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.wie.NavSIT.model.CampusGraph;
import com.wie.NavSIT.service.GraphLoader;
import com.wie.NavSIT.service.NavigationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final GraphLoader loader;
    private final NavigationService navigationService;
    private final RestTemplate restTemplate;

    @Value("${app.nlp.parse-url}")
    private String pythonUrl;

    // Per-user active paths (simple in-memory map)
    private final Map<String, List<String>> activePaths = new HashMap<>();

    @Autowired
    public ChatController(GraphLoader loader, NavigationService navigationService, RestTemplate restTemplate) {
        this.loader = loader;
        this.navigationService = navigationService;
        this.restTemplate = restTemplate;
    }

    @PostMapping
    public Map<String, Object> handleMessage(@RequestBody Map<String, Object> request) {
        String userMessage = (String) request.get("message");
        Double userLat = request.get("latitude") != null ? ((Number) request.get("latitude")).doubleValue() : null;
        Double userLon = request.get("longitude") != null ? ((Number) request.get("longitude")).doubleValue() : null;

        Map<String, String> payload = Map.of("message", userMessage);
        JsonNode response;
        try {
            response = postToNlpWithRetry(payload);
        } catch (RestClientException ex) {
            logger.error("NLP request failed after retries. url={}, message={}", pythonUrl, ex.getMessage(), ex);
            return Map.of("reply", "The NLP service is unavailable right now. Please try again in a moment.");
        }

        if (response == null) {
            return Map.of("reply", "The NLP service returned an empty response.");
        }

        String intent = response.has("intent") ? response.get("intent").asText() : "unknown";
        String intentType = response.has("intent_type") ? response.get("intent_type").asText() : intent;
        String entity = null;
        if (response.has("destination") && !response.get("destination").isNull()) {
            entity = response.get("destination").asText();
        } else if (response.has("entity")) {
            if (response.get("entity").isArray() && response.get("entity").size() > 0) {
                entity = response.get("entity").get(0).asText();
            } else {
                entity = response.get("entity").asText();
            }
        }
        String answer = response.has("response") ? response.get("response").asText() : "I'm not sure how to respond.";

        CampusGraph graph = loader.getGraph();
        Map<String, Object> result = new HashMap<>();

        if (isNavigationIntent(intent, intentType)
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

    private JsonNode postToNlpWithRetry(Map<String, String> payload) {
        RestClientException lastException = null;

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                logger.info("Calling NLP service. attempt={}, url={}", attempt, pythonUrl);
                return restTemplate.postForObject(pythonUrl, payload, JsonNode.class);
            } catch (RestClientException ex) {
                lastException = ex;
                logger.warn("NLP call attempt {} failed. url={}, error={}", attempt, pythonUrl, ex.getMessage());

                if (attempt < 2) {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        throw new RestClientException("Interrupted while retrying NLP request", interruptedException);
                    }
                }
            }
        }

        throw lastException;
    }

    private boolean isNavigationIntent(String intent, String intentType) {
        return startsWithIgnoreCase(intent, "navigation")
                || startsWithIgnoreCase(intentType, "navigation");
    }

    private boolean startsWithIgnoreCase(String value, String prefix) {
        return value != null && value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    @PostMapping("/update-node")
    public Map<String, Object> updateNode(@RequestBody Map<String, Object> position) {
        double lat = ((Number) position.get("latitude")).doubleValue();
        double lon = ((Number) position.get("longitude")).doubleValue();

        List<String> path = activePaths.get("user");
        return navigationService.computeNextNodeFromPosition(lat, lon, path);
    }
}
