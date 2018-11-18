package simulator.traffic;

import java.util.Iterator;
import java.util.LinkedList;

public abstract class PerFlowTraffic extends Traffic {
	public PerFlowTraffic() { }
	
	protected final void updateTraffic(LinkedList<Flow> flows, double time) {
		for (Iterator<Flow> iter = flows.iterator(); iter.hasNext(); ) {
			if (iter.next().updateReaminingSize(time)) {
				iter.remove();
			}
		}
//		boolean temp;
//		Flow flow;
//		for (int i = 0; i < flows.size(); i++) {
//			flow = flows.get(i);
//			System.out.println(flow.getReaminingSize());
//			temp = flow.updateReaminingSize(time);
//			System.out.println(flow.getReaminingSize());
//			System.out.println("\n");
//			if (temp) {
//				flows.remove(i);
//			}
//		}
	}
}