package com.example.dahlem.Hyperionmusic.Spectrum;

public class SpectrumElement implements Comparable<SpectrumElement>{
	private float freq;
	private float mag;
	private float phase;

	public SpectrumElement(float frequency, float magnitute, float phase) {
		this.freq = frequency;
		this.mag = magnitute;
		this.phase = phase;
	}

	@Override
	public int compareTo(SpectrumElement o) {
		return Float.compare(this.freq, o.freq);
	}

	public float getFreqeuncy() {
		return this.freq;
	}
	
	public float getMagnitude() {
		return this.mag;
	}
	
	public float getPhase() {
		return this.phase;
	}
	
	
}
