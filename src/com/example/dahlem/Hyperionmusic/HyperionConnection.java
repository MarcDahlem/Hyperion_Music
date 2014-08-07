package com.example.dahlem.Hyperionmusic;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.PriorityQueue;

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
	private int b;
	private int r;
	private int g;
	private int priority;
	private volatile boolean inside;
	private volatile boolean toclose;
	private final boolean debug;
	private int oldr;
	private int oldg;
	private int oldb;
	private int min;
	private int max;

	public HyperionConnection(String ip, int port, int priority, int threshold, int maxdb, boolean debug)
			throws UnknownHostException, IOException {
		this.debug = debug;
		this.socket = new Socket(ip, port);
		this.priority = priority;
		this.min = threshold;
		this.max = maxdb;
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
					if (socket != null && socket.isConnected()) {
						socket.close();
					}
				}
				this.inside = false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private byte[] computeImage(PriorityQueue<SpectrumElement> elems,
			int width, int height) {
		byte[] res = new byte[3 * width * height];
		float max = Float.NEGATIVE_INFINITY;
		float freq;
		float mag;
		float phase;
		float max_freq = 0;
		for (SpectrumElement elem : elems) {

			freq = elem.getFreqeuncy();

			mag = elem.getMagnitude();
			phase = elem.getPhase();
			if (max < mag /*&& freq > 100*/) {
				max = mag;
				max_freq = freq;
			}
		}

		int normalized_max = normalize_to_rgb_value(max, this.min, this.max);
		
		if (debug) {
			System.out.println("Max = " + max + ", freq = " + max_freq);
			System.out.println("Normalized max to RGB is '" +normalized_max+"'.");
		}

		int counter = 1;
		this.r = oldr;
		this.g = oldg;
		this.b = oldb;
		this.increaseColor(10);
		this.oldr = r;
		this.oldg = g;
		this.oldb = b;
		for (int i = 0; i < width * height; i++) {
			byte red = Integer.valueOf(r).byteValue();
			byte green = Integer.valueOf(g).byteValue();
			byte blue = Integer.valueOf(b).byteValue();
			res[3 * i] = Integer.valueOf(normalized_max).byteValue();
			res[3 * i + 1] = Integer.valueOf(normalized_max).byteValue();
			res[3 * i + 2] = Integer.valueOf(normalized_max).byteValue();
			if (counter * width <= i) {
				counter++;
				this.increaseColor(10);
			}

		}
		return res;
	}

	private int normalize_to_rgb_value(float value, int min, int max) {
		if (value < min) {
			System.out.println("The value recorded was lower than the given min value. Maybe think about decreasing the min value. Current value = " + value +", current min = " + min);
			return 0;
		}
		if (value > max) {
			System.out.println("The value recorded was higher than the given max. Maybe think about increasing the max value. Current value = " + value +", current max = " + max);
			return 255;
		} else {
			return (int) (((value - min) / (max - min)) * 255); // -40 -50 -30 = // (-40-(-50))/(-30-(-50)) // = 0,5
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
			if (socket != null && socket.isConnected()) {
				socket.close();
			}
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

	private void increaseColor(int interval) {
		if (this.b < 255) {
			this.b += interval;
			this.b = this.b > 255 ? 255 : this.b;
		} else {
			if (this.g < 255) {
				this.g += interval;
				this.g = this.g > 255 ? 255 : this.g;
				this.b = 0;
			} else {
				if (this.r < 255) {
					this.r += interval;
					this.r = this.r > 255 ? 255 : this.r;
					this.g = 0;
				} else {
					this.r = 0;
					this.g = 0;
					this.b = 0;
				}
			}
		}
	}
}
