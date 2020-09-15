package com.phopho.audiosteganography;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.File;

public class EnterPassword extends WizardPanel

{

    private JLabel selectFileLabel;

    private JPasswordField passwordField;

    private JLabel passwordLabel;

    private JPanel selectFilePanel;

    private File outputDirectory;

    private String password;

    // Create new form SelectOutputFile

    public EnterPassword( String stepText )

    {

        super( stepText );

        initComponents();

        setFirstFocusable( passwordField );

    }

    private void initComponents()

    {

        selectFileLabel = new JLabel();

        selectFilePanel = new JPanel();

        passwordLabel = new JLabel();

        passwordField = new JPasswordField();

        setLayout( new BorderLayout() );

        selectFileLabel.setHorizontalAlignment( SwingConstants.CENTER );

        selectFileLabel.setText( "Enter the Password and Click Next to Start the Extraction" );

        selectFileLabel.setBorder( new LineBorder( new Color( 204, 204, 204 ), 5 ) );

        add( selectFileLabel, BorderLayout.CENTER );

        selectFilePanel.setLayout( new BorderLayout() );

        selectFilePanel.setBorder( new LineBorder( new Color( 240, 240, 240 ), 5 ) );

        passwordLabel.setText( "  Password  " );

        selectFilePanel.add( passwordLabel, BorderLayout.WEST );

        selectFilePanel.add( passwordField, BorderLayout.CENTER );

        add( selectFilePanel, BorderLayout.SOUTH );

    }

    public boolean doValidation()

    {

        password = new String( passwordField.getPassword() );

        if ( password.trim().length() == 0 )

        {

            JOptionPane.showMessageDialog( this, "Enter a password" );

            passwordField.requestFocus();

            return false;

        }

        return true;

    }

    // Getter for property outputDirectory

    public File getOutputDirectory()

    {

        return outputDirectory;

    }

    // Getter for property password

    public String getPassword()

    {

        return password;

    }

}
