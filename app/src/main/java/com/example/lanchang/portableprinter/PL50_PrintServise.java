package com.example.lanchang.portableprinter;

import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.posapi.PosApi;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.example.lanchang.portableprinter.Interfaces.onPrintListener;
import com.example.lanchang.portableprinter.util.BarcodeCreater;
import com.example.lanchang.portableprinter.util.BitmapTools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

//public interface OnPrintListener {
//
//    void onFailed(int state);
//
//    void onFinish();
//
//    /**
//     * Get paper state
//     *
//     * @param state 0：Has paper   1：No paper
//     */
//    void onGetState(int state);
//
//    /**
//     * Set paper state
//     *
//     * @param state 0：Has paper   1：No paper
//     */
//    void onPrinterSetting(int state);
//}

public class PL50_PrintServise extends Service implements onPrintListener {

    public class PrintDataService {
        //1 text   2 bmp
        int mPrintType = 1;
        int mConcentration = 25;
        int mLeft = 0;
        byte[] mData = null;
        int mHeight = 0;
        int mWidth = 0;

        public int getPrintType() {
            return mPrintType;
        }
        public void setPrintType(int mPrintType) {
            this.mPrintType = mPrintType;
        }
        public int getConcentration() {
            return mConcentration;
        }
        public void setConcentration(int mConcentration) {
            this.mConcentration = mConcentration;
        }
        public int getLeft() {
            return mLeft;
        }
        public void setLeft(int mLeft) {
            this.mLeft = mLeft;
        }
        public byte[] getData() {
            return mData;
        }
        public void setData(byte[] mData) {
            this.mData = mData;
        }
        public int getHeight() {
            return mHeight;
        }
        public void setHeight(int mHeight) {
            this.mHeight = mHeight;
        }
        public int getWidth() {
            return mWidth;
        }
        public void setWidth(int mWidth) {
            this.mWidth = mWidth;
        }
    }
    private static final String TAG = "tag_log";//getSimpleName();
    private static final int PRINT_TYPE_TEXT = 1;
    private static final int PRINT_TYPE_BMP = 2;

    private PosApi mPosApi = null;
    private Context mContext = null;
    private Bitmap mBitmap = null;

    private ControlThread mControlThread = null;
    private Looper mSendLooper = null;
    private Handler mSendHandler = null;
    private LinkedList<PrintDataService> mSendList = null;
    private static final int MSG_PRINT_NEXT = 11;
 //   private OnPrintListener mListener = null;

    @Override
    public void onFailed(int state) {
        // TODO Auto-generated method stub
        switch (state) {
            case PosApi.ERR_POS_PRINT_NO_PAPER:
                showTip(getString(R.string.print_no_paper));
                break;
            case PosApi.ERR_POS_PRINT_FAILED:
                showTip(getString(R.string.print_failed));
                break;
            case PosApi.ERR_POS_PRINT_VOLTAGE_LOW:
                showTip(getString(R.string.print_voltate_low));
                break;
            case PosApi.ERR_POS_PRINT_VOLTAGE_HIGH:
                showTip(getString(R.string.print_voltate_high));
                break;
        }
    }
    @Override
    public void onFinish() {
        // TODO Auto-generated method stub
//        Toast.makeText(PrintBarcodeActivity.this, "Print Finished!",
//                Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onGetState(int state) {
        switch (state) {
            case 0:
                //                       Toast.makeText(PrinterActivity.this, "Has paper!",
//                                Toast.LENGTH_SHORT).show();

                break;
            case 1:

//                        Toast.makeText(PrinterActivity.this, "No paper!",
//                                Toast.LENGTH_SHORT).show();

                break;
        }
    }
    @Override
    public void onPrinterSetting(int state) {
        switch (state) {
            case 0:
//                        Toast.makeText(PrinterActivity.this, "Has paper",
//                                Toast.LENGTH_SHORT).show();
                break;
            case 1:
//                        Toast.makeText(PrinterActivity.this, "No paper",
//                                Toast.LENGTH_SHORT).show();
                break;
            case 2:
//                        Toast.makeText(PrinterActivity.this,
//                                "Detected black mark", Toast.LENGTH_SHORT).show();
                break;
        }
    }
    private class ControlThread extends Thread {
        @Override
        public void run() {
            log("Print Control thread[" + getId() + "] run...");
            Looper.prepare();
            mSendLooper = Looper.myLooper();
            mSendHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {

                        case MSG_PRINT_NEXT:
                            log("PRINT_NEXT_DATA");
                            print();
                            break;
                    }
                }
            };
            //mSendHandler.sendMessage(mSendHandler.obtainMessage(MSG_CONNECT_SERVER));
            Looper.loop();
            log("Print Control thread exit!!!!!!!!!");
        }
    }

    final String FILENAME_SD = "print_folder/printtxtfile.txt";
    final String PATH_SD = "mnt/sdcard/ScanManager/";
// final File PATH_SD = Environment.getExternalStorageDirectory();
//    final String FILENAME_IMAGE_SD = "print_image_folder/testprintfile_001.jpg";
 final String FILENAME_IMAGE_SD = "print_image_folder/printbmpfile_001.bmp";
// final String FILENAME_IMAGE_SD = "printbmpfile_001.bmp";

    final String LOG_TAG = "myLogs";
    boolean isStoped;
    int tflag;
     int id;

//    private PrintQueue mPrintQueue = null;
//    private PosApi mPosApi;
//    private Handler mSendHandler = null;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(LOG_TAG, "onCreate");
        isStoped = false;
        mPosApi = PosApi.getInstance(PL50_PrintServise.this);
     //   mPrintQueue = new PrintQueue(PL50_PrintServise.this, mPosApi);
        mContext = PL50_PrintServise.this;
        mSendList = new LinkedList<PrintDataService>();
    //    mPrintQueue = new PrintQueue(PL50_PrintServise.this, mPosApi, mSendHandler);
        init();

//        PL50_PrintServise.setOnPrintListener(new PL50_PrintServise.OnPrintListener() {
//
//            @Override
//            public void onGetState(int state) {
//                switch (state) {
//                    case 0:
// //                       Toast.makeText(PrinterActivity.this, "Has paper!",
////                                Toast.LENGTH_SHORT).show();
//
//                        break;
//                    case 1:
//
////                        Toast.makeText(PrinterActivity.this, "No paper!",
////                                Toast.LENGTH_SHORT).show();
//
//                        break;
//                }
//            }
//            @Override
//            public void onPrinterSetting(int state) {
//                switch (state) {
//                    case 0:
////                        Toast.makeText(PrinterActivity.this, "Has paper",
////                                Toast.LENGTH_SHORT).show();
//                        break;
//                    case 1:
////                        Toast.makeText(PrinterActivity.this, "No paper",
////                                Toast.LENGTH_SHORT).show();
//                        break;
//                    case 2:
////                        Toast.makeText(PrinterActivity.this,
////                                "Detected black mark", Toast.LENGTH_SHORT).show();
//                        break;
//                }
//            }
//            @Override
//            public void onFinish() {
//                // TODO Auto-generated method stub
////        Toast.makeText(PrintBarcodeActivity.this, "Print Finished!",
////                Toast.LENGTH_SHORT).show();
//            }
//            @Override
//            public void onFailed(int state) {
//                // TODO Auto-generated method stub
//                switch (state) {
//                    case PosApi.ERR_POS_PRINT_NO_PAPER:
//                        showTip(getString(R.string.print_no_paper));
//                        break;
//                    case PosApi.ERR_POS_PRINT_FAILED:
//                        showTip(getString(R.string.print_failed));
//                        break;
//                    case PosApi.ERR_POS_PRINT_VOLTAGE_LOW:
//                        showTip(getString(R.string.print_voltate_low));
//                        break;
//                    case PosApi.ERR_POS_PRINT_VOLTAGE_HIGH:
//                        showTip(getString(R.string.print_voltate_high));
//                        break;
//                }
//            }
        someTask();
        }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG,"onStartCommand");
        isStoped = false;
        tflag = flags;
        id = startId;
        return super.onStartCommand(intent, flags, startId);

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
        isStoped = true;
        //    stopSelf();
    }
    private void someTask() {
        Toast.makeText(this, "проверка" ,Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            public void run() {
                for (int i=1; i <= 10000;i++){
                    //              int d = Log.d(LOG_TAG, " i = " + i);
                    //                Toast.makeText(this, i ,Toast.LENGTH_SHORT).show();
                    if(isStoped == true){
                        break;
                    }
                    //     Log.d(LOG_TAG, " i = " + i + " flag = " + tflag + " id = " + id);
//commit
                    File f = new File(PATH_SD, FILENAME_SD);
                    File fi = new File(PATH_SD, FILENAME_IMAGE_SD);
                    if (f.exists()&&!f.isDirectory()){
                        Log.d(LOG_TAG,"файл есть");

                        printTextWithInput();
           //             mPosApi.printText(1, new byte[] {0x55, 0x56, 0x51, 0x7e, 0x7e, 0x58}, 6);
                    }else{
                        Log.d(LOG_TAG, "файла нет");
                    }
                    if (fi.exists()&&!f.isDirectory()){
                        Log.d(LOG_TAG,"файл картинки есть");
                        printBMP(fi);
                        //             mPosApi.printText(1, new byte[] {0x55, 0x56, 0x51, 0x7e, 0x7e, 0x58}, 6);
                    }else{
                        Log.d(LOG_TAG, "файла картинки нет");
                    }

                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
                if(isStoped == false) { stopSelf(); }
            }
        }).start();
    }
    private void someTask1() {
        Toast.makeText(this, "без проверки" ,Toast.LENGTH_SHORT).show();
        for (int i=1; i <= 15;i++){
            //              int d = Log.d(LOG_TAG, " i = " + i);
            //                Toast.makeText(this, i ,Toast.LENGTH_SHORT).show();
            Log.d(LOG_TAG, " i = " + i);
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }
    //   @androidx.annotation.Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    private void printTextWithInput() {
        try {
            int concentration = Integer.valueOf(("60").trim());
//            if (TextUtils.isEmpty(etContent.getText().toString())) {
//                return;
            String sp = "";
            File ft = new File(PATH_SD, FILENAME_SD);
            if (ft.exists()&&!ft.isDirectory()){
                sp = readFileSD();
            }else{

            }

            sp = readFileSD();
            StringBuilder sb = new StringBuilder();
            sb.append(sp);
            sb.append("\n");
            byte[] text = null;
            text = sb.toString().getBytes("UTF-8");
    //        text = sb.toString().getBytes("GBK");
    //        text = "test".getBytes("UTF-8");

            addPrintTextWithSize(1, concentration, text);

            print();
    //        printStart();


            ft.delete();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }
    }
    private void addPrintTextWithSize(int size, int concentration, byte[] data) {
        if (data == null) {
            return;
        }
        // 2 size Font
        byte[] _2x = new byte[] { 0x1b, 0x57, 0x02 };
        // 1 size Font
        byte[] _1x = new byte[] { 0x1b, 0x57, 0x01 };
        byte[] mData = null;
        if (size == 1) {
            mData = new byte[3 + data.length];
            System.arraycopy(_1x, 0, mData, 0, _1x.length);
            System.arraycopy(data, 0, mData, _1x.length, data.length);
            addText(concentration, mData);
        } else if (size == 2) {
            mData = new byte[3 + data.length];
            System.arraycopy(_2x, 0, mData, 0, _2x.length);
            System.arraycopy(data, 0, mData, _2x.length, data.length);
            addText(concentration, mData);
        }
    }
    private void showTip(String msg) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.tips))
                .setMessage(msg)
                .setNegativeButton(getString(R.string.close),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                // TODO Auto-generated method stub
                                dialog.dismiss();
                            }
                        }).show();
    }
    private String readFileSD() {
        String str = "";
        String str_buf = "";
        // проверяем доступность SD
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return "";
        }
        // получаем путь к SD
        //     File sdPath = Environment.getExternalStorageDirectory();
        //     File sdFile = new File(sdPath, FILENAME_SD);
        File sdFile = new File(PATH_SD, FILENAME_SD);

        if (sdFile.exists()&&!sdFile.isDirectory()){
            Log.d(LOG_TAG,"файл есть");
            try {
                // открываем поток для чтения
                BufferedReader br = new BufferedReader(new FileReader(sdFile));
                // читаем содержимое
                while ((str = br.readLine()) != null) {
                    str_buf+=str;
                    str_buf+="\n";
                }
                str_buf+="\n\n";
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            Log.d(LOG_TAG, "файла нет");
        }
        return str_buf;
    }
    public void addBmp(int concentration, int left, int width, int height, byte[] bmpData) {
        log("Print Queue addBmp");
        PrintDataService mData = new PrintDataService();
        mData.setConcentration(concentration);
        mData.setPrintType(PRINT_TYPE_BMP);
        mData.setLeft(left);
        mData.setWidth(width);
        mData.setHeight(height);
        mData.setData(bmpData);
        mSendList.add(mData);
    }
    public void init() {
        log("Print Queue start");
        mControlThread = new ControlThread();
        mControlThread.start();

        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(PosApi.ACTION_POS_COMM_STATUS);
        mContext.registerReceiver(mReceiver, mFilter);
    }
//    public void setOnPrintListener(PrintQueue.OnPrintListener listener) {
//        this.mListener = listener;
//    }
    public void print() {
        log("Print Queue do print...");
        if (mSendList == null) return;
        if (mSendList.size() == 0) {
//            if (mListener != null) {
//                mListener.onFinish();
//            }
            onFinish();
            return;
        }
        PrintDataService mData = mSendList.getFirst();
        int type = mData.getPrintType();
        switch (type) {
            case PRINT_TYPE_TEXT:
                mPosApi.printText(mData.getConcentration(), mData.getData(), mData.getData().length);
                break;
            case PRINT_TYPE_BMP:
                mPosApi.printImage(mData.getConcentration(), mData.getLeft(), mData.getWidth(),
                        mData.getHeight(), mData.getData());
                break;
            default:
                break;
        }
    }
    public void  printBMP(File fi){
        fi = new File(PATH_SD + FILENAME_IMAGE_SD);
    //    String dfdf = PATH_SD + FILENAME_IMAGE_SD;
   //     mBitmap = BitmapFactory.decodeFile(dfdf);
        String dfdf = fi.getAbsolutePath();
        mBitmap = BitmapFactory.decodeFile(dfdf);


        byte[] printData = BitmapTools.bitmap2PrinterBytes(mBitmap);
        addBmp(1, 0, mBitmap.getWidth(),
                mBitmap.getHeight(), printData);
//        int mWidth = 150;
//        int mHeight = 150;
//
//        mBitmap = BarcodeCreater.encode2dAsBitmap("1234567890", mWidth,
//                mHeight, 2);
//        printData = BitmapTools.bitmap2PrinterBytes(mBitmap);
//        addBmp(1, 100, mBitmap.getWidth(),
//                mBitmap.getHeight(), printData);
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
//        sb.append("\n");
//        sb.append("\n");
//        sb.append("\n");
//        sb.append("\n");
        sb.append("\n");
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        byte[] text = new byte[0];
        try {
            text = sb.toString().getBytes("GBK");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        addPrintTextWithSize(1, 1, text);
        print();
        fi.delete();
    }
    public void printNext() {
        log("Print Queue printNext");
        if (mSendList == null) return;
        if (!mSendList.isEmpty()) {
            mSendList.removeFirst();
        }
        mSendHandler.sendEmptyMessage(MSG_PRINT_NEXT);
    }
    public void printStop(int state) {
        log("Print Queue stop");
        if (mSendList == null) return;
        mSendHandler.removeMessages(MSG_PRINT_NEXT);
        mSendList.clear();
//        if (mListener != null) {
//            mListener.onFailed(state);
//        }
        onFailed(state);
    }
    public void addText(int concentration, byte[] textData) {
        log("Print Queue addText");
        PrintDataService mData = new PrintDataService();
        mData.setConcentration(concentration);
        mData.setPrintType(PRINT_TYPE_TEXT);
        mData.setData(textData);
        mSendList.add(mData);
    }
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            if (action.equalsIgnoreCase(PosApi.ACTION_POS_COMM_STATUS)) {
                int cmdFlag = intent.getIntExtra(PosApi.KEY_CMD_FLAG, -1);
                int status = intent.getIntExtra(PosApi.KEY_CMD_STATUS, -1);
                if (cmdFlag == PosApi.POS_PRINT_PICTURE || cmdFlag == PosApi.POS_PRINT_TEXT) {
                    switch (status) {
                        case PosApi.ERR_POS_PRINT_SUCC:
                            //Print Success
                            printNext();
                            break;
                        case PosApi.ERR_POS_PRINT_NO_PAPER:
                            //No paper
                            printStop(status);
                            break;
                        case PosApi.ERR_POS_PRINT_FAILED:
                            //Print Failed
                            printStop(status);
                            break;
                        case PosApi.ERR_POS_PRINT_VOLTAGE_LOW:
                            //Low Power
                            printStop(status);
                            break;
                        case PosApi.ERR_POS_PRINT_VOLTAGE_HIGH:
                            //Hight Power
                            printStop(status);
                            break;
                    }
                }
                if (cmdFlag == PosApi.POS_PRINT_GET_STATE) {
//                    if (mListener == null) {
//                        return;
//                    }
//                    mListener.onGetState(status);
                    onGetState(status);
                }
                if (cmdFlag == PosApi.POS_PRINT_SETTING) {
//                    if (mListener == null) {
//                        return;
//                    }
//                    mListener.onPrinterSetting(status);
                    onPrinterSetting(status);
                }
            }
        }
    };
    private void log(String msg) {
        Log.d(TAG, msg);
    }

    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
}
