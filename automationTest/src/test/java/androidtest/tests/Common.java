package androidtest.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;

public class Common{
	AndroidDriver driver;
	static int waitingTime = 30;

	WebDriverWait wait;

	protected AndroidDriver setUpCommonDriver () throws Exception {
		File rootPath = new File(System.getProperty("user.dir"));
		File appDir = new File(rootPath,"src/test/resources");
		File app = new File(appDir,"ownCloud.apk");
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability("platformName", "Android");
		capabilities.setCapability("deviceName", "test");
		capabilities.setCapability("app", app.getAbsolutePath());
		capabilities.setCapability("appPackage", "com.owncloud.android");
		capabilities.setCapability("appActivity", ".ui.activity.FileDisplayActivity");	
		capabilities.setCapability("appWaitActivity", ".authentication.AuthenticatorActivity");
		driver = new AndroidDriver(new URL("http://127.0.0.1:4723/wd/hub"), capabilities);
		driver.manage().timeouts().implicitlyWait(waitingTime, TimeUnit.SECONDS);
		wait = new WebDriverWait(driver, waitingTime, 50);
		return driver;

	}

	protected boolean waitForTextPresent(String text, AndroidElement element) throws InterruptedException{
		for (int second = 0;;second++){	
			if (second >= waitingTime)
				return false;
			try{
				if (text.equals(element.getText()))
					break;
			} catch (Exception e){

			}
			Thread.sleep(1000);
		}
		return true;
	}

	protected boolean isElementPresent(AndroidElement element, By by) {
		try {
			element.findElement(by);
			return true;
		} catch (NoSuchElementException e) {
			return false;
		}
	}

	protected boolean isElementPresent(AndroidElement element) {
		try{
			element.isDisplayed();
		} catch (NoSuchElementException e){
			return false;
		}
		return true;
	}

	//pollingTime in milliseconds
	public static void waitTillElementIsNotPresent (AndroidElement element, int pollingTime) throws Exception {
		for (int time = 0;;time += pollingTime){	
			if (time >= waitingTime * 1000) //convert to milliseconds
				break;
			try{
				element.isDisplayed();
			} catch (NoSuchElementException e){
				return;
			}
			Thread.sleep(pollingTime);
		}
		throw new TimeoutException();
	}

	protected void takeScreenShotOnFailed (String testName) throws IOException {
		File file  = ((RemoteWebDriver) driver).getScreenshotAs(OutputType.FILE);
		SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd");
		Date today = Calendar.getInstance().getTime(); 
		String screenShotName = "ScreenShots/" + dt1.format(today) + "/" + testName + ".png";
		FileUtils.copyFile(file, new File(screenShotName));
	}

	protected void assertIsInMainView() throws InterruptedException {
		assertTrue(waitForTextPresent("ownCloud", (AndroidElement) driver.findElementByAndroidUIAutomator("new UiSelector().resourceId(\"android:id/action_bar_title\")")));
		assertTrue(isElementPresent((AndroidElement) driver.findElementByAndroidUIAutomator("new UiSelector().description(\"Upload\")")));	
	}

	protected void assertIsNotInMainView() throws InterruptedException {
		AndroidElement fileElement;
		assertTrue(waitForTextPresent("ownCloud", (AndroidElement) driver.findElementByAndroidUIAutomator("new UiSelector().resourceId(\"android:id/action_bar_title\")")));
		try {
			fileElement = (AndroidElement) driver.findElementByAndroidUIAutomator("new UiSelector().description(\"Upload\")");
		} catch (NoSuchElementException e) {
			fileElement = null;
		}
		assertNull(fileElement);
	}
	
	protected void assertIsPasscodeRequestView() throws InterruptedException {
		assertTrue(waitForTextPresent("ownCloud", (AndroidElement) driver.findElementByAndroidUIAutomator("new UiSelector().resourceId(\"android:id/action_bar_title\")")));
		assertTrue(((AndroidElement) driver.findElementByAndroidUIAutomator("new UiSelector().text(\"Please, insert your pass code\")")).isDisplayed());

	}

	protected void assertIsInSettingsView() throws InterruptedException {
		assertTrue(waitForTextPresent("Settings", (AndroidElement) driver.findElementByAndroidUIAutomator("new UiSelector().resourceId(\"android:id/action_bar_title\")")));
	}

}
