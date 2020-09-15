package com.phopho.audiosteganography;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

public class Login extends JDialog

{

    private final JLabel usernameLabel;// = new JLabel();

    private final JLabel passwordLabel;// = new JLabel();

    private final JTextField usernameTextField; //= new JTextField( 15 );

    private final JPasswordField passwordField; //= new JPasswordField();

    private final JButton loginButton; //= new JButton( "Login" );

    private final JButton cancelButton; //= new JButton( "Cancel" );

    private final JLabel statusLabel;// = new JLabel( " " );

    public Login()

    {

        this( null, true );

    }

    public Login ( final JFrame parent, boolean modal )

    {

        super( parent, "Audio Steganography User Login", modal );

        usernameLabel = new JLabel();

        usernameLabel.setText( "Username: " );

        usernameLabel.setFont( new Font( "Dialog", Font.BOLD, 12 ) );

        passwordLabel = new JLabel();

        passwordLabel.setText( "Password: " );

        passwordLabel.setFont( new Font( "Dialog", Font.BOLD, 12 ) );

        JPanel jPanel = new JPanel( new GridLayout( 2, 1 ) );

        jPanel.add( usernameLabel );

        jPanel.add( passwordLabel );

        JPanel jPanel1 = new JPanel( new GridLayout( 2, 1 ) );

        usernameTextField = new JTextField( 15 );

        passwordField = new JPasswordField();

        jPanel1.add( usernameTextField );

        jPanel1.add( passwordField );

        JPanel jPanel2 = new JPanel();

        jPanel2.add( jPanel );

        jPanel2.add( jPanel1 );

        JPanel jPanel3 = new JPanel();

        loginButton = new JButton( "Login" );

        cancelButton = new JButton( "Cancel" );

        statusLabel = new JLabel( " " );

        jPanel3.add( loginButton );

        jPanel3.add( cancelButton );

        JPanel jPanel4 = new JPanel( new BorderLayout() );

        jPanel4.add( jPanel3, BorderLayout.CENTER );

        jPanel4.add( statusLabel, BorderLayout.NORTH );

        statusLabel.setForeground( Color.RED );

        statusLabel.setHorizontalAlignment( SwingConstants.CENTER );

        setLayout( new BorderLayout() );

        add( jPanel2, BorderLayout.CENTER );

        add( jPanel4, BorderLayout.SOUTH );

        pack();

        setLocationRelativeTo( null );

        setDefaultCloseOperation( DISPOSE_ON_CLOSE );

        addWindowListener(
                new WindowAdapter()

                {

                    @Override
                    public void windowClosing( WindowEvent event )

                    {

                        System.exit( 0 );

                    }

                }

        );

        loginButton.addActionListener(
                new ActionListener()

                {
                    @Override
                    public void actionPerformed( ActionEvent event )

                    {

                        if ( Arrays.equals( "stegProject".toCharArray(), passwordField.getPassword() )
                        && "stegProject".equals( usernameTextField.getText() ) )

                        {

                            parent.setVisible( true );

                            setVisible( false );

                        }

                        else

                        {

                            statusLabel.setText( "Invalid Username or Password" );

                        }

                    }

                }
        );

        cancelButton.addActionListener(
                new ActionListener()

                {

                    @Override
                    public void actionPerformed( ActionEvent event )

                    {

                        setVisible( false );

                        parent.dispose();

                        System.exit( 0 );

                    }

                }

        );


    }

}
