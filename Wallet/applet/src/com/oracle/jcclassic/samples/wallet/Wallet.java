/** 
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
 * 
 */

/*
 */

/*
 * @(#)Wallet.java	1.11 06/01/03
 */

package com.oracle.jcclassic.samples.wallet;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.OwnerPIN;

public class Wallet extends Applet {

    /* constants declaration */

    // code of CLA byte in the command APDU header
    final static byte Wallet_CLA = (byte) 0x80;

    // codes of INS byte in the command APDU header
    final static byte VERIFY = (byte) 0x20;
    final static byte CONCURS = (byte) 0x30;
    final static byte NOTA = (byte) 0x40;
    final static byte PRIORITATE = (byte) 0x50;
    final static byte INFO = (byte) 0x60;
    // cod disciplin
    final static short COD_D1 = 0x01;
    final static short COD_D2 = 0x02;
    final static short COD_D3 = 0x03;
    final static short COD_D4 = 0x04;
    final static short COD_D5 = 0x05;
    final static short zi1 = 0x05;
    final static short luna1 = 0x05;
    final static short an1 = 0x07E5;
    
    final static short zi2 = 0x05;
    final static short luna2 = 0x05;
    final static short an2 = 0x07E5;
    
    final static short zi3 = 0x05;
    final static short luna3 = 0x05;
    final static short an3 = 0x07E5;
    
    final static short zi4 = 0x05;
    final static short luna4 = 0x05;
    final static short an4 = 0x07E5;
    
    final static short zi5 = 0x05;
    final static short luna5 = 0x05;
    final static short an5 = 0x07E5;
    
    short NOTA_D1 = 0x07;
    short NOTA_D2 = 0x09;
    short NOTA_D3 = 0x07;
    short NOTA_D4 = 0x09;
    short NOTA_D5 = 0x07;
    
    //cod concurs
    final static short COD_C1 = 0x01;
    final static short COD_C2 = 0x02;
    final static short COD_C3 = 0x03;
    final static short COD_C4 = 0x04;
    final static short COD_C5 = 0x05;
    
    public short PUNCT_C1 = 0;
    short PUNCT_C2 = 0;
    short PUNCT_C3 = 0;
    short PUNCT_C4 = 0x55;
    short PUNCT_C5 = 0x50;

    final static byte PIN_TRY_LIMIT = (byte) 0x03;
    // maximum size PIN
    final static byte MAX_PIN_SIZE = (byte) 0x08;

    // update punctaj failed 6666 cod error
    final static short SW_PUNCT_UPDATE_FAILED = 0x1A0A;
    // cod disciplina gresit 4040
    final static short SW_DISCIPLINA_NOT_FOUND = 0x0FC8;
    final static short SW_VERIFICATION_FAILED = 0x6300;
    // signal the the PIN validation is required
    // for a credit or a debit transaction
    final static short SW_PIN_VERIFICATION_REQUIRED = 0x6301;
    // signal invalid transaction amount
    // amount > MAX_TRANSACTION_AMOUNT or amount < 0
    final static short SW_INVALID_TRANSACTION_AMOUNT = 0x6A83;

    
    final static byte[] puk = {0x09, 0x09, 0x09, 0x09, 0x09, 0x09, 0x09, 0x09, 0x09};
    
    /* instance variables declaration */
    OwnerPIN pin;

    private Wallet(byte[] bArray, short bOffset, byte bLength) {

        // It is good programming practice to allocate
        // all the memory that an applet needs during
        // its lifetime inside the constructor
        pin = new OwnerPIN(PIN_TRY_LIMIT, MAX_PIN_SIZE);

        byte iLen = bArray[bOffset]; // aid length
        bOffset = (short) (bOffset + iLen + 1);
        byte cLen = bArray[bOffset]; // info length
        bOffset = (short) (bOffset + cLen + 1);
        byte aLen = bArray[bOffset]; // applet data length

        // The installation parameters contain the PIN
        // initialization value
        pin.update(bArray, (short) (bOffset + 1), aLen);
        register();

    } // end of the constructor

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        // create a Wallet applet instance
        new Wallet(bArray, bOffset, bLength);
    } // end of install method

    @Override
    public boolean select() {

        // The applet declines to be selected
        // if the pin is blocked.
        if (pin.getTriesRemaining() == 0) {
            return false;
        }

        return true;

    }// end of select method

    @Override
    public void deselect() {

        // reset the pin value
        pin.reset();

    }

    @Override
    public void process(APDU apdu) {

        // APDU object carries a byte array (buffer) to
        // transfer incoming and outgoing APDU header
        // and data bytes between card and CAD

        // At this point, only the first header bytes
        // [CLA, INS, P1, P2, P3] are available in
        // the APDU buffer.
        // The interface javacard.framework.ISO7816
        // declares constants to denote the offset of
        // these bytes in the APDU buffer

        byte[] buffer = apdu.getBuffer();
        // check SELECT APDU command

        if (apdu.isISOInterindustryCLA()) {
            if (buffer[ISO7816.OFFSET_INS] == (byte) (0xA4)) {
                return;
            }
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }

        // verify the reset of commands have the
        // correct CLA byte, which specifies the
        // command structure
        if (buffer[ISO7816.OFFSET_CLA] != Wallet_CLA) {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }

        switch (buffer[ISO7816.OFFSET_INS]) {
            case PRIORITATE:
                getPunctaje(apdu);
                return;
            case INFO:
                getCardInfo(apdu);
                return;
            case CONCURS:
            	concurs(apdu);
                return;
            case NOTA:
                nota(apdu);
                return;
            case VERIFY:
                verify(apdu);
                return;
            case 0x2C:
            	reset_pin_try_counter(apdu);
            	return;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }

    } // end of process method
    
    private boolean check_puk(byte[] buffer, short offset, byte bytesRead) {
    	short i;
    	short puk_counter = 0;
    	for( i=ISO7816.OFFSET_CDATA ; i < ISO7816.OFFSET_CDATA + bytesRead ; i ++) {
    		if(puk[puk_counter] != buffer[i]) {
    			ISOException.throwIt(SW_VERIFICATION_FAILED);
    			return false;
    		}
    		puk_counter ++;
    	}
    	return true;
    }
    
    private void reset_pin_try_counter(APDU apdu) {
    	 byte[] buffer = apdu.getBuffer();
         // retrieve the PIN data for validation.
         byte byteRead = (byte) (apdu.setIncomingAndReceive());
         
         if (check_puk(buffer, ISO7816.OFFSET_CDATA, byteRead) == true) {
        	 pin.resetAndUnblock();
         }
    }

    private void concurs(APDU apdu) {

        // access authentication
        if (!pin.isValidated()) {
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }

        byte[] buffer = apdu.getBuffer();

        // Lc byte denotes the number of bytes in the
        // data field of the command APDU
        byte numBytes = buffer[ISO7816.OFFSET_LC];

        // indicate that this APDU has incoming data
        // and receive data starting from the offset
        // ISO7816.OFFSET_CDATA following the 5 header
        // bytes.
        byte byteRead = (byte) (apdu.setIncomingAndReceive());

        // it is an error if the number of data bytes
        // read does not match the number in Lc byte
        if ((numBytes != 2) || (byteRead != 2)) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // get the id and points
        byte[] command = new byte[2];
        command[0]=buffer[ISO7816.OFFSET_CDATA];
        command[1]=buffer[ISO7816.OFFSET_CDATA+1];

        if ((short)command[0] == COD_C1) {
        	PUNCT_C1 = (short)command[1];
        }else if((short)command[0] == COD_C2) {
        	PUNCT_C2 = (short)command[1];
        }else if((short)command[0] == COD_C3) {
        	PUNCT_C3 = (short)command[1];
        }else if((short)command[0] == COD_C4) {
        	PUNCT_C4 = (short)command[1];
        }else if((short)command[0] == COD_C5) {
        	PUNCT_C5 = (short)command[1];
        }else {
        	ISOException.throwIt(SW_PUNCT_UPDATE_FAILED);
        }
        
        short le = apdu.setOutgoing();

        if (le < 2) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // informs the CAD the actual number of bytes
        // returned
        apdu.setOutgoingLength((byte) 2);
        

            buffer[0] = (byte) (PUNCT_C1 >> 8);
            buffer[1] = (byte) (PUNCT_C1 & 0xFF);
 
    	
        // send the 4-byte balance and bonus_points at the offset
        // 0 in the apdu buffer
        apdu.sendBytes((short) 0, (short) 2);

    } // end of deposit method
    private void nota(APDU apdu) {

        // access authentication
        if (!pin.isValidated()) {
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }

        byte[] buffer = apdu.getBuffer();

        // Lc byte denotes the number of bytes in the
        // data field of the command APDU
        byte numBytes = buffer[ISO7816.OFFSET_LC];

        // indicate that this APDU has incoming data
        // and receive data starting from the offset
        // ISO7816.OFFSET_CDATA following the 5 header
        // bytes.
        byte byteRead = (byte) (apdu.setIncomingAndReceive());

        // it is an error if the number of data bytes
        // read does not match the number in Lc byte
        if ((numBytes != 2) || (byteRead != 2)) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // get the id and points
        byte[] command = new byte[2];
        command[0]=buffer[ISO7816.OFFSET_CDATA];
        command[1]=buffer[ISO7816.OFFSET_CDATA+1];

        if ((short)command[0] == COD_D1) {
        	NOTA_D1 = (short)command[1];
        }else if((short)command[0] == COD_D2) {
        	NOTA_D2 = (short)command[1];
        }else if((short)command[0] == COD_D3) {
        	NOTA_D3 = (short)command[1];
        }else if((short)command[0] == COD_D4) {
        	NOTA_D4 = (short)command[1];
        }else if((short)command[0] == COD_D5) {
        	NOTA_D5 = (short)command[1];
        }else {
        	ISOException.throwIt(SW_DISCIPLINA_NOT_FOUND);
        }
        
        short le = apdu.setOutgoing();

        if (le < 2) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // informs the CAD the actual number of bytes
        // returned
        apdu.setOutgoingLength((byte) 2);
        

            buffer[0] = (byte) (NOTA_D1 >> 8);
            buffer[1] = (byte) (NOTA_D1 & 0xFF);
 
    	
        // send the 4-byte balance and bonus_points at the offset
        // 0 in the apdu buffer
        apdu.sendBytes((short) 0, (short) 2);

    }

    private void getPunctaje(APDU apdu) {
    	
    	if (!pin.isValidated()) {
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }

        byte[] buffer = apdu.getBuffer();
        
        // inform system that the applet has finished
        // processing the command and the system should
        // now prepare to construct a response APDU
        // which contains data field
        short le = apdu.setOutgoing();

        if (le < 10) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // informs the CAD the actual number of bytes
        // returned
        apdu.setOutgoingLength((byte) 10);
        
    	// move the balance data into the APDU buffer
        // starting at the offset 0
        	buffer[0] = (byte) (PUNCT_C1 >> 8);
            buffer[1] = (byte) (PUNCT_C1 & 0xFF);	
        	// move the bonus_points data into the APDU buffer
            // starting at the offset 0
            buffer[2] = (byte) (PUNCT_C2 >> 8);
            buffer[3] = (byte) (PUNCT_C2 & 0xFF);
            
            buffer[4] = (byte) (PUNCT_C3 >> 8);
            buffer[5] = (byte) (PUNCT_C3 & 0xFF);
            
            buffer[6] = (byte) (PUNCT_C4 >> 8);
            buffer[7] = (byte) (PUNCT_C4 & 0xFF);
            
            buffer[8] = (byte) (PUNCT_C5 >> 8);
            buffer[9] = (byte) (PUNCT_C5 & 0xFF);
    	
        // send the 10-byt at the offset
        // 0 in the apdu buffer
        apdu.sendBytes((short) 0, (short) 10);

    }

    private void getCardInfo(APDU apdu) {
    	
    	if (!pin.isValidated()) {
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }

        byte[] buffer = apdu.getBuffer();
        byte numBytes = buffer[ISO7816.OFFSET_LC];

        byte byteRead = (byte) (apdu.setIncomingAndReceive());
        if ((numBytes != 1) || (byteRead != 1)) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        byte id = buffer[ISO7816.OFFSET_CDATA];
        
        // inform system that the applet has finished
        // processing the command and the system should
        // now prepare to construct a response APDU
        // which contains data field
        short le = apdu.setOutgoing();

        if (le < 12) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // informs the CAD the actual number of bytes
        // returned
        apdu.setOutgoingLength((byte) 12);
        
    	// move the balance data into the APDU buffer
        // starting at the offset 0
        if(id == COD_D1) {
        	buffer[0] = (byte) (NOTA_D1 >> 8);
            buffer[1] = (byte) (NOTA_D1 & 0xFF);	
        	// move the bonus_points data into the APDU buffer
            // starting at the offset 0
            buffer[2] = (byte) (PUNCT_C1 >> 8);
            buffer[3] = (byte) (PUNCT_C1 & 0xFF);
            
            buffer[4] = (byte) (COD_C1 >> 8);
            buffer[5] = (byte) (COD_C1 & 0xFF);
            
            buffer[6] = (byte) (an1 >> 8);
            buffer[7] = (byte) (an1 & 0xFF);
            buffer[8] = (byte) (zi1 >> 8);
            buffer[9] = (byte) (zi1 & 0xFF);
            buffer[10] = (byte) (luna1 >> 8);
            buffer[11] = (byte) (luna1 & 0xFF); 
        }else if(id == COD_D2) {
        	buffer[0] = (byte) (NOTA_D2 >> 8);
            buffer[1] = (byte) (NOTA_D2 & 0xFF);	
        	// move the bonus_points data into the APDU buffer
            // starting at the offset 0
            buffer[2] = (byte) (PUNCT_C2 >> 8);
            buffer[3] = (byte) (PUNCT_C2 & 0xFF);
            
            buffer[4] = (byte) (COD_C2 >> 8);
            buffer[5] = (byte) (COD_C2 & 0xFF);
            
            buffer[6] = (byte) (an2 >> 8);
            buffer[7] = (byte) (an2 & 0xFF);
            buffer[8] = (byte) (zi2 >> 8);
            buffer[9] = (byte) (zi2 & 0xFF);
            buffer[10] = (byte) (luna2 >> 8);
            buffer[11] = (byte) (luna2 & 0xFF); 
        }else if(id == COD_D3) {
        	buffer[0] = (byte) (NOTA_D3 >> 8);
            buffer[1] = (byte) (NOTA_D3 & 0xFF);	
        	// move the bonus_points data into the APDU buffer
            // starting at the offset 0
            buffer[2] = (byte) (PUNCT_C3 >> 8);
            buffer[3] = (byte) (PUNCT_C3 & 0xFF);
            
            buffer[4] = (byte) (COD_C3 >> 8);
            buffer[5] = (byte) (COD_C3 & 0xFF);
            
            buffer[6] = (byte) (an3 >> 8);
            buffer[7] = (byte) (an3 & 0xFF);
            buffer[8] = (byte) (zi3 >> 8);
            buffer[9] = (byte) (zi3 & 0xFF);
            buffer[10] = (byte) (luna3 >> 8);
            buffer[11] = (byte) (luna3 & 0xFF); 
            
        }else if(id == COD_D4) {
        	buffer[0] = (byte) (NOTA_D4 >> 8);
            buffer[1] = (byte) (NOTA_D4 & 0xFF);	

            buffer[2] = (byte) (PUNCT_C4 >> 8);
            buffer[3] = (byte) (PUNCT_C4 & 0xFF);
            
            buffer[4] = (byte) (COD_C4 >> 8);
            buffer[5] = (byte) (COD_C4 & 0xFF);
            
            buffer[6] = (byte) (an4 >> 8);
            buffer[7] = (byte) (an4 & 0xFF);
            buffer[8] = (byte) (zi4 >> 8);
            buffer[9] = (byte) (zi4 & 0xFF);
            buffer[10] = (byte) (luna4 >> 8);
            buffer[11] = (byte) (luna4 & 0xFF); 
            
        }else if(id == COD_D5) {
        	buffer[0] = (byte) (NOTA_D5 >> 8);
            buffer[1] = (byte) (NOTA_D5 & 0xFF);	
        	// move the point data into the APDU buffer
            // starting at the offset 0
            buffer[2] = (byte) (PUNCT_C5 >> 8);
            buffer[3] = (byte) (PUNCT_C5 & 0xFF);
            
            buffer[4] = (byte) (COD_C5 >> 8);
            buffer[5] = (byte) (COD_C5 & 0xFF);
            
            buffer[6] = (byte) (an5 >> 8);
            buffer[7] = (byte) (an5 & 0xFF);
            buffer[8] = (byte) (zi5 >> 8);
            buffer[9] = (byte) (zi5 & 0xFF);
            buffer[10] = (byte) (luna5 >> 8);
            buffer[11] = (byte) (luna5 & 0xFF); 
        }else {
        	ISOException.throwIt(SW_DISCIPLINA_NOT_FOUND);
        }
    	
        // send 12bytes the offset
        // 0 in the apdu buffer
        apdu.sendBytes((short) 0, (short) 12);

    } // end of method

    private void verify(APDU apdu) {
    	//eroare pin gresit 3 ori
    	if(pin.getTriesRemaining() == 0) {
    		ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
    	}

        byte[] buffer = apdu.getBuffer();
        // retrieve the PIN data for validation.
        byte byteRead = (byte) (apdu.setIncomingAndReceive());

        // check pin
        // the PIN data is read into the APDU buffer
        // at the offset ISO7816.OFFSET_CDATA
        // the PIN data length = byteRead
        if (pin.check(buffer, ISO7816.OFFSET_CDATA, byteRead) == false) {
            ISOException.throwIt(SW_VERIFICATION_FAILED);
        }

    } // end of validate method
} // end of class Wallet

