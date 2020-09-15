package com.phopho.audiosteganography;

import java.applet.Applet;
import java.applet.AudioClip;
import java.net.MalformedURLException;
import java.net.URL;

public class SoundLoader

{

    SoundList soundList;

    URL completeURL;

    String relativeURL;

    public SoundLoader( SoundList soundList, URL baseURL, String relativeURL )

    {

        this.soundList = soundList;

        try

        {

            completeURL = new URL( baseURL, relativeURL );

        }

        catch ( MalformedURLException malformedURLException )

        {

            System.err.println( malformedURLException.getMessage() );

        }

        this.relativeURL = relativeURL;

        start();

    }

    public void start()

    {

        AudioClip audioClip = Applet.newAudioClip( completeURL );

        soundList.putClip( audioClip, relativeURL );

    }

}
