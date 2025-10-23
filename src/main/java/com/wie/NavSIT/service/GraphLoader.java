package com.wie.NavSIT.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wie.NavSIT.model.CampusGraph;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

@Component
public class GraphLoader {

    private CampusGraph graph;

    @PostConstruct
    public void loadGraph() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        graph = mapper.readValue(new File("src/main/resources/campus_graph.json"), CampusGraph.class);
        System.out.println("Campus graph loaded! Nodes: " + graph.nodes.size() + ", Edges: " + graph.edges.size());
    }

    public CampusGraph getGraph() {
        return graph;
    }
}
