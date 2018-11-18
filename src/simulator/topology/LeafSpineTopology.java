package simulator.topology;

import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.HashSet;
//import java.util.LinkedList;

import simulator.simulators.Simulator;
//import simulator.traffic.Flow;

public abstract class LeafSpineTopology extends Topology{
//	public class Link {
//		public double remainBand;
//		public LinkedList<Flow> unallocFlows;
//		public HashSet<Flow> unallocFlowsHash;
//		public double rate;
//	}
//	
//	public class LinkComparator implements Comparator<Link> {
//		public int compare(Link link1, Link link2) {
//			link1.rate = link1.remainBand / link1.unallocFlows.size();
//			link2.rate = link2.remainBand / link2.unallocFlows.size();
//			return link1.rate > link2.rate ? 1 : (link1.rate == link2.rate ? 0 : -1);
//		}
//	}
//	
//	public final int[] linksPerLayer;
//	
//	public ArrayList<ArrayList<Link>> upLinks;
//	public ArrayList<ArrayList<Link>> downLinks;
	//leaf-spine topology 
	public LeafSpineTopology(int[] childrenPerNode, double[] bandPerLayer, int[] nodesPerLayer) {
//		linksPerLayer = new int[Simulator.config.layers];
		super();
		for (int i = Simulator.config.layers - 1; i >= 0; i--) {
			linksPerLayer[i] = nodesPerLayer[i+1] * childrenPerNode[i];
		}	
		upLinks = new ArrayList<ArrayList<Link>>();
		downLinks = new ArrayList<ArrayList<Link>>();
		for (int i = 0; i < Simulator.config.layers; i++) {
			ArrayList<Link> layerLink = new ArrayList<Link>();
			for (int j = 0; j < linksPerLayer[i]; j++) {
				Link link = new Link();
				link.price = 0.1;
				layerLink.add(link);
			}
			upLinks.add(layerLink);
			layerLink = new ArrayList<Link>();
			for (int j = 0; j < linksPerLayer[i]; j++) {
				Link link = new Link();
				link.price = 0.1;
				layerLink.add(link);
			}
			downLinks.add(layerLink);
		}
	}

//	public abstract void setRates(LinkedList<Flow> flows);
	
//	public final void resetRemainBand() {
//		for (int i = 0; i < linksPerLayer.length; i++) {
//			for (int j = 0; j < linksPerLayer[i]; j++) {
//				upLinks.get(i).get(j).remainBand = Simulator.config.bandPerLayer[i];
//				downLinks.get(i).get(j).remainBand = Simulator.config.bandPerLayer[i];
//			}
//		}
//	}
//	
//	public final void resetSharingFlows(LinkedList<Flow> flows) {
//		for (int i = 0; i < linksPerLayer.length; i++) {
//			for (int j = 0; j < linksPerLayer[i]; j++) {
//				upLinks.get(i).get(j).unallocFlows = new LinkedList<Flow>();
//				downLinks.get(i).get(j).unallocFlows = new LinkedList<Flow>();
//			}
//		}
//		for (Flow flow : flows) {
//			for (int i = 0; i < flow.hops; i++) {
//				upLinks.get(i).get(flow.upLinkId[i]).unallocFlows.add(flow);
//				downLinks.get(i).get(flow.downLinkId[i]).unallocFlows.add(flow);
//			}
//		}
//	}
//	
//	public final void resetSharingFlowsHash(LinkedList<Flow> flows) {
//		for (int i = 0; i < linksPerLayer.length; i++) {
//			for (int j = 0; j < linksPerLayer[i]; j++) {
//				upLinks.get(i).get(j).unallocFlowsHash = new HashSet<Flow>();
//				downLinks.get(i).get(j).unallocFlowsHash = new HashSet<Flow>();
//			}
//		}
//		for (Flow flow : flows) {
//			for (int i = 0; i < flow.hops; i++) {
//				upLinks.get(i).get(flow.upLinkId[i]).unallocFlowsHash.add(flow);
//				downLinks.get(i).get(flow.downLinkId[i]).unallocFlowsHash.add(flow);
//			}
//		}
//	}
//	
//	public final HashSet<Flow> getRelatedFlows(Flow flow) {
//		HashSet<Flow> flows = new HashSet<Flow>();
//		for (int i = 0; i < flow.hops; i++) {
//			flows.addAll(upLinks.get(i).get(flow.upLinkId[i]).unallocFlows);
//			flows.addAll(downLinks.get(i).get(flow.downLinkId[i]).unallocFlows);
//		}
//		return flows;
//	}
//	
//	public final ArrayList<HashSet<Flow>> getRelatedFlowsByHop(Flow flow) {
//		int hops = flow.hops;
//		ArrayList<HashSet<Flow>> flows = new ArrayList<HashSet<Flow>>(hops * 2);
//		for (int i = 0; i < hops; i++) {
//			flows.add(new HashSet<Flow>(upLinks.get(i).get(flow.upLinkId[i]).unallocFlows));
//			flows.add(new HashSet<Flow>(downLinks.get(i).get(flow.downLinkId[i]).unallocFlows));
//		}
//		return flows;
//	}
//	
//	public final void initRelatedFlow(LinkedList<Flow> flows) {
//		resetSharingFlows(Simulator.scheduler.getTraffic().getFlows());
//		for (Flow flow : flows) {
//			flow.initRelatedFlow();
//		}
//	}
//	
//	public final int getHopId(int hop, boolean isUp) {
//		return isUp ? hop : hop + Simulator.config.layers;
//	}
//	
//	protected abstract double getAvailBand(Flow flow);
//	
//	protected abstract void updateRemainBand(Flow flow, double allocatedRate);
}