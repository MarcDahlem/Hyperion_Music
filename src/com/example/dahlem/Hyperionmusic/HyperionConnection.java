package com.example.dahlem.Hyperionmusic;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.PriorityQueue;

import proto.Message.ClearRequest;
import proto.Message.ColorRequest;
import proto.Message.HyperionReply;
import proto.Message.HyperionRequest;
import proto.Message.ImageRequest;

import com.example.dahlem.Hyperionmusic.Spectrum.SpectrumElement;
import com.example.dahlem.Hyperionmusic.Spectrum.SpectrumEvent;
import com.example.dahlem.Hyperionmusic.Spectrum.SpectrumMessageListener;
import com.google.protobuf.ByteString;

public class HyperionConnection implements SpectrumMessageListener {

	private Socket socket;
	private int priority;
	private volatile boolean inside;
	private volatile boolean toclose;
	private final boolean debug;
	private int min_thresh;
	private int max_thresh;

	public HyperionConnection(String ip, int port, int priority, int threshold,
			int maxdb, boolean debug) throws UnknownHostException, IOException {
		this.debug = debug;
		this.socket = new Socket(ip, port);
		this.priority = priority;
		this.min_thresh = threshold;
		this.max_thresh = maxdb;
	}

	@Override
	public void handleSpectrumMessage(SpectrumEvent event) {
		if (!this.inside) { // throw away all events, that appear to fast
			this.inside = true;
			PriorityQueue<SpectrumElement> elems = event.getElements();
			byte[] image = computeImage(elems, 16, 9);
			try {
				this.setImage(image, 16, 9, this.priority);

				if (this.toclose) {
					performClose();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.inside = false;
		}
	}

	private void clear(int priority) throws IOException {
		ClearRequest clearRequest = ClearRequest.newBuilder()
				.setPriority(priority).build();

		HyperionRequest request = HyperionRequest.newBuilder()
				.setCommand(HyperionRequest.Command.CLEAR)
				.setExtension(ClearRequest.clearRequest, clearRequest).build();

		sendRequest(request);
	}

	private void clearall() throws IOException {
		HyperionRequest request = HyperionRequest.newBuilder()
				.setCommand(HyperionRequest.Command.CLEARALL).build();

		sendRequest(request);
	}

	private void performClose() throws IOException {
		if (socket != null && socket.isConnected()) {
			this.clear(this.priority);
			this.clearall();
			socket.close();
		}
	}

	private byte[] computeImage(PriorityQueue<SpectrumElement> elems,
			int width, int height) {
		byte[] res = new byte[3 * width * height];

		float freq;
		float mag;
		float phase;
		float max_bass = Float.NEGATIVE_INFINITY;
		float max_bass_freq = 0;
		float max_middle_freq = 0;
		float max_middle = Float.NEGATIVE_INFINITY;
		float max_high_freq = 0;
		float max_high = Float.NEGATIVE_INFINITY;

		int bass_min_freq = 200;
		int bass_max_freq = 1500;
		int middle_max_freq = 27000;
		int high_max_freq = Integer.MAX_VALUE;

		int t_bass_min = -20;
		int t_bass_max = 10;
		int t_middle_min = -30;
		int t_middle_max = -15;
		int t_high_min = -50;
		int t_high_max = -20;

		for (SpectrumElement elem : elems) {

			freq = elem.getFreqeuncy();

			mag = elem.getMagnitude();
			phase = elem.getPhase();

			if (freq >= bass_min_freq && freq <= high_max_freq) {
				if (freq < bass_max_freq) {
					// freq bass
					if (max_bass < mag) {
						max_bass = mag;
						max_bass_freq = freq;
					}
				} else {
					if (freq < middle_max_freq) {
						// freq middle
						if (max_middle < mag) {
							max_middle = mag;
							max_middle_freq = freq;
						}
					} else {
						// freq high
						if (max_high < mag) {
							max_high = mag;
							max_high_freq = freq;
						}
					}
				}
			}
		}

		int normalized_max_bass = normalize_to_rgb_value(max_bass, t_bass_min,
				t_bass_max);
		int normalized_max_middle = normalize_to_rgb_value(max_middle,
				t_middle_min, t_middle_max);
		int normalized_max_high = normalize_to_rgb_value(max_high, t_high_min,
				t_high_max);

		if (debug) {
			System.out.println("Max(bass) = " + max_bass + ", freq = "
					+ max_bass_freq);
			System.out.println("Max(middle) = " + max_middle + ", freq = "
					+ max_middle_freq);
			System.out.println("Max(high) = " + max_high + ", freq = "
					+ max_high_freq);
			System.out.println("Normalized max(bass) to RGB is '"
					+ normalized_max_bass + "'.");
			System.out.println("Normalized max(middle) to RGB is '"
					+ normalized_max_middle + "'.");
			System.out.println("Normalized max(high) to RGB is '"
					+ normalized_max_high + "'.");
		}

		int i;
		for (i = 0; i < 2*width; i++) {
			res[3 * i] = Integer.valueOf(0).byteValue();
			res[3 * i + 1] = Integer.valueOf(0).byteValue();
			res[3 * i + 2] = Integer.valueOf(normalized_max_bass).byteValue();
		}

		for (; i < ( 6.5*width); i++) {
			res[3 * i] = Integer.valueOf(0).byteValue();
			res[3 * i + 1] = Integer.valueOf(normalized_max_middle).byteValue();
			res[3 * i + 2] = Integer.valueOf(0).byteValue();
		}
		for (; i < (width * height); i++) {
			res[3 * i] = Integer.valueOf(normalized_max_high).byteValue();
			res[3 * i + 1] = Integer.valueOf(0).byteValue();
			res[3 * i + 2] = Integer.valueOf(0).byteValue();
		}
		return res;
	}

	private int normalize_to_rgb_value(float value, int min, int max) {
		if (value < min) {
			if (this.debug) {
				System.out
						.println("The value recorded was lower than the given min value. Maybe think about decreasing the min value. Current value = "
								+ value + ", current min = " + min);
			}
			return 0;
		}
		if (value > max) {
			if (this.debug) {
				System.out
						.println("The value recorded was higher than the given max. Maybe think about increasing the max value. Current value = "
								+ value + ", current max = " + max);
			}
			return 255;
		} else {
			return (int) (((value - min) / (max - min)) * 255); // -40 -50 -30 =
																// //
																// (-40-(-50))/(-30-(-50))
																// // = 0,5
		}

	}

	private void setImage(byte[] data, int width, int height, int priority,
			int duration_ms) throws IOException {
		ImageRequest imageRequest = ImageRequest.newBuilder()
				.setImagedata(ByteString.copyFrom(data)).setImagewidth(width)
				.setImageheight(height).setPriority(priority)
				.setDuration(duration_ms).build();

		HyperionRequest request = HyperionRequest.newBuilder()
				.setCommand(HyperionRequest.Command.IMAGE)
				.setExtension(ImageRequest.imageRequest, imageRequest).build();

		sendRequest(request);
	}

	private void setImage(byte[] data, int width, int height, int priority)
			throws IOException {
		setImage(data, width, height, priority, -1);
	}

	public void closeConnection() throws IOException {
		if (this.inside) {
			this.toclose = true;
		} else {
			performClose();
		}
	}

	private void setColor(Color color, int priority) throws IOException {
		setColor(color, priority, -1);
	}

	private void setColor(Color color, int priority, int duration_ms)
			throws IOException {
		ColorRequest colorRequest = ColorRequest.newBuilder()
				.setRgbColor(color.getRGB()).setPriority(priority)
				.setDuration(duration_ms).build();

		HyperionRequest request = HyperionRequest.newBuilder()
				.setCommand(HyperionRequest.Command.COLOR)
				.setExtension(ColorRequest.colorRequest, colorRequest).build();

		sendRequest(request);
	}

	private void sendRequest(HyperionRequest request) throws IOException {
		int size = request.getSerializedSize();
		// create the header
		byte[] header = new byte[4];
		header[0] = (byte) ((size >> 24) & 0xFF);
		header[1] = (byte) ((size >> 16) & 0xFF);
		header[2] = (byte) ((size >> 8) & 0xFF);
		header[3] = (byte) ((size) & 0xFF);

		// write the data to the socket
		if (!socket.isClosed()) {
			OutputStream output = socket.getOutputStream();
			output.write(header);
			request.writeTo(output);
			output.flush();
			HyperionReply reply = receiveReply();
			if (!reply.getSuccess()) {
				System.out.println(reply.toString());
			}
		}
	}

	private HyperionReply receiveReply() throws IOException {
		if (!socket.isClosed()) {
			InputStream input = socket.getInputStream();

			byte[] header = new byte[4];
			input.read(header, 0, 4);
			int size = (header[0] << 24) | (header[1] << 16) | (header[2] << 8)
					| (header[3]);
			byte[] data = new byte[size];
			input.read(data, 0, size);
			HyperionReply reply = HyperionReply.parseFrom(data);

			return reply;
		} else {
			return HyperionReply.parseFrom("Socket closed!".getBytes());
		}
	}
}
