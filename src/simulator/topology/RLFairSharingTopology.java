package simulator.topology;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;

import simulator.simulators.Simulator;
import simulator.traffic.Flow;
import simulator.traffic.Flow.FlowComparator;
import simulator.utils.Parameters;
import simulator.utils.Utils;

public class RLFairSharingTopology extends SharingTopology {
	private static final int RL_RESTRICT_FLOW_TIMES = 1;
	private static final int RL_FAIR_APPROX_TIMES = 100;
	private static final double PRICE_UPDATE_STEP_SIZE = 5 * Math.pow(10, -18);
	
	private double coefficient;
	private double[][] upTemporalPrice;
	private double[][] downTemporalPrice;
	public int clock = 0;
	public int memory = 0;
	
	public RLFairSharingTopology(int[] childrenPerNode, double[] bandPerLayer, int[] nodesPerLayer) {
		super(childrenPerNode, bandPerLayer, nodesPerLayer);
		coefficient = 0;
		int max = 0;
		for (int i = 0; i < Simulator.config.layers; i++) {
			if (max < linksPerLayer[i]) {
				max = linksPerLayer[i];
			}
		}	
		upTemporalPrice = new double [linksPerLayer.length][max];
		downTemporalPrice = new double [linksPerLayer.length][max];
	}
	
	public void setRatesApprox(LinkedList<Flow> flows) {
		resetSharingFlowsHash(flows);
		for (Flow flow : flows) {
			flow.setRate(getAvailBand(flow));
		}
		int initSize = flows.size() > 0 ? flows.size() : 1;
		PriorityQueue<Flow> unallocFlowsHash = new PriorityQueue<Flow>(initSize, new Flow.FlowComparator(FlowComparator.FLOW_COMPARATOR_TYPE.RATE));
		unallocFlowsHash.addAll(flows);
		excludeRestrictedFlowsApprox(unallocFlowsHash);
		while (!unallocFlowsHash.isEmpty()) {
			for (int i = 0; i < RL_FAIR_APPROX_TIMES; i++) {
				updateRatesApprox(unallocFlowsHash.poll());
				if (unallocFlowsHash.isEmpty()) {
					break;
				}
			}
			excludeRestrictedFlowsApprox(unallocFlowsHash);
		}
	}
	
	private void updateRatesApprox(Flow flow) {
		updateRemainBandHash(flow, flow.getRate());
	}
	
	private void excludeRestrictedFlowsApprox(PriorityQueue<Flow> flows) {
		boolean hasRestrictedFlows = true;
		int count = 0;
		while (hasRestrictedFlows && count < RL_RESTRICT_FLOW_TIMES) {
			hasRestrictedFlows = false;
			for (Iterator<Flow> iter = flows.iterator(); iter.hasNext(); ) {
				Flow flow = iter.next();
				double rate = flow.getAllocRate();
				if (rate - flow.getRate() < Parameters.RATE_GRANULARITY) {
					iter.remove();
					flow.setRate(rate);
					updateRatesHash(flow);
					hasRestrictedFlows = true;
					//break;
				}
			}
			count++;
		}
	}
	
	public void setRates(LinkedList<Flow> flows) {
		resetSharingFlowsHash(flows);
		for (Flow flow : flows) {
			flow.setRate(getAvailBand(flow));
			System.out.println(flow.getRate());
		}
		int initSize = flows.size() > 0 ? flows.size() : 1;
		PriorityQueue<Flow> unallocFlowsHash = new PriorityQueue<Flow>(initSize, new Flow.FlowComparator(FlowComparator.FLOW_COMPARATOR_TYPE.RATE));
		unallocFlowsHash.addAll(flows);
		excludeRestrictedFlows(unallocFlowsHash);
		while (!unallocFlowsHash.isEmpty()) {
			updateRatesHash(unallocFlowsHash.poll());
			excludeRestrictedFlows(unallocFlowsHash);
		}
	}
	
	public void setRatesWFQ(LinkedList<Flow> flows) {
		resetSharingFlowsHash(flows);
//		System.out.println(flows.size());
		for (Flow flow : flows) {
			flow.setRate(getAvailBandWFQ(flow));
		}
		for (Flow flow : flows) {
			for (int i = 0; i < flow.hops; i++) {
				upLinks.get(i).get(flow.upLinkId[i]).remainBand -= flow.getRate();
			}
		}
	} 
	public void setRatesFake(LinkedList<Flow> flows) {
		for (Flow flow : flows) {
			flow.setRate(flow.getAllocRate());
		}
	}
	
	protected double getAvailBand(Flow flow) {
		double availBand = Double.MAX_VALUE;
		for (int i = 0; i < flow.hops; i++) {
			Link link = upLinks.get(i).get(flow.upLinkId[i]);
			double remain = link.remainBand / link.unallocFlowsHash.size();
			if (remain < availBand) {
				availBand = remain;
			}
			link = downLinks.get(i).get(flow.downLinkId[i]);
			remain = link.remainBand / link.unallocFlowsHash.size();
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
	
	protected double getAvailBandWFQ(Flow flow) {
		double availBand = Double.MAX_VALUE;
		for (int i = 0; i < flow.hops; i++) {
			Link link = upLinks.get(i).get(flow.upLinkId[i]);
			double weightSum = 0;
			for (Flow relatedFlow : link.unallocFlowsHash) {
				weightSum += relatedFlow.getAllocRate();
			}
			double remain = link.remainBand * (flow.getAllocRate() / weightSum);
			if (remain < availBand) {
				availBand = remain;
			}
			link = downLinks.get(i).get(flow.downLinkId[i]);
			weightSum = 0;
			for (Flow relatedFlow : link.unallocFlowsHash) {
				weightSum += relatedFlow.getAllocRate();
			}
			remain = link.remainBand * (flow.getAllocRate() / weightSum);
			if (remain < availBand) {
				availBand = remain;
			}
		}
		availBand = Utils.checkRate(availBand);
		if (availBand < 0) {
			Simulator.output.error("weighted sharing topology alloc error");
		}
		return availBand;
	}
	
	private void excludeRestrictedFlows(PriorityQueue<Flow> flows) {
		boolean hasRestrictedFlows = true;
		while (hasRestrictedFlows) {
			hasRestrictedFlows = false;
			for (Iterator<Flow> iter = flows.iterator(); iter.hasNext(); ) {
				Flow flow = iter.next();
				double rate = flow.getAllocRate();
				if (rate - flow.getRate() < Parameters.RATE_GRANULARITY) {
					iter.remove();
					flow.setRate(rate);
					updateRatesHash(flow);
					hasRestrictedFlows = true;
					break;
				}
			}
		}
	}
	
	public final void updateLinkPrice(LinkedList<Flow> flows) {
		clock = 0;
		memory = 0;
		for (int i = 0; i < linksPerLayer.length; i++) {
			for (int j = 0; j < linksPerLayer[i]; j++) {
				double nextCoefficient = (1 + Math.sqrt(1 + 4 * coefficient * coefficient)) / 2;
				if (upLinks.get(i).get(j).unallocFlowsHash != null && !upLinks.get(i).get(j).unallocFlowsHash.isEmpty()) {
//					System.out.println(upLinks.get(i).get(j).unallocFlowsHash.size());
					double minErrorPerLink = Double.MAX_VALUE;
					double rateSum = 0;
					for (Flow flow: upLinks.get(i).get(j).unallocFlowsHash) {
//						if (flow.getErrorPerLink() >= 0) {
//							if (minErrorPerLink > flow.getErrorPerLink()) {
//								minErrorPerLink = flow.getErrorPerLink();
//								sign = 1;
//							}
//						}
//						else {
//							if (minErrorPerLink > -1 * flow.getErrorPerLink()) {
//								minErrorPerLink = -1 * flow.getErrorPerLink();
//								sign = -1;
//							}
//						}
						if (flow.getRate() > 0 && minErrorPerLink > flow.getErrorPerLink()) {
							minErrorPerLink = flow.getErrorPerLink();
						}
						rateSum += flow.getRate();
					}
					double nextUpTemporalPrice = upLinks.get(i).get(j).price + minErrorPerLink - PRICE_UPDATE_STEP_SIZE * (Simulator.config.bandPerLayer[i] - rateSum);
					upLinks.get(i).get(j).price = nextUpTemporalPrice - (1 - coefficient) / nextCoefficient * (nextUpTemporalPrice - upTemporalPrice[i][j]);
//					upLinks.get(i).get(j).price = nextUpTemporalPrice;
					if (upLinks.get(i).get(j).price < 0) {
						upLinks.get(i).get(j).price = 0;
					}
					if (i == 1 && j % 3 == 0) {
						if (upLinks.get(i).get(j).unallocFlowsHash != null && !upLinks.get(i).get(j).unallocFlowsHash.isEmpty()) {
							clock += 240 + upLinks.get(i).get(j).unallocFlowsHash.size() * 5;
							memory += 56 + upLinks.get(i).get(j).unallocFlowsHash.size() * 16;
						}
					}
					upTemporalPrice[i][j] = nextUpTemporalPrice;
				}
				if (downLinks.get(i).get(j).unallocFlowsHash != null && !downLinks.get(i).get(j).unallocFlowsHash.isEmpty()) {
//					System.out.println(downLinks.get(i).get(j).unallocFlowsHash.size());
					double minErrorPerLink = Double.MAX_VALUE;
					double rateSum = 0;
					for (Flow flow: downLinks.get(i).get(j).unallocFlowsHash) {
//						if (flow.getErrorPerLink() >= 0) {
//							if (minErrorPerLink > flow.getErrorPerLink()) {
//								minErrorPerLink = flow.getErrorPerLink();
//								sign = 1;
//							}
//						}
//						else {
//							if (minErrorPerLink > -1 * flow.getErrorPerLink()) {
//								minErrorPerLink = -1 * flow.getErrorPerLink();
//								sign = -1;
//							}
//						}
						if (minErrorPerLink > flow.getErrorPerLink()) {
							minErrorPerLink = flow.getErrorPerLink();
						}
						rateSum += flow.getRate();
					}
//					System.out.println(minErrorPerLink);
//					System.out.println('\n');
					double nextDownTemporalPrice = downLinks.get(i).get(j).price + minErrorPerLink - PRICE_UPDATE_STEP_SIZE * (Simulator.config.bandPerLayer[i] - rateSum);
					downLinks.get(i).get(j).price = nextDownTemporalPrice - (1 - coefficient) / nextCoefficient * (nextDownTemporalPrice - downTemporalPrice[i][j]);
//					downLinks.get(i).get(j).price = nextDownTemporalPrice;
					if (downLinks.get(i).get(j).price < 0) {
						downLinks.get(i).get(j).price = 0;
					}
					downTemporalPrice[i][j] = nextDownTemporalPrice;
				}
				coefficient = nextCoefficient;
			}
		}
//		System.out.println(Simulator.getTime());
//		System.out.println(clock);
//		System.out.println(memory);
//		System.out.println('\n');
	}
}