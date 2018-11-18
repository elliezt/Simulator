package simulator.topology;

import java.util.LinkedList;
import java.util.PriorityQueue;

import simulator.traffic.Flow;
import simulator.traffic.Flow.FlowComparator;

public class ProportionalSharingTopology extends SharingTopology {
	public static enum PROPORTION_TYPE {
		SIZE,
		REMAIN_SIZE,
	}
	
	private PROPORTION_TYPE proportionType;
	
	public ProportionalSharingTopology(int[] childrenPerNode, double[] bandPerLayer, int[] nodesPerLayer, PROPORTION_TYPE type) {
		super(childrenPerNode, bandPerLayer, nodesPerLayer);
		proportionType = type;
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
	
	protected double getAvailBand(Flow flow) {
		double availBand = Double.MAX_VALUE;
		for (int i = 0; i < flow.hops; i++) {
			Link link = upLinks.get(i).get(flow.upLinkId[i]);
			double remain = 0;
			double weightedSum = 0;
			for (Flow relatedFlow : link.unallocFlows) {
				if (proportionType == PROPORTION_TYPE.SIZE) {
					weightedSum += relatedFlow.size;
				}
				else if (proportionType == PROPORTION_TYPE.REMAIN_SIZE) {
					weightedSum += relatedFlow.getReaminingSize();
				}
			}
			if (proportionType == PROPORTION_TYPE.SIZE) {
				remain = link.remainBand / weightedSum * flow.size;
			}
			else if (proportionType == PROPORTION_TYPE.REMAIN_SIZE) {
				remain = link.remainBand / weightedSum * flow.getReaminingSize();
			}
			if (remain < availBand) {
				availBand = remain;
			}
			link = downLinks.get(i).get(flow.downLinkId[i]);
			weightedSum = 0;
			for (Flow relatedFlow : link.unallocFlows) {
				if (proportionType == PROPORTION_TYPE.SIZE) {
					weightedSum += relatedFlow.size;
				}
				else if (proportionType == PROPORTION_TYPE.REMAIN_SIZE) {
					weightedSum += relatedFlow.getReaminingSize();
				}
			}
			if (proportionType == PROPORTION_TYPE.SIZE) {
				remain = link.remainBand / weightedSum * flow.size;
			}
			else if (proportionType == PROPORTION_TYPE.REMAIN_SIZE) {
				remain = link.remainBand / weightedSum * flow.getReaminingSize();
			}
			if (remain < availBand) {
				availBand = remain;
			}
		}
		if (availBand < 0) {
			availBand = 0;
		}
		return availBand;
	}
}