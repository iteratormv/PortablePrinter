package com.example.lanchang.portableprinter.Interfaces;

public interface onPrintListener {

    void onFailed(int state);

    void onFinish();

    /**
     * Get paper state
     *
     * @param state 0：Has paper   1：No paper
     */
    void onGetState(int state);

    /**
     * Set paper state
     *
     * @param state 0：Has paper   1：No paper
     */
    void onPrinterSetting(int state);
}
