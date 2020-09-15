package com.phopho.audiosteganography;

import javax.swing.*;
import java.io.*;

public class ExtractAction implements WizardButtonListener

{

    private WizardFrame wizardFrame;

    private SelectInputFile selectInputFile;

    private SelectOutputFile selectOutputFile;

    private EnterPassword enterPassword;

    private EmbedExtractOptions embedExtractOptions;

    private VerifyOptions verifyOptions;

    private ShowProcessDetails showProcessDetails;

    private ShowProcessDetails showExtractedFileDetails;

    // Creates a new instance of EmbedAction

    public ExtractAction( SteganographyMain steganographyMain )

    {

        wizardFrame = new WizardFrame( "Extract Wizard", steganographyMain );

        wizardFrame.addWizardButtonListener( this );

        selectInputFile = new SelectInputFile( "1. Select an Input Audio File" );

        wizardFrame.addWizardPanel( selectInputFile );

        selectOutputFile = new SelectOutputFile( "2. Enter an Output File Name" );

        wizardFrame.addWizardPanel( selectOutputFile );

        enterPassword = new EnterPassword( "3. Enter Password" );

        wizardFrame.addWizardPanel( enterPassword );

        verifyOptions = new VerifyOptions( "4. Verify Options" );

        wizardFrame.addWizardPanel( verifyOptions );

        showProcessDetails = new ShowProcessDetails( "5. Extracting Data from the Audio File" );

        showProcessDetails.setDisplayText( "Extracting data from the Audio" );

        wizardFrame.addWizardPanel( showProcessDetails );

        showExtractedFileDetails = new ShowProcessDetails( "6. View Output File" );

        wizardFrame.addWizardPanel( showExtractedFileDetails );

        wizardFrame.fireSetFocus( 0, 1 );

    }

    // Getter for property wizardFrame

    public WizardFrame getWizardFrame()

    {

        return wizardFrame;

    }

    // Event handling for Wizard
    public void buttonClicked( WizardButtonEvent wizardButtonEvent )

    {

        if ( wizardButtonEvent.getButtonType() == WizardButtonListener.NEXT
                && wizardButtonEvent.getCard() == enterPassword )

        {

            embedExtractOptions = new EmbedExtractOptions();

            embedExtractOptions.setInputFile( selectInputFile.getSelectedFile() );

            embedExtractOptions.setOutputFile( selectOutputFile.getOutputFile() );

            embedExtractOptions.setPassword( enterPassword.getPassword() );

            embedExtractOptions.createExtractColumnData();

            verifyOptions.showChosenOptions( embedExtractOptions );

        }

        else if (wizardButtonEvent.getButtonType() == WizardButtonListener.NEXT
                && wizardButtonEvent.getCard() == verifyOptions )

        {

            ExtractProcess extractProcess =  new ExtractProcess( embedExtractOptions );

            ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();

            OutputStream out = System.out;

            System.setOut( new PrintStream( outputBuffer ) );

            extractProcess.startExtract();

            System.out.println( "Extraction Completed" );

            System.setOut( System.out );

            showProcessDetails.addOutputLine( new String( outputBuffer.toByteArray() ) );

        }

        else if ( wizardButtonEvent.getButtonType() == WizardButtonListener.NEXT
                && wizardButtonEvent.getCard() == showProcessDetails )

        {

            try

            {

                BufferedReader bufferedReader = new BufferedReader(
                        new FileReader( embedExtractOptions.getOutputFile() ) );


                String line = null;

                while( ( line = bufferedReader.readLine() ) != null )

                    showExtractedFileDetails.addOutputLine( line );

                bufferedReader.close();

                showExtractedFileDetails.setDisplayText( "Output File: " +
                        embedExtractOptions.getOutputFile().getAbsolutePath() );

            }

            catch ( FileNotFoundException fileNotFoundException )

            {

                fileNotFoundException.printStackTrace();

                JOptionPane.showMessageDialog( null, "Output file not created!" );

            }

            catch ( IOException ioException )

            {

                ioException.printStackTrace();

                JOptionPane.showMessageDialog( null, "File read error!" );

            }

        }

    }

}
