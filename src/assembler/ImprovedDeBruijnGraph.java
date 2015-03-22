package assembler;

import interfaces.IGraph;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import graph.debruijn.improved.*;

public class ImprovedDeBruijnGraph extends ImprovedDBG implements IGraph{
	public ImprovedDeBruijnGraph()
	{
		super();
	}
	
	public void constructGraph(File readsFile, int kmerSize) 
	{
		try (Scanner fileIn = new Scanner(readsFile)) {
			String currentLine = "";
			StringBuilder read = new StringBuilder();
			int readCount = 0;
			
			this.setKmerSize(kmerSize);
			System.out.println("kmer size: " + this.getK());
			
			while (fileIn.hasNextLine()) {
				currentLine = fileIn.nextLine();
				
				if (currentLine.startsWith(">")) {
					if (!read.toString().equals("")) {
						breakReadIntoKmersAndAddToGraph(read.toString().toUpperCase());
						readCount++;
					}
					read = new StringBuilder();
				}
				else
					read.append(currentLine.trim());
			}
			
			if (!read.toString().equals("")) {
				breakReadIntoKmersAndAddToGraph(read.toString().toUpperCase());
				readCount++;
			}
			System.out.println("Number of reads processed: " + readCount);
		}
		catch (FileNotFoundException e) {
			System.err.println("File not found: " + readsFile);
		}
	}
	
	public void traverseGraphToGenerateContigs(String generatedContigsFile)
	{
		DirectedEdge unvisitedEdge = null;
		Node node;
		BufferedWriter writer;
		ExecutorService es;
		boolean finished;
		int noOfConcurrentThreads;
		Iterator<Entry<String, Node>> nodeListMappingElements;
		try {
			writer = new BufferedWriter(new FileWriter(new File(generatedContigsFile)));
			new WriterThread(writer, this.getK());
			noOfConcurrentThreads = Runtime.getRuntime().availableProcessors();;
			
			System.out.println("Number of concurrent threads: "+noOfConcurrentThreads);
			es = Executors.newFixedThreadPool(noOfConcurrentThreads);
			
			//first loop through nodeListMappings to find unvisited edges from zero in-degree nodes
			nodeListMappingElements = nodeList.entrySet().iterator();
	        while(nodeListMappingElements.hasNext()) {
	            node = nodeListMappingElements.next().getValue();
	            if (node.getIndegree() == 0) {
	            	while (true) {
	            		unvisitedEdge = node.getUnvisitedEdge(unvisitedEdge);
	            		if (unvisitedEdge != null)
	            			es.execute(new TraversalThread(unvisitedEdge));
	            		else
	            			break;
	            	}
	            }
	        }
			
	        //second loop through nodeListMappings to find remaining unvisited edges
	        nodeListMappingElements = nodeList.entrySet().iterator();
	        while(nodeListMappingElements.hasNext()) {
	            node = nodeListMappingElements.next().getValue();
	            unvisitedEdge = node.getUnvisitedEdge();
            	if (unvisitedEdge != null)
            		es.execute(new TraversalThread(unvisitedEdge));
	        }
	        
			es.shutdown();
			
			try {
				finished = es.awaitTermination(3, TimeUnit.MINUTES);
				if(!finished)
					System.err.println("ImprovedDeBruijnGraph:generateContigs: a traversal thread's timeout elapsed before finishing execution");
			} catch (InterruptedException e) {
				System.err.println("ImprovedDeBruijnGraph:generateContigs: thread executer interrupted while waiting termination of traversal thread");
			}
			
			System.out.println("Number of contigs generated: " + WriterThread.getInstance().getContigCount());
			writer.close();
		} catch (IOException e) {
			System.err.println("ImprovedDeBruijnGraph:generateContigs: file "+generatedContigsFile+" cannot be created or opened");
		}
	}
}