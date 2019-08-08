package intersection;

public class CollisionSpace {
	
	private P[][] cells;
	
	// CollisionSpace constructor
	public CollisionSpace( int rows, int cols ) {
		initializeSpace( rows, cols );
	}
	
	// Java non-primitive array initialization
	public void initializeSpace( int rows, int cols ) {
		cells = new P[rows][cols];
		for (int i = 0; i < cells.length; i++) {
			for (int j = 0; j < cells[i].length; j++) {
				cells[i][j] = new P();
			}
		}
	}
	
	public P[][] get() {
		return cells;
	}
	
}
