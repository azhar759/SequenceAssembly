package DeBruijnGraph;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.Scanner;

public class DebruijnGraphStaticMethods
{
	public static void constructGraph(File readsFile, int kmerSize) 
	{
		try (Scanner fileIn = new Scanner(readsFile)) 
		{
			String currentLine = "";
			StringBuilder read = new StringBuilder();
			int readCount = 0;
			
			new DeBruijnGraph();
			DeBruijnGraph.getInstance().setKmerSize(kmerSize);
			System.out.println("kmer size: " + DeBruijnGraph.getInstance().getKmerSize());
			
			while (fileIn.hasNextLine())
			{
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
	
	public static void breakReadIntoKmersAndAddToGraph(String read) 
	{
		int kmerSize = DeBruijnGraph.getInstance().getKmerSize();
		for (int i = 0; i < read.length() - kmerSize + 1; i++)
			DeBruijnGraph.getInstance().addEdge(read.substring(i, i + kmerSize - 1), read.substring(i + 1, i + kmerSize));
	}
	
	public static void generateContigs(String OUTPUT_FILE)
	{
		DeBruijnGraph graph;
		LinkedList<DirectedEdge> contigEdgeList = new LinkedList<DirectedEdge>();
		DirectedEdge unvisitedEdge;
		BufferedWriter writer;
		int contigCount = 0;
		
		try
		{
			graph = DeBruijnGraph.getInstance();
			writer = new BufferedWriter(new FileWriter(new File(OUTPUT_FILE)));
			
			while (true)
			{
				unvisitedEdge = graph.getZeroInDegreeUnvisitedEdge();
				if(unvisitedEdge==null)
					unvisitedEdge = graph.getUnvisitedEdge();
				
				if(unvisitedEdge==null)
					break;
				else
				{
					DirectedEdge edge = unvisitedEdge;
					while (edge!=null)
					{
						edge.setVisited(true);
						contigEdgeList.add(edge);
						edge = graph.getUnvisitedEdge(edge.getEnd());
					}
					
					if(!contigEdgeList.isEmpty())
					{
						contigCount++;
						
						writer.write(contigEdgeList.getFirst().getKmer());
						for(int i=1; i<contigEdgeList.size(); i++)
							writer.write(contigEdgeList.get(i).getEnd().getKm1mer().charAt(graph.getKmerSize()-2));
						
						writer.flush();
						writer.write("\n");
					}
					
					while (!contigEdgeList.isEmpty())
					{
						edge = contigEdgeList.removeLast();
						edge = graph.getUnvisitedEdge(edge.getStart());
						if(edge!=null)
						{
							while (edge!=null) 
							{
								edge.setVisited(true);
								contigEdgeList.add(edge);
								edge = graph.getUnvisitedEdge(edge.getEnd());
							}
							
							if(!contigEdgeList.isEmpty())
							{
								contigCount++;
								writer.write(">c" + contigCount + ".1_EdgeCount_"+ contigEdgeList.size() +"\n");
								writer.write(contigEdgeList.getFirst().getKmer());
								for(int i=1; i<contigEdgeList.size(); i++)
									writer.write(contigEdgeList.get(i).getEnd().getKm1mer().charAt(graph.getKmerSize()-2));

								writer.flush();
								writer.write("\n");
							}
						}
					}
				}
			}
			writer.close();
			System.out.println("Number of contigs generated: "+contigCount);
		}
		catch (Exception e) {
			System.err.println("File not found: " + OUTPUT_FILE);
		}
	}
}
