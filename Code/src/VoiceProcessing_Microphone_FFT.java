import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.GainProcessor;
import be.tarsos.dsp.PitchShifter;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.io.jvm.WaveformWriter;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class VoiceProcessing_Microphone_FFT extends JFrame implements PitchDetectionHandler {
		
	// initialize general variables   
	private static final long serialVersionUID = 1L;
    private String name;
    private String note;
    private int octave;
    private int[] scale = new int[15]; // fifteen notes in a two-octave scale
    	
    // initialize components
    private AudioDispatcher dispatcher;
	private WaveformWriter writerUnedited;
	private PitchProcessor pitchAnalyzer;
	private PitchShifter pitchShifter;
	private GainProcessor volumeShifter;
	private WaveformWriter writerEdited;
    
    public static void main(String[] args) {
        
    	// get program ready to run
    	JFrame frame = new VoiceProcessing_Microphone_FFT();
		frame.pack();
        frame.setSize(340, 195);
        frame.setResizable(false);
		frame.setVisible(true);
     
    }
    
	public VoiceProcessing_Microphone_FFT() {
    	
    	// initialize GUI
        this.getContentPane().setLayout(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// initialize labels
        JLabel labelStatus = new JLabel();
        
        // initialize buttons
        JButton buttonStart = new JButton("Start");
        JButton buttonStop = new JButton("Stop");
             
        // add status label to GUI       
        labelStatus.setLocation(0, 30);
        labelStatus.setSize(340, 40);
        labelStatus.setHorizontalAlignment(SwingConstants.CENTER);
		getContentPane().add(labelStatus);
        
        // add start button to GUI
        buttonStart.setLocation(50, 100);
        buttonStart.setSize(100, 30);
        buttonStart.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
    			buttonStart.setEnabled(false);
    			buttonStop.setEnabled(true);
            	try {
        			startIO();
        		} catch (Exception e) {
        			System.out.print("Error with audio input/output!");
        		}
            }
        });
        getContentPane().add(buttonStart);
        
        // add stop button to GUI
        buttonStop.setLocation(190, 100);
        buttonStop.setSize(100, 30);
        buttonStop.setEnabled(false);
        buttonStop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
    			buttonStart.setEnabled(true);
    			buttonStop.setEnabled(false);
            	stopIO();
            }
        });
        getContentPane().add(buttonStop);
        
        // load user voice profile and create corresponding scale
		try {
			loadProfile();
	    	createScale();
			this.setTitle("Voice Processing for " + name);
	    	labelStatus.setText("<html><div style='text-align: center;'><font color='green'>User voice profile successfully loaded!<br>You can now start the voice processing.</font></div></html>");
		} catch (Exception e) {
            buttonStart.setEnabled(false);
    		this.setTitle("Voice Processing");
        	labelStatus.setText("<html><div style='text-align: center;'><font color='red'>No user voice profile found!<br>Please run the voice profiling program first.</font></div></html>");
		}
		
    }
    
    private void startIO() throws Exception {
    	
    	// delete audio file
		File fileUnedited = new File("Unedited.wav");
		File fileEdited = new File("FFT.wav");
		if (fileUnedited.exists()) {
			fileUnedited.delete();
		}
		if (fileEdited.exists()) {
			fileEdited.delete();
		}
        
        // initialize general variables
        int sampleRate = 44100; // in Hz
        int bufferSize = 2048;  // in samples
        
        // initialize audio stream
        AudioFormat format = new AudioFormat((float) sampleRate, 16, 1, true, false);
        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, format));
        line.open(format, bufferSize);
        line.start();
     	JVMAudioInputStream stream = new JVMAudioInputStream(new AudioInputStream(line));
     	
     	// initialize components
		dispatcher = new AudioDispatcher(stream, bufferSize, bufferSize-256);
		writerUnedited = new WaveformWriter(format, "Unedited.wav");
     	pitchAnalyzer = new PitchProcessor(PitchEstimationAlgorithm.YIN, sampleRate, bufferSize, this);
     	pitchShifter = new PitchShifter(1, sampleRate, bufferSize, bufferSize-256);
     	volumeShifter = new GainProcessor(4);
		writerEdited = new WaveformWriter(format, "FFT.wav");

     	// add components and start audio dispatcher
		dispatcher.addAudioProcessor(writerUnedited);
		dispatcher.addAudioProcessor(pitchAnalyzer);
		dispatcher.addAudioProcessor(pitchShifter);
		dispatcher.addAudioProcessor(volumeShifter);
		dispatcher.addAudioProcessor(writerEdited);
		dispatcher.addAudioProcessor(new AudioPlayer(dispatcher.getFormat()));
		new Thread(dispatcher).start();
		     	
    }
    
    private void stopIO() {
        
    	// remove components and stop audio dispatcher
		dispatcher.removeAudioProcessor(writerEdited);
		dispatcher.removeAudioProcessor(volumeShifter);
		dispatcher.removeAudioProcessor(pitchShifter);
        dispatcher.removeAudioProcessor(pitchAnalyzer);
		dispatcher.removeAudioProcessor(writerUnedited);
    	dispatcher.stop();
    	
    }
    
    private void loadProfile() throws Exception {
    	
   		// look for file
		File file = new File("Profile.txt");
		if (!file.exists()) {
			throw new Exception();
		}
    	
    	// read string from file
    	BufferedReader bufferedReader = new BufferedReader(new FileReader(file.getAbsoluteFile()));
    	name = bufferedReader.readLine();
    	note = bufferedReader.readLine();
    	octave = Integer.parseInt(bufferedReader.readLine());
    	bufferedReader.close();
    	
    }
    
    private void createScale() {
    	
    	// create two-octave scale for each note
		switch (note) {
			case "C":				
				scale[0]  = computeOffset("C")  + ((octave-1)*12) - 8;
				scale[1]  = computeOffset("D")  + ((octave-1)*12) - 8;
				scale[2]  = computeOffset("E")  + ((octave-1)*12) - 8;
				scale[3]  = computeOffset("F")  + ((octave-1)*12) - 8;
				scale[4]  = computeOffset("G")  + ((octave-1)*12) - 8;
				scale[5]  = computeOffset("A")  + ((octave-1)*12) - 8;
				scale[6]  = computeOffset("B")  + ((octave-1)*12) - 8;
				scale[7]  = computeOffset("C")  + (octave*12) - 8;
				scale[8]  = computeOffset("D")  + (octave*12) - 8;
				scale[9]  = computeOffset("E")  + (octave*12) - 8;
				scale[10] = computeOffset("F")  + (octave*12) - 8;
				scale[11] = computeOffset("G")  + (octave*12) - 8;
				scale[12] = computeOffset("A")  + (octave*12) - 8;
				scale[13] = computeOffset("B")  + (octave*12) - 8;
				scale[14] = computeOffset("C")  + ((octave+1)*12) - 8;
				break;
			case "C#":
				scale[0]  = computeOffset("C#") + ((octave-1)*12) - 8;
				scale[1]  = computeOffset("D#") + ((octave-1)*12) - 8;
				scale[2]  = computeOffset("F")  + ((octave-1)*12) - 8;
				scale[3]  = computeOffset("F#") + ((octave-1)*12) - 8;
				scale[4]  = computeOffset("G#") + ((octave-1)*12) - 8;
				scale[5]  = computeOffset("A#") + ((octave-1)*12) - 8;
				scale[6]  = computeOffset("C")  + (octave*12) - 8;
				scale[7]  = computeOffset("C#") + (octave*12) - 8;
				scale[8]  = computeOffset("D#") + (octave*12) - 8;
				scale[9]  = computeOffset("F")  + (octave*12) - 8;
				scale[10] = computeOffset("F#") + (octave*12) - 8;
				scale[11] = computeOffset("G#") + (octave*12) - 8;
				scale[12] = computeOffset("A#") + (octave*12) - 8;
				scale[13] = computeOffset("C")  + ((octave+1)*12) - 8;
				scale[14] = computeOffset("C#") + ((octave+1)*12) - 8;
				break;
			case "D":
				scale[0]  = computeOffset("D")  + ((octave-1)*12) - 8;
				scale[1]  = computeOffset("E")  + ((octave-1)*12) - 8;
				scale[2]  = computeOffset("F#") + ((octave-1)*12) - 8;
				scale[3]  = computeOffset("G")  + ((octave-1)*12) - 8;
				scale[4]  = computeOffset("A")  + ((octave-1)*12) - 8;
				scale[5]  = computeOffset("B")  + ((octave-1)*12) - 8;
				scale[6]  = computeOffset("C#") + (octave*12) - 8;
				scale[7]  = computeOffset("D")  + (octave*12) - 8;
				scale[8]  = computeOffset("E")  + (octave*12) - 8;
				scale[9]  = computeOffset("F#") + (octave*12) - 8;
				scale[10] = computeOffset("G")  + (octave*12) - 8;
				scale[11] = computeOffset("A")  + (octave*12) - 8;
				scale[12] = computeOffset("B")  + (octave*12) - 8;
				scale[13] = computeOffset("C#") + ((octave+1)*12) - 8;
				scale[14] = computeOffset("D")  + ((octave+1)*12) - 8;
				break;
			case "D#":
				scale[0]  = computeOffset("D#") + ((octave-1)*12) - 8;
				scale[1]  = computeOffset("F")  + ((octave-1)*12) - 8;
				scale[2]  = computeOffset("G")  + ((octave-1)*12) - 8;
				scale[3]  = computeOffset("G#") + ((octave-1)*12) - 8;
				scale[4]  = computeOffset("A#") + ((octave-1)*12) - 8;
				scale[5]  = computeOffset("C")  + (octave*12) - 8;
				scale[6]  = computeOffset("D")  + (octave*12) - 8;
				scale[7]  = computeOffset("D#") + (octave*12) - 8;
				scale[8]  = computeOffset("F")  + (octave*12) - 8;
				scale[9]  = computeOffset("G")  + (octave*12) - 8;
				scale[10] = computeOffset("G#") + (octave*12) - 8;
				scale[11] = computeOffset("A")  + (octave*12) - 8;
				scale[12] = computeOffset("C")  + ((octave+1)*12) - 8;
				scale[13] = computeOffset("D")  + ((octave+1)*12) - 8;
				scale[14] = computeOffset("D#") + ((octave+1)*12) - 8;
				break;
			case "E":
				scale[0]  = computeOffset("E")  + ((octave-1)*12) - 8;
				scale[1]  = computeOffset("F#") + ((octave-1)*12) - 8;
				scale[2]  = computeOffset("G#") + ((octave-1)*12) - 8;
				scale[3]  = computeOffset("A")  + ((octave-1)*12) - 8;
				scale[4]  = computeOffset("B")  + ((octave-1)*12) - 8;
				scale[5]  = computeOffset("C#") + (octave*12) - 8;
				scale[6]  = computeOffset("D#") + (octave*12) - 8;
				scale[7]  = computeOffset("E")  + (octave*12) - 8;
				scale[8]  = computeOffset("F#") + (octave*12) - 8;
				scale[9]  = computeOffset("G#") + (octave*12) - 8;
				scale[10] = computeOffset("A")  + (octave*12) - 8;
				scale[11] = computeOffset("B")  + (octave*12) - 8;
				scale[12] = computeOffset("C#") + ((octave+1)*12) - 8;
				scale[13] = computeOffset("D#") + ((octave+1)*12) - 8;
				scale[14] = computeOffset("E")  + ((octave+1)*12) - 8;
				break;
			case "F":
				scale[0]  = computeOffset("F")  + ((octave-1)*12) - 8;
				scale[1]  = computeOffset("G")  + ((octave-1)*12) - 8;
				scale[2]  = computeOffset("A")  + ((octave-1)*12) - 8;
				scale[3]  = computeOffset("A#") + ((octave-1)*12) - 8;
				scale[4]  = computeOffset("C")  + (octave*12) - 8;
				scale[5]  = computeOffset("D")  + (octave*12) - 8;
				scale[6]  = computeOffset("E")  + (octave*12) - 8;
				scale[7]  = computeOffset("F")  + (octave*12) - 8;
				scale[8]  = computeOffset("G")  + (octave*12) - 8;
				scale[9]  = computeOffset("A")  + (octave*12) - 8;
				scale[10] = computeOffset("A#") + (octave*12) - 8;
				scale[11] = computeOffset("C")  + ((octave+1)*12) - 8;
				scale[12] = computeOffset("D")  + ((octave+1)*12) - 8;
				scale[13] = computeOffset("E")  + ((octave+1)*12) - 8;
				scale[14] = computeOffset("F")  + ((octave+1)*12) - 8;
				break;
			case "F#":
				scale[0]  = computeOffset("F#") + ((octave-1)*12) - 8;
				scale[1]  = computeOffset("G#") + ((octave-1)*12) - 8;
				scale[2]  = computeOffset("A#") + ((octave-1)*12) - 8;
				scale[3]  = computeOffset("B")  + ((octave-1)*12) - 8;
				scale[4]  = computeOffset("C#") + (octave*12) - 8;
				scale[5]  = computeOffset("D#") + (octave*12) - 8;
				scale[6]  = computeOffset("F")  + (octave*12) - 8;
				scale[7]  = computeOffset("F#") + (octave*12) - 8;
				scale[8]  = computeOffset("G#") + (octave*12) - 8;
				scale[9]  = computeOffset("A#") + (octave*12) - 8;
				scale[10] = computeOffset("B")  + (octave*12) - 8;
				scale[11] = computeOffset("C#") + ((octave+1)*12) - 8;
				scale[12] = computeOffset("D#") + ((octave+1)*12) - 8;
				scale[13] = computeOffset("F")  + ((octave+1)*12) - 8;
				scale[14] = computeOffset("F#") + ((octave+1)*12) - 8;
				break;
			case "G":
				scale[0]  = computeOffset("G")  + ((octave-1)*12) - 8;
				scale[1]  = computeOffset("A")  + ((octave-1)*12) - 8;
				scale[2]  = computeOffset("B")  + ((octave-1)*12) - 8;
				scale[3]  = computeOffset("C")  + (octave*12) - 8;
				scale[4]  = computeOffset("D")  + (octave*12) - 8;
				scale[5]  = computeOffset("E")  + (octave*12) - 8;
				scale[6]  = computeOffset("F#") + (octave*12) - 8;
				scale[7]  = computeOffset("G")  + (octave*12) - 8;
				scale[8]  = computeOffset("A")  + (octave*12) - 8;
				scale[9]  = computeOffset("B")  + (octave*12) - 8;
				scale[10] = computeOffset("C")  + ((octave+1)*12) - 8;
				scale[11] = computeOffset("D")  + ((octave+1)*12) - 8;
				scale[12] = computeOffset("E")  + ((octave+1)*12) - 8;
				scale[13] = computeOffset("F#") + ((octave+1)*12) - 8;
				scale[14] = computeOffset("G")  + ((octave+1)*12) - 8;
				break;
			case "G#":
				scale[0]  = computeOffset("G#") + ((octave-1)*12) - 8;
				scale[1]  = computeOffset("A#") + ((octave-1)*12) - 8;
				scale[2]  = computeOffset("C")  + (octave*12) - 8;
				scale[3]  = computeOffset("C#") + (octave*12) - 8;
				scale[4]  = computeOffset("D#") + (octave*12) - 8;
				scale[5]  = computeOffset("F")  + (octave*12) - 8;
				scale[6]  = computeOffset("G")  + (octave*12) - 8;
				scale[7]  = computeOffset("G#") + (octave*12) - 8;
				scale[8]  = computeOffset("A#") + (octave*12) - 8;
				scale[9]  = computeOffset("C")  + ((octave+1)*12) - 8;
				scale[10] = computeOffset("C#") + ((octave+1)*12) - 8;
				scale[11] = computeOffset("D#") + ((octave+1)*12) - 8;
				scale[12] = computeOffset("F")  + ((octave+1)*12) - 8;
				scale[13] = computeOffset("G")  + ((octave+1)*12) - 8;
				scale[14] = computeOffset("G#") + ((octave+1)*12) - 8;
				break;
			case "A":
				scale[0]  = computeOffset("A")  + ((octave-1)*12) - 8;
				scale[1]  = computeOffset("B")  + ((octave-1)*12) - 8;
				scale[2]  = computeOffset("C#") + (octave*12) - 8;
				scale[3]  = computeOffset("D")  + (octave*12) - 8;
				scale[4]  = computeOffset("E")  + (octave*12) - 8;
				scale[5]  = computeOffset("F#") + (octave*12) - 8;
				scale[6]  = computeOffset("G#") + (octave*12) - 8;
				scale[7]  = computeOffset("A")  + (octave*12) - 8;
				scale[8]  = computeOffset("B")  + (octave*12) - 8;
				scale[9]  = computeOffset("C#") + ((octave+1)*12) - 8;
				scale[10] = computeOffset("D")  + ((octave+1)*12) - 8;
				scale[11] = computeOffset("E")  + ((octave+1)*12) - 8;
				scale[12] = computeOffset("F#") + ((octave+1)*12) - 8;
				scale[13] = computeOffset("G#") + ((octave+1)*12) - 8;
				scale[14] = computeOffset("A")  + ((octave+1)*12) - 8;
				break;	
			case "A#":
				scale[0]  = computeOffset("A#") + ((octave-1)*12) - 8;
				scale[1]  = computeOffset("C")  + (octave*12) - 8;
				scale[2]  = computeOffset("D")  + (octave*12) - 8;
				scale[3]  = computeOffset("D#") + (octave*12) - 8;
				scale[4]  = computeOffset("F")  + (octave*12) - 8;
				scale[5]  = computeOffset("G")  + (octave*12) - 8;
				scale[6]  = computeOffset("A")  + (octave*12) - 8;
				scale[7]  = computeOffset("A#") + (octave*12) - 8;
				scale[8]  = computeOffset("C")  + ((octave+1)*12) - 8;
				scale[9]  = computeOffset("D")  + ((octave+1)*12) - 8;
				scale[10] = computeOffset("D#") + ((octave+1)*12) - 8;
				scale[11] = computeOffset("F")  + ((octave+1)*12) - 8;
				scale[12] = computeOffset("G")  + ((octave+1)*12) - 8;
				scale[13] = computeOffset("A")  + ((octave+1)*12) - 8;
				scale[14] = computeOffset("A#") + ((octave+1)*12) - 8;
				break;
			case "B":
				scale[0]  = computeOffset("B")  + ((octave-1)*12) - 8;
				scale[1]  = computeOffset("C#") + (octave*12) - 8;
				scale[2]  = computeOffset("D#") + (octave*12) - 8;
				scale[3]  = computeOffset("E")  + (octave*12) - 8;
				scale[4]  = computeOffset("F#") + (octave*12) - 8;
				scale[5]  = computeOffset("G#") + (octave*12) - 8;
				scale[6]  = computeOffset("A#") + (octave*12) - 8;
				scale[7]  = computeOffset("B")  + (octave*12) - 8;
				scale[8]  = computeOffset("C#") + ((octave+1)*12) - 8;
				scale[9]  = computeOffset("D#") + ((octave+1)*12) - 8;
				scale[10] = computeOffset("E")  + ((octave+1)*12) - 8;
				scale[11] = computeOffset("F#") + ((octave+1)*12) - 8;
				scale[12] = computeOffset("G#") + ((octave+1)*12) - 8;
				scale[13] = computeOffset("A#") + ((octave+1)*12) - 8;
				scale[14] = computeOffset("B")  + ((octave+1)*12) - 8;
				break;
		}
	
    }
    
	@Override
	public void handlePitch(PitchDetectionResult result, AudioEvent audioEvent) {
								
		if (result.isPitched()) {	
			// compute pitch of audio event
			double pitch = result.getPitch();
			double pitchScale;
			double differenceCents;
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(String.format("Pitch is %.2f Hz", pitch));

			// shift pitch of audio event up or down (if in semitone proximity to note of scale)
			for (int i = 0; i < scale.length; i++) {
				pitchScale = computePitch(scale[i]);
				differenceCents = computeDifferenceCents(pitchScale, pitch);
				if (differenceCents > 0 && differenceCents <= 100) {
					pitchShifter.setPitchShiftFactor((float) computeDifferenceFactor(differenceCents));
					stringBuilder.append(String.format(", but shifted down to %.2f Hz (equals ", computePitch(scale[i])));
					stringBuilder.append("musical note " + computeNote(scale[i]) + ")");
					break;
				} else if (differenceCents < 0 && differenceCents > -100) {
					pitchShifter.setPitchShiftFactor((float) computeDifferenceFactor(differenceCents));
					stringBuilder.append(String.format(", but shifted up to %.2f Hz (equals ", computePitch(scale[i])));
					stringBuilder.append("musical note " + computeNote(scale[i]) + ")");
					break;
				}
			}
			System.out.println(stringBuilder.toString());
		}
			
	}
	
	private double computePitch(double key) {
		
		// compute pitch for given piano key
		double power = (key-49) / 12;
		return Math.pow(2, power) * 440;
		
	}
	
	private int computeOffset(String note) {
		
		// compute offset for given musical note
		int offset = 0;
		switch (note) {
			case "C":  offset = 0;  break;
			case "C#": offset = 1;  break;
			case "D":  offset = 2;  break;
			case "D#": offset = 3;  break;
			case "E":  offset = 4;  break;
			case "F":  offset = 5;  break;
			case "F#": offset = 6; break;
			case "G":  offset = 7; break;
			case "G#": offset = 8;  break;
			case "A":  offset = 9;  break;
			case "A#": offset = 10;  break;
			case "B":  offset = 11;  break;
		}
   		return offset;
		
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
	
	private double computeDifferenceCents(double pitch, double pitchRounded) {
		
		// compute difference in cents for two pitches
		double logarithm = Math.log(pitchRounded / pitch) / Math.log(2);
		double difference = Math.round(1200 * logarithm);
		return difference;
		
	}
	
	private double computeDifferenceFactor(double cents) {
		
		// compute factorial difference for given cents
		double power = (cents*Math.log(2)) / (1200*Math.log(Math.E));
		return 1 / Math.pow(Math.E, power);
		
	}
	
}