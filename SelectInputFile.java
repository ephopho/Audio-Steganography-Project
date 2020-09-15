package com.phopho.audiosteganography;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class SelectInputFile extends WizardPanel

{

    private JTextField imageTextField;

    private JPanel selectPanel;

    private JLabel audioLabel;

    private JScrollPane imageScrollPane;

    private JButton selectButton;

    private JLabel imagePreviewLabel;

    private JButton playAudio;

    private File selectedFile;

    // Creates a new form SelectInputFile

    public SelectInputFile ( String stepText )

    {

        super ( stepText );

        initComponents(); // setFirstFocusable( selectButton );

    }

    // This method is called from within th constructor to initialize the form

    private void initComponents()

    {

        selectPanel = new JPanel();

        imageTextField = new JTextField( "" );

        selectButton = new JButton();

        imageScrollPane = new JScrollPane();

        audioLabel = new JLabel( "" );

        imagePreviewLabel = new JLabel( "Audio Preview" );

        playAudio = new JButton( "Play Audio" );

        setLayout( new BorderLayout() );

        selectPanel.setLayout( new BorderLayout() );

        selectPanel.setBorder( new LineBorder( new Color( 240, 240, 240 ), 5 ) );

        imageTextField.setEditable( false );

        selectPanel.add( playAudio, BorderLayout.NORTH );

        playAudio.addActionListener(
                new ActionListener()

                {

                    public void actionPerformed( ActionEvent event )

                    {

                        playAudioActionPerformed( event ) ;

                    }

                }
        );

        selectPanel.add( imageTextField, BorderLayout.CENTER );

        selectButton.setMnemonic( 'F' );

        selectButton.setText( "Select File" );

        selectButton.addActionListener(
                new ActionListener()

                {

                    public void actionPerformed( ActionEvent event )

                    {

                        selectButtonActionPerformed( event );

                    }

                }

                );

        selectPanel.add( selectButton, BorderLayout.EAST );

        add( selectPanel, BorderLayout.SOUTH );

        audioLabel.setHorizontalAlignment( SwingConstants.CENTER );

        audioLabel.setBorder( new LineBorder( new Color( 204, 204, 204 ), 5  ) );

        add( imageScrollPane, BorderLayout.CENTER );

        audioLabel.setText( "Play Audio" );

        System.out.println( audioLabel.getText() );

        // audioLabel.setVisible( true );

        imagePreviewLabel.setHorizontalAlignment( SwingConstants.CENTER );

        imagePreviewLabel.setText( "Audio Preview" );

        add( imagePreviewLabel, BorderLayout.NORTH );

    }

    private void playAudioActionPerformed( ActionEvent event )

    {

        if ( imageTextField.getText() == null )

            JOptionPane.showMessageDialog( this, "Select an Input File First" );

        else

        {

            System.out.println( "Code to Play Audio..." );

            System.out.println( "AudioFile Path: " + selectedFile.getParent() );

            System.out.println( "AudioFile Name: " + selectedFile.getName() );

            String url = "file:///" + selectedFile.getParent() + "//";

            System.out.println( "URL: " + url );

            new SoundApplication( url, selectedFile.getName() );

        }

    }

    private void selectButtonActionPerformed( ActionEvent event )

    {

        JFileChooser chooser = GuiUtilities.getImageFileChooser();

        if ( imageTextField.getText() != null )

            chooser.setSelectedFile( new File( imageTextField.getText() ) );

        if ( chooser.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION )

        {

            selectedFile = chooser.getSelectedFile();

            audioLabel.setText( "Play File" );

            audioLabel.show();

            audioLabel.setVisible( true );
            // Icon( new ImageIcon( selectedFile.getAbsolutePath() ) );

            imageTextField.setText( selectedFile.getAbsolutePath() ) ;
        }

    }

    public boolean doValidation()

    {

        if ( imageTextField.getText().length() == 0 )

        {

            JOptionPane.showMessageDialog( this, "Select an Input File" );

            selectButton.requestFocus();

            return false;

        }

        else if ( ! new File( imageTextField.getText() ).exists() )

        {

            JOptionPane.showMessageDialog( this, "File Not Found! Select a Valid File" );

            selectButton.requestFocus();

            return false;

        }

        return true;

    }

    // Getter for property selectedFile

    public File getSelectedFile()

    {

        return selectedFile;

    }



}
