package org.spekl.spm.utils;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by jls on 6/16/2015.
 */
public class ProgressableFileDownload {

    private static final int maxSteps = 50;
    private static final String sep = "=";
    private int step;
    private String url;
    private String label;
    private boolean noConsole = false;
    private int overhang;
    private boolean indeterminate;
    private String destination;

    public ProgressableFileDownload(String label, String url, String destination) {
        this.url = url;
        this.label = label;
        this.destination = destination;
    }

    private String fill() {
        return fill(null, 0);
    }

    private String fill(String what, int howMany) {
        StringBuffer sb = new StringBuffer();

        int howManyInserted = 0;
        for (int i = 0; i < maxSteps; i++) {
            if (howManyInserted < howMany) {
                sb.append(what);
                howManyInserted++;
            } else
                sb.append(" ");
        }
        return sb.toString();
    }

    private void clear() {
        if (noConsole) return;

        for (int i = 0; i < overhang; i++) {
            System.out.print("\b \b");
        }
        System.out.flush();
    }

    public String download() throws DownloadException, InterruptedException {


        BufferedInputStream in = null;
        FileOutputStream out = null;

        try {
            URL uri = new URL(url);
            URLConnection conn = uri.openConnection();

            int size = conn.getContentLength();

            if (size < 0) {
                this.indeterminate = true;
            } else {
                this.indeterminate = false;
            }

            start();

            in = new BufferedInputStream(uri.openStream());
            out = new FileOutputStream(destination);
            byte data[] = new byte[1024];

            int count;
            int totalTimes = 0;
            double sumCount = 0.0;


            while ((count = in.read(data, 0, 1024)) != -1) {
                out.write(data, 0, count);

                sumCount+=count;

                if(totalTimes % 1000 == 0) {
                    if (indeterminate) {
                        tickIndeterminate();
                    } else {
                        tickPct(sumCount / (double) size);
                    }
                }
                totalTimes++;
            }

            done();

	    out.close();
        } catch (IOException  e) {
            throw new DownloadException(e.getMessage());
        } 

        return destination;
    }

    private void handleConsole()
    {
        if(noConsole){
            System.out.println("");
        }
    }
    public void start() {

        StringBuffer lineBuffer = new StringBuffer();
        String front = String.format("%-20s : [", label);
        String back  = "";


        if (indeterminate==false)
            back = String.format("%s] %-3s", fill(), "0%");
        else
            back = String.format("%s] %-3s", fill(), "-:-");

        System.out.print(lineBuffer.append(front).append(back).toString());

        step = 0;
        overhang = back.length();
        System.out.flush();
        handleConsole();
    }

    public void tickPct(double pct){

        // map it onto our step percent.
        step = (int)Math.floor(maxSteps*pct);

        clear();

        String stepPct = String.format("%d%s", ((int) (100 * ((double) step / (double) maxSteps))), "%");

        System.out.print(String.format("%s] %-3s", fill(sep, step), stepPct));
        System.out.flush();

        handleConsole();
    }

    public void tickIndeterminate() {


        clear();

        int direction = -1 * ((step / maxSteps) % 2);
        int effectivePosition = step % maxSteps;

        if (direction < 0) {
            effectivePosition = maxSteps - 1 - effectivePosition;
        }

        String buff = fill();
        char[] cBuff = buff.toCharArray();
        cBuff[effectivePosition] = '=';

        String finalBuffer = new String(cBuff);

        System.out.print(String.format("%s] %-3s", finalBuffer, "-:-"));
        System.out.flush();


        handleConsole();
        step++;
    }

    public void done() {
        step = maxSteps;
        clear();
        System.out.print(String.format("%s] %-3s", fill(sep, step), "100%"));
        System.out.println("");
        System.out.flush();
    }

    public static void main(String args[]) throws InterruptedException, DownloadException {
        ProgressableFileDownload fdl = new ProgressableFileDownload("openjml-1.1.1", "http://jmlspecs.sourceforge.net/openjml.zip", "test.zip");
        fdl.download();

    }

}


