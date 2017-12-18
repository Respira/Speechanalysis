package com.dvi.dimitri.speechanalysis;

import android.Manifest;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class MainActivity extends Activity {


    int SAMPLE_RATE =48000, skipdata=0;
    String LOG_TAG="tag";
    private static int buffersize;
    private static double startTime=0.0, endTime=0.0;
    private static boolean isSpeaking=false;
    private static   ArrayList<Double> audioRaw = new ArrayList<>();
    private static LineGraphSeries<DataPoint> seriesDB,seriesKlank,seriesKlan;
    private static   AudioDispatcher dispatcher;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String[] perm=new String[1];
        perm[0]=Manifest.permission.RECORD_AUDIO;
        ActivityCompat.requestPermissions(this,perm,10);

        Button play =(Button) findViewById(R.id.StartStopButton);
        Button sound =(Button) findViewById(R.id.StartPlayButton);
         buffersize =AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT);
        Log.e("buffer", buffersize + "");
        final GraphView graph = (GraphView) findViewById(R.id.graph);
        final GraphView graphdb = (GraphView) findViewById(R.id.graphdb);



        play.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    seriesDB = new LineGraphSeries<>();
                    seriesKlank= new LineGraphSeries<>();
                    seriesKlan= new LineGraphSeries<>();
                    seriesDB.setDrawDataPoints(true);
                    seriesKlank.setDrawDataPoints(true);
                    seriesKlan.setDrawDataPoints(true);
                    Log.e("buffer", buffersize + "");
if(buffersize>768){
    dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(SAMPLE_RATE, buffersize, buffersize - 768);
}else {
    //BUFFERSIZE 2000
    buffersize=2000;
    dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(SAMPLE_RATE, buffersize, buffersize-240);
    Log.e("BUFFER SET", buffersize+"");
}
                    dispatcher.addAudioProcessor(new AudioProcessor(


                    ) {
                        @Override
                        public boolean process(AudioEvent audioEvent) {
                    //Log.e("Overlap", audioEvent.getOverlap() + "");
                    Log.e("Processed samples", audioEvent.getSamplesProcessed() + "");
                    //Log.e("Processed buffersize", audioEvent.getBufferSize() + "");


                    //  seriesDB.appendData(new DataPoint(audioEvent.getTimeStamp(), audioEvent.getdBSPL()), true, 100000, true);
                    if (audioEvent.getdBSPL() > -100) {
                        if (!isSpeaking && audioEvent.getdBSPL() > -60) {
                            startTime = audioEvent.getTimeStamp();
                            isSpeaking = true;
                            endTime = 0;
                            Log.e("SOUND", "START");
                        } else if (isSpeaking && audioEvent.getdBSPL() <= -60) {
                            if (endTime == 0) {
                                endTime = audioEvent.getTimeStamp();
                            }
                            if (endTime + 1 < audioEvent.getTimeStamp()) {
                                endTime = 0;
                                startTime = 0;
                                isSpeaking = false;
                                Log.e("SOUND", "SILENCE");
                            }
                        }


                        seriesKlank.appendData(new DataPoint(audioEvent.getTimeStamp(), audioEvent.getdBSPL()), true, 50000, true);
                        audioRaw.add(audioEvent.getdBSPL());
                    }
                    if (audioEvent.getTimeStamp() > 4) {
                        Toast.makeText(getApplicationContext(), "# samples: " + audioRaw.size(), Toast.LENGTH_LONG).show();
                        dispatcher.stop();
                        Double median = calculateMedian(audioRaw);
                        for (int i = 0; i < audioRaw.size(); i++) {
                            //  Log.e("LOOPING","???");
                            if (i > 20) {
                                //Log.e("LOOPING",audioRaw.get(i)+"");
                                if (skipdata > 0) {
                                    skipdata--;

                                    // Log.e("SKIPPING","data");
                                } else {

                                    if (
                                        //inRange(audioRaw.get(i - 12), audioRaw.get(i - 10))//10MS
                                            audioRaw.get(i - 4) < audioRaw.get(i - 3)//20MS
                                                    && audioRaw.get(i - 3) < audioRaw.get(i - 2)//30MS
                                                    && audioRaw.get(i - 2) > audioRaw.get(i - 1)//40MS
                                                    && audioRaw.get(i - 1) > audioRaw.get(i)//50MS
                                                    //   && inRange(audioRaw.get(i - 2) , audioRaw.get(i))//60MS
                                                    && audioRaw.get(i - 2) > median
                                                    && audioRaw.get(i - 2) - audioRaw.get(i - 4) > 2
                                            ) {
                                        //     Log.e("Peak",audioRaw.get(i - 2)+"");
                                        Log.e("FOUND SYLLABLE", "" + audioRaw.get(i - 2));

                                        skipdata = 7;
                                        Iterator<DataPoint> it = seriesKlank.getValues(0, seriesKlank.getHighestValueX());
                                        while (it.hasNext()) {
                                            DataPoint dp = it.next();
                                            if (dp.getY() == audioRaw.get((i - 2))) {
                                                seriesKlan.appendData(dp, true, 50000, false);
                                            }
                                        }
                                    }

                                }
                            }

                        }
                    }
                            return false;
                        }

                        @Override
                        public void processingFinished() {

                        }
                    });



                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                            dispatcher.run();


                        }
                    }).run();

                }});




        sound.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e("soundsamples: ",audioRaw.size()+" CLICK");
                graphdb.removeAllSeries();
                graphdb.addSeries(seriesKlank);
                graphdb.getViewport().setXAxisBoundsManual(true);
                graphdb.getViewport().setYAxisBoundsManual(true);
                graphdb.getViewport().setScalable(true);
                graphdb.getViewport().setMaxX(seriesKlank.getHighestValueX());
                graphdb.getViewport().setMinX(seriesKlank.getLowestValueX());
               graphdb.getViewport().setMaxY(seriesKlank.getHighestValueY());
                graphdb.getViewport().setMinY(seriesKlank.getLowestValueY());
                Toast.makeText(getApplicationContext(),"Buffersize "+buffersize+" overlap "+(buffersize-768),Toast.LENGTH_LONG).show();
                Toast.makeText(getApplicationContext(),"SAMPLES checked per second: "+(audioRaw.size()/4)+"",Toast.LENGTH_LONG).show();

                seriesKlank.setDataPointsRadius(1);

                graph.removeAllSeries();
                graph.addSeries(seriesKlan);
                graph.getViewport().setScalable(true);

                graph.getViewport().setXAxisBoundsManual(true);
                graph.getViewport().setYAxisBoundsManual(true);
                graph.getViewport().setMinX(seriesKlank.getLowestValueX());
                graph.getViewport().setMaxX(seriesKlank.getHighestValueX());
               graph.getViewport().setMaxY(seriesKlank.getHighestValueY());
               graph.getViewport().setMinY(seriesKlank.getLowestValueY());


            }
        });


   //


    }








     // Indicates if recording / playback should stop


    private Double calculateMedian(ArrayList<Double> list){
        ArrayList<Double> listje=list;
       // Collections.sort(listje);
        Double median =-60.0;
        //median=listje.get(listje.size()/2);
        Log.e("Median",median+"");
        return median;
    }
    void recordAudio() {
/*        new Thread(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                GraphView graph = (GraphView) findViewById(R.id.graph);
                GraphView graphdb = (GraphView) findViewById(R.id.graphdb);
                LineGraphSeries<DataPoint> series = new LineGraphSeries<>();

                // buffer size in bytes
                int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);


                short[] audioBuffer = new short[bufferSize/2];

                AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize);

                if (record.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e(LOG_TAG, "Audio Record can't initialize!");
                    return;
                }
                record.startRecording();

                Log.v(LOG_TAG, "Start recording");

                long shortsRead = 0;
                long time=System.currentTimeMillis();
                while (System.currentTimeMillis()<time+500) {
                    int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
                    shortsRead += numberOfShort;

                    // Do something with the audioBuffer
                    for (short i : audioBuffer) {
                        audioRaw.add(i);
                    }


                  // Log.e(audioBuffer.toString(),audioRaw.size()+"");
                    audioBuffer= new short[bufferSize / 2];
                }
                Log.v("Voorbij limiet","");
                int teller=0;
                for (Double s : audioRaw) {

                    series.appendData(new DataPoint(teller,s),false,900000,false);

                    teller++;
                }
                graph.removeAllSeries();

                series.setDataPointsRadius(1);
                series.setThickness(7);
                graph.addSeries(series);

                graph.getViewport().setXAxisBoundsManual(true);
                graph.getViewport().setYAxisBoundsManual(true);
                graph.getViewport().setScalable(true);
                graph.getViewport().setMinY(series.getLowestValueY());
                graph.getViewport().setMinX(series.getLowestValueX());
                graph.getViewport().setMaxY(series.getHighestValueY());


                Log.v("Size of list "+audioRaw.size(), String.format("Recording stopped. Samples read: %d", shortsRead));





            }
        }).run();
  */  }
    void playaudio(ArrayList<Short> arr)
    {

        short[] generatedNoise=new short[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            generatedNoise[i]=arr.get(i);
        }
        float[] data=floatMe(generatedNoise);
        showValues(data);




        int minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                generatedNoise.length, AudioTrack.MODE_STREAM);

       // at.play();

     //       at.write(generatedNoise, 0, generatedNoise.length);




    }
    void showValues(float[] arr)
    {
Log.e("Size", arr.length+"");

        int lastx=1;
        float peak=0;
        boolean start=false;

     //   Log.e("______________", "___");
        for (int i = 0; i < arr.length; i++) {
           // Log.e("val", arr[i] + "");
           // Log.e("______________", "");
            if(Math.abs(arr[i])>peak)
            {
                peak=Math.abs(arr[i]);
            }


                    Log.e("top", arr[i - 1] + "");
                    double amplitude = Math.abs(arr[i]);
                    double db = 20.0f * Math.log10(amplitude);

                    Log.e("amplitude: " + amplitude * 100.0, "Decibel: " + Math.round(db));


                  /*  if(endwave>startwave){
                    double[] wave= new double[endwave-startwave];
                    for (int j = startwave; j <endwave; j++) {
                        wave[j-startwave]=arr.get(j).doubleValue()/32767.0;
                        Log.e("tag",wave[j-startwave]+"");
                    }*/


                 //   Log.e("rms for wave:", volumeRMS(wave)+"");}
            }
            Log.e("Peak",peak+"");
        double db = 20.0f * Math.log10(peak);
        Log.e("Loudness",db+"db");
    }

    public static boolean inRange(double first, double second)
    {
        if (first-second<4&&first-second>-4){
            return true;
        }else{
            return false;
        }
    }
    public static float[] floatMe(short[] pcms) {
        float[] floaters = new float[pcms.length];
        for (int i = 0; i < pcms.length; i++) {
            floaters[i] = pcms[i];
        }
        return floaters;
    }

}