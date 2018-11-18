package simulator.topology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;

import simulator.simulators.Simulator;
import simulator.traffic.Flow;
import simulator.traffic.Task;
import simulator.utils.Parameters;

public abstract class Topology {
	public class Link {
		public double remainBand;
		public double price;
		public LinkedList<Flow> unallocFlows;
		public HashSet<Flow> unallocFlowsHash;
		public double rate;
		
		public Link() {
			price = 0;
//			unallocFlows = new LinkedList<Flow>();
//			unallocFlowsHash = new HashSet<Flow>();
		}
	}
	
	public class LinkComparator implements Comparator<Link> {
		public int compare(Link link1, Link link2) {
			link1.rate = link1.remainBand / link1.unallocFlows.size();
			link2.rate = link2.remainBand / link2.unallocFlows.size();
			return link1.rate > link2.rate ? 1 : (link1.rate == link2.rate ? 0 : -1);
		}
	}
	
	public final int[] linksPerLayer;
	
	public ArrayList<ArrayList<Link>> upLinks;
	public ArrayList<ArrayList<Link>> downLinks;
	
	public Topology() {
		linksPerLayer = new int[Simulator.config.layers];
	}
	
	public Topology(int[] childrenPerNode, double[] bandPerLayer) {
//		linksPerLayer = new int[Simulator.config.layers];
//		linksPerLayer[Simulator.config.layers-1] = childrenPerNode[Simulator.config.layers-1];
//		for (int i = Simulator.config.layers - 2; i >= 0; i--) {
//			linksPerLayer[i] = linksPerLayer[i+1] * childrenPerNode[i];
//		}
		linksPerLayer = Simulator.config.linksPerLayer;
		
		upLinks = new ArrayList<ArrayList<Link>>();
		downLinks = new ArrayList<ArrayList<Link>>();
		for (int i = 0; i < Simulator.config.layers; i++) {
			ArrayList<Link> layerLink = new ArrayList<Link>();
			for (int j = 0; j < linksPerLayer[i]; j++) {
				Link link = new Link();
				layerLink.add(link);
			}
			upLinks.add(layerLink);
			layerLink = new ArrayList<Link>();
			for (int j = 0; j < linksPerLayer[i]; j++) {
				Link link = new Link();
				layerLink.add(link);
			}
			downLinks.add(layerLink);
		}
	}

	public abstract void setRates(LinkedList<Flow> flows);
	
	public final void resetRemainBand() {
		for (int i = 0; i < linksPerLayer.length; i++) {
			for (int j = 0; j < linksPerLayer[i]; j++) {
				upLinks.get(i).get(j).remainBand = Simulator.config.bandPerLayer[i];
				downLinks.get(i).get(j).remainBand = Simulator.config.bandPerLayer[i];
			}
		}
	}
	
	public final void resetRemainBand(double band) {
		if (Simulator.config.layers != 1) {
			Simulator.output.error("using wrong bandwidth reset method");
		}
		for (int i = 0; i < Simulator.config.linksPerLayer[0]; i++) {
			upLinks.get(0).get(i).remainBand = band;
			downLinks.get(0).get(i).remainBand = band;
		}
	}
	
	public final void resetRemainBand(double[] bandPerLayer) {
		for (int i = 0; i < Simulator.config.layers; i++) {
			for (int j = 0; j < Simulator.config.linksPerLayer[i]; j++) {
				upLinks.get(i).get(j).remainBand = bandPerLayer[i];
				downLinks.get(i).get(j).remainBand = bandPerLayer[i];
			}
		}
	}
	
	public final void resetRemainBand(double[] upBand, double[] downBand) {
		if (Simulator.config.layers != 1) {
			Simulator.output.error("using wrong bandwodth reset method");
		}
		for (int i = 0; i < Simulator.config.linksPerLayer[0]; i++) {
			upLinks.get(0).get(i).remainBand = upBand[i];
			downLinks.get(0).get(i).remainBand = downBand[i];
		}
	}
	
	public final void resetRemainBand(double[][] upBand, double[][] downBand) {
		for (int i = 0; i < Simulator.config.layers; i++) {
			for (int j = 0; j < Simulator.config.linksPerLayer[i]; j++) {
				upLinks.get(i).get(j).remainBand = upBand[i][j];
				downLinks.get(i).get(j).remainBand = downBand[i][j];
			}
		}
	}
	public final void resetSharingFlows(LinkedList<Flow> flows) {
		for (int i = 0; i < linksPerLayer.length; i++) {
			for (int j = 0; j < linksPerLayer[i]; j++) {
				upLinks.get(i).get(j).unallocFlows = new LinkedList<Flow>();
				downLinks.get(i).get(j).unallocFlows = new LinkedList<Flow>();
			}
		}
		for (Flow flow : flows) {
			for (int i = 0; i < flow.hops; i++) {
				upLinks.get(i).get(flow.upLinkId[i]).unallocFlows.add(flow);
				downLinks.get(i).get(flow.downLinkId[i]).unallocFlows.add(flow);
			}
		}
	}
	
	public final void resetSharingFlowsHash(LinkedList<Flow> flows) {
		for (int i = 0; i < linksPerLayer.length; i++) {
			for (int j = 0; j < linksPerLayer[i]; j++) {
				upLinks.get(i).get(j).unallocFlowsHash = new HashSet<Flow>();
				downLinks.get(i).get(j).unallocFlowsHash = new HashSet<Flow>();
			}
		}
		for (Flow flow : flows) {
			for (int i = 0; i < flow.hops; i++) {
				upLinks.get(i).get(flow.upLinkId[i]).unallocFlowsHash.add(flow);
				downLinks.get(i).get(flow.downLinkId[i]).unallocFlowsHash.add(flow);
			}
		}
	}
	
	public final HashSet<Flow> getRelatedFlows(Flow flow) {
		HashSet<Flow> flows = new HashSet<Flow>();
		for (int i = 0; i < flow.hops; i++) {
			flows.addAll(upLinks.get(i).get(flow.upLinkId[i]).unallocFlows);
			flows.addAll(downLinks.get(i).get(flow.downLinkId[i]).unallocFlows);
		}
		return flows;
	}
	
	public final ArrayList<HashSet<Flow>> getRelatedFlowsByHop(Flow flow) {
		int hops = flow.hops;
		ArrayList<HashSet<Flow>> flows = new ArrayList<HashSet<Flow>>(hops * 2);
		for (int i = 0; i < hops; i++) {
			flows.add(new HashSet<Flow>(upLinks.get(i).get(flow.upLinkId[i]).unallocFlows));
			flows.add(new HashSet<Flow>(downLinks.get(i).get(flow.downLinkId[i]).unallocFlows));
		}
		return flows;
	}
	
	public final void initRelatedFlow(LinkedList<Flow> flows) {
		resetSharingFlows(Simulator.scheduler.getTraffic().getFlows());
		for (Flow flow : flows) {
			flow.initRelatedFlow();
		}
	}
	
	public final int getHopId(int hop, boolean isUp) {
		return isUp ? hop : hop + Simulator.config.layers;
	}
	
	static public final boolean checkRateValidityTask(LinkedList<Task> tasks) {
		double[][] upRate = new double[Simulator.config.layers][];
		double[][] downRate = new double[Simulator.config.layers][];
		for (int i = 0; i < Simulator.config.layers; i++) {
			upRate[i] = new double[Simulator.config.linksPerLayer[i]];
			Arrays.fill(upRate[i], 0);
			downRate[i] = new double[Simulator.config.linksPerLayer[i]];
			Arrays.fill(downRate[i], 0);
		}
		for (Task task : tasks) {
			for (Flow flow : task.getFlows()) {
				for (int i = 0; i < Simulator.config.layers; i++) {
					upRate[i][flow.upLinkId[i]] += flow.getRate();
					downRate[i][flow.downLinkId[i]] += flow.getRate();
				}
			}
		}
		for (int i = 0; i < Simulator.config.layers; i++) {
			for (int j = 0; j < Simulator.config.linksPerLayer[i]; j++) {
				if (upRate[i][j] - Simulator.config.bandPerLayer[i] > Parameters.RATE_GRANULARITY) {
					//System.out.println("up link layer " + i + " number " + j + " allocated rate " + upRate[i][j]);
					return false;
				}
				if (downRate[i][j] - Simulator.config.bandPerLayer[i] > Parameters.RATE_GRANULARITY) {
					//System.out.println("down link layer " + i + " number " + j + " allocated rate " + downRate[i][j]);
					return false;
				}
			}
		}
		return true;
	}
	
	protected abstract double getAvailBand(Flow flow);
	
	protected abstract void updateRemainBand(Flow flow, double allocatedRate);
}