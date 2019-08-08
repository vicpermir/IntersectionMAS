package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;

import intersection.*;
import agents.IntersectionManagerAgent;

public class CanvasWorld extends JFrame {

	private static final long serialVersionUID = 1L;
	public static int MAXWORLDX, MAXWORLDY;
	public static int vehicleSize = 16;
	
	@SuppressWarnings("unused")
	private IntersectionManagerAgent im;
	
	// Panels
	private JTextArea loggerPanel;
	private WorldPane worldPane;
	
	// Data structures
	List<Lane> laneList;
	int cellSize;
	List<P> cellList;
	Map<String, P> vehicleSet;
	
	// StatsCollector fieldsç
	int vehiclesSpawned = 0;
	StatsCollectorPanel statsCollector;
	JTextField vehiclesSpawnedField = new JTextField("0");
	JTextField vehiclesActiveField = new JTextField("0");
	JTextField reservationsField = new JTextField("0");
	JTextField averageDelayField = new JTextField("NaN");
	
	
	public CanvasWorld( IntersectionManagerAgent im, int maxX, int maxY) {
		super();
		
		MAXWORLDX = maxX;
		MAXWORLDY = maxY;
		
		//Make it white
		this.getContentPane().setBackground(Color.WHITE);
		
		this.im = im;
		this.laneList = new ArrayList<Lane>();
		this.cellList = new ArrayList<P>();
		this.vehicleSet = new HashMap<String, P>();
				
    	// Draw the interface
        setTitle("Intersection Simulator");
        
        // Default close operation
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); 
        
        // Shutdown JADE runtime on close
        this.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e){
                int confirmed = JOptionPane.showConfirmDialog(null, 
                        "Are you sure you want to exit the program?", "Close operation warning",
                        JOptionPane.YES_NO_OPTION);

                if (confirmed == JOptionPane.YES_OPTION) {
                	im.shutDown();
                	System.exit(0);
                }
            }
        });
        
        // Set layour
        this.getContentPane().setLayout(new GridBagLayout());
        
        // Fluid layout
      	GridBagConstraints canvasConstraints = new GridBagConstraints();
      	canvasConstraints.fill 			= GridBagConstraints.BOTH;
      	canvasConstraints.weightx 		= 1; 		//Percentage of space this will take horizontally
      	canvasConstraints.weighty 		= 0.7; 		//Percentage of space this will take vertically
      	canvasConstraints.gridx 		= 0; 		//Select column
      	canvasConstraints.gridy 		= 0; 		//Select row
        
        // Add panels
      	worldPane = new WorldPane();
        add( worldPane, canvasConstraints );
        
        
		// Console panel
		this.loggerPanel = new JTextArea(10, 20);
		this.loggerPanel.setEditable(false);
		
		GridBagConstraints loggerConstraints = new GridBagConstraints();
		loggerConstraints.fill			= GridBagConstraints.BOTH;
		loggerConstraints.weightx 		= 1; 		//Percentage of space this will take horizontally
		loggerConstraints.weighty 		= 0.3; 		//Percentage of space this will take vertically
		loggerConstraints.gridx 		= 0; 		//Select column
		loggerConstraints.gridy 		= 1; 		//Select row
		
		// Automatic scrolling
		DefaultCaret caret = (DefaultCaret) loggerPanel.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		add(new JScrollPane(loggerPanel), loggerConstraints);
        
		setResizable(false);
        pack();
        setVisible(true);
        
        
        // StatsCollector fields
        
	    vehiclesSpawnedField.setEditable(false);
	    vehiclesSpawnedField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
	    vehiclesActiveField.setEditable(false);
	    vehiclesActiveField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	    
	    reservationsField.setEditable(false);
	    reservationsField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

	    averageDelayField.setEditable(false);
	    averageDelayField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	    
		statsCollector = new StatsCollectorPanel();
	    
        
        log("Simulator initialized.");
		    	
	}
	
	public void addLane( Lane lane ) {
    	laneList.add(lane);
    	log("New Lane spawned from " + lane.getStartPosition() + " to " + lane.getEndPosition() + ".");
    }
	
	public void addCell( P cell ) {
    	cellList.add(cell);
    	log("New Cell spawned at " + cell + ".");
    }
	
	public void addVehicle( String aid, P position ) {
    	vehicleSet.put(aid, position);
    	vehiclesSpawned++;
    	updateVehiclesSpawned();
    	updateActiveVehicles();
    	log("New Vehicle spawned at " + vehicleSet.get(aid) + ".");		
	}
	
	public void updateVehicle( String aid, float x, float y) {
		vehicleSet.get(aid).set(x, y);
	}
	
	public void removeVehicle( String aid ) {
		if (vehicleSet.containsKey(aid)){
			vehicleSet.remove(aid);
			updateVehiclesSpawned();
			updateActiveVehicles();
	    	log("Vehicle AID=" + aid + " removed.");
		} else {
	    	log("Unable to remove Vehicle AID=" + aid + " (not found).");
		}
	}
	
	public void setCellSize( int cellSize ) {
		this.cellSize = cellSize;
	}
	
	public void updateVehiclesSpawned() {
		statsCollector.setSpawnedVehicles(vehiclesSpawned);
	}
	
	public void updateActiveVehicles() {
		statsCollector.setActiveVehicles(vehicleSet.size());
	}
	
	public void updateReservations( int nReservations) {
		statsCollector.setReservations(nReservations);
	}	
	
	public void updateAverageDelay( long delay) {
		statsCollector.setAverageDelay(delay);
	}	
	
    public class WorldPane extends JPanel implements ActionListener {

		private static final long serialVersionUID = -5450806525078415492L;
		private static final int FPS = 40;
		Color color;
		private float x1, x2, y1, y2;
		
		private Timer timer = new Timer(1000/FPS, this);
		
        Font intFont = new Font("Arial", Font.BOLD, 16);
        Font carFont = new Font("Courier New", Font.BOLD, 14);
        
		public WorldPane( ) {
			
            setForeground(Color.DARK_GRAY);
                        
            Dimension size = new Dimension(MAXWORLDX, MAXWORLDY);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
            setSize(size);
                        
            setLayout(null);
			
            timer.start();
        }
		
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
                        
            Graphics2D g2d = (Graphics2D) g.create();
            
            g2d.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2d.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            // Draw lanes
            Line2D line = new Line2D.Float();
        	for ( Lane lane : laneList ) {
        		x1 = lane.getStartPosition().getX();
        		y1 = lane.getStartPosition().getY();
        		x2 = lane.getEndPosition().getX();
        		y2 = lane.getEndPosition().getY();
        			        			
        		g2d.setColor(Color.GRAY);
        		g2d.setStroke(new BasicStroke(1.5f));
        		line.setLine(x1, y1, x2, y2);
    			g2d.draw(line);
        		        		
        	}
            
            
            // Draw cells
        	Rectangle2D rect = new Rectangle2D.Float();
        	//setFont(carFont);
        	for ( P cell : cellList ) {
        		
        		// Get coordinates of the segment
        		x1 = cell.getX();
        		y1 = cell.getY();
        		
            	g2d.setColor(Color.DARK_GRAY);
            	rect.setFrame( x1 - (cellSize/2) + 1, y1 - (cellSize/2) + 1, cellSize - 1, cellSize - 1 );
        		g2d.fill(rect);
        		        		
        	}
            
        	// Draw vehicles
            Ellipse2D ellipse = new Ellipse2D.Float();
            for ( HashMap.Entry<String, P> vehicle : vehicleSet.entrySet() ) {
            	
            	// Get vehicle data
            	P position  = vehicle.getValue();
            	x1 		= position.getX();
            	y1 		= position.getY();
            	
            	// Draw Oval
            	g2d.setColor(Color.RED);
            	ellipse.setFrame(x1 - (vehicleSize / 2), y1 - (vehicleSize / 2), vehicleSize, vehicleSize);
				g2d.fill(ellipse);
            	
            	// Draw Label     
            	g2d.setColor(Color.BLACK);
            	g2d.setColor(color);
            	g2d.drawString( vehicle.getKey().toString(), x1 - (vehicleSize / 4) -1, y1 + (vehicleSize / 4) -1);
            	            	
            }
            
			// Dispose
            g2d.dispose();
        }
                

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource().equals(timer)) {
				this.repaint();
			}
		}

    }
    
	private class StatsCollectorPanel extends JFrame {
		
		private static final long serialVersionUID = 1L;
		
		public StatsCollectorPanel() {
			super("Stats Collector");
			
			JPanel pane = new JPanel();
			pane.setBorder(new EmptyBorder(10, 10, 10, 10));
			pane.setLayout(new GridLayout(0, 2));
			
			JLabel label1 = new JLabel("Spawned vehicles: ");
			label1.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		    pane.add(label1);
		    pane.add(vehiclesSpawnedField);

			JLabel label2 = new JLabel("Active vehicles: ");
			label2.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		    pane.add(label2);
		    pane.add(vehiclesActiveField);

			JLabel label3 = new JLabel("Active reservations: ");
			label3.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		    pane.add(label3);
		    pane.add(reservationsField);
		    
			JLabel label4 = new JLabel("Average delay: ");
			label4.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		    pane.add(label4);
		    pane.add(averageDelayField);
		    
		    add(pane);		    
		    pack();
		    setVisible(true);
		}
		
		public void setSpawnedVehicles( int cars ) {
			vehiclesSpawnedField.setText( "" + cars);
		}
		
		
		public void setActiveVehicles( int cars ) {
			vehiclesActiveField.setText( "" + cars);
		}
		
		public void setReservations( int nReserv ) {
			reservationsField.setText( "" + nReserv);
		}
		
		public void setAverageDelay( long delay ) {
			averageDelayField.setText( "" + delay + " (ms)");
		}
		
	}

	public void log(String string) {
		Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String time = sdf.format(cal.getTime());
		
		loggerPanel.append("[" + time + "] " + string + System.lineSeparator());
	}
	
}