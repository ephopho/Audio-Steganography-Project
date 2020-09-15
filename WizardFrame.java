package com.phopho.audiosteganography;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.Arrays;

public class WizardFrame extends JInternalFrame


{

    private JButton backButton;

    private JButton finishButton;

    private JLabel lineLabel;

    private JPanel stepsPanel;

    private JPanel wizardPanel;

    private JLabel stepsLabel;

    private JButton cancelButton;

    private JButton nextButton;

    private JPanel buttonsPanel;

    private JLabel blankLabel;

    // Utility field used by event firing mechanism

    private EventListenerList listenerList = null;

    private ArrayList panelList;

    private int sceneCount;

    private static int currentScene;

    SteganographyMain steganographyMain;

    // Creates new form of SteganographyWizardFame

    public WizardFrame( String title, SteganographyMain steganographyMain1 )

    {

        super( title, true, true );

        steganographyMain = steganographyMain1;

        initComponents();

        panelList = new ArrayList();

        finishButton.setVisible( false );

        setSize ( 600, 400 );

        setVisible( true );

    }

    public void addWizardPanel( WizardPanel wizardPanel1 )

    {

        String display = wizardPanel1.getStepText();

        JLabel jLabel = new JLabel();

        jLabel.setText( display );

        jLabel.setFont( new Font( "SansSerif", Font.PLAIN, 13 ) );

        jLabel.setBackground( new Color( 204, 204, 204 ) );

        jLabel.setForeground( new Color( 255, 255, 255 ) );

        if ( display.length() / 2 > lineLabel.getText().length() )

        {

            char[] lineFill = new char[ display.length() * 75 / 100 ];

            Arrays.fill( lineFill, '_' );

            lineLabel.setText( new String ( lineFill ) );

        }

        stepsPanel.add( jLabel );

        wizardPanel1.setWizardLabel( jLabel );

        wizardPanel.add( wizardPanel1, "scene" + ++ sceneCount);

    }

    private void initComponents()

    {

        stepsPanel = new JPanel();

        stepsLabel = new JLabel();

        lineLabel = new JLabel();

        blankLabel = new JLabel();

        wizardPanel = new JPanel();

        buttonsPanel = new JPanel();

        backButton = new JButton();

        nextButton = new JButton();

        finishButton = new JButton();

        cancelButton = new JButton();

        setPreferredSize( new Dimension( 450, 300 ) );

        stepsPanel.setLayout( new BoxLayout( stepsPanel, BoxLayout.Y_AXIS ) );

        stepsPanel.setBackground( new Color( 0, 80, 0 ) );

        stepsLabel.setBackground( new Color( 204, 204, 204 ) );

        stepsLabel.setForeground( new Color( 255, 255, 255 ) );

        stepsLabel.setText( "     Steps " );

        stepsLabel.setFont( new Font( "Monospaced", Font.BOLD, 22 ) );

        stepsPanel.add( stepsLabel );

        lineLabel.setBackground( new Color( 204, 204, 204  ) );

        lineLabel.setFont( new Font( "Dialog", 1, 18 ) );

        lineLabel.setForeground( new Color( 255, 255, 255 ) );

        lineLabel.setText( "_____" );

        stepsPanel.add( lineLabel );

        stepsPanel.add( blankLabel );

        getContentPane().add( stepsPanel, BorderLayout.WEST );

        wizardPanel.setLayout( new CardLayout() );

        wizardPanel.setBorder( new LineBorder( new Color( 200, 200, 200 ) ) );

        getContentPane().add( wizardPanel, BorderLayout.CENTER );

        buttonsPanel.setLayout( new FlowLayout( FlowLayout.RIGHT ) );

        backButton.setMnemonic( 'B' );

        backButton.setText( "< Back" );

        backButton.addActionListener(
                new ActionListener()

                {

                    public void actionPerformed( ActionEvent event )

                    {

                        backButtonClicked( event );

                    }

                }
        );

        buttonsPanel.add( backButton );

        nextButton.setMnemonic( 'N' );

        nextButton.setText( "Next >" );

        nextButton.addActionListener(
                new ActionListener()

                {

                    public void actionPerformed( ActionEvent event )

                    {

                        nextButtonClicked( event );

                    }

                }
        );

        buttonsPanel.add( nextButton );

        finishButton.setMnemonic( 'F');

        finishButton.setText( "Finish" );

        finishButton.setEnabled( false );

        finishButton.addActionListener(
                new ActionListener()

                {

                    public void actionPerformed( ActionEvent event )

                    {

                        finishButtonClicked( event );

                    }

                }
        );

        buttonsPanel.add( finishButton );

        cancelButton.setMnemonic( 'C' );

        cancelButton.setText( "Cancel" );

        cancelButton.addActionListener(
                new ActionListener()

                {
                    public void actionPerformed( ActionEvent event )

                    {

                        cancelButtonClicked( event );

                    }

                }
        );

        buttonsPanel.add( cancelButton );

        getContentPane().add( buttonsPanel, BorderLayout.SOUTH );

        pack();

    }

    private void cancelButtonClicked( ActionEvent event )

    {

        fireButtonClickedEvent( WizardButtonListener.CANCEL,
                wizardPanel.getComponents()[ currentScene - 1 ], currentScene );

        steganographyMain.picLabel.setVisible( true );

        this.dispose();

    }

    private void finishButtonClicked( ActionEvent event )

    {

        fireButtonClickedEvent( WizardButtonListener.FINISH, wizardPanel.getComponents()[ currentScene - 1 ], currentScene );


    }

    private void nextButtonClicked( ActionEvent event )

    {

        if ( ! checkValidationStatus() )

        return;

        if ( sceneCount == currentScene )

            return;

        ( ( CardLayout ) wizardPanel.getLayout() ).next( wizardPanel );

        fireButtonClickedEvent( WizardButtonListener.NEXT, wizardPanel.getComponents()[ currentScene - 1 ], currentScene  );

        fireSetFocus( currentScene, currentScene + 1 );

    }

    private void backButtonClicked( ActionEvent event )

    {

        if ( currentScene == 1 )

            return;

        ( ( CardLayout ) wizardPanel.getLayout() ).previous( wizardPanel );

        fireButtonClickedEvent( WizardButtonListener.BACK, wizardPanel.getComponents()[ currentScene - 1 ], currentScene );

        fireSetFocus( currentScene, currentScene  - 1 );

    }

    private boolean checkValidationStatus()

    {

        Component[] components = wizardPanel.getComponents();

        return ( ( WizardPanel ) components[ currentScene - 1 ] ).doValidation();

    }

    public void fireSetFocus( int fromScene, int toScene )

    {

        Component[] components = wizardPanel.getComponents();

        if ( toScene > sceneCount || toScene <= 0 )

            return;

        ( ( WizardPanel ) components[ toScene - 1 ] ).focusGained(
                new FocusEvent( components[ toScene - 1 ], FocusEvent.FOCUS_GAINED )
        );

        currentScene = toScene;

        if ( sceneCount == currentScene )

            finishButton.setEnabled( true );

        else

            finishButton.setEnabled( false );

        if ( currentScene == 1 )

            backButton.setEnabled( false );

        else

            backButton.setEnabled( true );

        if ( currentScene == sceneCount )

        {

            cancelButton.setText( "Finish" );

            cancelButton.setMnemonic( 'F' );

            nextButton.setEnabled( false );

        }

        else

        {

            cancelButton.setText( "Cancel" );

            cancelButton.setMnemonic( 'C' );

            nextButton.setEnabled( true );

        }

        if ( fromScene > sceneCount || fromScene <= 0 )

            return;

        ( ( WizardPanel ) components[ fromScene - 1 ] ).focusLost(
                new FocusEvent( components[ fromScene - 1 ], FocusEvent.FOCUS_GAINED )
        );

    }

    // Registers WizardButtonListener to receive events

    public synchronized void addWizardButtonListener( WizardButtonListener wizardButtonListener )

    {

        if ( listenerList == null )

            listenerList = new EventListenerList();

        listenerList.add( WizardButtonListener.class, wizardButtonListener );
    }

    // Removes wizardButtonListener from the list of listeners

    public synchronized void removeWizardButtonListener( WizardButtonListener wizardButtonListener )

    {

        listenerList.remove( WizardButtonListener.class, wizardButtonListener );

    }

    protected void fireButtonClickedEvent( int buttonType, Component scene, int scenePosition )

    {

        Object[] listeners = listenerList.getListenerList();

        if ( listeners == null )

            return;

        for ( int a = 0; a < listeners.length; a++ )

        {

            if ( listeners[ a ] instanceof WizardButtonListener )

            {

                WizardButtonEvent wizardButtonEvent = new WizardButtonEvent( this, buttonType,
                         scene, scenePosition );

                ( ( WizardButtonListener ) listeners[ a ] ).buttonClicked( wizardButtonEvent );

            }

        }

    }

}
