import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class VoiceProfiling_File extends JFrame implements PitchDetectionHandler {
		
	// initialize general variables   
	private static final long serialVersionUID = 1L;
    private String name;
    private String note;
    private int octave;
    private int[] keyOccurrences = new int[88]; // eighty-eight piano keys
    	
    // initialize components
    private AudioDispatcher dispatcher;
	private PitchProcessor pitchAnalyzer;
    
    public static void main(String[] args) {
        
    	// get program ready to run
    	JFrame frame = new VoiceProfiling_File();
		frame.pack();
        frame.setSize(653, 403);
        frame.setResizable(true);
		frame.setVisible(true);
     
    }
    
	public VoiceProfiling_File() {
    	
    	// initialize GUI
        this.getContentPane().setLayout(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Voice Profiling");
		
		// initialize labels
		JLabel labelName = new JLabel("1. Write your first name into the text field.");	
		JLabel labelText = new JLabel("2. Press the start button and let the file play. When it's done, press the stop button.");
		JLabel labelProfile = new JLabel("3. Press the button to create your personal voice profile.");
        JLabel labelStatus = new JLabel();
		
		// initialize text fields
        JTextField fieldName = new JTextField();        
        
        // initialize buttons
        JButton buttonStart = new JButton("Start");
        JButton buttonStop = new JButton("Stop");
        JButton buttonProfile = new JButton("Create profile");
        
        // initialize separators
        JSeparator lineOne = new JSeparator();
        JSeparator lineTwo = new JSeparator();
		
        // add name label to first part of GUI
        labelName.setLocation(30, 20);
        labelName.setSize(653, 30);
		getContentPane().add(labelName);
		
        // add name field to first part of GUI
		fieldName.setLocation(29, 60);
		fieldName.setSize(150, 30);
		getContentPane().add(fieldName);
		
        // add separator between first and second part of GUI
		lineOne.setLocation(0, 115);
		lineOne.setSize(653, 100);
		getContentPane().add(lineOne);
		
        // add text label to second part of GUI
		labelText.setLocation(30, 145);
		labelText.setSize(653, 30);
		getContentPane().add(labelText);
        
        // add start button to second part of GUI
        buttonStart.setLocation(25, 192);
        buttonStart.setSize(100, 30);
        buttonStart.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
    			buttonStart.setEnabled(false);
    			buttonStop.setEnabled(true);
    			fieldName.setEditable(false);
            	try {
        			name = fieldName.getText();
        			startIO();
        		} catch (Exception e) {
        			System.out.print("Error with audio input/output!");
        		}
            }
        });
        getContentPane().add(buttonStart);
        
        // add stop button to second part of GUI
        buttonStop.setLocation(150, 192);
        buttonStop.setSize(100, 30);
        buttonStop.setEnabled(false);
        buttonStop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
    			buttonStop.setEnabled(false);
    			buttonProfile.setEnabled(true);
            	stopIO();
            }
        });
        getContentPane().add(buttonStop);      
        
        // add separator between second and third part of GUI
        lineTwo.setLocation(0, 249);
        lineTwo.setSize(653, 100);
		getContentPane().add(lineTwo);
        
        // add profile label to third part of GUI       
		labelProfile.setLocation(30, 279);
		labelProfile.setSize(653, 30);
		getContentPane().add(labelProfile);
        
        // add profile button to third part of GUI
        buttonProfile.setLocation(25, 318);
        buttonProfile.setSize(160, 30);
        buttonProfile.setEnabled(false);
        buttonProfile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
            	buttonProfile.setEnabled(false);
    			try {
					createProfile();
	            	labelStatus.setVisible(true);
	            	labelStatus.setText("<html><font color='green'>Profile successfully created! You can now close this window.</font></html>");
				} catch (Exception e) {
	            	labelStatus.setVisible(true);
	            	labelStatus.setText("<html><font color='red'>Error with creating profile! Please close this window and try again.</font></html>");
				}
            }
        });
        getContentPane().add(buttonProfile);
        
        // add status label to third part of GUI       
        labelStatus.setLocation(205, 317);
        labelStatus.setSize(500, 30);
        labelStatus.setVisible(false);
		getContentPane().add(labelStatus);
       
    }
    
    private void startIO() throws Exception {
        
        // initialize general variables
        int sampleRate = 44100; // in Hz
        int bufferSize = 2048;  // in samples        
     	
     	// initialize components
		dispatcher = AudioDispatcherFactory.fromFile(new File("Unedited.wav"), bufferSize, 0);
     	pitchAnalyzer = new PitchProcessor(PitchEstimationAlgorithm.YIN, sampleRate, bufferSize, this);
     	
     	// add components and start audio dispatcher
		dispatcher.addAudioProcessor(new AudioPlayer(dispatcher.getFormat()));
		dispatcher.addAudioProcessor(pitchAnalyzer);
		new Thread(dispatcher).start();
		     	
    }
    
    private void stopIO() {
    			
    	// determine most frequently hit musical note
    	int max = 0;
        int maxIndex = 0;
        for (int i = 0; i < keyOccurrences.length; i++) {
            if (keyOccurrences[i] > max) {
                max = keyOccurrences[i];
                maxIndex = i;
            }
        }
        note = computeNote(maxIndex);
        octave = (maxIndex+8) / 12;
        
    	// remove components and stop audio dispatcher
        dispatcher.removeAudioProcessor(pitchAnalyzer);
    	dispatcher.stop();
    	
    }
    
    private void createProfile() throws Exception {
    	
    	// build string to be written in file
    	StringBuilder stringBuilder = new StringBuilder();
   		stringBuilder.append(name);
    	stringBuilder.append("\n");
   		stringBuilder.append(note);
   		stringBuilder.append("\n");
   		stringBuilder.append(octave);
    	
   		// create file
		File file = new File("Profile.txt");
		if (!file.exists()) {
			file.createNewFile();
		}

		// write string into file
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
		bufferedWriter.write(stringBuilder.toString());
		bufferedWriter.close();
		
    }
    
	@Override
	public void handlePitch(PitchDetectionResult result, AudioEvent audioEvent) {
								
		if (result.isPitched()) {		
			// compute pitch of audio event and round it to closest piano key
			double pitch = result.getPitch();						
			double keyRounded = Math.round(computeKey(pitch));
			
			// update key occurrences array
			if (keyRounded <= 88) {
				int index = (int) (keyRounded-1);
				keyOccurrences[index]++;
			}
		}
			
	}
	
	private double computeKey(double pitch) {
		
		// compute piano key for given pitch
		double logarithm = Math.log(pitch/440) / Math.log(2);
		return logarithm * 12 + 49;
	
	}
	
	private String computeNote(int key) {
		
		// compute musical note for given piano key
		String note = "N/A";
		switch (key % 12) {
			case 0:  note = "G#"; break;
			case 1:  note = "A";  break;
			case 2:  note = "A#"; break;
			case 3:  note = "B";  break;
			case 4:  note = "C";  break;
			case 5:  note = "C#"; break;
			case 6:  note = "D";  break;
			case 7:  note = "D#"; break;
			case 8:  note = "E";  break;
			case 9:  note = "F";  break;
			case 10: note = "F#"; break;
			case 11: note = "G";  break;
		}
   		return note;
		
	}
	
}