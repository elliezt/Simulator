package simulator.traffic;

import java.util.Iterator;
import java.util.LinkedList;

import simulator.utils.Parameters;

public abstract class PerTaskTraffic extends Traffic {
	public PerTaskTraffic() { }
	
	protected final void updateTraffic(LinkedList<Task> tasks, double time) {
		for (Iterator<Task> taskIter = tasks.iterator(); taskIter.hasNext(); ) {
			Task task = taskIter.next();
//			for (int index = 0; index < task.getAvailableFlows().size(); index ++) {
//				if (task.getFlows().get(index).updateReaminingSize(time)) {
//					task.getFlows().remove(index);
//					task.getAvailableFlows().remove(index);
//					task.availIndex --;
//				}
//			}
			for (Iterator<Flow> flowIter = task.getFlows().iterator(); flowIter.hasNext(); ) {
				if (flowIter.next().updateReaminingSize(time)) {
					flowIter.remove();
					task.availIndex --;
				}
			}
//			System.out.println(task.getFlows().size());
//			System.out.println(task.getAvailableFlows().size());
//			System.out.println(task.getAvailableFlows().get(0).id);
//			System.out.println('\n');
			for (int index = 0; index < task.getAvailableFlows().size(); index ++) {
				if (task.getAvailableFlows().get(index).getReaminingSize() <= Parameters.SIZE_GRANULARITY) {
					task.getAvailableFlows().remove(index);
				}
			}
//			for (Iterator<Flow> flowIter = task.getAvailableFlows().iterator(); flowIter.hasNext(); ) {
//				if (flowIter.next().updateReaminingSize(time)) {
//					flowIter.remove();
//				}
//			}
//			System.out.println('\n');
//			for (Iterator<Flow> flowIter = task.getAvailableFlows().iterator(); flowIter.hasNext(); ) {
////				System.out.printf("1%b\n",flowIter.next().updateReaminingSize(time));
//				if (flowIter.next().updateReaminingSize(time)) {
//					flowIter.remove();
//				}
//			}
//			System.out.println(task.getFlows().size());
//			System.out.println(task.getAvailableFlows().size());
			//System.out.printf("2%b\n",task.getFlows().isEmpty());
			if (task.getFlows().isEmpty()) {
				taskIter.remove();
			}
		}
	}
}