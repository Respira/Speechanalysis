package com.dvi.dimitri.speechanalysis;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.base.Stopwatch;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.filters.LowPassFS;
import be.tarsos.dsp.filters.LowPassSP;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class FeedBackServiceDSP extends Service
{

    private double startTime=0, endTime=0,totalTime;

    private static int SAMPLE_RATE =48000, numSyllables=0;
    private static int buffersize;

    private static AudioDispatcher dispatcher;



    private static LineGraphSeries<DataPoint> seriesDB,seriesKlank,seriesKlan;

    protected final Messenger mServerMessenger = new Messenger(new IncomingHandler(this));
    protected boolean mIsListening, isSpeaking;
    static final int MSG_RECOGNIZER_START_LISTENING = 1;
    static final int MSG_RECOGNIZER_CANCEL = 2;

    final static String SEND_RATIO = "SEND_RATIO";
    final static String SEND_CLARITY = "SEND_CLARITY";

    final static String BEGIN_END = "BEGIN_END";
    final static String ABORT_ERROR = "ERROR";


    private static Wave dataWave;

    @Override
    public void onCreate()
    {
        super.onCreate();
        //create a wave object to store the data
       dataWave=new Wave();
        seriesKlan = new LineGraphSeries<>();
        seriesKlank = new LineGraphSeries<>();
        //Check what the minimum buffersize is for this device.
        buffersize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT);

        //If the buffer <16MS we make buffers of 16ms
        if(buffersize<768)
        {
            //Smallest buffer is 16ms
            buffersize=768;
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(SAMPLE_RATE,buffersize,0);
        }else
            //buffersize is bigger than 16ms. overlap buffer-16MS
        {
            //Overlap to get 16ms steps
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(SAMPLE_RATE,buffersize,buffersize-768);
        }
        Log.e("buffer", buffersize + "");

        //filter lower than 50Hz
        dispatcher.addAudioProcessor(new LowPassSP(50, 48000));
        dispatcher.addAudioProcessor(new AudioProcessor(){
            @Override
            public boolean process(AudioEvent audioEvent) {
                //Check for valid data
                if (audioEvent.getdBSPL() > -100) {
                  /*
                    dataWave.addDouble(audioEvent.getTimeStamp(),audioEvent.getdBSPL());
                    //peak when not speaking
                    if (!isSpeaking && audioEvent.getdBSPL() > -60) {
                        onStartSpeech();
                        startTime = audioEvent.getTimeStamp();
                        isSpeaking = true;
                        endTime = 0;
                        Log.e("SOUND", "START");
                    } else if (isSpeaking && audioEvent.getdBSPL() > -60) {
                        endTime = 0;
                    } else if (isSpeaking && audioEvent.getdBSPL() <= -60) {
                        if (endTime == 0) {
                            endTime = audioEvent.getTimeStamp();
                        } else if (endTime + 1 < audioEvent.getTimeStamp())
                            totalTime = endTime - startTime;
                        endTime = 0;
                        startTime = 0;
                        isSpeaking = false;
                        numSyllables = dataWave.getSyllables(seriesKlank, seriesKlan);
                        Log.e("SOUND", "SILENCE");
                        Log.e("Time", totalTime + "");
                        Log.e("Syllables", numSyllables + "");
                        onStopSpeech();
                        sendSyllables(numSyllables);
                        dataWave = new Wave();
                    }
                */}
                for (float x : audioEvent.getFloatBuffer()) {
                    Log.e("",x+"");
                }
                return false;
            }

            @Override
            public void processingFinished() {

            }
        });
    }






    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            boolean receiver = (boolean) intent.getExtras().get("receiver");
            if(receiver){
                Message messageStart = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
                try {

                    mServerMessenger.send(messageStart);

                   // am.setStreamVolume(AudioManager.STREAM_MUSIC,0,0);
                } catch (RemoteException e) {
                    e.getMessage();
                }}
        }catch(Exception e){e.printStackTrace();}



 /* We want this service to continue running until it is explicitly
       stopped, so return sticky. */
        return START_NOT_STICKY;

    }




    @Override
    public void onDestroy()
    {
        super.onDestroy();


        if (dispatcher != null)
        {
            dispatcher.stop();
        }

       // ApplicationWPM.saveGraph(timeMap,getApplicationContext());

    }

    public void onStopSpeech(){
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                Intent intent = new Intent();
                intent.setAction(BEGIN_END);
                intent.putExtra("BEGIN", false);
                sendBroadcast(intent);//$NON-NLS-1$
                return null;
            }
        }.doInBackground(null);
    }
    public void onStartSpeech(){
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                Intent intent = new Intent();
                intent.setAction(BEGIN_END);
                intent.putExtra("BEGIN", true);
                sendBroadcast(intent);//$NON-NLS-1$
                return null;
            }
        }.doInBackground(null);
    }
     public void sendSyllables(final int syll){
         //send syllables from service to
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                Intent intent = new Intent();
                intent.setAction(SEND_RATIO);
                intent.putExtra("DATAPASSED", syll);
                sendBroadcast(intent);
                return null;
            }
        }.doInBackground(null);
    }
    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return mServerMessenger.getBinder();

    }


    protected  class IncomingHandler extends Handler
    {
        private WeakReference<FeedBackServiceDSP> mtarget;

        IncomingHandler(FeedBackServiceDSP target)
        {
            mtarget = new WeakReference<FeedBackServiceDSP>(target);
        }


        @Override
        public void handleMessage(Message msg)
        {

            final FeedBackServiceDSP target = mtarget.get();

            switch (msg.what)
            {
                case MSG_RECOGNIZER_START_LISTENING:

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    {
                        // turn off beep sound

                    }
                    if (!target.mIsListening)
                    {
                        dispatcher.run();
                        target.mIsListening = true;

                        //   Log.e(TAG, "message start listening"); //$NON-NLS-1$
                    }
                    break;

                case MSG_RECOGNIZER_CANCEL:

                    target.mIsListening = false;
                    dispatcher.stop();
                    // Log.e(TAG, "message canceled recognizer"); //$NON-NLS-1$
                    break;
            }
        }
    }

}