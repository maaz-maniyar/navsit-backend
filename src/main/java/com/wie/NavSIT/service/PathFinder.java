package com.wie.NavSIT.service;

import com.wie.NavSIT.model.CampusGraph;

import java.util.*;

public class PathFinder {

    /**
     * Finds path by node names (used for dynamic updates).
     */
    public static List<String> findPathByName(CampusGraph graph, String from, String to) {
        Map<String, List<String>> adj = buildAdjacencyList(graph);
        Map<String, String> prev = new HashMap<>();
        Queue<String> q = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        q.add(from);
        visited.add(from);

        while (!q.isEmpty()) {
            String current = q.poll();
            if (current.equals(to)) break;

            for (String neighbor : adj.getOrDefault(current, new ArrayList<>())) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    prev.put(neighbor, current);
                    q.add(neighbor);
                }
            }
        }

        if (!prev.containsKey(to)) return null;

        List<String> path = new ArrayList<>();
        String curr = to;
        while (curr != null) {
            path.add(curr);
            curr = prev.get(curr);
        }
        Collections.reverse(path);
        return path;
    }

    /**
     * For coordinate paths (existing code kept intact).
     */
    public static List<double[]> findPath(CampusGraph graph, String from, String to) {
        List<String> path = findPathByName(graph, from, to);
        if (path == null) return null;

        List<double[]> coords = new ArrayList<>();
        for (String node : path) {
            coords.add(graph.nodes.get(node));
        }
        return coords;
    }

    private static Map<String, List<String>> buildAdjacencyList(CampusGraph graph) {
        Map<String, List<String>> adj = new HashMap<>();
        for (List<String> edge : graph.edges) {
            if (edge.size() == 2) {
                adj.computeIfAbsent(edge.get(0), k -> new ArrayList<>()).add(edge.get(1));
                adj.computeIfAbsent(edge.get(1), k -> new ArrayList<>()).add(edge.get(0));
            }
        }
        return adj;
    }
}
