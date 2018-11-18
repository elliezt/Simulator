package simulator.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

import simulator.simulators.Simulator;
import simulator.traffic.Flow;

public class Config {
	public String trafficPath;
	public int layers;
	public int[] childrenPerNode;
	public int[] nodesPerLayer;
	public double[] bandPerLayer;
	public int numOfHosts;
	public final int[] linksPerLayer;
	public int[] hostsPerLayer;
	public int[] idCountFactor;
	public Random random;
	
	public Config(String path) {
		random = new Random(1);
		trafficPath = path;
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(path+"config"));
			String s;
			while ((s = bufferedReader.readLine()) != null) {
				String[] args = s.split(" ");
				if (args[0].equalsIgnoreCase("children_per_node")) {
					childrenPerNode = new int[args.length - 1];
					for (int i = 0; i < args.length - 1; i++) {
						childrenPerNode[i] = new Integer(args[i+1]);
					}
				}
				else if (args[0].equalsIgnoreCase("band_per_layer")) {
					bandPerLayer = new double[args.length - 1];
					for (int i = 0; i < args.length - 1; i++) {
						bandPerLayer[i] = new Double(args[i+1]);
//						System.out.println(bandPerLayer[i]);
					}
				}
				else if (args[0].equalsIgnoreCase("nodes_per_layer")) {
					nodesPerLayer = new int[args.length - 1];
					for (int i = 0; i < args.length - 1; i++) {
						nodesPerLayer[i] = new Integer(args[i+1]);
					}
				}
			}
			layers = childrenPerNode.length;
			if (childrenPerNode.length != bandPerLayer.length) {
				Simulator.output.error("Topology in config has error!!!");
			}
			numOfHosts = childrenPerNode[0] * nodesPerLayer[1];
//			for (int i = 1; i < childrenPerNode.length; i++) {
//				numOfHosts *= childrenPerNode[i];
//			}
			bufferedReader.close();
		} catch (IOException e) {
			Simulator.output.error("config file reading exception", e);
		}
		linksPerLayer = new int[childrenPerNode.length];
//		linksPerLayer[childrenPerNode.length-1] = childrenPerNode[childrenPerNode.length-1];
//		for (int i = childrenPerNode.length - 2; i >= 0; i--) {
//			linksPerLayer[i] = linksPerLayer[i+1] * childrenPerNode[i];
//		}
		for (int i = childrenPerNode.length - 1; i >= 0; i--) {
			linksPerLayer[i] = childrenPerNode[i] * nodesPerLayer[i+1]; 
		}
		hostsPerLayer = new int[childrenPerNode.length];
		hostsPerLayer[0] = childrenPerNode[0];
		for (int i = 1; i < childrenPerNode.length; i++) {
//			hostsPerLayer[i] = hostsPerLayer[i-1] * childrenPerNode[i];
			hostsPerLayer[i] = hostsPerLayer[i-1] * childrenPerNode[i] * nodesPerLayer[i-1] / nodesPerLayer[i] / childrenPerNode[i-1];
		}
		idCountFactor = new int[childrenPerNode.length];
		idCountFactor[0] = 1;
		idCountFactor[1] = childrenPerNode[0];
		for (int i = 2; i < childrenPerNode.length; i++) {
			idCountFactor[i] = idCountFactor[i-1] * childrenPerNode[i-1] * nodesPerLayer[i-2] / nodesPerLayer[i-1] / childrenPerNode[i-2];
		}
	}

	public int getFlowHops(Flow flow) {
		int flowHops = 0;
		while (flow.src/hostsPerLayer[flowHops] != flow.dst/hostsPerLayer[flowHops]) {
			flowHops++;
		}
		return flowHops+1;
	}
	
	public int getLinkId(int host, int hop) {
		return host/idCountFactor[hop];
	}
	//for leaf-spine topology
	public int getUpLinkId(int host, int hop, int lowHopId) {
			int lowLinksPerNode = nodesPerLayer[hop] * childrenPerNode[hop-1] / nodesPerLayer[hop-1];
			int highLinksPerNode = nodesPerLayer[hop+1] * childrenPerNode[hop] / nodesPerLayer[hop];
			int min = (host / idCountFactor[hop]) * lowLinksPerNode * highLinksPerNode;
			min = min + (lowHopId % lowLinksPerNode) * highLinksPerNode;
			int max = min + highLinksPerNode;
			return random.nextInt(max-min) + min; 
//			return min + flowId % highLinksPerNode;
	}
	
	public int getDownLinkId(int host, int hop, int lowHopId) {
			int lowLinksPerNode = nodesPerLayer[hop] * childrenPerNode[hop-1] / nodesPerLayer[hop-1];
			int highLinksPerNode = nodesPerLayer[hop+1] * childrenPerNode[hop] / nodesPerLayer[hop];
			int min = (host / idCountFactor[hop]) * lowLinksPerNode * highLinksPerNode;
			min = min + (lowHopId % lowLinksPerNode) * highLinksPerNode;
			int max = min + highLinksPerNode;
			//ECMP
			return random.nextInt(max-min) + min; 
//			return min + flowId % highLinksPerNode;
	}
	
	public int getHighestDownLinkId(int host, int hop, int lowHopId, int lastHopId) {
			int lowLinksPerNode = nodesPerLayer[hop] * childrenPerNode[hop-1] / nodesPerLayer[hop-1];
			int highLinksPerNode = nodesPerLayer[hop+1] * childrenPerNode[hop] / nodesPerLayer[hop];
			int min = (host / idCountFactor[hop]) * lowLinksPerNode * highLinksPerNode;
//			System.out.println(host / idCountFactor[hop]);
			min = min + (lowHopId % lowLinksPerNode) * highLinksPerNode;
			return min + lastHopId % highLinksPerNode;
	}
}