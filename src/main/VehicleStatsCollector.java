package main;

import java.util.ArrayList;
import java.util.List;

public class VehicleStatsCollector {
	
	List<Long> delayList = new ArrayList<Long>();
	long totalSum = 0;
	
	public void addDelay( long delay ) {
		totalSum += delay;
		delayList.add(delay);
		//System.out.println("[StatsCollector] Vehicle finished, added delay=" + delay + "ms.");
	}
	
	public long getAverageDelay() {
		return (totalSum / delayList.size());
	}
	
}
