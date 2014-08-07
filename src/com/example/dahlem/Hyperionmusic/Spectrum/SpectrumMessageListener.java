package com.example.dahlem.Hyperionmusic.Spectrum;

import java.io.IOException;
import java.util.EventListener;

public interface SpectrumMessageListener extends EventListener {

	void handleSpectrumMessage(SpectrumEvent event);
}
