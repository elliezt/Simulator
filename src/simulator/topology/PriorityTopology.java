package simulator.topology;

//import java.io.File;
//import java.io.FileWriter;
import java.util.LinkedList;
//import java.io.IOException;

import simulator.traffic.Flow;

public class PriorityTopology extends Topology {
	public PriorityTopology(int[] childrenPerNode, double[] bandPerLayer) {
		super(childrenPerNode, bandPerLayer);
	}
	
	public void setRates(LinkedList<Flow> flows) {
		for (Flow flow : flows) {
			double availBand = getAvailBand(flow);
			flow.setRate(availBand);
			updateRemainBand(flow, availBand);
		}
	}
	
	protected double getAvailBand(Flow flow) {
		double availBand = Double.MAX_VALUE;
		for (int i = 0; i < flow.hops; i++) {
			double remain = upLinks.get(i).get(flow.upLinkId[i]).remainBand;
			if (remain < availBand) {
				availBand = remain;
			}
			remain = downLinks.get(i).get(flow.downLinkId[i]).remainBand;
			if (remain < availBand) {
				availBand = remain;
			}
		}
		return availBand;
	}
	
	protected void updateRemainBand(Flow flow, double allocatedRate) {
		for (int i = 0; i < flow.hops; i++) {
			upLinks.get(i).get(flow.upLinkId[i]).remainBand -= allocatedRate;
			downLinks.get(i).get(flow.downLinkId[i]).remainBand -= allocatedRate;
		}
	}
}