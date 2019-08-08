package main;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class Main{
	
	//Start the RMA
	private static final boolean startRMA = false;
		
    public static void main(String arg[]) {
    	
		//Get a hold on JADE runtime
		jade.core.Runtime rt = jade.core.Runtime.instance();
		
		//Exit the JVM when there are no more containers around
		rt.setCloseVM(true);

		//Create a profile for the main container
		Profile profile = new ProfileImpl(null, 1099, null);
		profile.setParameter(Profile.CONTAINER_NAME, "Main container");
		
		//Container that will hold the agents
		jade.wrapper.AgentContainer mainContainer = rt.createMainContainer(profile);

		//Start RMA
		if (startRMA) {
			try {
				
				AgentController rma = mainContainer.createNewAgent("rma", "jade.tools.rma.rma", new Object[0]);
				rma.start();

			} catch (StaleProxyException e1) {

				System.out.println("Error starting the rma agent");
				e1.printStackTrace();
			}
		}
		
		// Create and start IntersectionManager agent
		try {	
			
			// Start the simulation agent
			AgentController worldAgent = mainContainer.createNewAgent("worldAgent", "agents.WorldAgent", new Object[]{rt, mainContainer});
			worldAgent.start();

			// FOR TESTING ONLY: Spawn a single vehicle agent
			//AgentController V0Agent = mainContainer.createNewAgent("0", "agents.VehicleAgent", new Object[]{"intersectionManager"});
			//V0Agent.start();
			
		} catch (StaleProxyException e) {

			System.out.println("ERROR: Unable to start intersection manager agent.");
			e.printStackTrace();
		}
    }
    
}
