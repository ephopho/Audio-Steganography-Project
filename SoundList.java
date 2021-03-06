package com.phopho.audiosteganography;

import javax.swing.*;
import java.applet.AudioClip;
import java.net.URL;
import java.util.Hashtable;

public class SoundList extends Hashtable

{

    JApplet applet;

    URL baseURL;

    public SoundList( URL baseURL )

    {

        super( 5 );

        this.baseURL = baseURL;

        System.out.println( "Base URL: " + baseURL );

    }

    public void startLoading( String relativeURL )

    {

        new SoundLoader( this, baseURL, relativeURL );

        System.out.println( "Relative URL: " + relativeURL );

    }

    public AudioClip getClip( String relativeURL )

    {

        return ( AudioClip ) get(relativeURL );

    }

    public void putClip( AudioClip clip, String relativeURL )

    {

        put( relativeURL, clip );

    }

}
