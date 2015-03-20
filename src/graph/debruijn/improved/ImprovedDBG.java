package graph.debruijn.improved;
import interfaces.IGraph;

import java.io.File;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ImprovedDBG implements IGraph {
	protected ConcurrentHashMap<String, Node> nodeList;
	protected int kmerSize;
	
	protected ImprovedDBG() {
		super();
		nodeList = new ConcurrentHashMap<String, Node>();
	}
	
	protected Node addNode(String km1mer) {
		if (nodeList.containsKey(km1mer))
			return nodeList.get(km1mer);
		Node newNode = new Node(km1mer);
		nodeList.put(km1mer, newNode);
		return newNode;
	}
	
	protected DirectedEdge addEdge(String prefixString, String suffixString) {
		Node prefixNode, suffixNode;

		prefixNode = nodeList.get(prefixString);
		if (prefixNode == null)
			prefixNode = addNode(prefixString);

		suffixNode = nodeList.get(suffixString);
		if (suffixNode == null)
			suffixNode = addNode(suffixString);

		return prefixNode.addEdgeTo(suffixNode);
	}
	
	protected int getKmerSize() { return this.kmerSize; }
	protected void setKmerSize(int value) { this.kmerSize = value; }
	
	public void displayNodes() {
		System.out.println("Nodes (indeg, outdeg): ");
		for (Node node : nodeList.values()) {
			System.out.println(node.getKm1mer() + " (" + node.getIndegree() + ", " + node.getOutdegree() + ")");
		}
		System.out.println();
	}
	
	public void displayEdges() {
		System.out.println("Edges: ");
		for (Node node : nodeList.values()) {
			for (DirectedEdge edge : node.edgeList) {
				edge.print();
			}
		}
		System.out.println();
	}
	
	public void displayGraph() {
		System.out.println("Graph adjacency List:");
		for (Node node : nodeList.values()) {
			System.out.print(node.getKm1mer() + " --> ");
			for (DirectedEdge edge : node.edgeList) {
				System.out.print(edge.getEnd().getKm1mer() + "[" + edge.getWeight() + "], ");
			}
			System.out.println();
		}
		System.out.println();
	}
	
	protected DirectedEdge getUnvisitedEdge() {
		Iterator<Entry<String, Node>> iter = nodeList.entrySet().iterator();
        DirectedEdge edge;
        while(iter.hasNext()) {
            edge = iter.next().getValue().getUnvisitedEdge();
            if (edge != null)
            	return edge;
        }
		return null;
	}
	
	protected DirectedEdge getUnvisitedEdgeFromZeroIndegreeNode() {
		Iterator<Entry<String, Node>> iter = nodeList.entrySet().iterator();
        Node node;
        DirectedEdge edge;
        while(iter.hasNext()) {
            node = iter.next().getValue();
            if (node.getIndegree() == 0) {
            	edge = node.getUnvisitedEdge();
            	if (edge != null)
            		return edge;
            }
        }
		return null;
	}
	
	protected boolean isEulerian() {
    	@SuppressWarnings("unused")
		int nbal = 0, nsemi = 0, nneither = 0;
    	for (Node node : nodeList.values()) {
    		if (node.isBalanced())
    			nbal++;
    		else if (node.isSemiBalanced())
    			nsemi++;
    		else
    			nneither++;
    	}
    	if (nneither == 0 && nsemi == 2) // path
    		return true;
    	if (nneither == 0 && nsemi == 0) //cycle
    		return true;
    	return false;
    }
	
	protected void addKmersToGraph(String read) 
	{
		int kmerSize = this.getKmerSize();
		for (int i = 0; i < read.length() - kmerSize + 1; i++)
			this.addEdge(read.substring(i, i + kmerSize - 1), read.substring(i + 1, i + kmerSize));
	}
	
	public abstract void constructGraph(File readsFile, int kmerSize);
	public abstract void traverseGraphToGenerateContigs(String generatedContigsFile);
}
