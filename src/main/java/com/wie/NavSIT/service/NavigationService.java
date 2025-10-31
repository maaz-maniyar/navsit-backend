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

        // 1. Find nearest node along path
        String nearestNode = null;
        double minDistance = Double.MAX_VALUE;
        for (String node : path) {
            double[] coords = graph.nodes.get(node);
            double distance = CampusGraph.haversine(lat, lon, coords[0], coords[1]);
            if (distance < minDistance) {
                minDistance = distance;
                nearestNode = node;
            }
        }

        result.put("currentNode", nearestNode);
        int idx = path.indexOf(nearestNode);

        // 2. If near the destination
        if (idx == path.size() - 1) {
            result.put("nextNode", null);
            result.put("nextCoordinates", null);
            result.put("reply", "You've reached your destination!");
            return result;
        }

        // 3. If the user is close enough (within ~15 meters), move to next node
        double[] nearestCoords = graph.nodes.get(nearestNode);
        double distanceToNearest = CampusGraph.haversine(lat, lon, nearestCoords[0], nearestCoords[1]);
        if (distanceToNearest < 15 && idx + 1 < path.size()) {
            nearestNode = path.get(idx + 1);
            idx++;
        }

        // 4. Define next node based on current index
        String nextNode = (idx + 1 < path.size()) ? path.get(idx + 1) : path.get(idx);
        result.put("nextNode", nextNode);
        result.put("nextCoordinates", graph.nodes.get(nextNode));
        result.put("reply", "Continue towards " + nextNode);

        return result;
    }

}

