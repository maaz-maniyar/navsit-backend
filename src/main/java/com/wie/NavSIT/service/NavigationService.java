package com.wie.NavSIT.service;


import com.wie.NavSIT.model.CampusGraph;
import org.springframework.stereotype.Service;


import java.util.*;


@Service
public class NavigationService {


    private final GraphLoader loader;


    public NavigationService(GraphLoader loader) {
        this.loader = loader;
    }


    private Map<String, List<String>> buildAdjacency(CampusGraph graph) {
        Map<String, List<String>> adj = new HashMap<>();
        if (graph.edges == null) return adj;


        for (List<String> edge : graph.edges) {
            if (edge.size() == 2) {
                adj.computeIfAbsent(edge.get(0), k -> new ArrayList<>()).add(edge.get(1));
                adj.computeIfAbsent(edge.get(1), k -> new ArrayList<>()).add(edge.get(0));
            }
        }
        return adj;
    }
    // Dijkstra using haversine weights
    public List<String> shortestPath(String from, String to) {
        CampusGraph graph = loader.getGraph();
        if (graph == null || graph.nodes == null) return null;
        if (!graph.nodes.containsKey(from) || !graph.nodes.containsKey(to)) return null;


        Map<String, List<String>> adj = buildAdjacency(graph);


// distances and previous
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingDouble(dist::get));


        for (String node : graph.nodes.keySet()) {
            dist.put(node, Double.POSITIVE_INFINITY);
            prev.put(node, null);
        }
        dist.put(from, 0.0);
        pq.add(from);


        while (!pq.isEmpty()) {
            String u = pq.poll();
            if (u.equals(to)) break;


            List<String> neighbors = adj.getOrDefault(u, new ArrayList<>());
            for (String v : neighbors) {
                double[] cu = graph.nodes.get(u);
                double[] cv = graph.nodes.get(v);
                if (cu == null || cv == null) continue;
                double w = CampusGraph.haversine(cu[0], cu[1], cv[0], cv[1]);
                double alt = dist.get(u) + w;
                if (alt < dist.get(v)) {
                    dist.put(v, alt);
                    prev.put(v, u);
                    pq.remove(v); // reinsert to update priority
                    pq.add(v);
                }
            }
        }
        if (prev.get(to) == null && !from.equals(to)) return null;


        List<String> path = new ArrayList<>();
        String cur = to;
        while (cur != null) {
            path.add(cur);
            cur = prev.get(cur);
        }
        Collections.reverse(path);
        return path;
    }


    // Return coordinates list for a named path
    public List<double[]> pathToCoordinates(List<String> path) {
        CampusGraph graph = loader.getGraph();
        if (path == null) return null;
        List<double[]> coords = new ArrayList<>();
        for (String node : path) {
            coords.add(graph.nodes.get(node));
        }
        return coords;
    }
    // Determine next node given current lat/lon and full path
    public Map<String, Object> computeNextNodeFromPosition(double lat, double lon, List<String> path) {
        CampusGraph graph = loader.getGraph();
        Map<String, Object> result = new HashMap<>();
        if (path == null || path.isEmpty()) return result;


        String nearest = graph.findNearestNode(lat, lon);
        result.put("currentNode", nearest);


        int idx = -1;
        if (nearest != null) idx = path.indexOf(nearest);


        if (idx == -1) {
// Not exactly on node — return next path node (1st remaining)
            result.put("nextNode", path.size() > 1 ? path.get(1) : path.get(0));
            result.put("nextCoordinates", graph.nodes.get(result.get("nextNode")));
            result.put("reply", "Continue toward " + result.get("nextNode"));
        } else if (idx < path.size() - 1) {
// Trim path and return next
            List<String> remaining = path.subList(idx, path.size());
            result.put("remainingPath", remaining);
            result.put("nextNode", remaining.size() > 1 ? remaining.get(1) : null);
            result.put("nextCoordinates", remaining.size() > 1 ? graph.nodes.get(remaining.get(1)) : null);
            result.put("reply", remaining.size() > 1 ? "Proceeding to next: " + remaining.get(1) : "Reached destination");
        } else {
            result.put("nextNode", null);
            result.put("nextCoordinates", null);
            result.put("reply", "You\'ve reached your destination!");
        }
        return result;
    }
}

