package com.phopho.audiosteganography;

import javax.swing.*;
import java.awt.*;

public class ShowProcessDetails extends WizardPanel

{

    private JTextArea outputTextArea;

    private JScrollPane outputScrollPane;

    private JLabel embedLabel;

    // Creates new form InitEmbedProcess

    public ShowProcessDetails( String details )

    {

        super( details );

        initComponents();

    }

    public void setDisplayText( String text )

    {

        embedLabel.setText( text );

    }

    public void addOutputLine( String line )

    {

        outputTextArea.append( line + "\n" );

    }

    private void initComponents()

    {
        outputScrollPane = new JScrollPane();

        outputTextArea = new JTextArea();

        embedLabel = new JLabel();

        setLayout( new BorderLayout() );

        outputTextArea.setEditable( false );

        outputScrollPane.setViewportView( outputTextArea );

        add( outputScrollPane, BorderLayout.CENTER );

        embedLabel.setHorizontalAlignment( SwingConstants.CENTER );

        embedLabel.setText( "Embedding Data In Audio...." );

        add( embedLabel, BorderLayout.NORTH );

    }

    public boolean doValidation()

    {

        return true;

    }

}
