package agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import behaviours.WorldVehicleSpawnBehaviour;
import jade.core.Agent;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import jade.wrapper.AgentContainer;
import jade.core.Runtime;

public class WorldAgent extends Agent {

	private static final long serialVersionUID = 1L;
	private AgentContainer mainContainer;
	private List<String> intersectionIdentifiers;
	private Map<String, AgentController> intersectionAgents;
	private Runtime rt;
	private long vehicleSpawnDelay = 1250L;		// Delay between a vehicle spawn in ms (default: 1250)
	
	protected void setup() {
		
		// Initialize arguments
		rt = (Runtime) this.getArguments()[0];
		mainContainer = (AgentContainer) this.getArguments()[1];
		
		// Initialize intersection agents data structures
		intersectionIdentifiers = new ArrayList<String>();
		intersectionAgents = new HashMap<String, AgentController>();
				
		// Add intersections identifiers, in this case only one
		intersectionIdentifiers.add("intersectionManager");
		
		// Creat and start the radar agent
		String radarAgentID = "radarAgent";
		try {
			AgentController radarAgent = mainContainer.createNewAgent(radarAgentID, "agents.RadarAgent", new Object[]{});
			radarAgent.start();
		
		} catch (StaleProxyException e) {
			System.out.println("ERROR: Unable to start the Radar Agent.");
			e.printStackTrace();
		}
		
		// Create and start intersection agents
		for ( String id : intersectionIdentifiers ) {
			try {
					
				AgentController agentController = mainContainer.createNewAgent(id, "agents.IntersectionManagerAgent", new Object[]{rt});
				agentController.start();
				intersectionAgents.put(id, agentController);
			
			} catch (StaleProxyException e) {
				System.out.println("ERROR: Unable to start intersection manager agent with ID=" +  id + ".");
				e.printStackTrace();
			}
				
		}
		
		// Wait for intersection initialization
		try {
			Thread.sleep(1500L);
		}catch (Exception e) {
			e.printStackTrace();
		}
			
		// Spawn vehicle agents
	  	addBehaviour( new WorldVehicleSpawnBehaviour(intersectionIdentifiers, radarAgentID) );
		
	}


	public long getVehicleSpawnDelay() {
		return vehicleSpawnDelay;
	}


	public void setVehicleSpawnDelay(long vehicleSpawnDelay) {
		this.vehicleSpawnDelay = vehicleSpawnDelay;
	}

}
