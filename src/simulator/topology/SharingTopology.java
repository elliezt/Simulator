package simulator.topology;

import simulator.traffic.Flow;

public abstract class SharingTopology extends Topology {
	public SharingTopology(int[] childrenPerNode, double[] bandPerLayer, int[] nodesPerLayer) {
		super(childrenPerNode, bandPerLayer);
	}
	
	protected final void updateRemainBand(Flow flow, double allocatedRate) {
		for (int i = 0; i < flow.hops; i++) {
			upLinks.get(i).get(flow.upLinkId[i]).remainBand -= allocatedRate;
			upLinks.get(i).get(flow.upLinkId[i]).unallocFlows.remove(flow);
			downLinks.get(i).get(flow.downLinkId[i]).remainBand -= allocatedRate;
			downLinks.get(i).get(flow.downLinkId[i]).unallocFlows.remove(flow);
		}
	}
	
	protected final void updateRemainBandHash(Flow flow, double allocatedRate) {
		for (int i = 0; i < flow.hops; i++) {
			upLinks.get(i).get(flow.upLinkId[i]).remainBand -= allocatedRate;
			upLinks.get(i).get(flow.upLinkId[i]).unallocFlowsHash.remove(flow);
			downLinks.get(i).get(flow.downLinkId[i]).remainBand -= allocatedRate;
			downLinks.get(i).get(flow.downLinkId[i]).unallocFlowsHash.remove(flow);
		}
	}
	
	protected final void updateRelatedFlows(Flow flow) {
		for (int i = 0; i < flow.hops; i++) {
			for (Flow relatedFlow : upLinks.get(i).get(flow.upLinkId[i]).unallocFlows) {
				relatedFlow.setRate(getAvailBand(relatedFlow));
			}
			for (Flow relatedFlow : downLinks.get(i).get(flow.downLinkId[i]).unallocFlows) {
				relatedFlow.setRate(getAvailBand(relatedFlow));
			}
		}
	}
	
	protected final void updateRelatedFlowsHash(Flow flow) {
		for (int i = 0; i < flow.hops; i++) {
			for (Flow relatedFlow : upLinks.get(i).get(flow.upLinkId[i]).unallocFlowsHash) {
				relatedFlow.setRate(getAvailBand(relatedFlow));
			}
			for (Flow relatedFlow : downLinks.get(i).get(flow.downLinkId[i]).unallocFlowsHash) {
				relatedFlow.setRate(getAvailBand(relatedFlow));
			}
		}
	}
	
	protected void updateRates(Flow flow) {
		updateRemainBand(flow, flow.getRate());
		updateRelatedFlows(flow);
	}
	
	protected void updateRatesHash(Flow flow) {
		updateRemainBandHash(flow, flow.getRate());
		updateRelatedFlowsHash(flow);
	}
}