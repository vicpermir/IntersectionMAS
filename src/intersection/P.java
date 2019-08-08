package intersection;

public class P {
	
	private float x;
	private float y;

	// Constructors
	public P() {
		this.x = 0.0f;
		this.y = 0.0f;
	}
	
	public P( P point ) {
		this.x = point.getX();
		this.y = point.getY();
	}

	public P( float x, float y ) {
		this.x = x;
		this.y = y;
	}
	
	public P( double x, double y ) {
		this.x = (float) x;
		this.y = (float) y;
	}
	
	public P( int x, int y ) {
		this.x = (float) x;
		this.y = (float) y;
	}
	
	// Getters and setters
	public float getX() {
		return this.x;
	}

	public float getY() {
		return this.y;
	}
	
	public void setX( float x ){
		this.x = x;
	}
	
	public void setY( float y ){
		this.y = y;
	}
	
	public void set(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	// Distance between two points
	public double dist( float x1, float y1, float x2, float y2 ) {
		x1 -= x2;
		y1 -= y2;
		return Math.sqrt(x1 * x1 + y1 * y1);
	}
	
	public double dist( P p ) {
		return this.dist(p.getX(), p.getY(), getX(), getY());
	}
	
	// toString
	public String toString() {
		return "("+this.x+", "+this.y+")";
	}
	
	// clone
	public P clone() {
		return new P(this);
	}
	
	// equals
    public boolean equals(Object obj) {
    	if ( obj instanceof P ) {
    		P p = (P) obj;
    		return (getX() == p.getX()) && (getY() == p.getY());
    	}
    	return super.equals(obj);
    }

}
