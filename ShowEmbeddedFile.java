package com.phopho.audiosteganography;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class ShowEmbeddedFile extends WizardPanel

{

    private JLabel outputImageLabel;

    private JScrollPane inputImageScrollPane;

    private JLabel inputFileNameLabel;

    private JRadioButton inputImageRadioButton;

    private JRadioButton outputImageRadioButton;

    private JPanel inputImagePanel;

    private JScrollPane outputImageScrollPane;

    private JLabel outputFileNameLabel;

    private ButtonGroup choiceButtonGroup;

    private JPanel outputImagePanel;

    private JSplitPane imageSplitPane;

    private JPanel optionsPanel;

    private JLabel inputImageLabel;

    private File inputFile;

    private File outputFile;

    // Creates new form ShowOutputFile

    public ShowEmbeddedFile( String display )

    {

        super( display );

        initComponents();

        imageSplitPane.setDividerLocation( 0.5 );

        imageSplitPane.revalidate();

        choiceButtonGroup.add( inputImageRadioButton );

        choiceButtonGroup.add( outputImageRadioButton );

    }

    private void initComponents()

    {

        choiceButtonGroup = new ButtonGroup();

        imageSplitPane = new JSplitPane();

        inputImagePanel = new JPanel();

        inputImageScrollPane = new JScrollPane();

        inputImageLabel = new JLabel();

        inputFileNameLabel = new JLabel();

        outputImagePanel = new JPanel();

        outputImageScrollPane = new JScrollPane();

        outputImageLabel = new JLabel();

        outputFileNameLabel = new JLabel();

        optionsPanel = new JPanel();

        inputImageRadioButton = new JRadioButton();

        outputImageRadioButton = new JRadioButton();

        setLayout( new BorderLayout() );

        imageSplitPane.setDividerLocation( 100 );

        imageSplitPane.setOneTouchExpandable( true );

        inputImagePanel.setLayout( new BorderLayout() );

        inputImageLabel.setHorizontalAlignment( SwingConstants.CENTER );

        inputImageScrollPane.setViewportView( inputImageLabel );

        inputImagePanel.add( inputImageScrollPane, BorderLayout.CENTER );

        inputImagePanel.add( inputFileNameLabel, BorderLayout.NORTH );

        imageSplitPane.setRightComponent( inputImagePanel );

        outputImagePanel.setLayout( new BorderLayout() );

        outputImageLabel.setHorizontalAlignment( SwingConstants.CENTER );

        outputImageScrollPane.setViewportView( outputImageLabel );

        outputImagePanel.add( outputImageScrollPane, BorderLayout.CENTER );

        outputImagePanel.add( outputFileNameLabel, BorderLayout.NORTH );

        imageSplitPane.setLeftComponent( outputImagePanel );

        add( imageSplitPane, BorderLayout.CENTER );

        inputImageRadioButton.setMnemonic( 'O' );

        inputImageRadioButton.setText( "Play Output Audio" );

        inputImageRadioButton.addActionListener(
                new ActionListener()

                {

                    public void actionPerformed( ActionEvent event )

                    {

                        inputImageRadioActionPerformed( event );

                    }

                }
        );

        optionsPanel.add( inputImageRadioButton );

        outputImageRadioButton.setMnemonic( 'i' );

        outputImageRadioButton.setText( "Play Input Audio" );

        outputImageRadioButton.addActionListener(
                new ActionListener()

                {

                    public void actionPerformed( ActionEvent event )

                    {

                        outputImageRadioButtonActionPerformed( event );

                    }

                }
        );

        optionsPanel.add( outputImageRadioButton );

        add( optionsPanel, BorderLayout.SOUTH );

    }

    public void setImageFiles( File inputFile, File outputFile )

    {

        this.inputFile = inputFile;

        this.outputFile = outputFile;

        inputImageLabel.setIcon( new ImageIcon( inputFile.getAbsolutePath() ) );

        inputFileNameLabel.setText( "Input File: " + inputFile.getAbsolutePath() );

        outputImageLabel.setIcon( new ImageIcon( outputFile.getAbsolutePath() ) );

        outputFileNameLabel.setText( "Output File: " + outputFile.getAbsolutePath() );

    }

    private void outputImageRadioButtonActionPerformed( ActionEvent event )

    {

        System.out.println( "Code to play..." );

        System.out.println( "Code to Play Audio..." );

        System.out.println( "AudioFile Path: " + outputFile.getParent() );

        System.out.println( "AudioFile Name: " + outputFile.getName() );

        String url = "file:///" + inputFile.getParent() + "//";

        System.out.println( "URL: " + url );

        new SoundApplication( url, outputFile.getName() );

    }

    private void inputImageRadioActionPerformed( ActionEvent event )

    {

        System.out.println( "Code to play..." );

        System.out.println( "Code to Play Audio..." );

        System.out.println( "AudioFile Path: " + inputFile.getParent() );

        System.out.println( "AudioFile Name: " + inputFile.getName() );

        String url = "file:///" + inputFile.getParent() + "//";

        System.out.println( "URL: " + url );

        new SoundApplication( url, inputFile.getName() );

    }

    public boolean doValidation()

    {

        return true;

    }

}
