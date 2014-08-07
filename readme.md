# Hyperion Music

## What it is:

A client for the ambilight clone [hyperion](https://github.com/tvdzwan/hyperion) that is able to analyze audio and control the leds by that.

## What it uses;
The client is a [proto client](https://github.com/tvdzwan/hyperion/wiki/Java-proto-client-example), that sends small control messages to hyperion over the network.

The audio analysis is based on gstreamer, especially the gstreamer plugin [spectrum](http://gstreamer.freedesktop.org/data/doc/gstreamer/head/gst-plugins-good-plugins/html/gst-plugins-good-plugins-spectrum.html)

The client itself is written in Java and uses the library [gstreamer-java](https://code.google.com/p/gstreamer-java/)

## Where to run it
Firstly, the client is developed for linux. Maybe in future, there will be added the code and tutorial to run it on windows as well.

Secondly, the raspberry pi is too slow to analyze the audio in realtime, especially in combination with java.
If someone wants to adapt it to cpp, you're very welcome!

## How to configure the tool:
So far, the client can only be configured directly in the [source code](https://github.com/MarcDahlem/Hyperion_Music/blob/master/src/com/example/dahlem/Hyperionmusic/Main.java#L39).

Change the following fields in order to run it:
   - spec_bands: Amount of frequency bands that should be analyzed
   - audio_frequency: self explaining. 
   - num_channels: 1 or 2, depending if you want to analyze stereo or mono
   - device: The device that should be used to capture audio. For details on how to find the audio source, take a look at the following subsection
   - pulse: Flag indicating, if the given device is a device of pulse or alsa. Used to switch internally the audiosrc to alsasrc or pulsesrc accordingly
   - priority: Priority with which hyperion should be controlled
   - threshold = the minimal threshold for the frequencies in dB. Everything under this threshold is set to the threshold itself
   - maxdb: not used at the moment.
   - interval: the interval which is used to analyse the audio stream in nano seconds.
   - hyperion_ip: ip or network name of the raspberry that runs hyperion.
   - hyperion_port: the port of the protoserver of hyperion
   - debug: if enabled, some debug messages are printed when running
   -
Furthermore, one can change the boundaries that are used to devide bass, middle and high frequencies in [this file](https://github.com/MarcDahlem/Hyperion_Music/blob/master/src/com/example/dahlem/Hyperionmusic/HyperionConnection.java#L101)

At this file, the calculation also takes place of the image that is then be sent to hyperion.

### How to find the right device:
Basically, every alsa and pulse audio source can be used.

To list alsa sources one can use the command 'alist -l'
For details take a look to [stackoverflow](http://superuser.com/questions/53957/what-do-alsa-devices-like-hw0-0-mean-how-do-i-figure-out-which-to-use)

In order to get the name and pulse src running one needs to 

1. Find the monitor for the audio output (under the active 'sink'): `pactl list`
2. Unmute this monitor with: `pacmd
>>> set-source-mute <monitor_name> false
>>> exit`
3. Use this monitor as device name in the music client
(For details check [stackoverflow](http://stackoverflow.com/questions/7502380/streaming-pulseaudio-to-file-possibly-with-gstreamer))

## How to setup the project

First, one needs to install eclipse and import this project.

Besides eclipse, one needs to install the following packages:

`sudo apt-get install libjna-java protobuf-compiler libprotobuf-java libprotobuf8`

Then, install the protobuf-dt eclipse plugin with the following update site:
 http://protobuf-dt.googlecode.com/git/update-site
 (Install can be found under 'Help --> Install new Software --> Add --> <add url> --> check "Google Inc." --> Finish')
 
 Now, after restarting eclipse, set up the protobuf properties:
 Rightclick on the project, select 'Properties' --> 'Protocol Buffer'
 
 - Then check 'Enable project specific setting'
 - Under 'Compiler' check 'Compile .proto files on save' and Options --> 'Generate java'
 
 Afterwards, link or copy the file ['message.proto'](https://github.com/tvdzwan/hyperion/blob/master/libsrc/protoserver/message.proto) into your project folder. Open it once, change some whitespaces and save. This will call the proto compiler 'protoc'.
 
 In order to use the generated java files, one must inlcude them to the project.
 Do this by adding the src-gen folder: Project-Properties --> Java Build Path --> Sources --> Add folder --> Add the src-gen folder
 
 Last but not least, one needs to include all libararies that are used by this project as well:
 Rightclick on project --> Properties --> Java build path --> Libraries --> Add Jar /Add external jar
 The following jar files must be included:
 
 - gstreamer-java.jar (Tested is [version 1.6](https://code.google.com/p/gstreamer-java/downloads/list))

 - jna.jar (Tested is [version 4.1.0](https://github.com/twall/jna))
 
-  jna.platform-jar (Tested is [version 4.1.0](https://github.com/twall/jna))
 
 - protobuf.jar (can be found after installing `libprotobuf-java` under /usr/share/java/ )
 
 - Afterwards, the project should be able to run.
 
 ## How to run the client
 
 Run it from command line or eclipse
 
 It can be controlled with 's + ENTER' for starting the analysis, or 'q + ENTER' to quit.
 
 
 ## Limitations
 
 - Only Linux supported so far
 - Sometimes, the 'q' doesn't really close the program. This is a bug of gstreamer-java, when stopping the pipeline
 - Sometimes, after a failed 'q', starting is not working, or better, gives no audio results. Try it multiple times or restart eclipse if that happens. You can also enable 'debug' and take al llok at the debug messages.
