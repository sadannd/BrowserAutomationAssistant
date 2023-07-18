import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;

public class BrowserUtil {
    private String destinationDriverFilePath;
    private String detectedWholeVersion = null;
    private static final Class<?> PKG = BeginStepMeta.class;
    private static final Logger logger = LoggerFactory.getLogger(BrowserUtil.class);
    private WebDriver driver = null;
    private HashMap<String, File> driverMap = new HashMap<>();
    private String tenantOrgCode;
	private int detectedVersion;
	private String versionInFile = null;
	private boolean isDriverClosed;
	private boolean versionDetectedUsingBase;
	private String versionInfoFilePath;

	public BrowserUtil() {
		destinationDriverFilePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		int lastIndexOfSlash;
		if (!StringUtil.isEmpty(destinationDriverFilePath)) {
			lastIndexOfSlash = destinationDriverFilePath.lastIndexOf('/');
			destinationDriverFilePath = destinationDriverFilePath.substring(0, lastIndexOfSlash);
		}
		versionInfoFilePath = destinationDriverFilePath + File.separator + "versions.info";
		File versionInfo = new File(versionInfoFilePath);
		if (!versionInfo.exists()) {
			initBrowserVersionProperties(versionInfo);
		}
    }

    public void copyDrivers() throws Exception {
        try {
            if(Const.isWindows()) {
				writeDriverFile(Browser.IE.name() + "Base.exe");
			}

        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }


    private void copyDrivers(Browser browser, int ver) throws ProcessStudioException {
        try {
        	if(Const.isWindows())
        		writeDriverFile(browser.name() + ver + ".exe");
        	else if(Const.isLinux() || Const.isOSX())
        		writeDriverFile(browser.name() + ver);
        } catch (Exception e) {
            if (detectedWholeVersion != null) {
                throw new ProcessStudioException(BaseMessages.getString(PKG, "WEBGUI.BROWSER.VERSION.ERROR",
                        browser.getDisplayName(), detectedWholeVersion));
            } else {
                throw new ProcessStudioException(BaseMessages.getString(PKG, "WEBGUI.BROWSER.VERSION.ERROR",
                        browser.getDisplayName(), ver));
            }
        }
    }

	private String getDriverZipDirectory(){
		String path = System.getProperty("user.dir");
		String pluginDirName = "GUI-Automation";
		if (StringUtils.isNotBlank(this.tenantOrgCode)) {
			// agent context
			path += File.separator + "storage" + File.separator + "files";
		} else {
			path += File.separator + "files";
		}
		path += File.separator + FileCategory.PLUGIN.name() + File.separator + pluginDirName;
		return path;
	}

	private File getLatestDriverZip(String driverZipName, long date) {
		File driverZip = new File(getDriverZipDirectory() + File.separator + driverZipName);
		if (driverZip.exists()) {
			if (date > 0 ){
				if (driverZip.lastModified() > date) {
					return driverZip;
				}else{
					return null;
				}
			} else {
				return driverZip;
			}
		} else {
			return null;
		}
	}

    private void writeDriverFile(String driverName) throws IOException {
    	File driverFile = null;
    	InputStream in;
    	if(Const.isWindows())
    	{
    		driverFile = new File(destinationDriverFilePath + File.separator + "webui_drivers" + File.separator + driverName);

    		if (!driverFile.exists() && driverName.contains(Browser.IE.name())) {
				in = BeginStep.class.getClassLoader().getResourceAsStream("webui_drivers/" + driverName);
				if(null!=in){
					FileUtils.copyInputStreamToFile(in, driverFile);
				}
			}
    	}
   		else if(Const.isLinux()) {
    		driverFile = new File(destinationDriverFilePath + File.separator + "webui_drivers" + File.separator + "linux" + File.separator + driverName);
    	}
		else if(Const.isOSX()) {
			driverFile = new File(destinationDriverFilePath + File.separator + "webui_drivers" + File.separator + "mac" + File.separator + driverName);
		}

		if (!driverName.contains(Browser.IE.name())) {
			String driverZipName;
			if(Const.isWindows()) {
				driverZipName = driverName.replace(".exe", ".zip");
			}else{
				driverZipName = driverName + ".zip";
			}

			File driverZip;
			if(null!= driverFile && driverFile.exists()){
				driverZip = getLatestDriverZip( driverZipName,  driverFile.lastModified());
				if(null!=driverZip){
					try {
						extractAndCopyLatestDriver(driverZip, driverName, driverFile);
					}catch (IOException ioException){
						logger.error("Error while extracting driver zip : {} ", ioException);
						//do nothing as driver is already present in webui_drivers directory.
					}
				}
			}else {
				driverZip = getLatestDriverZip(driverZipName,0);
				if(null!=driverZip){
					try {
						extractAndCopyLatestDriver(driverZip, driverName, driverFile);
					}catch(IOException ioException){
						logger.error("Error while extracting driver zip : {} ", ioException);
					}
				} else{
					throw new IOException(BaseMessages.getString(PKG, "WebGUI.Driver.File.NotPresent", driverName));
				}
			}
		}
        driverMap.put(driverName.replace(".exe", ""), driverFile);
        if(null!=driverFile && driverFile.exists()) {
        	driverFile.setExecutable(true, false);
        }
    }

	private void extractAndCopyLatestDriver(File driverZip, String driverName, File driverFile) throws IOException {
		File tempFile = new File(getDriverZipDirectory() + File.separator + "temp");
		ZipUtils.unzipFile(driverZip.getAbsolutePath(), tempFile.getAbsolutePath());
		if (driverFile.exists()) {
			driverFile.delete();
		}
		FileUtils.moveFile(new File(tempFile.getAbsolutePath() + File.separator + driverName), driverFile);
		FileUtils.deleteDirectory(tempFile);
	}


    private void auditLogBrowserChangeAndNotify(int storedBrowserVersion, String versionInFile, Browser browser,
                                                String wfInstanceID, boolean isNotif, String tenantCode) {
        try {
            if (null != wfInstanceID) {
                if (storedBrowserVersion != -1) {
                    AuditLog auditLog = new AuditLog(AuditLogOperation.UPDATE, AuditLogEntity.PS_PLUGIN,
                            AuditLogLevel.INFO);
                    ArrayList<AuditLog> auditLogList = new ArrayList<>();
                    auditLogList.add(auditLog);
                    auditLog.setDescription(BaseMessages.getString(PKG, "WEBGUI.VERSION.CHANGE",
                            browser.getDisplayName(), versionInFile, detectedWholeVersion));
                    KettleEnvironment.getWorkflowUtil().sendRequestToAEServer(WorkflowUtilOperation.DO_AUDIT_LOG,Long.parseLong(wfInstanceID), auditLogList);
                    if (isNotif) {
                        EmailContents contents = new EmailContents();
                        contents.setSubject(BaseMessages.getString(PKG, "WEBGUI.VERSION.CHANGE.SUBJECT"));
                        contents.setBody(BaseMessages.getString(PKG, "WEBGUI.VERSION.CHANGE", browser.getDisplayName(),
                                versionInFile, detectedWholeVersion));
                        contents.setRoles("ROLE_TENANT_ADMIN,ROLE_WORKFLOW_ADMIN");
                        contents.setAsync(true);
                        KettleEnvironment.getWorkflowUtil().sendRequestToAEServer(WorkflowUtilOperation.SEND_MAIL,Long.parseLong(wfInstanceID),tenantCode, contents, null);
                    }
                }
            }
        } catch (IOException | AEUtilsException ie) {
            logger.info(BaseMessages.getString(PKG, "WebGUI.Begin.AuditLogError"));
        }
    }

    @Deprecated
    public WebDriver start(String browserName, String wfInstanceID, boolean isNotif, String tenantCode, boolean maximize) throws Exception {
        driver = start(browserName, wfInstanceID, isNotif, tenantCode, maximize, null);
        return driver;
    }

    @Deprecated
    public WebDriver start(String browserName, String wfInstanceID, boolean isNotif, String tenantCode, boolean maximize, String defaultDownloadDirectory) throws Exception {
        driver = start(browserName, wfInstanceID, isNotif, tenantCode, maximize, defaultDownloadDirectory, false, (String) null);
        return driver;
    }

    public WebDriver start(String browserName, String wfInstanceID, boolean isNotif, String tenantCode, boolean maximize, String defaultDownloadDirectory, boolean isIgnoreIeProtectedModeSettings, String initialURL) throws Exception {
		return start(browserName, wfInstanceID, isNotif, tenantCode, maximize, defaultDownloadDirectory, isIgnoreIeProtectedModeSettings, initialURL, false);
	}

	public WebDriver start(String browserName, String wfInstanceID, boolean isNotif, String tenantCode, boolean maximize, String defaultDownloadDirectory, boolean isIgnoreIeProtectedModeSettings, String initialURL, boolean isSpy) throws Exception {
		Browser browser = null;
		this.tenantOrgCode = tenantCode;
		for (Browser b : Browser.values()) {
			if (browserName.equals(b.toString())) {
				browser = b;
				break;
			}
		}

		detectedVersion = 0;
		setBrowserVersion(browser,maximize,defaultDownloadDirectory, isIgnoreIeProtectedModeSettings,initialURL);
		readWriteBrowserVersions(browser,wfInstanceID,isNotif,tenantCode);

		if(null!=browser) {
			logger.info(BaseMessages.getString(PKG, "WEBGUI.BROWSER.InfoLevel.BrowserDetails",
					browser.getDisplayName(), detectedWholeVersion));
		}
		try {
			if (versionDetectedUsingBase) {
				if (isDriverClosed) {
					callStartBrowser(browser,detectedVersion,maximize,defaultDownloadDirectory,isIgnoreIeProtectedModeSettings,initialURL);
				}
			} else {
				callStartBrowser(browser,detectedVersion,maximize,defaultDownloadDirectory,isIgnoreIeProtectedModeSettings,initialURL);
			}
		} catch (SessionNotCreatedException se) {
			throw new ProcessStudioException(BaseMessages.getString(PKG, "WEBGUI.INCOMPATIBLE.DRIVER"));
		}

		return driver;
	}

    @Deprecated
    private void startBrowser(Browser browser, int ver, boolean maximize) throws IllegalStateException, WebDriverException, ProcessStudioException, Exception {
        this.startBrowser(browser, ver, maximize, null);
    }

    @Deprecated
    private void startBrowser(Browser browser, int ver, boolean maximize, String defaultDownloadDirectory) throws IllegalStateException, WebDriverException, ProcessStudioException, Exception {
        this.startBrowser(browser, ver, maximize, defaultDownloadDirectory, false, null);
    }

    private void startBrowser(Browser browser, int ver, boolean maximize, String defaultDownloadDirectory, boolean isIgnoreIeProtectedModeSettings, String initialURL) throws Exception {
		switch (browser) {
			case CHROME:
				if (ver == -1) {
					System.setProperty("webdriver.chrome.driver",
							driverMap.get(Browser.CHROME.name() + "Base").getAbsolutePath());
				} else {
					copyDrivers(Browser.CHROME, ver);
					System.setProperty("webdriver.chrome.driver",
							driverMap.get(Browser.CHROME.name() + ver).getAbsolutePath());
				}
				ChromeOptions chromeOptions = new ChromeOptions();
				chromeOptions.addArguments("--ignore-certificate-errors");
				if (ver >= 70) {
					chromeOptions.setExperimentalOption("useAutomationExtension", Boolean.FALSE);
				}
				if (ver < 70) {
					chromeOptions.addArguments("disable-infobars");
				} else {
					chromeOptions.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
					chromeOptions.setExperimentalOption("useAutomationExtension", false);
				}
				if (maximize) {
					chromeOptions.addArguments("--start-maximized");
				}
				if (StringUtils.isNotBlank(defaultDownloadDirectory)) {
					HashMap<String, Object> chromePrefs = new HashMap<>();
					chromePrefs.put("profile.default_content_settings.popups", 0);
					chromePrefs.put("download.default_directory", defaultDownloadDirectory);
					chromeOptions.setExperimentalOption("prefs", chromePrefs);
				}
				DesiredCapabilities cap = DesiredCapabilities.chrome();
				cap.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
				cap.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
				chromeOptions.merge(cap);
				driver = new ChromeDriver(chromeOptions);
				break;
			case IE:
				System.setProperty("webdriver.ie.driver", driverMap.get(Browser.IE.name() + "Base").getAbsolutePath());
				if (isIgnoreIeProtectedModeSettings) {
					DesiredCapabilities capabilities = DesiredCapabilities.internetExplorer();
					capabilities.setCapability(CapabilityType.BROWSER_NAME, "IE");
					capabilities.setCapability(InternetExplorerDriver.IGNORE_ZOOM_SETTING, true);
					capabilities.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
					if (StringUtils.isNotBlank(initialURL)) {
						capabilities.setCapability(InternetExplorerDriver.INITIAL_BROWSER_URL, initialURL);
					}
					InternetExplorerOptions ieOptions=new InternetExplorerOptions();
					ieOptions.merge(capabilities);
					driver = new InternetExplorerDriver(ieOptions);
				} else {
					driver = new InternetExplorerDriver();
				}

				if (maximize) {
					driver.manage().window().maximize();
				}
				break;
			case FIREFOX:
			default:
				if (ver == -1) {
					System.setProperty("webdriver.gecko.driver", driverMap.get(Browser.FIREFOX.name() + "Base").getAbsolutePath());
				} else {
					copyDrivers(Browser.FIREFOX, ver);
					System.setProperty("webdriver.gecko.driver", driverMap.get(Browser.FIREFOX.name() + ver).getAbsolutePath());
				}

				if (StringUtils.isNotBlank(defaultDownloadDirectory)) {
					FirefoxProfile profile = new FirefoxProfile();
					FirefoxOptions firefoxOptions = new FirefoxOptions();
					profile.setPreference("browser.download.folderList", 2);
					profile.setPreference("browser.download.manager.showWhenStarting", false);
					profile.setPreference("browser.download.dir", defaultDownloadDirectory);
					profile.setPreference("browser.helperApps.neverAsk.saveToDisk",
							"application/octet-stream,text/csv,application/x-msexcel,application/excel,application/x-excel,application/vnd.ms-excel,image/png,image/jpeg,text/html,text/plain,application/msword,application/xml");
					profile.setPreference("browser.helperApps.alwaysAsk.force", false);
					profile.setPreference("browser.download.manager.alertOnEXEOpen", false);
					profile.setPreference("browser.download.manager.focusWhenStarting", false);
					profile.setPreference("browser.download.manager.useWindow", false);
					profile.setPreference("browser.download.manager.showAlertOnComplete", false);
					profile.setPreference("browser.download.manager.closeWhenDone", false);
					firefoxOptions.setProfile(profile);
					driver = new FirefoxDriver(firefoxOptions);
				} else {
					driver = new FirefoxDriver();
				}
				if (maximize) {
					driver.manage().window().maximize();
				}
				break;
			case MSEDGE:
				copyDrivers(Browser.MSEDGE, ver);
				System.setProperty("webdriver.edge.driver", driverMap.get(Browser.MSEDGE.name() + ver).getAbsolutePath());
				driver=new EdgeDriver();
				if (maximize) {
					driver.manage().window().maximize();
				}
				break;
		}
	}
	public String getBrowserVersion(String[] commandArray,Browser browser,String dir, boolean isRegistry) throws ProcessStudioException {
		BufferedReader br = null;
		Process process = null;
		try {
			String line;
			String browserVersion = "";
			process = Runtime.getRuntime().exec(commandArray);
			process.waitFor();
			br = new BufferedReader(
					new InputStreamReader(process.getInputStream()));
			while ((line = br.readLine()) != null) {
				if (StringUtils.isNotBlank(line)) {
					browserVersion += line;
					if (!isRegistry) {
						break;
					}
				}
			}
			if (isRegistry) {
				browserVersion = browserVersion.substring(browserVersion.lastIndexOf(" ") + 1);
			}
			if (StringUtils.isBlank(browserVersion)) {
				/*if ErrorStream returns 'No Instance(s) Available' in case of windows or
				'microsoft-edge' or 'firefox' or 'google-chrome' command not found' in case of linux
				'no such file or directory' in case of Mac
				we can conclude that browser is
				not installed properly at default location  in case of windows
				and not installed properly in case of linux and Mac*/
				String browserDetectionError = Const.EMPTY_STRING;
				br = new BufferedReader(
						new InputStreamReader(process.getErrorStream()));
				while ((line = br.readLine()) != null) {
					browserDetectionError += line;
				}
				logger.error(BaseMessages.getString(PKG, "WEBGUI.BROWSER.Error.Detection",browser.getDisplayName(), dir, browserDetectionError));
			}
			return browserVersion;
		} catch (Exception e) {
			logger.error(BaseMessages.getString(PKG, "WEBGUI.BROWSER.Error.Detection",browser.getDisplayName(), dir, e.getMessage()));
			return null;
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException ioException) {
				}
			}
			if (process != null) {
				process.destroy();
			}
		}
	}

	public Integer splitBrowserVersion(String browserVersionInfo, String splitBy, int pos ) {
		String arr[] = browserVersionInfo.split(splitBy);
		detectedWholeVersion = arr[pos];
		return Integer.parseInt(arr[pos].split("\\.")[0]);
	}

	public void setBrowserVersion(Browser browser,boolean maximize, String defaultDownloadDirectory, boolean isIgnoreIeProtectedModeSettings,String initialURL) throws ProcessStudioException {
		try {
			boolean isRegistry = false;
			String browserVersion;
			if (Const.isWindows()) {
				//query returns 'Version=91.0.864.87' ---version might differ
				browserVersion = getBrowserVersion(Commands.valueOf(browser.name() + "_WINDOWS_32BIT").getCommand().split(" "), browser, "PROGRAMFILES(X86)",false);
				//Checking browser version in second directory if not installed in first.
				if (StringUtils.isBlank(browserVersion)) {
					browserVersion = getBrowserVersion(Commands.valueOf(browser.name() + "_WINDOWS_64BIT").getCommand().split(" "), browser,"PROGRAMFILES",false);
				}
				if (StringUtils.isBlank(browserVersion) && browser==Browser.CHROME) {
					browserVersion = getBrowserVersion(Commands.valueOf(browser.name() + "_WINDOWS_APPDATA").getCommand().split(" "), browser,"APPDATA",false);
				}
				if (StringUtils.isBlank(browserVersion)) {
					//Checking Registry
					//query returns HKEY_LOCAL_MACHINE\Software\Mozilla\Mozilla Firefox    (Default)    REG_SZ    97.0.1
					browserVersion = getBrowserVersion(Commands.valueOf(browser.name() + "_WINDOWS_REGISTRY").getCommand().split(" "), browser, "REGISTRY", true);
					if (StringUtils.isNotBlank(browserVersion)) {
						isRegistry = true;
					}
				}
				if(StringUtils.isBlank(browserVersion)){
					versionDetectedUsingBase=true;
					browserVersion = detectBrowserVersionUsingStoredVersion(browser, browser.name() + "Base.exe", maximize, defaultDownloadDirectory,isIgnoreIeProtectedModeSettings, initialURL);
				}
				if (StringUtils.isBlank(browserVersion)) {
					throw new ProcessStudioException(BaseMessages.getString(PKG, "WEBGUI.Browser.NotFound", browser.getDisplayName()));
				}
				if (isRegistry) {
					detectedWholeVersion=browserVersion;
					detectedVersion = Integer.parseInt(browserVersion.split("\\.")[0]);
				} else if (!versionDetectedUsingBase) {
					detectedVersion = splitBrowserVersion(browserVersion, "=", 1);
				} else {
					detectedVersion = Integer.parseInt(browserVersion);
				}
			} else if (Const.isOSX()) {
				//query returns 'Microsoft Edge 93.0.933.1' ---version might differ
				//query returns 'Google Chrome 92.0.4515.131' ---version might differ
				//query returns 'Mozilla Firefox 92.0.4515.131' ---version might differ
				String command = Commands.valueOf(browser.name() + "_MAC").getCommand();
				int lastSpaceIndex = command.lastIndexOf(" ");
				String[] commandArray = new String[]{command.substring(0, lastSpaceIndex),
						command.substring(lastSpaceIndex + 1)};
				browserVersion = getBrowserVersion(commandArray, browser,"Applications",false);
				if (StringUtils.isBlank(browserVersion)) {
					throw new ProcessStudioException(BaseMessages.getString(PKG, "WEBGUI.Browser.NotFound", browser.getDisplayName()));
				}
			} else {
				//query returns 'Microsoft Edge 93.0.933.1' ---version might differ
				//query returns 'Google Chrome 92.0.4515.131' ---version might differ
				//query returns 'Mozilla Firefox 92.0.4515.131' ---version might differ
				browserVersion = getBrowserVersion(Commands.valueOf(browser.name() + "_LINUX").getCommand().split(" "), browser,"usr/bin",false);
				if(StringUtils.isBlank(browserVersion)){
					versionDetectedUsingBase=true;
					browserVersion = detectBrowserVersionUsingStoredVersion(browser, browser.name() + "Base", maximize, defaultDownloadDirectory,isIgnoreIeProtectedModeSettings, initialURL);
				}
				if (StringUtils.isBlank(browserVersion)) {
					throw new ProcessStudioException(BaseMessages.getString(PKG, "WEBGUI.Browser.NotFound", browser.getDisplayName()));
				}
				if (!versionDetectedUsingBase) {
					detectedVersion = splitBrowserVersion(browserVersion, " ", 2);
				} else {
					detectedVersion = Integer.parseInt(browserVersion);
				}
			}
		} catch (Exception e) {
			logger.error(BaseMessages.getString(PKG, "WEBGUI.Error.UnexpectedError.BrowserVersionDetection", e.getMessage()));
			throw new ProcessStudioException(BaseMessages.getString(PKG, "WEBGUI.Browser.NotFound", browser.getDisplayName()));
		}
	}

	private void readWriteBrowserVersions(Browser browser, String wfInstanceID, boolean isNotif, String tenantCode) throws IOException {

		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(versionInfoFilePath);
			Properties prop = new Properties();
			prop.load(fis);
			versionInFile = prop.getProperty(browser.name());
			if (Integer.parseInt(versionInFile.split("\\.")[0]) != detectedVersion) {
				fos = new FileOutputStream(versionInfoFilePath);
				//edit browsers version
				prop.put(browser.name(), String.valueOf(detectedWholeVersion));
				prop.store(fos, null);
				try {
					auditLogBrowserChangeAndNotify(Integer.parseInt(versionInFile.split("\\.")[0]), versionInFile, browser, wfInstanceID, isNotif,
							tenantCode);
				} catch (Exception e) {
					logger.warn(BaseMessages.getString(PKG, "WEBGUI.Error.NotificationFailed", e.getMessage()));
				}
			}
		} catch (Exception e) {
			logger.warn(BaseMessages.getString(PKG, "WEBGUI.Error.ReadOrUpdateFile", e.getMessage()));
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					// Ignore close exception
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					// Ignore close exception
				}
			}
		}

	}
	private void initBrowserVersionProperties(File versionInfo) {

		//create versions.info file and initialize each browser version to -1

		Properties props = new Properties();
		props.put(Browser.IE.name(), "-1");
		props.put(Browser.CHROME.name(), "-1");
		props.put(Browser.FIREFOX.name(), "-1");
		props.put(Browser.MSEDGE.name(), "-1");

		//if any of the previous browser version file is found fetch the version and overwrite the existing initialized props(-1)
		// and store in new versions.info file and delete the older file

		fetchBrowserOlderVersion(new File(destinationDriverFilePath + File.separator + "chrome.info"),Browser.CHROME.name(),props);
		fetchBrowserOlderVersion(new File(destinationDriverFilePath + File.separator + "firefox.info"),Browser.FIREFOX.name(),props);
		fetchBrowserOlderVersion(new File(destinationDriverFilePath + File.separator + "ie.info"),Browser.IE.name(),props);

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(versionInfo);
			props.store(fos, null);
		} catch (Exception e) {
			logger.error(BaseMessages.getString(PKG, "WEBGUI.Error.FileCreation", e.getMessage()));
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					// Ignore close exception
				}
			}
		}
	}

	public void fetchBrowserOlderVersion(File file, String propKey, Properties props) {
		try {
			if (file.exists()) {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				versionInFile = reader.readLine();
				if (StringUtils.isNotBlank(versionInFile)) {
					props.put(propKey, versionInFile);
				}
				reader.close();
				file.delete();
			}
		} catch (IOException e) {
			logger.error(BaseMessages.getString(PKG, "WEBGUI.Error.FetchingBrowserOlderVersion", e.getMessage()));
		}
	}

	private String detectBrowserVersionUsingStoredVersion(Browser browser, String driverName, boolean maximize, String defaultDownloadDirectory, boolean isIgnoreIeProtectedModeSettings, String initialURL) {
		String version;
		FileInputStream fis = null;
		try {
			if (browser.equals(Browser.MSEDGE)) {
				return null;
			}
			int storedBrowserVersion;
			logger.info(BaseMessages.getString(PKG, "WEBGUI.info.FallbackInitiated"));
			fis = new FileInputStream(versionInfoFilePath);
			Properties prop = new Properties();
			prop.load(fis);
			versionInFile = prop.getProperty(browser.name());
			storedBrowserVersion = Integer.parseInt(versionInFile.split("\\.")[0]);

			if (versionInFile.equals("-1")) {
				logger.info(BaseMessages.getString(PKG, "WEBGUI.info.VersionDetection.UsingBaseDriver"));
				copyBaseDriver(driverName);
				callStartBrowser(browser,-1,maximize,defaultDownloadDirectory,isIgnoreIeProtectedModeSettings,initialURL);
			} else {
				try {
					logger.info(BaseMessages.getString(PKG, "WEBGUI.Info.VersionDetection.UsingStoredBrowserVersion"));
					callStartBrowser(browser,storedBrowserVersion,maximize,defaultDownloadDirectory,isIgnoreIeProtectedModeSettings,initialURL);
				} catch (Exception se) {
					try {
						logger.info(BaseMessages.getString(PKG, "WEBGUI.info.VersionDetection.UsingBaseDriver"));
						copyBaseDriver(driverName);
						callStartBrowser(browser,-1,maximize,defaultDownloadDirectory,isIgnoreIeProtectedModeSettings,initialURL);
					} catch (Exception e) {
						throw new ProcessStudioException(BaseMessages.getString(PKG, "WEBGUI.INCOMPATIBLE.DRIVER"));
					}
				}
			}
			Capabilities cap = ((RemoteWebDriver) driver).getCapabilities();
			version =  cap.getVersion();
			detectedWholeVersion = version;
			if((storedBrowserVersion!=Integer.parseInt(version.split("\\.")[0]))) {
				driver.quit();
				isDriverClosed = true;
			}
			return version.split("\\.")[0];
		} catch (Exception e) {
			logger.error(BaseMessages.getString(PKG, "WEBGUI.Error.UnexpectedError.BrowserVersionDetection", e));
			return null;
		}
		finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					// Ignore close exception
				}
			}
		}
	}

	private void copyBaseDriver(String driverName) throws IOException {
		File driverFile = null;
		InputStream in = null;
		if (Const.isWindows()) {
			driverFile = new File(destinationDriverFilePath + File.separator + "webui_drivers" + File.separator + driverName);
		} else if (Const.isLinux()) {
			driverFile = new File(destinationDriverFilePath + File.separator + "webui_drivers" + File.separator + "linux" + File.separator + driverName);
		} else if (Const.isOSX()) {
			driverFile = new File(destinationDriverFilePath + File.separator + "webui_drivers" + File.separator + "mac" + File.separator + driverName);
		}
		if (null != driverFile && !driverFile.exists()) {
			if(Const.isWindows()) {
				in = BeginStep.class.getClassLoader().getResourceAsStream("webui_drivers/" + driverName);
			}else if(Const.isLinux()){
				in = BeginStep.class.getClassLoader().getResourceAsStream("webui_drivers/linux/" + driverName);
			}
			if (null != in) {
				FileUtils.copyInputStreamToFile(in, driverFile);
			}
		}
		driverMap.put(driverName.replace(".exe", ""), driverFile);
		if(driverFile.exists()) {
			driverFile.setExecutable(true, false);
		}
	}
	private void callStartBrowser(Browser browser, int ver, boolean maximize, String defaultDownloadDirectory,
								  boolean isIgnoreIeProtectedModeSettings, String initialURL) throws Exception {
		if (isIgnoreIeProtectedModeSettings) {
			startBrowser(browser, ver, maximize, defaultDownloadDirectory, true, initialURL);
		} else if (StringUtils.isNotBlank(defaultDownloadDirectory)) {
			startBrowser(browser, ver, maximize, defaultDownloadDirectory);
		} else {
			startBrowser(browser, ver, maximize);
		}
	}
}
