package com.phopho.audiosteganography;

import java.util.EventListener;

public interface WizardButtonListener extends EventListener

{

    int BACK = 0;

    int NEXT = 1;

    int FINISH = 2;

    int CANCEL = 3;

    void buttonClicked( WizardButtonEvent wizardButtonEvent );

}
