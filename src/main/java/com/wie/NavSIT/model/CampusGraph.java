package com.wie.NavSIT.model;

import java.util.List;
import java.util.Map;

public class CampusGraph {
    // nodeName -> [latitude, longitude]
    public Map<String, double[]> nodes;
    public List<List<String>> edges;

    /**
     * Finds the nearest node in the campus graph to the user's current GPS location.
     *
     * @param lat user's current latitude
     * @param lon user's current longitude
     * @return name of the nearest node
     */
    public String findNearestNode(double lat, double lon) {
        String nearest = null;
        double minDist = Double.MAX_VALUE;

        for (Map.Entry<String, double[]> entry : nodes.entrySet()) {
            double[] nodeCoords = entry.getValue();
            double nodeLat = nodeCoords[0];
            double nodeLon = nodeCoords[1];

            double dLat = Math.toRadians(nodeLat - lat);
            double dLon = Math.toRadians(nodeLon - lon);

            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                    + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(nodeLat))
                    * Math.sin(dLon / 2) * Math.sin(dLon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double distance = 6371 * c; // Earth radius in km

            if (distance < minDist) {
                minDist = distance;
                nearest = entry.getKey();
            }
        }

        return nearest;
    }
}
