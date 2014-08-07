package com.example.dahlem.Hyperionmusic;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Scanner;

import org.gstreamer.Gst;
/**
 * 
 * @author marc
 * This Application consists of a console that controls a gstreamer pipeline to analyze music.
 * 
 */

public class Main {

	public static void main(String[] args){
		args = Gst.init("Hyperion.Music", args);
		//create and init a 'gui'
		new Thread(new Runnable(){
			@Override
			public void run(){
				try {
				System.out.println("Please press 's' to start, 'q' to quit");
				Scanner sc = new Scanner(System.in);
				boolean running=true;
				boolean runningMR=false;
				MusicReader musicReader=null;
				String in;
		        while(running && sc.hasNextLine()) {
		        	in = sc.nextLine();
		        	switch (in) {
		        		case "s":
		        			if (runningMR) {
		        				System.out.println("Music is already analyzed. Press 'q' to stop.");
		        			}else  {
		        				runningMR=true;
		        				try {
		        					musicReader = new MusicReader(128,44100,2, "alsa_output.pci-0000_00_1b.0.iec958-stereo.monitor",100,-50, 0, 80000000, true, false); //250000000 = 0,25s //10000000 = 0,01seconds //80000000 = 0,08seconds
									//musicReader = new MusicReader(128,8000,1, "plughw:0,0",100, true, true);
								} catch (UnknownHostException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
		        				musicReader.start();
		        			}
		        		break;
		        		case "q":
		        			System.out.println("Stopping program...");
		        			if (runningMR) {
		        				musicReader.stop();
		        				runningMR = false;
		        			}
		        			running=false;
		        			break;
		        		default:
		        			System.out.println("Unknown command. Please press 's' or 'q' followed by an \\CR");
		        			
		        	}
		        }
		        sc.close();
		        System.out.println("Program stopped successfully");
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
	}
}
