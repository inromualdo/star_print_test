package com.example.pub_mac.sampleapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.starmicronics.stario.PortInfo;
import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.stario.StarPrinterStatus;
import com.starmicronics.starioextension.ICommandBuilder;
import com.starmicronics.starioextension.StarIoExt;

import java.util.ArrayList;
import java.util.List;


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

    public byte[] createRasterImageData() {
        ICommandBuilder builder = StarIoExt.createCommandBuilder(StarIoExt.Emulation.EscPosMobile); // set printer emulation (SM-S220i ESC/POS : EscPosMobile emulation )
        builder.beginDocument();
        Bitmap image = create2inchRasterReceiptImage();
        builder.appendBitmap(image, false);
        builder.endDocument();
        return builder.getCommands();
    }


    public Bitmap create2inchRasterReceiptImage() {
        /**
         * This will print without error
         */
        String textToPrint = "Test to print \n.\n.\n.";
        /**
         * This will print maybe the first time and throw port error
         */
        String longTextToPrint = "Test to print will not print because too long \n.\n.\n.";
        String arabicText = "ان عدة الشهور عند الله اثنا عشر شهرا في كتاب الله يوم خلق السماوات والارض منها اربعة حرم ذلك الدين القيم فلاتظلموا فيهن انفسكم وقاتلوا المشركين كافة كما يقاتلونكم كافة واعلموا ان الله مع المتقينان عدة الشهور عند الله اثنا عشر شهرا في كتاب الله يوم خلق السماوات والارض منها اربعة حرم ذلك الدين القيم فلاتظلموا فيهن انفسكم وقاتلوا المشركين كافة كما يقاتلونكم كافة واعلموا ان الله مع المتقينان عدة الشهور عند الله اثنا عشر شهرا في كتاب الله يوم خلق السماوات والارض منها اربعة حرم ذلك الدين القيم فلاتظلموا فيهن انفسكم وقاتلوا المشركين كافة كما يقاتلونكم كافة واعلموا ان الله مع المتقينان عدة الشهور عند الله اثنا عشر شهرا في كتاب الله يوم خلق السماوات والارض منها اربعة حرم ذلك الدين القيم فلاتظلموا فيهن انفسكم وقاتلوا المشركين كافة كما يقاتلونكم كافة واعلموا ان الله مع المتقينان عدة الشهور عند الله اثنا عشر شهرا في كتاب الله يوم خلق السماوات والارض منها اربعة حرم ذلك الدين القيم فلاتظلموا فيهن انفسكم وقاتلوا المشركين كافة كما يقاتلونكم كافة واعلموا ان الله مع المتقينان عدة الشهور عند الله اثنا عشر شهرا في كتاب الله يوم خلق السماوات والارض منها اربعة حرم ذلك الدين القيم فلاتظلموا فيهن انفسكم وقاتلوا المشركين كافة كما يقاتلونكم كافة واعلموا ان الله مع المتقينان عدة الشهور عند الله اثنا عشر شهرا في كتاب الله يوم خلق السماوات والارض منها اربعة حرم ذلك الدين القيم فلاتظلموا فيهن انفسكم وقاتلوا المشركين كافة كما يقاتلونكم كافة واعلموا ان الله مع المتقينان عدة الشهور عند الله اثنا عشر شهرا في كتاب الله يوم خلق السماوات والارض منها اربعة حرم ذلك الدين القيم فلاتظلموا فيهن انفسكم وقاتلوا المشركين كافة كما يقاتلونكم كافة واعلموا ان الله مع المتقينان عدة الشهور عند الله اثنا عشر شهرا في كتاب الله يوم خلق السماوات والارض منها اربعة حرم ذلك الدين القيم فلاتظلموا فيهن انفسكم وقاتلوا المشركين كافة كما يقاتلونكم كافة واعلموا ان الله مع المتقينان عدة الشهور عند الله اثنا عشر شهرا في كتاب الله يوم خلق السماوات والارض منها اربعة حرم ذلك الدين القيم فلاتظلموا فيهن انفسكم وقاتلوا المشركين كافة كما يقاتلونكم كافة واعلموا ان الله مع المتقين";

        int      textSize = 25;
        return createBitmapFromText(longTextToPrint, textSize, PAPER_SIZE_TWO_INCH);
    }



    public Bitmap createBitmapFromText(String printText, int textSize, int printWidth) {
        Paint paint = new Paint();
        Bitmap bitmap;
        Canvas canvas;

        paint.setTextSize(textSize);

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
