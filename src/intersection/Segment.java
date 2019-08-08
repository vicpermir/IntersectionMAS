package intersection;

import java.util.HashMap;
import java.util.Map;

public class Segment {

	private Map<String, Lane> lanes;
	
	public Segment () {
		this.lanes = new HashMap<String, Lane>();
	}
	
	public boolean addLane( String id, P start, P end ){
		return lanes.putIfAbsent(id, new Lane(id, start, end) ) == null;
	}
	
	public boolean removeLane( String id ) {
		return lanes.remove(id) != null;
	}
	
	public Lane getLane( String id ) {
		return lanes.get(id);
	}
	
	public Map<String, Lane> getLanes(){
		return lanes;
	}
	
}
