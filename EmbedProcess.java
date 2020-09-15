package com.phopho.audiosteganography;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class EmbedProcess

{

    private EmbedExtractOptions embedExtractOptions;

    // Creates a new instance of EmbedProcess

    public EmbedProcess( EmbedExtractOptions embedExtractOptions )

    {

        this.embedExtractOptions =  embedExtractOptions;

    }

    public void startEmbed()

    {

        File inputFile = null;

        File outputFile = null;

        Image image = null;

        FileOutputStream dataOut = null;

        String password = null;

        String comment = null;

        String embedFileName = null;

        String inputFile1 = null;

        String outputFile1 = null;

        int quality;

        inputFile = embedExtractOptions.getInputFile();

        outputFile = embedExtractOptions.createOutputFile(150 );

        embedFileName = embedExtractOptions.getEmbedFile().getAbsolutePath();

        password = embedExtractOptions.getPassword();

        comment = embedExtractOptions.getComment();

        quality = embedExtractOptions.getQuality();

        inputFile1 = inputFile.getAbsolutePath();

        outputFile1 = outputFile.getAbsolutePath();

        try


        {

            dataOut = new FileOutputStream( outputFile );

            System.out.println( "Reading Attributes Wait...." );

            System.out.println( "<Source - AudioFile> " + inputFile );

            System.out.println( "<Output - AudioFile> " + outputFile);

            System.out.println ("<Comment ----------> " + comment );

            System.out.println( "<Password ---------> " + password );

            Steganography hide = new Steganography( inputFile1, embedFileName, outputFile1,
                    password.toCharArray() );

            if ( hide.acceptable )

            {

                hide.encode();

                System.out.println( "Completed...( EmbedProcess.java )" );

            }

            else

                System.out.println( "Error occur...( Audio.java )" );

            dataOut.close();

        }

        catch ( IOException exception )

        {

            JOptionPane.showMessageDialog( null, "Error embedding audio" );

            exception.printStackTrace();

        }

        catch ( Exception exception )

        {

            JOptionPane.showMessageDialog( null, "Error embedding audio" );

            exception.printStackTrace();

        }

    }

}
