package com.example.dahlem.Hyperionmusic.Spectrum;

import java.util.EventObject;
import java.util.PriorityQueue;

public class SpectrumEvent extends EventObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = -964311735543422590L;
	private PriorityQueue<SpectrumElement> elems;

	public SpectrumEvent (Object source) {
		super(source);
		this.elems = new PriorityQueue<SpectrumElement>();
	}

	public void addSpectrumElem(SpectrumElement elem) {
		this.elems.add(elem);
	}

	public PriorityQueue<SpectrumElement> getElements() {
		return this.elems;
	}
}
