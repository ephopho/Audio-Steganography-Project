package com.phopho.audiosteganography;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SteganographyMain extends JFrame

{

    private JMenuItem exitMenuItem;

    private JMenu fileMenu;

    private JMenu fileMenu1;

    private JDesktopPane jDesktopPane;

    private JMenuItem extractMenuItem;

    private JMenuItem embedMenuItem;

    private JMenuItem aboutMenuItem;

    private JMenuBar jMenuBar;

    public JLabel picLabel;

    private Login loginDialog;

    public static void main( String[] args )

    {

        SwingUtilities.invokeLater(
                new Runnable()

                {

                    @Override
                    public void run()

                    {

                        new SteganographyMain().show();

                    }

                }
        );

    }

    public SteganographyMain()

    {

        loginDialog = new Login( this, true );

        loginDialog.setVisible( true );

        initComponents();

        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();

        setSize( 610, 455 );

        setLocationRelativeTo( null );

        setResizable( false );

    }

    public void initComponents()

    {

        jDesktopPane = new JDesktopPane();

        jMenuBar = new JMenuBar();

        fileMenu = new JMenu();

        fileMenu1 = new JMenu();

        embedMenuItem = new JMenuItem();

        extractMenuItem = new JMenuItem();

        exitMenuItem = new JMenuItem();

        aboutMenuItem = new JMenuItem();

        picLabel = new JLabel( new ImageIcon( "lisa.jpg" ) );

        picLabel.setBounds( 0, 0, 600, 400 );

        add( picLabel );

        setTitle( "Audio Steganography Project" );

        addWindowListener(
                new WindowAdapter()

                {

                    public void windowClosing( WindowEvent windowEvent )

                    {

                        exitForm( windowEvent );

                    }

                }
        );

        getContentPane().add( jDesktopPane, BorderLayout.CENTER );

        fileMenu.setMnemonic( 'F' );

        fileMenu.setText( "File" );

        fileMenu1.setMnemonic( 'H' );

        fileMenu1.setText( "Help" );

        embedMenuItem.setMnemonic( 'M' );

        embedMenuItem.setText( "Embed" );

        embedMenuItem.addActionListener(
                new ActionListener()

                {

                    public void actionPerformed( ActionEvent event )

                    {

                        embedMenuItemClicked( event );

                    }

                }
        );

        fileMenu.add( embedMenuItem );

        extractMenuItem.setMnemonic( 'T' );

        extractMenuItem.setText( "Extract" );

        extractMenuItem.addActionListener(
                new ActionListener()

                {

                    public void actionPerformed( ActionEvent event )

                    {

                        extractMenuItemClicked( event );

                    }

                }
        );

        fileMenu.add( extractMenuItem );

        exitMenuItem.setMnemonic( 'X' );

        exitMenuItem.setText( "Exit" );

        exitMenuItem.addActionListener(
                new ActionListener()

                {

                    public void actionPerformed( ActionEvent event )

                    {

                        exitMenuItemClicked( event );

                    }

                }
        );

        fileMenu1.add( aboutMenuItem );

        aboutMenuItem.setMnemonic( 'A' );

        aboutMenuItem.setText( "About" );

        aboutMenuItem.addActionListener(
                new ActionListener()

                {

                    public void actionPerformed( ActionEvent event )

                    {

                        aboutMenuItemClicked( event );

                    }

                }
        );

        fileMenu.add( exitMenuItem );

        fileMenu1.add( aboutMenuItem );

        jMenuBar.add( fileMenu );

        jMenuBar.add( fileMenu1 );

        setJMenuBar( jMenuBar );

        pack();

    }

    private void exitMenuItemClicked( ActionEvent event )

    {

        System.exit( 0 );

    }

    private void extractMenuItemClicked( ActionEvent event )

    {

        System.out.println( "Extract Action Selected..." );

        picLabel.setVisible( false );

        WizardFrame wizardFrame = new ExtractAction( this ).getWizardFrame();

        jDesktopPane.add( wizardFrame );

        wizardFrame.moveToFront();

    }

    private void embedMenuItemClicked( ActionEvent event )

    {

        System.out.println( "Embed Action Selected..." );

        picLabel.setVisible( false );

        WizardFrame wizardFrame = new EmbedAction( this ).getWizardFrame();

        jDesktopPane.add( wizardFrame );

        wizardFrame.moveToFront();

    }

    private void aboutMenuItemClicked( ActionEvent event )

    {

        JOptionPane.showMessageDialog(this,"   Audio Steganography Using L.S.B\n\n"
                + " ","About", JOptionPane.PLAIN_MESSAGE );

    }

    private void exitForm( WindowEvent event )

    {

        System.exit( 0 );

    }

}
