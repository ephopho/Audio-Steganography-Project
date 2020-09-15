package com.phopho.audiosteganography;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class EmbedAction implements WizardButtonListener

{

    private WizardFrame wizardFrame;

    private SelectInputFile selectInputFile;

    private SelectOutputDirectory selectOutputDirectory;

    private SelectDataToEmbed selectDataToEmbed;

    private EnterOtherOptions enterOtherOptions;

    private EmbedExtractOptions embedExtractOptions;

    private VerifyOptions verifyOptions;

    private ShowProcessDetails showProcessDetails;

    private ShowEmbeddedFile showEmbeddedFile;

    SteganographyMain steganographyMain;

    public EmbedAction( SteganographyMain steganographyMain1 )

    {

        steganographyMain = steganographyMain1;

        wizardFrame = new WizardFrame( "Embed Wizard", steganographyMain1 );

        wizardFrame.addWizardButtonListener( this );

        selectInputFile = new SelectInputFile( "1. Select an Input Audio File" );

        wizardFrame.addWizardPanel( selectInputFile );

        selectOutputDirectory = new SelectOutputDirectory( "2. Select an Output Directory" );

        wizardFrame.addWizardPanel( selectOutputDirectory );

        selectDataToEmbed = new SelectDataToEmbed( "3. Select Data to Embed" );

        wizardFrame.addWizardPanel( selectDataToEmbed );

        enterOtherOptions = new EnterOtherOptions( "4. Enter Password/Comment/Quality" );

        wizardFrame.addWizardPanel( enterOtherOptions );

        verifyOptions = new VerifyOptions( "5. Verify Options" );

        wizardFrame.addWizardPanel( verifyOptions );

        showProcessDetails = new ShowProcessDetails( "6. Embedding Data into the Audio" );

        wizardFrame.addWizardPanel( showProcessDetails );

        showEmbeddedFile = new ShowEmbeddedFile( "7. View Output File" );

        wizardFrame.addWizardPanel( showEmbeddedFile );

        wizardFrame.fireSetFocus( 0, 1 );

    }

    public WizardFrame getWizardFrame()

    {

        return wizardFrame;

    }

    public void buttonClicked( WizardButtonEvent wizardButtonEvent )

    {

        if ( wizardButtonEvent.getButtonType() ==
                WizardButtonListener.NEXT && wizardButtonEvent.getCard() == enterOtherOptions )

        {

            embedExtractOptions = new EmbedExtractOptions();

            embedExtractOptions.setInputFile( selectInputFile.getSelectedFile() );

            embedExtractOptions.setOutputDirectory( selectOutputDirectory.getOutputDirectory() );

            embedExtractOptions.setEmbedTextOrFile( selectDataToEmbed.isTextOrFile() );

            if ( selectDataToEmbed.isTextOrFile() )

                embedExtractOptions.setEmbedText( selectDataToEmbed.getEmbeddedText() );

            else

                embedExtractOptions.setEmbedFile( selectDataToEmbed.getSelectedFile() );

            embedExtractOptions.setPassword( enterOtherOptions.getPassword() );

            embedExtractOptions.setComment( enterOtherOptions.getComment() );

            embedExtractOptions.setQuality( enterOtherOptions.getQuality() );

            embedExtractOptions.createEmbedColumnData();

            verifyOptions.showChosenOptions( embedExtractOptions );

        }

        else if ( wizardButtonEvent.getButtonType() ==
            WizardButtonListener.NEXT && wizardButtonEvent.getCard() == verifyOptions )
        {

        EmbedProcess embedProcess = new EmbedProcess( embedExtractOptions );

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        OutputStream out = System.out;

        System.setOut( new PrintStream( byteArrayOutputStream ) );

        embedProcess.startEmbed();

        System.out.println( "Embedding Process Completed." );

        System.setOut( System.out );

        showProcessDetails.addOutputLine( new String ( byteArrayOutputStream.toByteArray() ) );

        }

        else if ( wizardButtonEvent.getButtonType() == WizardButtonListener.NEXT
                && wizardButtonEvent.getCard() == showProcessDetails )

        {

            showEmbeddedFile.setImageFiles( embedExtractOptions.getInputFile(), embedExtractOptions.getOutputFile() );

        }

    }

}
