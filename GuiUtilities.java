package com.phopho.audiosteganography;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;

public class GuiUtilities

{

    // Creates a new instance of GuiUtilities

    public GuiUtilities()

    {


    }

    public static JFileChooser getImageFileChooser()

    {

        JFileChooser chooser =  new JFileChooser( "." );

        chooser.setFileFilter( new FileFilter()

        {

            public boolean accept( File file )

            {

                if ( file.isDirectory() )

                    return true;

                if ( file.getName().endsWith( ".wav" ) )

                    return true;

                return false;

            }

            public String getDescription()

            {

                return "wav files";

            }

        } );

        return chooser;

    }

}
