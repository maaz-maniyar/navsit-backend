package com.wie.NavSIT.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wie.NavSIT.model.CampusGraph;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;

@Service
public class GraphLoader {

    private CampusGraph graph;

    @PostConstruct
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getClassLoader().getResourceAsStream("campus_graph.json");
            this.graph = mapper.readValue(is, CampusGraph.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CampusGraph getGraph() {
        return graph;
    }
}
