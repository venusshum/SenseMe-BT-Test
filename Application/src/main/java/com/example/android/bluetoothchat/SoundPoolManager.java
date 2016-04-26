package com.example.android.bluetoothchat;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;

import java.util.HashMap;
import java.util.Vector;


/**
 * Created by Venus on 16/09/2015.
 */
/* reference http://stackoverflow.com/questions/3039078/playing-multiple-sounds-using-soundmanager */
public class SoundPoolManager {
        private SoundPool mSoundPool;
        private HashMap<Integer, Integer> mSoundPoolMap;
        private AudioManager mAudioManager;
        private Context mContext;
        private Vector<Integer> mAvailableSounds = new Vector<Integer>();
        private Vector<Integer> mKillSoundQueue = new Vector<Integer>();
        //private Handler mHandler = new Handler();
        private SoundPool.Builder sp21;
        private boolean sploaded = false;

        public SoundPoolManager(){

        }

        public void initSounds(Context theContext) {
                mContext = theContext;
                mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);

                mSoundPoolMap = new HashMap<Integer, Integer>();
                mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
                sploaded = false;
                mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                        @Override
                        public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                                sploaded = true;
                        }
                });

        }

        public void addSound(int Index, int SoundID)
        {
                mAvailableSounds.add(Index);
                try {
                        mSoundPoolMap.put(Index, mSoundPool.load(mContext, SoundID, 1));
                } catch (Exception e){}
        }

        public void playSound(int index) {
                // dont have a sound for this obj, return.
                if(mAvailableSounds.contains(index) && sploaded){
                        try {
                                int streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                //int streamVolume = 1;
                                int soundId = mSoundPool.play(mSoundPoolMap.get(index), streamVolume, streamVolume, 1, 0, 1f);
                                /*
                                int soundID;
                                int volume = 1;
                                soundID = mSoundPool.load(mContext, R.raw.piano_c5, 1);
                                mSoundPool.play(soundID, volume, volume, 1, 0, 1f);
                                */
                                /*
                                mKillSoundQueue.add(soundId);

                                // schedule the current sound to stop after set milliseconds
                                mHandler.postDelayed(new Runnable() {
                                        public void run() {
                                                if (!mKillSoundQueue.isEmpty()) {
                                                        mSoundPool.stop(mKillSoundQueue.firstElement());
                                                }
                                        }
                                }, 3000);
                                */

                        }catch (Exception e){e.printStackTrace();}
                }
        }

        public void close() {
                mSoundPool.release();
                mSoundPool = null;
        }

}
