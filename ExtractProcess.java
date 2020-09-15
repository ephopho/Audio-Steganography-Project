package com.phopho.audiosteganography;

import java.io.File;
import java.io.FileOutputStream;

public class ExtractProcess

{

    private EmbedExtractOptions embedExtractOptions;

    private static File file; // carrier file

    private static byte[] carrier; // carrier data

    private static int[] coefficient; // dct values

    private static FileOutputStream fileOutputStream; // embedded file ( output file )

    private static String embeddedFileName; // output file name

    private static String password;

    // Creates a new instance of ExtractProcess

    public ExtractProcess( EmbedExtractOptions embedExtractOptions )

    {

        this.embedExtractOptions = embedExtractOptions;

        this.file = embedExtractOptions.getInputFile();

        embeddedFileName = embedExtractOptions.getOutputFile().getAbsolutePath();

        password = embedExtractOptions.getPassword();

    }

    public void startExtract()

    {

        System.out.println( "Extracting..." );

        System.out.println( "Audio File " + embedExtractOptions.getInputFile().getAbsolutePath() );

        System.out.println( "File " + embeddedFileName );

        System.out.println( "Password ---- >" + password );

        Steganography unveil = new Steganography( embedExtractOptions.getInputFile().getAbsolutePath(),
                embeddedFileName, password.toCharArray() );

        if ( ! unveil.decode() )

            System.out.println( "Error occurred during decrypt!, maybe the message is too big" );

        System.out.println( "Completed" );

    }

}
