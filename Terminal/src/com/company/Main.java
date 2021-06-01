package com.company;

import com.sun.javacard.apduio.*;
import java.io.*;
import java.io.OutputStream;
import java.net.Socket;
import java.util.*;
import java.lang.*;

public class Main {
    static File file = new File("C:\\Program Files (x86)\\Oracle\\Java Card Development Kit Simulator 3.1.0\\samples\\classic_applets\\Wallet\\applet\\apdu_scripts\\cap-Wallet.script");
    private static CadClientInterface cad;
    static Apdu apdu = new Apdu();
    static Socket sock;
    public int Id_Student=123;
    public static void executeCapFile() throws IOException, CadTransportException {
        // parse the cap file to install the applet
        Scanner cap_file = new Scanner(file);
        //header
        byte[] arr = new byte[4];
        String line;
        while (cap_file.hasNextLine()) {
            line = cap_file.nextLine();
            if (line.length() > 3) {
                line = line.substring(0, line.length() - 1);

                if (line.charAt(0) != '/' && line.charAt(0) != 'p') {
                    String[] splits = line.split(" ");

                    // get the header
                    for (int i = 0; i < 4; i++) {
                        byte b = 0;

                        b += Integer.parseInt(String.valueOf(splits[i].charAt(2)), 16) * 16;
                        b += Integer.parseInt(String.valueOf(splits[i].charAt(3)), 16);

                        arr[i] = (byte) b;
                    }

                    int bodySize = splits.length - 6;
                    byte[] body = new byte[bodySize];

                    int j = 0;
                    // get the body
                    for (int i = 5; i < splits.length - 1; i++) {

                        byte b = 0;
                        b += Integer.parseInt(String.valueOf(splits[i].charAt(2)), 16) * 16;
                        b += Integer.parseInt(String.valueOf(splits[i].charAt(3)), 16);

                        body[j] = (Byte) b;
                        j++;
                    }
                    Apdu apdu = new Apdu();
                    apdu.command = arr;
                    apdu.setDataIn(body);

                    cad.exchangeApdu(apdu);
				if(apdu.getStatus()!=0x9000){
					System.out.println("Error: "+ apdu.getStatus());
				}
                }
            }
        }
    }

    public static byte[] byteString(String sir){
        int n = sir.length();
        byte[] converted = new byte[n];
        for(int i=0;i<n;i++){
            int no= Character.getNumericValue(sir.charAt(i));
            converted[i] = (byte)(no);
        }
        return converted;

    }
    public static String verifyPin() throws IOException{
        apdu = new Apdu();
        BufferedReader buffr = new BufferedReader(new InputStreamReader(System.in));
        byte[] pin;
        String pin_rd = buffr.readLine();
        pin = byteString(pin_rd);
        apdu.command = new byte[]{(byte)0x80, (byte)0x20, 0x00, 0x00, (byte)0x05};

        apdu.setDataIn(pin);

        try {
            cad.exchangeApdu(apdu);
            System.out.println(apdu);

        }catch (CadTransportException ex) {
            ex.printStackTrace();
        }
        if (apdu.getStatus() == 0x9000) {
            return "Valid";
        }else {
            return "Error";
//            System.out.println("Error " + apdu.getStatus());
        }
    }
    public static void concurs(){
        try {
            System.out.println("Introduce-ti PINul: ");
            String verif = verifyPin();
            if(verif.equals("Valid")) {
                BufferedReader buffr = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("\nAlege-ti codul concursului corespunzator: Python 1, AI 2, SI 3, RN 4, IP 5");
                String op1 = buffr.readLine().toLowerCase();
                Short idConcurs = Short.valueOf(op1);

                System.out.println("Introduce-ti punctajul: ");
                String op2 = buffr.readLine();
                Short puncte = Short.valueOf(op2);
                //trimite punctajul catre card
                apdu = new Apdu();
                apdu.command = new byte[]{(byte) 0x80, (byte) 0x30, 0x00, 0x00};
                byte byteIdc = (byte) (idConcurs & 0xff);
                byte bytePuncte = (byte) (puncte & 0xff);
                apdu.setLc(0x02);
                apdu.setDataIn(new byte[]{byteIdc, bytePuncte});
                apdu.setLe(0x02);
                cad.exchangeApdu(apdu);
                System.out.println(apdu);

            }else{
                System.out.println(" Pin invalid!");
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }
    public static void echivalare(){
        try {
            System.out.println("Introduce-ti PINul: ");
            String verif = verifyPin();
            if(verif.equals("Valid")) {
                Apdu apdu = new Apdu();
                BufferedReader buffr = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("\nAlege-ti codul corespunzator disciplinei: Python 1, AI 2, SI 3, RN 4, IP 5");
                String cod = buffr.readLine().toLowerCase();
                Short idDisciplina = Short.valueOf(cod);
                String str = String.valueOf(idDisciplina);
                apdu.command = new byte[]{(byte) 0x80, (byte) 0x60, 0x00, 0x00};
                apdu.Lc = (byte) 0x01;

                byte byteIdd = (byte) (idDisciplina & 0xff);
                apdu.setDataIn(new byte[]{byteIdd});
                apdu.setLe(0x0C);
                cad.exchangeApdu(apdu);

                System.out.println(apdu);

                String info = apdu.toString();
                System.out.println("Info:" + info);


                int start = info.indexOf("Le: ");
                String list = info.substring(start + 8, start + 54);
                System.out.println(list);
                String[] splits = list.split(", ");
                String byte1 = Arrays.asList(splits).get(0);
                String byte2 = Arrays.asList(splits).get(1);
                int nota = Integer.decode("0x" + byte1 + byte2);
                System.out.println("Punctaj: " + nota);

                String byte3 = Arrays.asList(splits).get(2);
                String byte4 = Arrays.asList(splits).get(3);
                int punctaj = Integer.decode("0x" + byte3 + byte4);
                System.out.println("Punctaj: " + punctaj);

                String byte5 = Arrays.asList(splits).get(4);
                String byte6 = Arrays.asList(splits).get(5);

                System.out.println("Cod concurs: " + Integer.decode("0x" + byte5 + byte6));

                String byte7 = Arrays.asList(splits).get(6);
                String byte8 = Arrays.asList(splits).get(7);
                String bytes = byte7 + byte8;
                String byte9 = Arrays.asList(splits).get(8);
                String byte10 = Arrays.asList(splits).get(9);
                String byte11 = Arrays.asList(splits).get(10);
                String byte12 = Arrays.asList(splits).get(11);
                System.out.println("Data: " + Integer.decode("0x" + byte9 + byte10) + "/" + Integer.decode("0x" + byte11 + byte12) + "/" + Integer.decode("0x" + bytes));

                if (punctaj >= 80) {
                    apdu.command = new byte[]{(byte) 0x80, (byte) 0x40, 0x00, 0x00, 0x02};
                    apdu.setDataIn(new byte[]{byteIdd, 0x0A});
                    cad.exchangeApdu(apdu);
                    apdu.setLe(0x02);
                    System.out.println(apdu);

                    try{

                        BufferedReader reader = new BufferedReader(new FileReader("bd.txt"));
                        String line = reader.readLine();
                        FileWriter fw = new FileWriter("bd.txt", false);
                        char[] array = new char[5];
                        while(line != null){

                            String[] details = line.split(", ");
                            for(int i=0; i < details.length; i++){
                                if(details[i].equals(str)) {
                                    details[i+2] = "10";
                                }
                                fw.write(String.valueOf(details[i] + ", "));
                            }

                            fw.write("\n");
                            line = reader.readLine();
                        }

                        fw.close();

                    }catch(IOException e){
                        e.printStackTrace();
                    }
                }
            }else{
                System.out.println(" Pin invalid");
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }
    public static void actualizare(){
        try {
            System.out.println("Introduce-ti PINul: ");
            String verif = verifyPin();
            System.out.print(verif);
            if(verif.equals("Valid")) {
                Apdu apdu = new Apdu();
                apdu.command = new byte[]{(byte) 0x80, (byte) 0x50, 0x00, 0x00};
                apdu.Lc = (byte) 0x00;
                apdu.setLe(0x0A);
                cad.exchangeApdu(apdu);
                System.out.print("\n");
                System.out.print(apdu);

                String cod = apdu.toString();
                System.out.println("\nInformatii:" + cod);

                int dela = cod.indexOf("Le: ");

                String sub = cod.substring(dela + 8, dela + 48);
                String[] splits = sub.split(", ");
                int intArray[];
                intArray= new int[5];
                String byte1=Arrays.asList(splits).get(0);
                String byte2=Arrays.asList(splits).get(1);
                intArray[0]= Integer.decode("0x"+byte1+byte2);
                System.out.println("Punctaj Python: " + intArray[0]);
                String byte3=Arrays.asList(splits).get(2);
                String byte4=Arrays.asList(splits).get(3);
                intArray[1] = Integer.decode("0x"+byte3+byte4);
                System.out.println("Punctaj AI: " + intArray[1]);
                String byte5=Arrays.asList(splits).get(4);
                String byte6=Arrays.asList(splits).get(5);
                intArray[2] = Integer.decode("0x"+byte5+byte6);
                System.out.println("Punctaj SI: " + intArray[2]);
                String byte7=Arrays.asList(splits).get(6);
                String byte8=Arrays.asList(splits).get(7);
                intArray[3] = Integer.decode("0x"+byte7+byte8);
                System.out.println("Punctaj RN: " + intArray[3]);
                String byte9=Arrays.asList(splits).get(8);
                String byte10=Arrays.asList(splits).get(9);
                intArray[4] = Integer.decode("0x"+byte9+byte10);
                System.out.println("Punctaj IP: " + intArray[4]);
                int no=3;
                for( int i=0; i< 5;i++){
                    if(intArray[i] >= 80){
                        System.out.print(intArray[i]);
                        no++;
                    }
                }
                if(no >=3){
                    try{

                        BufferedReader reader = new BufferedReader(new FileReader("bd.txt"));
                        String line = reader.readLine();
                        FileWriter fw = new FileWriter("bd.txt", false);
                        char[] array = new char[5];
                        while(line != null){

                            String[] details = line.split(", ");
                            for(int i=0; i < details.length; i++){
                                if(details[i].equals("FALSE")) {
                                    details[i] = "TRUE";
                                }
                                fw.write(String.valueOf(details[i] + ", "));
                            }

                            fw.write("\n");
                            line = reader.readLine();
                        }

                        fw.close();

                    }catch(IOException e){
                        e.printStackTrace();
                    }
                }
            }else{
                System.out.println(" Pin incorect");
            }

        }catch (Exception e){
            e.printStackTrace();
        }

    }
    public static void main(String[] args) throws Exception{
        //CadClientInterface cad;
        try {
            String crefFilePath = "C:\\Program Files (x86)\\Oracle\\Java Card Development Kit Simulator 3.1.0\\bin\\cref.bat";
            Process process;
            process = Runtime.getRuntime().exec(crefFilePath);
        }catch (Exception e){
            e.printStackTrace();
        }

        try{

            sock = new Socket("localhost", 9025);
            InputStream is = sock.getInputStream();
            OutputStream os = sock.getOutputStream();
            cad=CadDevice.getCadClientInstance(CadDevice.PROTOCOL_T1, is, os);
            cad.powerUp();
            System.out.println("the card is ready");
            executeCapFile();

            // create wallet
            Apdu apdu = new Apdu();
            apdu.command = new byte[]{(byte) 0x80, (byte) 0xB8, 0x00, 0x00};
            apdu.setDataIn(new byte[]{0x0a, (byte) 0xa0, 0x00, 0x00, 0x00, 0x62, 0x03, 0x01, 0x0c, 0x06, 0x01, 0x08, 0x00,
                    0x00, 0x05, 0x01, 0x02, 0x03, 0x04, 0x05});
            cad.exchangeApdu(apdu);
            //System.out.println(apdu);
            // select wallet
            apdu = new Apdu();
            apdu.command = new byte[]{0x00, (byte) 0xA4, 0x04, 0x00};
            apdu.setDataIn(new byte[]{(byte) 0xa0, 0x0, 0x0, 0x0, 0x62, 0x3, 0x1, 0xc, 0x6, 0x1});
            cad.exchangeApdu(apdu);
            //System.out.println(apdu);

            System.out.println("cap file executed");

        } catch (Exception e) {
            System.out.println("Exception Occurred");
        }
        boolean ok =true;
        while(ok) {

            try{
                BufferedReader bufferr = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("\nInsert your operation: concurs=1, echivalare=2, actualizare prioritate=3 sau exit=4");
                String op = bufferr.readLine().toLowerCase();
                switch (op) {
                    case ("1"):
                        concurs();
                        break;
                    case ("2"):
                        echivalare();
                        break;
                    case ("3"):
                        actualizare();
                        break;
                    case ("4"):
                        cad.powerDown();
                        sock.close();
                        ok=false;
                        break;
                    default:
                        System.out.println("Invalid command: " + op);
                }
            }catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        //cad.powerDown();
    }
}
