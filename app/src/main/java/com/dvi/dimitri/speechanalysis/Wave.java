package com.dvi.dimitri.speechanalysis;

import android.util.Log;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * Created by Dimitri on 25/08/2017.
 */
 class Wave {
    private int skipdata=0;
private ArrayList<Double> data, timelapse;

    Wave(){
    this.data=new ArrayList<Double>();
    this.timelapse = new ArrayList<Double>();


}
 void addDouble(Double time, Double x)
{
this.data.add(x);
this.timelapse.add(time);
}


 int getSyllables(LineGraphSeries<DataPoint> input, LineGraphSeries<DataPoint> output)
    {
        int numSyll=0;
        Double median=calculateMedian(new ArrayList<Double>(this.data));
        for (int i = 0; i < this.data.size(); i++) {
            if (i > 5) {
                //If a syllable is found we skip a few steps
                if (skipdata > 0) {
                    skipdata--;
                } else {

                    if (
                                    this.data.get(i - 4) < this.data.get(i - 3)//16MS
                                    && this.data.get(i - 3) < this.data.get(i - 2)//32MS
                                    && this.data.get(i - 2) > this.data.get(i - 1)//48MS
                                    && this.data.get(i - 1) > this.data.get(i)//64MS
                                    //WAVE NOT POINTY?
                                    && inRange(this.data.get(i - 3) , this.data.get(i-2))
                                    && inRange(this.data.get(i - 2) , this.data.get(i-1))
                                    //ABOVE TRESHOLD
                                    && this.data.get(i - 2)> median
                                    //PRECEIDING DIP
                                    && this.data.get(i - 2)-this.data.get(i - 5)>2
                            ) {
                        Log.e("FOUND SYLLABLE", "" + this.data.get(i - 2));
                        numSyll++;
                        skipdata = 4;

                                output.appendData(new DataPoint(this.timelapse.get(i-2),this.timelapse.get(i-2)), true, 50000, false);
                            }
                        }
                    }

                }


return numSyll;
        }

    public static boolean inRange(double first, double second)
    {
        if (first-second<4&&first-second>-4){
            return true;
        }else{
            return false;
        }
    }
    private Double calculateMedian(ArrayList<Double> list){
        Collections.sort(list);
        Double median=list.get(list.size()/2);
        Log.e("Median",median+"");
        return median;
    }
    }


