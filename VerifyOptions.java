package com.phopho.audiosteganography;

import javax.swing.*;
import java.awt.*;

public class VerifyOptions extends WizardPanel

{

    private JLabel messageLabel;

    private JTable choiceTable;

    // Create new form verifyOptions

    public VerifyOptions( String display )

    {

        super( display );

        initComponents();

    }

    public void showChosenOptions( EmbedExtractOptions embedExtractOptions )

    {

        JScrollPane viewPane = null;

        if ( choiceTable == null && viewPane == null )

        {

            choiceTable = new JTable( embedExtractOptions );

            viewPane = new JScrollPane( choiceTable );

            add( viewPane, BorderLayout.CENTER );

        }

        else

        {

            choiceTable.setModel( embedExtractOptions );

        }

    }

    private void initComponents()

    {

        messageLabel = new JLabel();

        setLayout( new BorderLayout() );

        messageLabel.setText( "                       Click Next to Complete the Process" );

        add( messageLabel, BorderLayout.SOUTH );

    }

    public boolean doValidation()

    {

        return true;

    }

}
