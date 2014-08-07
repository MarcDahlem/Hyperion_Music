package com.example.dahlem.Hyperionmusic;

import java.io.IOException;
import java.net.UnknownHostException;

import org.gstreamer.Bus;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.GstObject;
import org.gstreamer.Pipeline;

import com.example.dahlem.Hyperionmusic.Spectrum.SpectrumBusReceiver;

public class MusicReader {

	private Pipeline pipe;
	private int spect_bands;
	private int audio_frequency;
	private int num_channels;
	private boolean debug;
	private String device;
	private HyperionConnection hyperion;
	private int priority;
	private boolean pulse;
	private int threshold;
	private int maxdb;
	private int interval;
	private String hyperion_ip;
	private int hyperion_port;

	public MusicReader(int spect_bands, int audio_frequency, int num_channels, String device, int priority, int threshold, int maxdb, int interval, String hyperion_ip, int hyperion_port, boolean pulse, boolean debug) throws UnknownHostException, IOException {
		this.spect_bands = spect_bands;
		this.audio_frequency = audio_frequency;
		this.num_channels = num_channels;
		this.maxdb =maxdb;
		this.device=device;
		this.debug = debug;
		this.priority = priority;
		this.pulse=pulse;
		this.threshold=threshold;
		this.interval = interval;
		this.hyperion_ip = hyperion_ip;
		this.hyperion_port = hyperion_port;
		this.init();
	}

	private void addBusMessageListeners(Pipeline pipe) throws UnknownHostException, IOException {

		// get the bus
		Bus bus = pipe.getBus();

		// connect error messages na stop the pipe if an error occured
		bus.connect(new Bus.ERROR() {

			@Override
			public void errorMessage(GstObject source, int code, String message) {
				System.out.println("Error: " + message);/*
														 * MusicPipeEvent event
														 * = new MusicPipeEvent(
														 * MusicReader.this,
														 * source,
														 * MusicPipeEventType
														 * .GST_ERROR, message);
														 * MusicReader
														 * .this.notifyPipelineEvent
														 * (event);
														 * MusicReader.this
														 * .stop();
														 */
			}
		});

		// connect info messages
		bus.connect(new Bus.INFO() {
			@Override
			public void infoMessage(GstObject source, int code, String message) {
				System.out.println("Info: " + message); /*
														 * MusicPipeEvent event
														 * = new MusicPipeEvent(
														 * MusicReader.this,
														 * source,
														 * MusicPipeEventType
														 * .GST_INFO, message);
														 * ConnectionPipe
														 * .this.notifyPipelineEvent
														 * (event);
														 */
			}
		});

		// connect warnings
		bus.connect(new Bus.WARNING() {

			@Override
			public void warningMessage(GstObject source, int code,
					String message) {
				System.out.println("Warning: " + message);
				/*
				 * ConnectionPipeEvent event = new
				 * ConnectionPipeEvent(ConnectionPipe.this, source,
				 * ConnectionPipeEventType.GST_WARNING, message);
				 * ConnectionPipe.this.notifyPipelineEvent(event);
				 */
			}
		});

		SpectrumBusReceiver sBusreceiver = new SpectrumBusReceiver(audio_frequency,spect_bands,num_channels,debug);
		this.hyperion = new HyperionConnection(this.hyperion_ip,this.hyperion_port, this.priority, this.threshold, this.maxdb, this.debug);
		sBusreceiver.register(hyperion);
		bus.connect(sBusreceiver);

		// connect EOS detection and stop the pipe if EOS detected
		bus.connect(new Bus.EOS() {

			private boolean reantrance = false;

			@Override
			public void endOfStream(GstObject source) {
				System.out.println("EOS");/*
										 * if (!this.reantrance) {
										 * this.reantrance=true;
										 * ConnectionPipe.this.stop();
										 * ConnectionPipeEvent event = new
										 * ConnectionPipeEvent
										 * (ConnectionPipe.this, source,
										 * ConnectionPipeEventType.STOP,
										 * "EOS detected");
										 * ConnectionPipe.this.notifyPipelineEvent
										 * (event); }
										 */
			}
		});

	}

	/*
	 * gst-launch-0.10 -m alsasrc device=plughw:1,0 num-buffers=10000 !
	 * audio/x-raw-int,rate=8000,channels=1,depth=16 ! volume volume=3 ! queue !
	 * audioconvert ! spectrum bands=128 threshold=-90 post-messages=true
	 * message-phase=false message-magnitude=true interval=250000000 ! fakesink
	 */
	public void init() throws UnknownHostException, IOException {
		// create the main connection bin
		Pipeline pipe = new Pipeline("Connection Pipe on the Client");
		// add error, warning, eos and info listeners
		this.addBusMessageListeners(pipe);

		// create audio source
		Element src;
		if (this.pulse){
			src = ElementFactory.make("pulsesrc", "audio source");
		} else {
			src = ElementFactory.make("alsasrc", "audio source");
		}
		src.set("device", device);
		pipe.add(src);

		Element audiofilter = ElementFactory.make("capsfilter", "audio filter");
		audiofilter.setCaps(Caps.fromString("audio/x-raw-int,rate="
				+ audio_frequency + ",channels=" + num_channels + ",depth=16"));
		pipe.add(audiofilter);

		Element audioconvert = ElementFactory.make("audioconvert",
				"audioconvert");
		pipe.add(audioconvert);

		Element volume = ElementFactory.make("volume", "volume enhancer");
		volume.set("volume", 5.0);
		pipe.add(volume);

		Element spectrum = ElementFactory.make("spectrum",
				"spectrum audio analyzer");
		spectrum.set("bands", spect_bands);
		spectrum.set("threshold", this.threshold);
		spectrum.set("post-messages", true);
		spectrum.set("message-phase", true);
		spectrum.set("interval", this.interval);
		pipe.add(spectrum);

		// create the network connection over tcp
		Element fakeSink = ElementFactory.make("fakesink", "fakesink");
		fakeSink.set("sync", true);
		pipe.add(fakeSink);

		// link all elements
		Element.linkMany(src, audiofilter, audioconvert, volume, spectrum,
				fakeSink);/*
						 * Element.linkPadsFiltered(audioconvert, "sink",
						 * volume, "src", caps);
						 * Element.linkMany(volume,spectrum);
						 * Element.linkMany(spectrum,fakeSink);
						 */

		// set the pipe
		this.pipe = pipe;
	}

	public void start() {
		if (this.pipe == null) {
			throw new IllegalStateException(
					"Pipeline must be initialized before starting...");
		}
		System.out.println ("Starting pipe...");
		this.pipe.play();
		System.out.println ("Pipe started.");
	}

	public void stop() throws IOException {
		if (this.pipe == null) {
			throw new IllegalStateException(
					"Pipeline not initialized. Nothing to stop...");
		}
		System.out.println ("Close connection...");
		this.hyperion.closeConnection();
		System.out.println ("Connection closed. Stop pipe...");
		this.pipe.stop();
		System.out.println ("Pipe stopped.");
	}
}
