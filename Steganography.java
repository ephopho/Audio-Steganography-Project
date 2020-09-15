//Hiding Text in .Wav Audio File

package com.phopho.audiosteganography;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.util.Arrays;

public class Steganography

{

    public boolean acceptable = true;

    private AudioInputStream audioInputStream;

    private AudioFormat audioFormat;

    private byte[] audioBytes;

    private byte[] buffer;

    private byte[] cipherBuffer ;

    private byte[] clearBuffer;

    private String outputFile;

    private char[] password;

    PBEKeySpec pbeKeySpec;

    //First Constructor

    public Steganography( String audioFile, String plainText, String outputFile, char[] password )

    {

        this.password = password;

        this.outputFile = outputFile;

        readAudio( audioFile );

        acceptable = possible( plainText );

    }

    // Second Constructor

    public Steganography( String audioFile, String plainText, char[] password )

    {

        this.password = password;

        outputFile = plainText;

        readAudio( audioFile );

    }

    // Method to Encode

    public void encode()

    {

        int c = 0;

        int a = 1;

        int plainText;

        byte plainBuffer;

        //Hiding the cipher text in WAV file
        //First encode the length of the plain text

        plainText = cipherBuffer.length;

        for ( int b = 1; b <= 32; b++ )

        {

            if ( ( plainText & 0x80000000 ) != 0 ) // MSB of plain text is "1"

                audioBytes[ a ] = ( byte )( audioBytes[ a ] | 0x01 );

            else if ( ( audioBytes[ a ] & 0x01 ) != 0 ) // MSB of plain text is "0" & LSB of audio is "1"

            {


                audioBytes[ a ] = ( byte )( audioBytes[ a ] >>> 1 );

                audioBytes[ a ] = ( byte )( audioBytes[ a ] << 1 );

            }

            plainText = ( int )( plainText << 1 );

            a++;

        }

        // Start encoding the message

        while ( c < cipherBuffer.length )

        {

            plainBuffer = cipherBuffer[ c ];

            for ( int b = 1; b <= 8; b++ )

            {

                if ( ( plainBuffer & 0x80 ) != 0 ) // MSB of plain buffer is "1"

                audioBytes[ a ] = ( byte )( audioBytes[ a ] | 0x01 );

                else if( ( audioBytes[ a ] & 0x01 ) != 0 ) // MSB of plain text and LSB of audio is "1"

                {

                    audioBytes[ a ] = ( byte )( audioBytes[ a ] >>> 1 );

                    audioBytes[ a ] = ( byte )( audioBytes[ a ] << 1 );

                }

                plainBuffer = ( byte )( plainBuffer << 1 );

                a++;

            }// for b

            c++;

        }// while c

        System.out.println();

        // Write the byte array to an audio file

        File fileOut = new File( outputFile );

        ByteArrayInputStream byteInputStream = new ByteArrayInputStream( audioBytes );

        AudioInputStream audioInputStream1 = new AudioInputStream( byteInputStream,
                 audioInputStream.getFormat(), audioInputStream.getFrameLength() );

        if ( AudioSystem.isFileTypeSupported( AudioFileFormat.Type.WAVE,audioInputStream1 ) )

        {

            try

            {

                AudioSystem.write( audioInputStream1, AudioFileFormat.Type.WAVE, fileOut );

                System.out.println( "Steganographed WAV file is written as " + "outFile" + "..." );

            }

            catch ( Exception exception )

            {
                System.err.println( "Sound File write error" );

            }

        }


    }

    // Method to decode audio file

    public boolean decode()

    {

        boolean success = true;

        byte out = 0;

        int length = 0;

        int c = 1; // Start of plain text in audioBytes

        System.out.println( "Retrieving the ciphertext from WAV file..." );

        // First decode the first 32 samples to find the length of the message text

        length = length & 0x00000000;

        for ( int b = 1; b <= 32; b++ )

        {

            length = length << 1;

            if ( ( audioBytes[ c ] & 0x01 ) != 0 )

                length = length | 0x00000001;

            c++;

        }

        buffer = new byte[ length ];

        // Now decode the message

        out = ( byte )( out & 0x00 );

        for ( int a = 0; a < length; a++ )

        {

            for ( int b = 1; b <= 8; b++ )

            {

                out = ( byte ) ( out << 1 );

                if ( ( audioBytes[ c ] & 0x01 ) != 0 )

                    out = ( byte ) ( out | 0x01 );

                c++;

            }// for b

            buffer[ a ] = out;

            out = ( byte )( out & 0x00 );

        }// for a

        decrypt();

        try

        {

            System.out.println( "Writing the decrypted hidden message to " + outputFile + "..." );

            FileOutputStream outFile = new FileOutputStream( outputFile );

            outFile.write( clearBuffer );

            outFile.close();

        }

        catch ( Exception exception )

        {

            System.out.println( "Caught Exception: " + exception );

        }

        return true;

    }// decode

    // Method to read the audio file

    private void readAudio( String audioFile )

    {

        File audioFile1 = new File( audioFile );

        int totalFramesRead = 0;

        System.out.println( "Reading (WAV) audio file..." );

        try

        {

            audioInputStream = AudioSystem.getAudioInputStream( audioFile1 );

            // audioFormat = audioInoutStream.getFormat();

            int bytesPerFrame = audioInputStream.getFormat().getFrameSize();

            // Set an arbitrary buffer size of 1024 frames

            int numBytes = 154600 * bytesPerFrame;

            audioBytes = new byte[ numBytes ];

            try

            {
                int numBytesRead = 0;

                int numFramesRead = 0;

                // Try to read numBytes bytes from the file

                while ( ( numBytesRead = audioInputStream.read( audioBytes ) ) != -1 )

                {

                    // Calculate the number of frames actually read

                    numFramesRead = numBytesRead / bytesPerFrame;

                    totalFramesRead += numFramesRead;

                    // Working with the audio data that's now in the audioBytes array

                }

            }

            catch ( Exception exception )

            {

                System.out.println( "Audio file error: " + exception );

            }

        }

        catch ( Exception exception1 )

        {

            System.out.println( "Audio file error: " + exception1 );

        }

    }// readAudio

    // is it possible to do steganography with the current file

    private boolean possible( String plainText )

    {

        // accessing the input file
       try

        {

            System.out.println( "Reading the plaintext file... " + plainText );

            FileInputStream fileInputStream2 = new FileInputStream( plainText );

            BufferedInputStream bufferedInputStream = new BufferedInputStream( fileInputStream2 );

            int len = bufferedInputStream.available();

            buffer = new byte[ len ];

            while ( bufferedInputStream.available() != 0 )

                len = bufferedInputStream.read( buffer );

            bufferedInputStream.close();

            fileInputStream2.close();

        }

       catch ( Exception exception )

       {

           System.out.println( "Could Not Read Plain Text Caught Exception: " + exception );

       }

       try

       {

           encrypt();

           if ( cipherBuffer.length * 8 > audioBytes.length )

               return false;

           else
               return true;

       }

       catch ( Exception exception )

       {

           System.out.println( "Caught Exception: " + exception );

       }

       return true;

    }// Method possible

    //Generated Password relevant to underlying algorithm from characters

    private char[] generatePassword( char[] inputValue ) throws IOException

    {

        char[] lineBuffer;

        char[] buffer1;

        int a;

        buffer1 = lineBuffer = new char[ 128 ];

        int room = buffer1.length;

        int offset = 0;

        int c;

        int index = 0;

        int lengthOfInputValue = inputValue.length;

        System.out.println( "Debug:inputValue: " + inputValue );

        System.out.println( "Debug:lengthOfInputValue: " + lengthOfInputValue );

        loop:

        // while ( true )

            while ( index < lengthOfInputValue )

            {

                // switch ( c = in.read() )

                switch ( c = inputValue[ index ] )

                {

                    case -1:

                    case '\n':

                        System.out.println( "Debug:: I am in NewLine or -1" );

                        break loop;

                    case '\r':

                        System.out.println( "Debug:: I am in Carriage Return " );

                        index ++;

                        int c1 = inputValue[ index ];

                        if ( ( c1 != '\n' ) && ( c1 != -1 ) )

                        {

                            index --;

                            System.out.println( "Debug:: I am in Carriage Return IF Block " );

                        }

                        else

                            break loop;

                    default:

                        if ( -- room < 0 )

                        {

                            buffer1 = new char[ offset + 128 ];

                            room = buffer1.length - offset - 1;

                            System.arraycopy( lineBuffer, 0, buffer1, 0, offset );

                            Arrays.fill( lineBuffer, ' ' );

                            lineBuffer = buffer1;

                        }

                        buffer1[ offset ++ ] = ( char ) c;

                        break;

                }// Switch

                index ++;

            }// while

            if ( offset == 0 )

                return null;

            char[] ret = new char[ offset ];

            System.arraycopy( buffer1, 0, ret, 0, offset );

            Arrays.fill( buffer1, ' ' );

            System.out.println( "...Password Generated: " + ret );

            return ret;

    }

    // Read user password from given input stream

    private char[] readPassword( InputStream inputStream ) throws IOException

    {

        char[] lineBuffer;

        char[] buffer2;

        int a;

        buffer2 = lineBuffer = new char[ 128 ];

        int room = buffer2.length;

        int offset = 0;

        int c;

        loop:

        while ( true )

        {

            switch ( c = inputStream.read() )

            {

                case -1:

                case '\n':

                    break loop;

                case'\r':

                    int c2 = inputStream.read();

                    if ( ( c2 != '\n' ) && ( c2 != -1 ) )

                    {

                        if ( !( inputStream instanceof PushbackInputStream ) )

                            inputStream = new PushbackInputStream( inputStream );

                        ( (PushbackInputStream ) inputStream ).unread( c2 );

                    }

                    else

                        break loop;

                default:

                    if ( -- room < 0 )

                    {

                        buffer2 = new char[ offset + 128 ];

                        room = buffer2.length - offset - 1;

                        System.arraycopy( lineBuffer, 0, buffer2, 0, offset );

                        Arrays.fill( lineBuffer, ' ' );

                        lineBuffer = buffer2;

                    }

                    buffer2[ offset ++ ] = ( char ) c;

                    break;

            }// switch

        }// while

        if ( offset == 0 )

            return null;

        char[] ret = new char[ offset ];

        System.arraycopy( buffer2, 0, ret, 0, offset );

        Arrays.fill( buffer2, ' '  );

        System.out.println( "Password Captured: " + ret );

        return ret;

    }// Read password

    // Password based encryption

    private void encrypt()


    {

        // PBEKeySpec pbeKeySpec;

        PBEParameterSpec pbeParameterSpec;

        SecretKeyFactory secretKeyFactory;

        // Salt used for the password

        byte[] salt = { ( byte )0xc7, ( byte )0x73, ( byte )0x21, ( byte )0x8c,
                 ( byte )0x7e, ( byte )0xc8, ( byte )0xee, ( byte ) 0x99 };

        // Iteration count

        int count = 20;

        // Create PBE parameter

        pbeParameterSpec= new PBEParameterSpec( salt, count );

        // Prompt user for encryption password
        // Collect user password as char array (using the
        // "readPassword" method from above), and convert
        // to SecretKey object, using a PBE key factory

        try

        {

            System.out.println( "Password Verification: " + password );

            pbeKeySpec = new PBEKeySpec( password );

            System.out.println( "Encrypting the plaintext message..." );

            secretKeyFactory = SecretKeyFactory.getInstance( "PBEWithMD5AndDES" );

            SecretKey pbeKey = secretKeyFactory.generateSecret( pbeKeySpec );

            // Create PBE Cipher

            Cipher pbeCipher = Cipher.getInstance( "PBEWithMD5AndDES" );

            // Initialize PBE Cipher with key and parameters

            pbeCipher.init( Cipher.ENCRYPT_MODE, pbeKey, pbeParameterSpec );

            // Encrypt the cleatext

            cipherBuffer = pbeCipher.doFinal( buffer );

        }

        catch ( Exception exception )

        {

            System.out.println( "Caught Exception: " + exception );

            exception.printStackTrace();

        }


    }// Method encrypt

    // Password based decryption

    private void decrypt()

    {

        PBEKeySpec pbeKeySpec;

        PBEParameterSpec pbeParameterSpec;

        SecretKeyFactory secretKeyFactory;

        // same salt as in Method encrypt

        byte[] salt = { ( byte )0xc7, ( byte )0x73, ( byte )0x21, ( byte )0x8c,
                ( byte )0x7e, ( byte )0xc8, ( byte )0xee, ( byte )0x99 };

        // Same iteration count in Method encrypt

        int count = 20;

        // Create PBE parameter set

        pbeParameterSpec = new PBEParameterSpec( salt, count );

        // Prompt user for encryption password
        // Collect user password as char array ( using the
        // "readPassword" method from above ), and convert
        // to SecretKey object, using a PBE key factory

        try

        {

            System.out.println();

            System.out.print( "Enter encryption password: " );

            pbeKeySpec = new PBEKeySpec( password );

            System.out.println( "Decrypting the ciphertext..." );

            secretKeyFactory = SecretKeyFactory.getInstance( "PBEWithMD5AndDES" );

            SecretKey pbeKey = secretKeyFactory.generateSecret( pbeKeySpec );

            // Create PBE Cipher

            Cipher pbeCipher = Cipher.getInstance( "PBEWithMD5AndDES" );

            // Initialize PBE Cipher with key and parameters

            pbeCipher.init( Cipher.DECRYPT_MODE, pbeKey, pbeParameterSpec );

            // Decrypt the cleartext

            try

            {

                clearBuffer = pbeCipher.doFinal( buffer );

            }

            catch ( Exception exception )

            {

                System.out.println( "Password does not match!! ");

                System.out.println( "Caught Exception" + exception );

            }

        }

        catch ( Exception exception1 )

        {
            System.out.println( "Caught Exception1: " + exception1 );

        }

    }// Method decrypt

}
