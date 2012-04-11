package com.netease.automate.test.master;

import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

import junit.framework.TestCase;

public class TopologicalOrderTest extends TestCase {
    private static final String MASTER = "master";
    private static final String PROXY = "proxy";
    private static final String ROBOT = "robot";
    private static final String OPENFIRE = "openfire";

    public void testOrder() {
        DirectedGraph<String, DefaultEdge> graph = new SimpleDirectedGraph<String, DefaultEdge>(DefaultEdge.class);

        graph.addVertex(MASTER);
        graph.addVertex(PROXY);
        graph.addVertex(ROBOT);
        graph.addVertex(OPENFIRE);

        graph.addEdge(ROBOT, OPENFIRE);
        graph.addEdge(PROXY, OPENFIRE);
        graph.addEdge(ROBOT, MASTER);
        graph.addEdge(PROXY, MASTER);
        graph.addEdge(OPENFIRE, MASTER);
        
        CycleDetector<String, DefaultEdge> detector = new CycleDetector<String, DefaultEdge>(graph);
        
        if(detector.detectCycles()) {
            System.out.println("cycles detected");
        }

        TopologicalOrderIterator<String, DefaultEdge> iter = new TopologicalOrderIterator<String, DefaultEdge>(graph);

        while (iter.hasNext()) {
            System.out.println(iter.next());
        }

        Set<DefaultEdge> edgeSet = graph.incomingEdgesOf(MASTER);
        for (DefaultEdge e : edgeSet) {
            System.out.println(" <- " + graph.getEdgeSource(e));
        }
    }
}
