package com.phopho.audiosteganography;

import javax.swing.*;
import java.awt.*;

public class EnterOtherOptions extends WizardPanel

{

    private JLabel qualityLabel;

    private JScrollPane jScrollPane;

    private JPanel passwordPanel;

    private JPanel commentPanel;

    private JPasswordField passwordField;

    private JTextArea commentTextArea;

    private JLabel passwordLabel;

    private JLabel commentLabel;

    private JPanel qualityPanel;

    private JSlider qualitySlider;

    private int quality;

    private String comment;

    private String password;

    // Create new form EnterOtherOptions

    public EnterOtherOptions( String display )

    {

        super( display );

        initComponents();

        setFirstFocusable( passwordField );

    }

    // Correct this when needed
    private void initComponents()

    {

        passwordPanel = new JPanel();

        passwordLabel = new JLabel();

        passwordField = new JPasswordField();

        commentPanel = new JPanel();

        commentLabel = new JLabel();

        jScrollPane = new JScrollPane();

        commentTextArea = new JTextArea();

        qualityPanel = new JPanel();

        qualityLabel = new JLabel();

        qualitySlider = new JSlider();

        setLayout( new BorderLayout() );

        passwordPanel.setLayout( new BoxLayout( passwordPanel, BoxLayout.X_AXIS ) );

        passwordLabel.setText( " Password " );

        passwordPanel.add( passwordLabel );

        passwordPanel.add( passwordField );

        add( passwordPanel, BorderLayout.NORTH );

        commentPanel.setLayout( new BoxLayout( commentPanel, BoxLayout.X_AXIS ) );

        commentLabel.setText( " Comment " );

        commentLabel.setMaximumSize( new Dimension( 58, 16 ) );

        commentLabel.setMinimumSize( new Dimension( 58, 16 ) );

        commentPanel.add( commentLabel );

        jScrollPane.setViewportView( commentTextArea );

        commentPanel.add( jScrollPane );

        add( commentPanel, BorderLayout.CENTER );

        qualityPanel.setLayout( new BoxLayout( qualityPanel, BoxLayout.X_AXIS ) );

        qualityLabel.setText( " Quality    " );

        qualityLabel.setMaximumSize( new Dimension( 58, 16 ) );

        qualityLabel.setMinimumSize( new Dimension( 58, 16 ) );

        qualityPanel.add( qualityLabel );

        qualitySlider.setMajorTickSpacing( 10 );

        qualitySlider.setMinorTickSpacing( 1 );

        qualitySlider.setPaintLabels( true );

        qualitySlider.setPaintTicks( true );

        qualitySlider.setSnapToTicks( true );

        qualitySlider.setValue( 80 );

        qualityPanel.add( qualitySlider );

        add( qualityPanel, BorderLayout.SOUTH );

    }

    public boolean doValidation()

    {

        password = new String( passwordField.getPassword() );

        quality = qualitySlider.getValue();

        comment = commentTextArea.getText();

        if ( password.trim().length() == 0 )

        {

            JOptionPane.showMessageDialog( this, "Enter a password/phrase" );

            passwordField.requestFocus();

            return false;

        }

        else if ( quality < 40 )

        {

            JOptionPane.showMessageDialog( this,
                    "Warning! Lower quality values will distort the Audio" );


        }

        return true;

    }

    // Getter for property comment

    public String getComment()

    {

        return comment;

    }

    // Getter for property password

    public String getPassword()

    {

        return password;

    }

    // Getter for property quality

    public int getQuality()

    {

        return quality;

    }


}
