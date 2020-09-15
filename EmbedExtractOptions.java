package com.phopho.audiosteganography;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class EmbedExtractOptions extends AbstractTableModel

{

    private File inputFile;

    private File outputDirectory;

    private File outputFile;

    private String embedText;

    private File embedFile;

    private boolean embedTextOrFile;

    private String password;

    private String comment;

    private int quality;

    private static final String inputFileDescription = "Input File";

    private static final String outputDirectoryDescription = "Output Directory";

    private static final String outputFileDescription = "Output File";

    private static final String embedTextDescription = "Text to be embedded";

    private static final String embedFileDescription = "File to be embedded";

    private static final String passwordDescription = "Password";

    private static final String commentDescription = "Comment";

    private static final String qualityDescription = "Quality";

    String[] columnNames = { "Property", "Option Selected" };

    Object[][] columnData;

    public EmbedExtractOptions()

    {


    }

    public void createEmbedColumnData()

    {

        char[] mask = new char[ password.length() ];

        Arrays.fill( mask, '*' );

        columnData = new Object[][]{

                { inputFileDescription, inputFile.getAbsolutePath() },
                { outputDirectoryDescription, outputDirectory.getAbsolutePath() },
                { (embedTextOrFile ) ? embedTextDescription : embedFileDescription,
                        ( embedTextOrFile ) ? embedText : embedFile.getAbsolutePath() },
                { passwordDescription, new String( mask ) },
                { commentDescription, comment }, { qualityDescription, new Integer( quality ) }
        };

    }

    public void createExtractColumnData()

    {

        char[] mask = new char[ password.length() ];

        Arrays.fill( mask, '*' );

        columnData = new Object[][]{ { inputFileDescription, inputFile.getAbsolutePath() },
                { outputFileDescription, outputFile.getAbsolutePath() },
                { passwordDescription, new String( mask ) }
        };

    }

    public File createOutputFile( int suffixCount )

    {

        String outputFileName = inputFile.getName().substring( 0, inputFile.getName().lastIndexOf( "." ) )
                + ".wav";

        outputFile = new File( outputDirectory.getAbsolutePath() + System.getProperty( "file.separator" )
        + outputFileName );

        int a = 1;

        while ( outputFile.exists() )

        {

            outputFileName = outputFileName.substring( 0, outputFileName.lastIndexOf( ".") )
                    + ( a ++ ) + ".wav";

            outputFile = new File( outputDirectory.getAbsolutePath() + System.getProperty( "file.separator" )
                    + outputFileName );

            if ( a > suffixCount )

                return null;

        }

        return outputFile;

    }

    public File getEmbedFile()

    {

        if ( isEmbedTextOrFile() )

        {

            try

            {

                File tempFile = File.createTempFile( "ctl", "dh");

                FileWriter writer = new FileWriter( tempFile );

                writer.write( getEmbedText() );

                writer.close();

                return tempFile;
            }

            catch ( IOException ioException )

            {

                String errorMessage = "Error creating temporary file";

                System.out.println( "ALERT! :" + errorMessage );

                JOptionPane.showMessageDialog( null, errorMessage );

            }

        }
        return embedFile;
    }

    public String getColumnName( int index )

    {

        return columnNames[ index ];

    }

    public int getColumnCount()

    {

        return columnNames.length;

    }

    public int getRowCount()

    {

        return columnData.length;

    }

    public Object getValueAt( int row, int column )

    {

        return columnData[ row ][ column ];

    }

    public String toString()

    {

        return "\tInput File\t" + inputFile.getAbsolutePath() + "\n\n" +
                "\tOutput Directory\t" + inputFile.getAbsolutePath() + "\n\n" +
                ( ( embedTextOrFile ) ? "\tText to be embedded\t" + "\n\n"
                        : "\tFile to be embedded\t" + embedFile.getAbsolutePath()
                + "\n\n" ) + "\tPassword\t" + password + "\n\n" + ( ( comment == null ) ? "\tComment\t"
                        + comment + "\n\n" : "" ) + "\tQuality\t" + "\n\n";

    }

    public String getComment()

    {

        return comment;

    }

    public void setComment( String comment )

    {

        this.comment = comment;

    }

    public void setEmbedFile( File embedFile )

    {

        this.embedFile = embedFile;

    }

    public String getEmbedText()

    {

        return embedText;

    }

    public void setEmbedText( String embedText )

    {

        this.embedText = embedText;

    }

    public boolean isEmbedTextOrFile()

    {

        return embedTextOrFile;

    }

    public void setEmbedTextOrFile( boolean embedTextOrFile )

    {

        this.embedTextOrFile = embedTextOrFile;

    }

    public File getInputFile()


    {

        return inputFile;

    }

    public void setInputFile( File inputFile )

    {

        this.inputFile = inputFile;

    }

    public File getOutputDirectory()

    {

        return outputDirectory;

    }

    public void setOutputDirectory( File outputDirectory )

    {

        this.outputDirectory = outputDirectory;

    }

    public String getPassword()

    {

        return password;

    }

    public void setPassword( String password )

    {

        this.password = password;

    }

    public int getQuality()

    {

        return quality;

    }

    public void setQuality( int quality )

    {

        this.quality = quality;

    }

    public File getOutputFile()

    {

        return outputFile;

    }

    public void setOutputFile( File outputFile )

    {
        this.outputFile = outputFile;

    }



}
