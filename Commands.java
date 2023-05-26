package com.automationedge.ps.workflow.steps.begin;

public enum Commands {
    CHROME_LINUX("google-chrome --version"),
    CHROME_WINDOWS_32BIT("cmd.exe /C wmic datafile where name=\"%PROGRAMFILES(X86):\\=\\\\%\\\\Google\\\\Chrome\\\\Application\\\\chrome.exe\" get Version /value"),
    CHROME_WINDOWS_64BIT("cmd.exe /C wmic datafile where name=\"%PROGRAMFILES:\\=\\\\%\\\\Google\\\\Chrome\\\\Application\\\\chrome.exe\" get Version /value"),
    CHROME_MAC("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome --version"),
    CHROME_WINDOWS_APPDATA("cmd.exe /C wmic datafile where name=\"%LOCALAPPDATA:\\=\\\\%\\\\Google\\\\Chrome\\\\Application\\\\chrome.exe\" get Version /value"),
    CHROME_WINDOWS_REGISTRY("REG QUERY HKCU\\Software\\Google\\Chrome\\BLBeacon /v version"),
    FIREFOX_LINUX("firefox -v"),
    FIREFOX_WINDOWS_64BIT("cmd.exe /C wmic datafile where name=\"%PROGRAMFILES:\\=\\\\%\\\\Mozilla Firefox\\\\firefox.exe\" get Version /value"),
    FIREFOX_WINDOWS_32BIT("cmd.exe /C wmic datafile where name=\"%PROGRAMFILES(X86):\\=\\\\%\\\\Mozilla Firefox\\\\firefox.exe\" get Version /value"),
    FIREFOX_MAC("/Applications/Firefox.app/Contents/MacOS/firefox -v"),
    FIREFOX_WINDOWS_REGISTRY("REG QUERY \"HKLM\\Software\\Mozilla\\Mozilla Firefox\" /v \"\""),
    MSEDGE_LINUX("microsoft-edge --version"),
    MSEDGE_WINDOWS_32BIT("cmd.exe /C wmic datafile where name=\"%PROGRAMFILES(X86):\\=\\\\%\\\\Microsoft\\\\Edge\\\\Application\\\\msedge.exe\" get Version /value"),
    MSEDGE_WINDOWS_64BIT("cmd.exe /C wmic datafile where name=\"%PROGRAMFILES:\\=\\\\%\\\\Microsoft\\\\Edge\\\\Application\\\\msedge.exe\" get Version /value"),
    MSEDGE_MAC("/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge -version"),
    MSEDGE_WINDOWS_REGISTRY("REG QUERY HKCU\\Software\\Microsoft\\Edge\\BLBeacon /v version"),
    IE_WINDOWS_64BIT("cmd.exe /C wmic datafile where name=\"%PROGRAMFILES:\\=\\\\%\\\\Internet Explorer\\\\iexplore.exe\" get Version /value"),
    IE_WINDOWS_32BIT("cmd.exe /C wmic datafile where name=\"%PROGRAMFILES(X86):\\=\\\\%\\\\Internet Explorer\\\\iexplore.exe\" get Version /value"),
    IE_WINDOWS_REGISTRY("REG QUERY \"HKLM\\Software\\Microsoft\\Internet Explorer\" /v svcVersion");
    private final String command;

    public String getCommand() {
        return command;
    }

    Commands(String description) {
        this.command = description;

    }
}
