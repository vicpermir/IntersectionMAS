package intersection;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import intersection.Lane;

public class FourWayIntersectionSingleLane {
	
	private Map<String, Segment> segments  = new HashMap<String, Segment>();
	private Segment north, east, south, west;
	private int startX, startY, cellX, cellY;
	
	private CollisionSpace cSpace 		   = new CollisionSpace(2, 2);
	private Map<String, Path> validPaths   = new HashMap<String, Path>();
	private P[][] cells;
	
	// Reservations
	private Map<P, Reservation> reservations = new HashMap<P, Reservation>();
	
	
	public FourWayIntersectionSingleLane ( int worldX, int worldY, int cellSize ) {
		
		// worldX, worldY are the maximum X,Y of the map		
		startX = (worldX/2) - cellSize;
		startY = (worldY/2) - cellSize;
		
		// Initialize segments and lanes
		north = new Segment();
		north.addLane( "N0"		, new P(startX + (cellSize/2), 10) 		  				, new P(startX + (cellSize/2), startY));
		north.addLane( "N1"		, new P(startX + (cellSize/2)*3, startY)  				, new P(startX + (cellSize/2)*3, 10));
		segments.put("N", north);
		
		east = new Segment();
		east.addLane( "E0"		, new P(worldX-10, startY + (cellSize/2)) 				, new P(startX + (cellSize*2), startY + (cellSize/2)));
		east.addLane( "E1"		, new P(startX + (cellSize*2), startY + 3*(cellSize/2)) , new P(worldX-10, startY + 3*(cellSize/2)));
		segments.put("E", east);
		
		south = new Segment();
		south.addLane( "S0" 	, new P(startX + 3*(cellSize/2), worldY-10) 			, new P(startX + 3*(cellSize/2), startY + (2*cellSize)));
		south.addLane( "S1"		, new P(startX + (cellSize/2), startY + (2*cellSize))   , new P(startX + (cellSize/2), worldY-10));
		segments.put("S", south);
		
		west = new Segment();
		west.addLane( "W0"		, new P(10, startY + 3*(cellSize/2)) 	  				, new P(startX, startY + 3*(cellSize/2)));
		west.addLane( "W1"		, new P(startX, startY + (cellSize/2))	  				, new P(10, startY + (cellSize/2)));
		segments.put("W", west);
			
		
		
		// Initialize collisionSpace
		cells = cSpace.get();
		for (int row = 0; row < cells.length; row++) {
			
		    for (int col = 0; col < cells[row].length; col++) {
		    			    	
		    	cellX = startX + (col * cellSize) + (cellSize/2);
		    	cellY = startY + (row * cellSize) + (cellSize/2);
		    	P cell = new P(cellX, cellY);
		    	cells[row][col] = cell;
		    	reservations.put(cell, new Reservation());
		    	
		    }
		    
		}
		
		// Initialize validPaths
		// Paths from incoming NORTH lane to others
		validPaths.put("N0W1", new Path( segments.get("N").getLane("N0"), segments.get("W").getLane("W1")));
		validPaths.get("N0W1").addToPath(cells[0][0]);
		validPaths.get("N0W1").closePath();
		
		validPaths.put("N0S1", new Path( segments.get("N").getLane("N0"), segments.get("S").getLane("S1")));
		validPaths.get("N0S1").addToPath(cells[0][0]);
		validPaths.get("N0S1").addToPath(cells[1][0]);
		validPaths.get("N0S1").closePath();
		
		validPaths.put("N0E1", new Path( segments.get("N").getLane("N0"), segments.get("E").getLane("E1")));
		validPaths.get("N0E1").addToPath(cells[0][0]);
		validPaths.get("N0E1").addToPath(cells[1][1]);
		validPaths.get("N0E1").closePath();
		
		
		// Paths from incoming EAST lane to others
		validPaths.put("E0W1", new Path( segments.get("E").getLane("E0"), segments.get("W").getLane("W1")));
		validPaths.get("E0W1").addToPath(cells[0][1]);
		validPaths.get("E0W1").addToPath(cells[0][0]);
		validPaths.get("E0W1").closePath();
		
		validPaths.put("E0N1", new Path( segments.get("E").getLane("E0"), segments.get("N").getLane("N1")));
		validPaths.get("E0N1").addToPath(cells[0][1]);
		validPaths.get("E0N1").closePath();
		
		validPaths.put("E0S1", new Path( segments.get("E").getLane("E0"), segments.get("S").getLane("S1")));
		validPaths.get("E0S1").addToPath(cells[0][1]);
		validPaths.get("E0S1").addToPath(cells[1][0]);
		validPaths.get("E0S1").closePath();
			
		
		// Paths from incoming SOUTH lane to others
		validPaths.put("S0W1", new Path( segments.get("S").getLane("S0"), segments.get("W").getLane("W1")));
		validPaths.get("S0W1").addToPath(cells[1][1]);
		validPaths.get("S0W1").addToPath(cells[0][0]);
		validPaths.get("S0W1").closePath();
		
		validPaths.put("S0E1", new Path( segments.get("S").getLane("S0"), segments.get("E").getLane("E1")));
		validPaths.get("S0E1").addToPath(cells[1][1]);
		validPaths.get("S0E1").closePath();
		
		validPaths.put("S0N1", new Path( segments.get("S").getLane("S0"), segments.get("N").getLane("N1")));
		validPaths.get("S0N1").addToPath(cells[1][1]);
		validPaths.get("S0W1").addToPath(cells[0][1]);
		validPaths.get("S0W1").closePath();
		
		
		// Paths from incoming WEST lane to others
		validPaths.put("W0E1", new Path( segments.get("W").getLane("W0"), segments.get("E").getLane("E1")));
		validPaths.get("W0E1").addToPath(cells[1][0]);
		validPaths.get("W0E1").addToPath(cells[1][1]);
		validPaths.get("W0E1").closePath();
		
		validPaths.put("W0S1", new Path( segments.get("W").getLane("W0"), segments.get("S").getLane("S1")));
		validPaths.get("W0S1").addToPath(cells[1][0]);
		validPaths.get("W0S1").closePath();
		
		validPaths.put("W0N1", new Path( segments.get("W").getLane("W0"), segments.get("N").getLane("N1")));
		validPaths.get("W0N1").addToPath(cells[1][0]);
		validPaths.get("W0N1").addToPath(cells[0][1]);
		validPaths.get("W0N1").closePath();
		
	}
	
	public List<Lane> getIncomingLanes() {
		List<Lane> list = new ArrayList<Lane>();
		
		list.add( north.getLane("N0") );
		list.add( east.getLane("E0") );
		list.add( south.getLane("S0") );
		list.add( west.getLane("W0") );
		
		return list;
	}
	
	public List<Lane> getOutgoingLanes() {
		List<Lane> list = new ArrayList<Lane>();
		
		list.add( north.getLane("N1") );
		list.add( east.getLane("E1") );
		list.add( south.getLane("S1") );
		list.add( west.getLane("W1") );
		
		return list;
	}
		
	public List<Lane> getAllLanes() {
		List<Lane> list = new ArrayList<Lane>();
		
		list.addAll(north.getLanes().values());
		list.addAll(east.getLanes().values());
		list.addAll(south.getLanes().values());
		list.addAll(west.getLanes().values());
		
		return list;
	}
	
	public List<P> getCells() {
		List<P> list = new ArrayList<P>();
		
		for (int row = 0; row < cells.length; row++) {
			for (int col = 0; col < cells[row].length; col++) {
				 list.add(cells[row][col]);   	
			}	    
		}
		
		return list;
	}
	
	public Path getPath( String path ) {
		return validPaths.get(path);
	}

	public boolean tryReserve(P cell, long start, long end, String vehicleAgent) {
		long tfStart, tfEnd;
		for (TimeFrame tf : reservations.get(cell).getTimeFrames() ) {
			tfStart = tf.getStartTime();
			tfEnd   = tf.getEndTime();
			if( start >= tfStart && start <= tfEnd )  {
				return false;
			} else if ( end >= tfStart  && end <= tfEnd ) {
				return false;
			} else if ( start >= tfStart && end <= tfEnd ) {
				return false;
			}
		}
		return true;
	}

	public void reserve(P cell, long start, long end, String vehicleAgent) {
		//System.out.println("RESERVE: cell=" + cell + ", vehicle=" + vehicleAgent + ", start= " + start + ", end=" + end + ".");
		reservations.get(cell).addTimeFrame( vehicleAgent, start +1000L, end + 1000L);
	}
	
	// Remove all expired reservations
	public void clearExpiredReservations()  {
		//int removed = 0;
		//int total = 0;
		long currentTime = new Date().getTime();
		for ( Reservation reservation : reservations.values() ) {
			
			for ( int i=0; i < reservation.size(); i++ ) {
				if ( reservation.get(i).getEndTime() < currentTime)  {
					reservation.remove(i);
					//removed++;
				}
				//total++;
			}
		}
		//System.out.println("clearExpired(): Total reservations=" + total + ", removed this turn=" + removed);
	}
	
	public int getNumberOfReservations() {
		int total = 0;
		for ( Reservation reservation : reservations.values() ) {
			total += reservation.size();
		}
		return total;
	}
	
}
