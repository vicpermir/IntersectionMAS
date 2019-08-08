package intersection;

import java.util.ArrayList;
import java.util.List;

public class Path {
	
	private Lane startLane, endLane;
	private List<P> path;
	private boolean closed = false;
	
	public Path ( Lane startLane, Lane endLane) {
		this.startLane = startLane;
		this.endLane = endLane;
		this.path = new ArrayList<P>();
	}
	
	public void addToPath( P cell ) {
		if (!closed) path.add(cell);
	}
	
	public void closePath(){
		this.closed = true;
	}
	

	// Getters and setters
	public Lane getStartLane() {
		return this.startLane;
	}
	
	public Lane getEndLane() {
		return this.endLane;
	}
	
	public List<P> getCells() {
		return path;
	}
	
}
