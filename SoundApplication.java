package com.phopho.audiosteganography;

import java.applet.AudioClip;
import java.net.MalformedURLException;
import java.net.URL;

public class SoundApplication

{

    SoundList soundList;

    String wavFile;

    String chosenFile;

    AudioClip onceClip;

    URL codeBase;

    public SoundApplication( String url, String file )

    {

        chosenFile = wavFile = file;

        try

        {

            codeBase = new URL( url );

        }

        catch ( MalformedURLException malformedURLException )

        {

            System.err.println( malformedURLException );

        }

        soundList = new SoundList( codeBase );

        soundList.startLoading( wavFile );

        chosenFile = wavFile;

        onceClip = soundList.getClip( chosenFile );

        onceClip.play();

    }

}
