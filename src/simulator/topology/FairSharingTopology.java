package simulator.topology;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;

import simulator.simulators.Simulator;
import simulator.traffic.Flow;
import simulator.traffic.Flow.FlowComparator;
import simulator.utils.Parameters;
import simulator.utils.Utils;

public class FairSharingTopology extends SharingTopology {
	public FairSharingTopology(int[] childrenPerNode, double[] bandPerLayer, int[] nodesPerLayer) {
		super(childrenPerNode, bandPerLayer, nodesPerLayer);
	}
	
	public void setRates(LinkedList<Flow> flows) {
		resetSharingFlows(flows);
		for (Flow flow : flows) {
			flow.setRate(getAvailBand(flow));
		}
		int initSize = flows.size() > 0 ? flows.size() : 1;
		PriorityQueue<Flow> unallocFlows = new PriorityQueue<Flow>(initSize, new Flow.FlowComparator(FlowComparator.FLOW_COMPARATOR_TYPE.RATE));
		unallocFlows.addAll(flows);
		while (!unallocFlows.isEmpty()) {
			updateRates(unallocFlows.poll());
		}
	}
	
	public void setRatesSearch(LinkedList<Flow> flows) {
		resetSharingFlows(flows);
		for (Flow flow : flows) {
			flow.setRate(getAvailBand(flow));
		}
		LinkedList<Flow> unallocFlows = new LinkedList<Flow>(flows);
		while (!unallocFlows.isEmpty()) {
			double minRate = Double.MAX_VALUE;
			for (Flow flow : unallocFlows) {
				if (flow.getRate() < minRate) {
					minRate = flow.getRate();
				}
			}
			for (Iterator<Flow> iter = unallocFlows.iterator(); iter.hasNext(); ) {
				Flow flow = iter.next();
				if (flow.getRate() == minRate) {
					iter.remove();
					updateRates(flow);
				}
			}
		}
	}
	
	public void setRatesSearchImproved(LinkedList<Flow> flows) {
		resetSharingFlows(flows);
		for (Flow flow : flows) {
			flow.setRate(getAvailBand(flow));
		}
		LinkedList<Flow> unallocFlows = new LinkedList<Flow>(flows);
		Collections.sort(unallocFlows, new Flow.FlowComparator(FlowComparator.FLOW_COMPARATOR_TYPE.RATE));
		while (!unallocFlows.isEmpty()) {
			double minRate = Double.MAX_VALUE;
			for (Flow flow : unallocFlows) {
				if (flow.getRate() < minRate) {
					minRate = flow.getRate();
				}
			}
			for (Iterator<Flow> iter = unallocFlows.iterator(); iter.hasNext(); ) {
				Flow flow = iter.next();
				if (flow.getRate() == minRate) {
					iter.remove();
					updateRates(flow);
				}
			}
		}
	}
	
	public void setRatesLink(LinkedList<Flow> flows) {
		LinkedList<Link> unallocLinks = resetSharingFlowsLink(flows);
		for (Flow flow : flows) {
			flow.setRate(getAvailBand(flow));
		}
		while (!unallocLinks.isEmpty()) {
			Collections.sort(unallocLinks, new LinkComparator());
			Link link = unallocLinks.poll();
			LinkedList<Flow> allocFlows = new LinkedList<Flow>(link.unallocFlows);
			for (Flow flow : allocFlows) {
				updateRemainBand(flow, flow.getRate());
			}
			double minRate = link.rate;
			while (!unallocLinks.isEmpty() && unallocLinks.peek().rate - minRate < Parameters.RATE_GRANULARITY) {
				link = unallocLinks.poll();
				LinkedList<Flow> nextAllocFlows = new LinkedList<Flow>(link.unallocFlows);
				allocFlows.addAll(link.unallocFlows);
				for (Flow flow : nextAllocFlows) {
					updateRemainBand(flow, flow.getRate());
				}
			}
			for (Flow flow : allocFlows) {
				updateRelatedFlows(flow);
			}
		}
	}
	
	public LinkedList<Link> resetSharingFlowsLink(LinkedList<Flow> flows) {
		LinkedList<Link> unallocLinks = new LinkedList<Link>();
		for (int i = 0; i < linksPerLayer.length; i++) {
			for (int j = 0; j < linksPerLayer[i]; j++) {
				upLinks.get(i).get(j).unallocFlows = new LinkedList<Flow>();
				downLinks.get(i).get(j).unallocFlows = new LinkedList<Flow>();
			}
		}
		for (Flow flow : flows) {
			for (int i = 0; i < flow.hops; i++) {
				if (upLinks.get(i).get(flow.upLinkId[i]).unallocFlows.isEmpty()) {
					unallocLinks.add(upLinks.get(i).get(flow.upLinkId[i]));
				}
				upLinks.get(i).get(flow.upLinkId[i]).unallocFlows.add(flow);
				if (downLinks.get(i).get(flow.downLinkId[i]).unallocFlows.isEmpty()) {
					unallocLinks.add(downLinks.get(i).get(flow.downLinkId[i]));
				}
				downLinks.get(i).get(flow.downLinkId[i]).unallocFlows.add(flow);
			}
		}
		return unallocLinks;
	}
	
	protected double getAvailBand(Flow flow) {
		double availBand = Double.MAX_VALUE;
		for (int i = 0; i < flow.hops; i++) {
			Link link = upLinks.get(i).get(flow.upLinkId[i]);
			double remain = link.remainBand / link.unallocFlows.size();
			if (remain < availBand) {
				availBand = remain;
			}
			link = downLinks.get(i).get(flow.downLinkId[i]);
			remain = link.remainBand / link.unallocFlows.size();
			if (remain < availBand) {
				availBand = remain;
			}
		}
		availBand = Utils.checkRate(availBand);
		if (availBand < 0) {
			Simulator.output.error("fair sharing topology alloc error");
		}
		return availBand;
	}
}