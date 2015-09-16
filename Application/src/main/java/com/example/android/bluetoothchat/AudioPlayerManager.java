package com.example.android.bluetoothchat;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Created by Venus on 16/09/2015.
 */
public class AudioPlayerManager {
    /* Class AudioPlayManager using AudioTrack*/
    class AudioPlayManager {

        private volatile boolean playing;

        private final double sample[] = new double[2000];   //numSamples
        //private final double freqOfTone = 440; // hz
        private final byte generatedSnd[] = new byte[4000];  //2 * numSamples
        char playtone;
        private final int sampleRate = 8000;

        public AudioPlayManager(char thisTone) {
            super();
            setPlaying(false);
            playtone = thisTone;
            genTonefromChar();
        }
        public void play() {

            //final Context context = aContext;

            new Thread() {

                @Override
                public void run() {

                    try {
                        // Close the input streams.

                        // Create a new AudioTrack object using the same parameters as the AudioRecord
                        // object used to create the file.
                        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                                sampleRate,
                                AudioFormat.CHANNEL_OUT_MONO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                generatedSnd.length,
                                AudioTrack.MODE_STATIC);
                        // Start playback
                        audioTrack.write(generatedSnd, 0, generatedSnd.length);
                        audioTrack.play();

                        // Write the music buffer to the AudioTrack object
                        //while(playing){
                        //    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                        //}

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }.start();
        }

        void genTonefromChar(){
            switch (playtone) {
                case 'c':
                    genTone(261.30);
                    break;
                case 'd':
                    genTone(293.66);
                    break;
                case 'e':
                    genTone(329.63);
                    break;
                case 'f':
                    genTone(349.23);
                    break;
                case 'g':
                    genTone(392.00);
                    break;
                case 'a':
                    genTone(440);
                    break;
                case 'b':
                    genTone(493.88);
                    break;
                case 'C':
                    genTone(523.25);
                    break;
            }
        }

        void genTone(double tmpTone){
            // fill out the array
            for (int i = 0; i < 2000; ++i) {
                sample[i] = Math.sin(2 * Math.PI * i / (4000/tmpTone));
            }

            // convert to 16 bit pcm sound array
            // assumes the sample buffer is normalised.
            int idx = 0;
            for (double dVal : sample) {
                // scale to maximum amplitude
                short val = (short) ((dVal * 32767));
                // in 16 bit wav PCM, first byte is the low order byte
                generatedSnd[idx++] = (byte) (val & 0x00ff);
                generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

            }
        }
        /*
            void playSound(){
                int sampleRate = 8000;
                try {
                    AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                            sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                            AudioTrack.MODE_STREAM);

                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                    audioTrack.play();
                }
                catch (Exception e){}
            }
        */
        public void setPlaying(boolean playing) {
            this.playing = playing;
        }

        public boolean isPlaying() {
            return playing;
        }
    }

}
