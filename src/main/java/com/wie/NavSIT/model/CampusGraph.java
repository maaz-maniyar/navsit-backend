package com.wie.NavSIT.model;


import java.util.List;
import java.util.Map;


public class CampusGraph {


    // JSON mapped fields
    public Map<String, double[]> nodes; // nodeName -> [lat, lon]
    public List<List<String>> edges; // adjacency list as pairs


    // Internal stability tracking
    private String lastNearestNode = null;
    private static final double STABILITY_THRESHOLD = 15.0; // meters


    // Haversine distance calculator (meters)
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // radius of Earth in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }


    // Stable nearest node detection
    public String findNearestNode(double lat, double lon) {
        double minDistance = Double.MAX_VALUE;
        String nearestNode = null;


        if (nodes == null || nodes.isEmpty()) return null;


        for (Map.Entry<String, double[]> entry : nodes.entrySet()) {
            double[] coords = entry.getValue();
            double distance = haversine(lat, lon, coords[0], coords[1]);
            if (distance < minDistance) {
                minDistance = distance;
                nearestNode = entry.getKey();
            }
        }


        if (lastNearestNode != null && !lastNearestNode.equals(nearestNode)) {
            double[] lastCoords = nodes.get(lastNearestNode);
            if (lastCoords != null) {
                double lastDistance = haversine(lat, lon, lastCoords[0], lastCoords[1]);
                if (lastDistance - minDistance < STABILITY_THRESHOLD) {
                    nearestNode = lastNearestNode;
                }
            }
        }


        lastNearestNode = nearestNode;
        return nearestNode;
    }
}