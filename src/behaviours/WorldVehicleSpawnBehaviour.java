package behaviours;

import java.util.List;
import java.util.Random;

import agents.WorldAgent;
import jade.core.behaviours.Behaviour;
import jade.wrapper.AgentController;

public class WorldVehicleSpawnBehaviour extends Behaviour {

	private static final long serialVersionUID = 1L;
	private int vehicleCounter = 0;
	private List<String> intersections;
	
	private Random randomGenerator = new Random();
	private String randomIntersection, radarAgentID;
	private long vehicleSpawnDelay;
	private int index;
	
	
	public WorldVehicleSpawnBehaviour( List<String> intersectionAgents, String radarAgent) {
		this.intersections = intersectionAgents;
		this.radarAgentID = radarAgent;
	}

	@Override
	public void action() {
		
		// Select a random intersection to spawn the vehicle in
		index = randomGenerator.nextInt(intersections.size());
		randomIntersection = intersections.get(index);
		
		vehicleSpawnDelay = ((WorldAgent) myAgent).getVehicleSpawnDelay();
				
		try {

			Thread.sleep( vehicleSpawnDelay );
			
			AgentController agent = myAgent.getContainerController().createNewAgent("" + vehicleCounter , "agents.VehicleAgent",
					new Object[]{randomIntersection, radarAgentID}
			);

			agent.start();
			
			//System.out.println("[" + getBehaviourName() + "] VehicleAgent " + vehicleCounter + " has been spawned at "+ randomIntersection + ".");
						
		} catch (Exception e) {

			System.out.println("Error starting a vehicle agent.");
			e.printStackTrace();
		}
		
		vehicleCounter++;
		
	}

	@Override
	public boolean done() {
		return false;
	}

}
