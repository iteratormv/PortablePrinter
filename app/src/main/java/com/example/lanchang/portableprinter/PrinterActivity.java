package com.example.lanchang.portableprinter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.posapi.PosApi;
import android.posapi.PosApi.OnCommEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.example.lanchang.portableprinter.PL50_PrintServise;
import com.example.lanchang.portableprinter.PrintBarcodeActivity;
import com.example.lanchang.portableprinter.PrintQueue;
import com.example.lanchang.portableprinter.R;
import com.example.lanchang.portableprinter.util.PowerUtil;

import java.io.UnsupportedEncodingException;

public class PrinterActivity extends Activity {

  private PosApi mApi = null;
  private Button mBtnPsam = null;
  private Button mBtnPrinter = null;

  private PrintQueue mPrintQueue = null;
  private PosApi mPosApi;

  private boolean isServiceStart = false;



  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.pt_activity_main);
    initViews();

    //Power on
    PowerUtil.power("1");

    //init
    mApi = PosApi.getInstance(this);
    mApi.setOnComEventListener(mCommEventListener);
    //get interface
    mApi.initDeviceEx("/dev/ttyMT2");

    mPosApi = PosApi.getInstance(this);
    mPrintQueue = new PrintQueue(this, mPosApi);
    mPrintQueue.init();
    mPrintQueue.setOnPrintListener(new PrintQueue.OnPrintListener() {

      @Override
      public void onGetState(int state) {
        switch (state) {
          case 0:
            Toast.makeText(PrinterActivity.this, "Has paper!",
                    Toast.LENGTH_SHORT).show();

            break;
          case 1:

            Toast.makeText(PrinterActivity.this, "No paper!",
                    Toast.LENGTH_SHORT).show();

            break;
        }
      }

      @Override
      public void onPrinterSetting(int state) {
        switch (state) {
          case 0:
            Toast.makeText(PrinterActivity.this, "Has paper",
                    Toast.LENGTH_SHORT).show();
            break;
          case 1:
            Toast.makeText(PrinterActivity.this, "No paper",
                    Toast.LENGTH_SHORT).show();
            break;
          case 2:
            Toast.makeText(PrinterActivity.this,
                    "Detected black mark", Toast.LENGTH_SHORT).show();
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
    });
  }

  private void initViews() {
    mBtnPrinter = (Button) this.findViewById(R.id.btn_printer);
    mBtnPrinter.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        // TODO Auto-generated method stub
        startActivity(new Intent(PrinterActivity.this, PrintBarcodeActivity.class));
      }
    });
  }

  OnCommEventListener mCommEventListener = new OnCommEventListener() {

    @Override
    public void onCommState(int cmdFlag, int state, byte[] resp, int respLen) {
      // TODO Auto-generated method stub
      switch (cmdFlag) {
        case PosApi.POS_INIT:
          if (state == PosApi.COMM_STATUS_SUCCESS) {
            Toast.makeText(getApplicationContext(), "Initialization success", Toast.LENGTH_SHORT)
                .show();
          } else {
            Toast.makeText(getApplicationContext(), "Failed to initialize", Toast.LENGTH_SHORT)
                .show();
          }
          break;
      }
    }
  };

  public void onClickStart(View view){
    isServiceStart = true;
    startService(new Intent(PrinterActivity.this, PL50_PrintServise.class));
  }
  public void onClickStop(View view){
      if (mApi != null) {
      mApi.closeDev();
    }
    PowerUtil.power("0");
    isServiceStart = false;
    stopService(new Intent(PrinterActivity.this, PL50_PrintServise.class));
  }
  public void onClickTestPrint(View view){
    printTextWithInput();
  }
  public void onClickCreateFile(View view){
    Toast.makeText(getApplicationContext(), "create file", Toast.LENGTH_SHORT)
            .show();
  }
  public void onClickReadFile(View view){
    Toast.makeText(getApplicationContext(), "read file", Toast.LENGTH_SHORT)
            .show();
  }

  @Override
  protected void onDestroy() {
    // TODO Auto-generated method stub
    super.onDestroy();
//    if(isServiceStart = false){
//      if (mApi != null) {
//        mApi.closeDev();
//      }
//      PowerUtil.power("0");
//    }
  }
  private void printTextWithInput() {
    try {
      int concentration = Integer.valueOf(("60").trim());
//            if (TextUtils.isEmpty(etContent.getText().toString())) {
//                return;


      StringBuilder sb = new StringBuilder();
      sb.append("test english text");
      sb.append("\n");
      byte[] text = null;
      text = sb.toString().getBytes("GBK");

      addPrintTextWithSize(1, concentration, text);

      mPrintQueue.printStart();
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
      mPrintQueue.addText(concentration, mData);
    } else if (size == 2) {
      mData = new byte[3 + data.length];
      System.arraycopy(_2x, 0, mData, 0, _2x.length);
      System.arraycopy(data, 0, mData, _2x.length, data.length);
      mPrintQueue.addText(concentration, mData);
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
}
