package com.phopho.audiosteganography;

import java.awt.*;
import java.util.EventObject;

public class WizardButtonEvent extends EventObject

{

    private int buttonType;

    private Component card;

    private int cardPosition;

    // Constructor to creates a new instance of WizardButtonEvent

    public WizardButtonEvent( WizardFrame wizardFrame, int buttonType,
                              Component card, int cardPosition )

    {

        super( wizardFrame );

        this.buttonType = buttonType;

        this.card = card;

        this.cardPosition = cardPosition;

    }

    // Getter for property buttonType

    public int getButtonType()


    {

        return buttonType;

    }

    // Setter for property buttonType

    public void setButtonType( int buttonType )

    {

        this.buttonType = buttonType;

    }

    // Getter for property card

    public Component getCard()

    {

        return card;

    }

    // Setter for property card

    public void setCard( Component card )

    {

        this.card = card;

    }

    // Getter for property cardPosition

    public int getCardPosition()

    {

        return cardPosition;

    }

    // Setter for property cardPosition

    public void setCardPosition( int cardPosition )

    {

        this.cardPosition = cardPosition;

    }

}
