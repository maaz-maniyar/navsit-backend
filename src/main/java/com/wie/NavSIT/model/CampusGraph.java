package com.wie.NavSIT.model;

import java.util.List;
import java.util.Map;

public class CampusGraph {
    public Map<String, double[]> nodes; // nodeName -> [lat, lon]
    public List<List<String>> edges;    // [[from, to], ...]
}
