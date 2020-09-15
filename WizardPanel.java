package com.phopho.audiosteganography;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public abstract class WizardPanel extends JPanel implements FocusListener


{

    private JLabel wizardLabel = null;

    private String stepText = null;

    private Component firstFocusable = null;

    // Create new form WizardPanel

    public WizardPanel( String stepText )

    {

        this.stepText = stepText;

        initComponents();

    }

    private void initComponents()

    {

        setLayout( new BorderLayout() );

    }

    public void focusGained( FocusEvent focusEvent )

    {

        //JOptionPane.showMessageDialog( this, "Gain" );

        if ( wizardLabel == null )

            return;

        wizardLabel.setFont( wizardLabel.getFont().deriveFont( Font.BOLD ) );

        if ( firstFocusable != null ) firstFocusable.requestFocus();

    }

    public void focusLost( FocusEvent focusEvent )

    {

        // JOptionPane.showMessageDialog( this, "Loss" );

        if ( wizardLabel == null )

            return;

        wizardLabel.setFont( wizardLabel.getFont().deriveFont( Font.PLAIN) );

    }

    public abstract boolean doValidation();

    // Getter for property wizardLabel

    public JLabel getWizardLabel()

    {

        return wizardLabel;

    }

    // Setter for property wizardLabel

    public void setWizardLabel( JLabel wizardLabel )

    {

        this.wizardLabel = wizardLabel;

    }

    // Getter for proper stepText

    public String getStepText()

    {

        return stepText;

    }

    // Setter for property stepText

    public void setStepText( String stepText )

    {

        this.stepText = stepText;

    }

    // Getter for property firstFocusable

    public Component getFirstFocusable()

    {

        return firstFocusable;

    }

    // Setter for property firstFocusable

    public void setFirstFocusable( Component firstFocusable )

    {

        this.firstFocusable = firstFocusable;

    }

}
