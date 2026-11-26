package com.custommenu.core;
public enum ScreenType {
    INTRO("intro"), MAINMENU("mainmenu"), PAUSE("pause"), GAMEOVER("gameover");
    public final String folder;
    ScreenType(String f) { folder = f; }
}
