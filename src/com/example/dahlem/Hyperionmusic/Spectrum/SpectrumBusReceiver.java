package com.example.dahlem.Hyperionmusic.Spectrum;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.gstreamer.Bus;
import org.gstreamer.Bus.MESSAGE;
import org.gstreamer.Message;
import org.gstreamer.Structure;
import org.gstreamer.ValueList;

public class SpectrumBusReceiver implements MESSAGE {
	
	private int audio_frequency;
	private int spect_bands;
	private int num_channels;
	private boolean debug;
	private List<SpectrumMessageListener> listeners;

	public SpectrumBusReceiver(int audio_frequency, int spect_bands,int num_channels,boolean debug) {
		this.listeners = new LinkedList<SpectrumMessageListener>();
		this.audio_frequency=audio_frequency;
		this.spect_bands=spect_bands;
		this.num_channels=num_channels;
		this.debug=debug;
	}

	@Override
	public void busMessage(Bus bus, Message message) {
		Structure struct = message.getStructure();
		String name = struct.getName();
		if (debug) {
			System.out.println("Bus message received from '" + name
					+ "'");
		}
		if ("spectrum".equals(name)) {
			ValueList magnitutes = struct.getValueList("magnitude");
			ValueList phases = struct.getValueList("phase");

			float mag, phase, freq;
			SpectrumEvent event = new SpectrumEvent(this);
			for (int i = 0; i < spect_bands; ++i) {
				freq = ((audio_frequency / num_channels) * i + audio_frequency
						/ (num_channels * 2))
						/ spect_bands;
				mag = magnitutes.getFloat(i);
				phase = phases.getFloat(i);
				if (debug) {
					System.out.println("band " + i + " freq " + freq
							+ ": magnitute " + mag + "dB, phase "
							+ phase);
				}
				SpectrumElement elem = new SpectrumElement(freq,mag,phase);
				event.addSpectrumElem(elem);
				
			}
			this.informListeners(event);

		}
	}
	
	public void register(SpectrumMessageListener listener) {
		this.listeners.add(listener);
	}
	
	private void informListeners(SpectrumEvent event) {
		for (SpectrumMessageListener listener: this.listeners) {
			listener.handleSpectrumMessage(event);
		}
	}

}
