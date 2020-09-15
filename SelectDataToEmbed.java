package com.phopho.audiosteganography;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class SelectDataToEmbed extends WizardPanel

{

    private JLabel selectFileLabel;

    private JRadioButton enterTextRadioButton;

    private JPanel choicePanel;

    private JPanel enterTextPanel;

    private JLabel embedLabel;

    private JScrollPane enterTextScrollPane;

    private JTextField embedFileTextField;

    private JRadioButton selectFileRadioButton;

    private JTextArea embedTextArea;

    private JPanel chooseFilePanel;

    private JButton selectButton;

    private JPanel selectFilePanel;

    private ButtonGroup choiceButtonGroup;

    private JPanel optionsPanel;

    private File selectedFile;

    private boolean textOrFile;

    private String embeddedText;

    // Creates new form SelectDataToEmbed

    public SelectDataToEmbed( String display )

    {

        super( display );

        initComponents();

        choiceButtonGroup.add( selectFileRadioButton );

        choiceButtonGroup.add( enterTextRadioButton );

        setFirstFocusable( selectFileRadioButton );

    }

    // This method is called from within the constructor to initialize the form

    private void initComponents()

    {

        choiceButtonGroup = new ButtonGroup();

        choicePanel = new JPanel();

        selectFileRadioButton = new JRadioButton();

        enterTextRadioButton = new JRadioButton();

        optionsPanel = new JPanel();

        selectFilePanel = new JPanel();

        selectFileLabel = new JLabel();

        chooseFilePanel = new JPanel();

        embedFileTextField = new JTextField();

        selectButton = new JButton();

        enterTextPanel = new JPanel();

        enterTextScrollPane = new JScrollPane();

        embedTextArea = new JTextArea();

        embedLabel = new JLabel();

        setLayout( new BorderLayout() );

        choicePanel.setBorder( new LineBorder( new Color( 204, 204, 204 ), 7 ) );

        selectFileRadioButton.setMnemonic( 'S' );

        selectFileRadioButton.setSelected( true );

        selectFileRadioButton.setText( "Select File" );

        selectFileRadioButton.addActionListener(
                new ActionListener()

                {

                    public void actionPerformed( ActionEvent event )

                    {

                        selectFileRadioButtonActionPerformed( event );

                    }

                }
        );

        choicePanel.add( selectFileRadioButton );

        enterTextRadioButton.setMnemonic( 'E' );

        enterTextRadioButton.setText( "Enter Text" );

        enterTextRadioButton.addActionListener(
                new ActionListener()

                {

                    public void actionPerformed( ActionEvent event )

                    {

                        enterTextRadioButtonActionPerformed( event );

                    }

                }
        );

        choicePanel.add( enterTextRadioButton );

        add( choicePanel, BorderLayout.NORTH );

        optionsPanel.setLayout( new CardLayout() );

        optionsPanel.setBorder( new LineBorder( new Color( 240, 240, 240  ), 5 ) );

        selectFilePanel.setLayout( new BorderLayout() );

        selectFileLabel.setHorizontalAlignment( SwingConstants.CENTER );

        selectFileLabel.setText( "Select the File to Embed" );

        selectFileLabel.setBorder( new LineBorder( new Color( 240, 240, 240 ), 5 ) );

        selectFilePanel.add( selectFileLabel, BorderLayout.CENTER );

        chooseFilePanel.setLayout( new BorderLayout() );

        embedFileTextField.setEditable( false );

        chooseFilePanel.add( embedFileTextField, BorderLayout.CENTER );

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

        chooseFilePanel.add( selectButton, BorderLayout.EAST );

        selectFilePanel.add( chooseFilePanel, BorderLayout.SOUTH );

        optionsPanel.add( selectFilePanel, "card3" );

        enterTextPanel.setLayout( new BorderLayout() );

        enterTextScrollPane.setViewportView( embedTextArea );

        enterTextPanel.add( enterTextScrollPane, BorderLayout.CENTER );

        embedLabel.setText( "                 Enter the Text to be Embedded into the Audio" );

        enterTextPanel.add( embedLabel, BorderLayout.SOUTH );

        optionsPanel.add( enterTextPanel, "card2" );

        add( optionsPanel, BorderLayout.CENTER );

    }

    private void selectButtonActionPerformed( ActionEvent event )

    {

        JFileChooser chooser = new JFileChooser( "." );

        if ( embedFileTextField.getText() != null )

            chooser.setSelectedFile( new File( embedFileTextField.getText() ) );

        if ( chooser.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION )

        {

            selectedFile = chooser.getSelectedFile();

            embedFileTextField.setText( selectedFile.getAbsolutePath() );

        }

    }

    private void enterTextRadioButtonActionPerformed( ActionEvent event )

    {

        ( ( CardLayout ) optionsPanel.getLayout() ).previous( optionsPanel );

        textOrFile = true;

    }

    private void selectFileRadioButtonActionPerformed( ActionEvent event )

    {

        ( ( CardLayout ) optionsPanel.getLayout() ).next( optionsPanel );

        textOrFile = false;

    }

    public boolean doValidation()

    {

        // embeddedText = embedTextArea.getText().trim();

        embeddedText = embedTextArea.getText();

        if ( selectFileRadioButton.isSelected() )

        {

            if ( embedFileTextField.getText().length() == 0 )

            {

                JOptionPane.showMessageDialog( this, "Select an Input File" );

                selectButton.requestFocus();

                return false;

            }

            else if ( ! new File( embedFileTextField.getText() ).exists() )

            {

                JOptionPane.showMessageDialog( this, "File not found! Select a Valid File" );

                selectButton.requestFocus();

                return false;

            }
        }

        else if ( enterTextRadioButton.isSelected() )

        {

            if ( embeddedText.length() == 0 )

            {

                JOptionPane.showMessageDialog( this, "Enter Some Text to Embed" );

                embedTextArea.requestFocus();

                return false;

            }

            // Handle too long text inputted by the sender

            else if ( embeddedText.length() > 2500 )

            {

                JOptionPane.showMessageDialog( this, "Text to Embed is too Long" );

                embedTextArea.requestFocus();

                return false;

            }

        }

        return true;

    }

    //Getter for property textOrFile

    public boolean isTextOrFile()

    {

        return textOrFile;

    }

    // Setter for property textOrFile

    public void setTextOrFile( boolean textOrFile )

    {

        this.textOrFile = textOrFile;

    }

    // Getter for property embeddedText

    public String getEmbeddedText()

    {

        return embeddedText;

    }

    public File getSelectedFile()

    {

        return selectedFile;

    }

}
