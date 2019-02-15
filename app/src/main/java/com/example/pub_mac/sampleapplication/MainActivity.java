package com.example.pub_mac.sampleapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;


import com.starmicronics.stario.PortInfo;
import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.stario.StarPrinterStatus;

import com.starmicronics.starioextension.ICommandBuilder;
import com.starmicronics.starioextension.StarIoExt;
import com.starmicronics.starioextension.StarIoExt.CharacterCode;
import com.starmicronics.starioextension.ICommandBuilder.CodePageType;
import com.starmicronics.starioextension.ICommandBuilder.InternationalType;
import com.starmicronics.starioextension.ICommandBuilder.AlignmentPosition;
import com.starmicronics.starioextension.ICommandBuilder.BarcodeSymbology;
import com.starmicronics.starioextension.ICommandBuilder.BarcodeWidth;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private StarIOPort mPort;

    private String  mPortName = null;
    private String  mPortSettings;
    private int     mTimeout;
    private Context mContext;

    private byte[] mCommands;

    private List<PortInfo> mPortList;

    public static final int PAPER_SIZE_TWO_INCH = 384;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ListView listView = (ListView) findViewById(R.id.listview);

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1);



        findViewById(R.id.button_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mPortList = StarIOPort.searchPrinter("BT:", mContext);

                    for (PortInfo port : mPortList)
                    {
                        Log.i("Log","Port Name: " + port.getPortName());
                        Log.i("Log","Mac Address: " + port.getMacAddress());
                        Log.i("Log","Model Name: " + port.getModelName());

                        adapter.add(port.getPortName());

                    }


                    listView.setAdapter(adapter);

                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                            String text = (String)listView.getItemAtPosition(position);
                            String msg = "Selected Printer is " + text;
                            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();

                            mPortName = text;
                        }
                    });


                }
                catch (StarIOPortException e) {
                    mPortList = new ArrayList<>();
                }
            }
        });




        findViewById(R.id.button_print).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mPortSettings  = "mini"; //for SM-S220i EscPos emulation
                mPortSettings += ";l";  //for retry getport
                mTimeout      = 10000; //10 sec

                try
                {
                    //Open port
                    if (mPort == null)
                    {
                        mPort = StarIOPort.getPort(mPortName, mPortSettings, mTimeout, mContext);

                    }

                    //Check printer status
                    StarPrinterStatus status;

                    status = mPort.beginCheckedBlock();

                    if (status.offline) {
                        throw new StarIOPortException("A printer is offline.");
                    }



                    //Create data
                    mCommands = createRasterImageData();

                    //Send print data
                    mPort.writePort(mCommands, 0, mCommands.length);

                    //Check printer status
                    mPort.setEndCheckedBlockTimeoutMillis(30000);     // 30000mS!!!
                    status = mPort.endCheckedBlock();


                    if (status.coverOpen) {
                        throw new StarIOPortException("Printer cover is open");
                    } else if (status.receiptPaperEmpty) {
                        throw new StarIOPortException("Receipt paper is empty");
                    } else if (status.offline) {
                        throw new StarIOPortException("Printer is offline");
                    }


                } catch (StarIOPortException e) {
                    e.printStackTrace();
                } finally
                {

                    if (mPort != null && mPortName != null) {
                        try {
                            StarIOPort.releasePort(mPort);
                        } catch (StarIOPortException e) {
                            // Nothing
                        }
                        mPort = null;
                    }
                }

            }
        });
    }

    private Bitmap createBitmap(String printText, int textSize, String fontFamily, boolean isBold, int printWidth) {
        Paint paint = new Paint();
        Bitmap bitmap;
        Canvas canvas;

        paint.setTextSize(textSize);

        if (fontFamily == null || fontFamily.isEmpty())
            fontFamily = "Questrial-Regular";
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/"+fontFamily+".ttf");
        if (isBold)
            typeface = Typeface.create(typeface, Typeface.BOLD);
        paint.setTypeface(typeface);

        paint.getTextBounds(printText, 0, printText.length(), new Rect());

        TextPaint textPaint = new TextPaint(paint);
        android.text.StaticLayout staticLayout = new StaticLayout(printText, textPaint, printWidth, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);

        // Create bitmap
        bitmap = Bitmap.createBitmap(staticLayout.getWidth(), staticLayout.getHeight(), Bitmap.Config.ARGB_8888);

        // Create canvas
        canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        canvas.translate(0, 0);
        staticLayout.draw(canvas);
        return bitmap;
    }


    public byte[] createRasterImageData() {

        ICommandBuilder builder = StarIoExt.createCommandBuilder(StarIoExt.Emulation.EscPosMobile); // set printer emulation (SM-S220i ESC/POS : EscPosMobile emulation )

        builder.beginDocument();

//        Bitmap image = create2inchRasterReceiptImage();
//        builder.appendBitmap(image, false);

        builder.appendBitmapWithAlignment(createBitmap("\n RIGHTCOM \n\n", 40, "", true, PAPER_SIZE_TWO_INCH), false, AlignmentPosition.Center);
        builder.appendBitmapWithAlignment(createBitmap("Orange Jordan Sweifieh \n\n", 25, "", false, PAPER_SIZE_TWO_INCH), false, AlignmentPosition.Center);
        builder.appendBitmapWithAlignment(createBitmap("14 Fevriver 2019           11:23 \n\n", 25, "", false, PAPER_SIZE_TWO_INCH), false, AlignmentPosition.Center);
        builder.appendBitmapWithAlignment(createBitmap("Motif : \n", 25, "", true, PAPER_SIZE_TWO_INCH), false, AlignmentPosition.Center);
        builder.appendBitmapWithAlignment(createBitmap("ORANGE CARE \n\n", 25, "", true, PAPER_SIZE_TWO_INCH), false, AlignmentPosition.Center);
        builder.appendBitmapWithAlignment(createBitmap("Ticket : \n", 25, "", false, PAPER_SIZE_TWO_INCH), false, AlignmentPosition.Center);
        builder.appendBitmapWithAlignment(createBitmap("0CA124 \n\n", 40, "", true, PAPER_SIZE_TWO_INCH), false, AlignmentPosition.Center);
        builder.appendBitmapWithAlignment(createBitmap("\n www.right-q.com \n\n", 20, "", false, PAPER_SIZE_TWO_INCH), false, AlignmentPosition.Center);

        builder.appendCutPaper(ICommandBuilder.CutPaperAction.PartialCutWithFeed);

        builder.endDocument();

        return builder.getCommands();
    }


//    public void printTicket(Context context, String strPrintArea, Ticket myticket, StarMicronicsPrinterListener listener) {
//        String labelCode = PrefUtils.getCodeLabel();
//        String labelMotif = PrefUtils.getMotifLabel();
//        ArrayList<byte[]> list = new ArrayList<byte[]>();
//
//        TextPrintSettings textPrintSettings = PrefUtils.getStartPrintSettings();
//
//
//        // Company name ==> CENTER BOLD
//        list.addAll(formatValue(textPrintSettings.getCompany_style(), textPrintSettings.getCompany_size(), myticket.getCompany() + "\n\n\n"));
//
//        // Agency name ==> CENTER NORMAL
//        list.addAll(formatValue(textPrintSettings.getAgency_name_style(), textPrintSettings.getAgency_name_size(), myticket.getAgence() + "\n" + "\n\n"));
//
//        // Visit hour ==> date --> LEFT <==> RIGHT <-- hour ... LEFT
//        list.addAll(formatValue(textPrintSettings.getVisite_hour_style(), textPrintSettings.getVisite_hour_size(), myticket.getArrivalDate() + "        " + myticket.getArrivalHour() + "\n\n"));
//
//        // Label motif ==> CENTER BOLD
//        list.addAll(formatValue(textPrintSettings.getLabel_motif_style(), textPrintSettings.getLabel_motif_size(), labelMotif+"\n"));
//
//        // Motif ==> CENTER BOLD
//        list.addAll(formatValue(textPrintSettings.getMotif_style(), textPrintSettings.getMotif_size(), myticket.getService() + "\n\n"));
//
//        // Label ticket ==> CENTER NORMAL
//        list.addAll(formatValue(textPrintSettings.getLabel_ticket_style(), textPrintSettings.getLabel_ticket_size(), labelCode+"\n"));
//
//        // Ticket ==> CENTER BOLD
//        list.addAll(formatValue(textPrintSettings.getTicket_style(), textPrintSettings.getTicket_size(), myticket.getTicket() + "\n\n"));
//
//        // Signature ==> CENTER NORMAL
//        list.add(("www.right-q.com\n\n").getBytes());
//
//
//
//
//
//
//
//
////        for(int i = 0; i< PrefUtils.getNumberTiket(); i++) {
////            // if (strPrintArea.equals("2inch (58mm)"))
////            //{
////            list.add(new byte[]{0x1d, 0x57, (byte) 0x80, 0x31}); // Page Area Setting <GS> <W> nL nH (nL = 128, nH = 1)
////
////            list.add(new byte[]{0x1b, 0x61, 0x01}); // Center Justification <ESC> a n (0 Left, 1 Center, 2 Right)
////
////            list.addAll(formatValue(textPrintSettings.getCompany_style(), textPrintSettings.getCompany_size(), myticket.getCompany() + "\n\n\n"));
//////            list.add(new byte[]{0x1b, 0x45, 0x01}); // Begin Strong text
//////            list.add(new byte[]{0x1d, 0x21, 0x11});//Begin Size
//////            list.add((myticket.getCompany() + "\n\n\n").getBytes());
//////            list.add(new byte[]{0x1d, 0x21, 0x00});//End size
//////            list.add(new byte[]{0x1b, 0x45, 0x00}); // End Strong text
////            list.addAll(formatValue(textPrintSettings.getAgency_name_style(), textPrintSettings.getAgency_name_size(), myticket.getAgence() + "\n" + "\n\n"));
//////            list.add(new byte[]{0x1b, 0x45, 0x01}); // Begin Strong text
//////
//////            list.add((myticket.getAgence() + "\n" + "\n\n").getBytes());
//////
//////
//////            list.add(new byte[]{0x1b, 0x45, 0x00}); // End Strong text
////            list.add(new byte[]{0x1b, 0x61, 0x00}); // Left Alignment
////            list.addAll(formatValue(textPrintSettings.getVisite_hour_style(), textPrintSettings.getVisite_hour_size(), myticket.getArrivalDate() + "        " + myticket.getArrivalHour() + "\n\n"));
//////            list.add((myticket.getArrivalDate() + "        " + myticket.getArrivalHour() + "\n\n").getBytes());
////            list.add(new byte[]{0x1b, 0x61, 0x01});//Center and write service name
//////            list.add(new byte[]{0x1b, 0x45, 0x01}); // Begin Strong text
////            if (PrefUtils.isEnablePrintLabel() && myticket.getService() != null && !myticket.getService().isEmpty() && !labelMotif.isEmpty()) {
////                list.addAll(formatValue(textPrintSettings.getLabel_motif_style(), textPrintSettings.getLabel_motif_size(), labelMotif+"\n"));
//////                list.add((labelMotif + "\n").getBytes());
////            }
////            list.addAll(formatValue(textPrintSettings.getMotif_style(), textPrintSettings.getMotif_size(), myticket.getService() + "\n\n"));
//////            list.add((myticket.getService() + "\n\n").getBytes());
//////            list.add(new byte[]{0x1b, 0x45, 0x00}); // End Strong text
////
//////            list.add(new byte[]{0x1b, 0x45, 0x01}); // Begin Strong text
////            if (PrefUtils.isEnablePrintLabel() && !labelCode.isEmpty()) {
////                list.addAll(formatValue(textPrintSettings.getLabel_ticket_style(), textPrintSettings.getLabel_ticket_size(), labelCode+"\n"));
//////                list.add((labelCode+"\n").getBytes());
////            }
//////            list.add(new byte[]{0x1b, 0x45, 0x00}); // End Strong text
////
////            list.addAll(formatValue(textPrintSettings.getTicket_style(), textPrintSettings.getTicket_size(), myticket.getTicket() + "\n\n"));
//////            list.add(new byte[]{0x1d, 0x21, 0x11}); // Width and Height Character Expansion <GS> ! n
//////            list.add(new byte[]{0x1b, 0x45, 0x01}); // Set Emphasized Printing ON
//////
//////            list.add((myticket.getTicket() + "\n\n").getBytes());
//////            list.add(new byte[]{0x1b, 0x45, 0x00});
//////            list.add(new byte[]{0x1d, 0x21, 0x00}); // Cancel Expansion - Reference Star Portable Printer Programming Manual
////
////            String displaytext_peoplewaiting = "";
////            String displaytext_averagewaiting = "";
////
////            String langue = RightQ.getLocale();
////
////            if (langue.equalsIgnoreCase(LocaleUtils.ENGLISH)) {
////                if (PrefUtils.isPrinterShowPeopleWaiting()) {
////                    if (myticket.getPeopleWaiting() > 0) {
////                        displaytext_peoplewaiting = "There are " + myticket.getPeopleWaiting() + " people before you";
////                    } else {
////                        displaytext_peoplewaiting = "There are no people before you";
////                    }
////                }
////                if (PrefUtils.isPrinterShowTimeWaiting()) {
////                    if (myticket.getAverageWaiting() > 0) {
////                        displaytext_averagewaiting = "Waiting time : " + myticket.getAverageWaiting();
////                    }
////                }
////            } else if (langue.equalsIgnoreCase(LocaleUtils.FRENCH)) {
////                if (PrefUtils.isPrinterShowPeopleWaiting()) {
////                    if (myticket.getPeopleWaiting() > 0) {
////                        displaytext_peoplewaiting = "Il y a " + myticket.getPeopleWaiting() + " personne(s) avant vous";
////                    } else {
////                        displaytext_peoplewaiting = "Il n'y a personne avant vous";
////                    }
////                }
////
////                if (PrefUtils.isPrinterShowTimeWaiting()) {
////                    if (myticket.getAverageWaiting() > 0) {
////                        displaytext_averagewaiting = "Attente moyenne : " + myticket.getAverageWaiting();
////                    }
////                }
////
////            } else if (langue.equalsIgnoreCase(LocaleUtils.ARABIC)) {
////
////                if (PrefUtils.isPrinterShowPeopleWaiting()) {
////                    if (myticket.getPeopleWaiting() > 0) {
////                        displaytext_peoplewaiting = " أشخاص قبل " + myticket.getPeopleWaiting() + " هناك ";
////
////                    } else {
////                        displaytext_peoplewaiting = "لا يوجد أشخاص أمامك";
////
////                    }
////                }
////                if (PrefUtils.isPrinterShowTimeWaiting()) {
////                    if (myticket.getAverageWaiting() > 0) {
////                        displaytext_averagewaiting = myticket.getAverageWaiting() + " وقت الانتظار : ";
////                    }
////                }
////            } else if (langue.equalsIgnoreCase(LocaleUtils.PORTUGESE)) {
////
////                if (PrefUtils.isPrinterShowPeopleWaiting()) {
////                    if (myticket.getPeopleWaiting() > 0) {
////                        displaytext_peoplewaiting = "Há " + myticket.getPeopleWaiting() + " pessoas antes de você ";
////                    } else {
////                        displaytext_peoplewaiting = "Não há pessoas antes de você";
////                    }
////                }
////                if (PrefUtils.isPrinterShowTimeWaiting()) {
////                    if (myticket.getAverageWaiting() > 0) {
////                        displaytext_averagewaiting = "Tempo de espera : " + myticket.getAverageWaiting();
////                    }
////                }
////            } else if (langue.equalsIgnoreCase(LocaleUtils.LUXEMBOURG)) {
////
////                if (PrefUtils.isPrinterShowPeopleWaiting()) {
////                    if (myticket.getPeopleWaiting() > 0) {
////                        displaytext_peoplewaiting = "Et gi " + myticket.getPeopleWaiting() + " Leit ier Dir";
////                    } else {
////                        displaytext_peoplewaiting = "Et gi keng Leit ier Dir";
////                    }
////                }
////                if (PrefUtils.isPrinterShowTimeWaiting()) {
////                    if (myticket.getAverageWaiting() > 0) {
////                        displaytext_averagewaiting = "akut Zäit : " + myticket.getAverageWaiting();
////                    }
////                }
////            } else {
////
////                if (PrefUtils.isPrinterShowPeopleWaiting()) {
////                    if (myticket.getPeopleWaiting() > 0) {
////                        displaytext_peoplewaiting = "There are " + myticket.getPeopleWaiting() + " people before you";
////                    } else {
////                        displaytext_peoplewaiting = "There are no people before you";
////                    }
////                }
////
////                if (PrefUtils.isPrinterShowTimeWaiting()) {
////                    if (myticket.getAverageWaiting() > 0) {
////                        displaytext_averagewaiting = "Waiting time : " + myticket.getAverageWaiting();
////                    }
////                }
////            }
////
////
////            if (RightQ.getLocale().equals(LocaleUtils.ARABIC)) {
////                try {
////                    list.add((displaytext_peoplewaiting + "\n\n").getBytes("UTF-16"));
////                    list.add((displaytext_peoplewaiting + "\n\n").getBytes());
////                    list.add((displaytext_peoplewaiting + "\n\n").getBytes("UTF-8"));
////                    Log.e("Charset", Charset.availableCharsets().keySet().toString());
////                } catch (Exception e) {
////                    HyperLog.e(TAG, e.getMessage());
////                    Log.e("Encode", e.toString());
////                    list.add((displaytext_peoplewaiting + "\n\n").getBytes());//Waiting people
////                }
////            } else {
////                list.add((displaytext_peoplewaiting + "\n\n").getBytes());//Waiting people
////            }
////
////
////            //Check if waiting time sentence not empty
////            if (!displaytext_averagewaiting.equals("") && RightQ.getLocale().equals(LocaleUtils.ARABIC)) {
////                try {
////                    list.add((displaytext_averagewaiting + " \n\n").getBytes("UTF-16"));
////                } catch (Exception e) {
////                    HyperLog.e(TAG, e.getMessage());
////                    Log.e("Encode", e.toString());
////                    list.add((displaytext_averagewaiting + " \n\n").getBytes());
////                }
////            } else if (!displaytext_averagewaiting.equals("") && !RightQ.getLocale().equals(LocaleUtils.ARABIC)) {
////                list.add((displaytext_averagewaiting + " min\n\n").getBytes());
////            }
////            list.add(("\n\n").getBytes());
////
////
////            list.add(("www.right-q.com\n\n").getBytes());
////
////            list.add("\n\n\n".getBytes());
////
////        }
//
//        // return sendCommand(context, portName, "portable;escpos", list, listener);
////        return sendCommand(context, portName, "mini", list, listener);
//        //   }
//
//        //     return sendCommand(context, portName, "portable;escpos", list,listener);
//    }


    //    public Bitmap create2inchRasterReceiptImage() {
//        String textToPrint =
//                "\nSMALL BUSINESSES : مبيعات وخدمات المؤسسات والشركات" +
//                        "\n\nBILLS PAYMENT : دفع الفواتير" +
//                        "\n\nAFTER SALE : خدمات ما بعد البيع" +
//                        "\n\nCOMPLAINTS : االشكاوي" +
//                        "\n\nOrange Care : صيانه الاجهزة الخلوية";
//
////        String textToPrint =
////                "   أهلا بك\n" +
////                        "\n" +
////                        "مرحبا\n" +
////                        "\n" +
////                        "-----------------------------\n" +
////                        "\n" +
////                        "\n Ceci est un test" + " تحية\n";
//
//        int      textSize = 25;
//        Typeface mtypeface = Typeface.createFromAsset(getAssets(), "fonts/Questrial-Regular.ttf");
//        Typeface typeface = Typeface.create(mtypeface, Typeface.BOLD);
////        Typeface typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);
//
//        return createBitmapFromText(textToPrint, textSize, PAPER_SIZE_TWO_INCH, typeface);
//    }



    public Bitmap createBitmapFromText(String printText, int textSize, int printWidth, Typeface typeface) {
        Paint paint = new Paint();
        Bitmap bitmap;
        Canvas canvas;

        paint.setTextSize(textSize);
        paint.setTypeface(typeface);

        paint.getTextBounds(printText, 0, printText.length(), new Rect());

        TextPaint textPaint = new TextPaint(paint);
        android.text.StaticLayout staticLayout = new StaticLayout(printText, textPaint, printWidth, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);

        // Create bitmap
        bitmap = Bitmap.createBitmap(staticLayout.getWidth(), staticLayout.getHeight(), Bitmap.Config.ARGB_8888);

        // Create canvas
        canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        canvas.translate(0, 0);
        staticLayout.draw(canvas);

        return bitmap;
    }
}
