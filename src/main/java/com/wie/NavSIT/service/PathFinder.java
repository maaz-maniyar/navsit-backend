package com.wie.NavSIT.service;

import com.wie.NavSIT.model.CampusGraph;

import java.util.*;

public class PathFinder {

    // Calculate Euclidean distance between two coordinates
    private static double distance(double[] a, double[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        return Math.sqrt(dx*dx + dy*dy);
    }

    // Dijkstra algorithm to find shortest path
    public static List<double[]> findPath(CampusGraph graph, String start, String end) {
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Set<String> visited = new HashSet<>();

        for (String node : graph.nodes.keySet()) dist.put(node, Double.MAX_VALUE);
        dist.put(start, 0.0);

        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingDouble(dist::get));
        pq.add(start);

        // Build adjacency map
        Map<String, List<String>> adj = new HashMap<>();
        for (List<String> edge : graph.edges) {
            adj.computeIfAbsent(edge.get(0), k -> new ArrayList<>()).add(edge.get(1));
            adj.computeIfAbsent(edge.get(1), k -> new ArrayList<>()).add(edge.get(0));
        }

        while (!pq.isEmpty()) {
            String current = pq.poll();
            if (visited.contains(current)) continue;
            visited.add(current);

            if (current.equals(end)) break;

            for (String neighbor : adj.getOrDefault(current, new ArrayList<>())) {
                double alt = dist.get(current) + distance(graph.nodes.get(current), graph.nodes.get(neighbor));
                if (alt < dist.get(neighbor)) {
                    dist.put(neighbor, alt);
                    prev.put(neighbor, current);
                    pq.add(neighbor);
                }
            }
        }

        // Reconstruct path
        List<double[]> path = new ArrayList<>();
        String u = end;
        if (!prev.containsKey(u) && !u.equals(start)) return path; // no path found

        while (u != null) {
            path.add(graph.nodes.get(u));
            u = prev.get(u);
        }
        Collections.reverse(path);
        return path;
    }
}
