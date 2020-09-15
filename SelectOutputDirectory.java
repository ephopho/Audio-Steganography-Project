package com.phopho.audiosteganography;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class SelectOutputDirectory extends WizardPanel

{

    private JLabel selectFileLabel;

    private JTextField imageTextField;

    private JButton selectButton;

    private JPanel selectFilePanel;

    private File outputDirectory;

    // Create new form SelectedOutputFile

    public SelectOutputDirectory( String stepText )

    {

        super( stepText );

        initComponents();

        setFirstFocusable( selectButton );

    }

    private void initComponents()

    {

        selectFileLabel = new JLabel();

        selectFilePanel = new JPanel();

        imageTextField = new JTextField();

        selectButton = new JButton();

        setLayout( new BorderLayout() );

        selectFileLabel.setHorizontalAlignment( SwingConstants.CENTER );

        selectFileLabel.setText( "Select the Output Directory" );

        selectFileLabel.setBorder( new LineBorder( new Color( 204, 204, 204 ), 5 ) );

        add( selectFileLabel, BorderLayout.CENTER );

        selectFilePanel.setLayout( new BorderLayout() );

        selectFilePanel.setBorder( new LineBorder( new Color( 240, 240, 240 ), 7 ) );

        selectFilePanel.add( imageTextField, BorderLayout.CENTER );

        selectButton.setMnemonic( 'D' );

        selectButton.setText( "Select Directory" );

        selectButton.addActionListener(
                new ActionListener()

                {

                    public void actionPerformed( ActionEvent event )

                    {

                        selectButtonActionPerformed( event );

                    }

                }
        );

        selectFilePanel.add( selectButton, BorderLayout.EAST );

        add( selectFilePanel, BorderLayout.SOUTH );

    }

    private void selectButtonActionPerformed( ActionEvent event )

    {

        JFileChooser chooser = new JFileChooser( "." );

        if ( imageTextField.getText() != null )

            chooser.setSelectedFile( new File( imageTextField.getText() ) );

        chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );

        if ( chooser.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION );

        {

            outputDirectory = chooser.getSelectedFile();

            imageTextField.setText( outputDirectory.getAbsolutePath() );

        }

    }

    public boolean doValidation()

    {

        if ( imageTextField.getText().length() == 0 )

        {

            JOptionPane.showMessageDialog( this, "Select an Output Directory" );

            selectButton.requestFocus();

            return false;

        }

        else if ( new File( imageTextField.getText() ) == null )

        {

            JOptionPane.showMessageDialog( this, "Select a Valid Directory/Path" );

            selectButton.requestFocus();

            return false;

        }

        else if ( ! new File( imageTextField.getText() ).exists() )

        {

            JOptionPane.showMessageDialog( this, "Directory Does Not Exist!" );

            selectButton.requestFocus();

            return false;

        }

        else if ( ! new File( imageTextField.getText() ).isDirectory() )

        {

            JOptionPane.showMessageDialog( this, "File is Not a Directory!" );

            selectButton.requestFocus();

            return false;

        }

        return true;

    }

    // Getter for property outputDirectory

    public File getOutputDirectory()

    {

        return outputDirectory;

    }

}
