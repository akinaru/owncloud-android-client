package com.owncloud.android.test.ui.actions;

import java.util.HashMap;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.remote.RemoteWebElement;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.owncloud.android.test.ui.models.CertificatePopUp;
import com.owncloud.android.test.ui.models.ElementMenuOptions;
import com.owncloud.android.test.ui.models.UploadFilesView;
import com.owncloud.android.test.ui.models.LoginForm;
import com.owncloud.android.test.ui.models.FileListView;
import com.owncloud.android.test.ui.models.MenuList;
import com.owncloud.android.test.ui.models.NewFolderPopUp;
import com.owncloud.android.test.ui.models.RemoveConfirmationView;
import com.owncloud.android.test.ui.models.SettingsView;
import com.owncloud.android.test.ui.models.WaitAMomentPopUp;
import com.owncloud.android.test.ui.testSuites.Common;

public class Actions {

	public static FileListView login(String url, String user, String password, Boolean isTrusted, AndroidDriver driver) throws InterruptedException {
		LoginForm loginForm = new LoginForm(driver);
		CertificatePopUp certificatePopUp = loginForm.typeHostUrl(url);	
		if(!isTrusted){
			WebDriverWait wait = new WebDriverWait(driver, 30);
			//sometimes the certificate has been already accept and it doesn't appear again
			try {
				wait.until(ExpectedConditions.visibilityOf(certificatePopUp.getOkButtonElement()));
				//we need to repaint the screen because of some element are misplaced
				driver.rotate(ScreenOrientation.LANDSCAPE);
				driver.rotate(ScreenOrientation.PORTRAIT);
				certificatePopUp.clickOnOkButton();
			}catch (NoSuchElementException e) {

			}

		}
		loginForm.typeUserName(user);
		loginForm.typePassword(password);
		//TODO. Assert related to check the connection?
		return loginForm.clickOnConnectButton();
	}

	public static WaitAMomentPopUp createFolder(String folderName, FileListView fileListView){
		NewFolderPopUp newFolderPopUp = fileListView.clickOnNewFolderButton();
		newFolderPopUp.typeNewFolderName(folderName);
		WaitAMomentPopUp waitAMomentPopUp = newFolderPopUp.clickOnNewFolderOkButton();
		//TODO. assert here
		return waitAMomentPopUp;
	}


	public static AndroidElement scrollTillFindElement (String elementName, AndroidElement element, AndroidDriver driver) {
		AndroidElement fileElement;

		if(element.getAttribute("scrollable").equals("true")){
			HashMap<String, String> scrollObject = new HashMap<String, String>();
			scrollObject.put("text", elementName);
			scrollObject.put("element", ( (RemoteWebElement) element).getId());
			driver.executeScript("mobile: scrollTo", scrollObject);
		}
		try {
			fileElement = (AndroidElement) driver.findElementByName(elementName);
		} catch (NoSuchElementException e) {
			fileElement = null;
		}
		return fileElement;
	}


	public static void deleteAccount (FileListView fileListView) {	
		MenuList menulist = fileListView.clickOnMenuButton();
		SettingsView settingView = menulist.clickOnSettingsButton();
		deleteAccount(settingView);
	}

	public static void deleteAccount (SettingsView settingsView) {
		settingsView.tapOnAccountElement(1, 1000);
		settingsView.clickOnDeleteAccountElement();
	}

	public static void clickOnMainLayout(AndroidDriver driver){
		driver.tap(1, 0, 0, 1);
	}

	//TODO. convert deleteFodler and deleteFile in deleteElement
	public static AndroidElement deleteElement(String elementName,  FileListView fileListView, AndroidDriver driver) throws Exception{
		AndroidElement fileElement;
		WaitAMomentPopUp waitAMomentPopUp;
		try{
			//To open directly the "file list view" and we don't need to know in which view we are
			driver.startActivity("com.owncloud.android", ".ui.activity.FileDisplayActivity");
			fileElement = (AndroidElement) driver.findElementByName(elementName);
			ElementMenuOptions menuOptions = fileListView.longPressOnElement(elementName);
			RemoveConfirmationView removeConfirmationView = menuOptions.clickOnRemove();;
			waitAMomentPopUp = removeConfirmationView.clickOnRemoteAndLocalButton();
			Common.waitTillElementIsNotPresent(waitAMomentPopUp.getWaitAMomentTextElement(), 100);
		}catch(NoSuchElementException e){
			fileElement=null;
		}
		return fileElement;
	}

	public static FileListView uploadFile(String elementName,  FileListView fileListView) throws InterruptedException{
		fileListView.clickOnUploadButton();
		UploadFilesView uploadFilesView = fileListView.clickOnFilesElementUploadFile();
		uploadFilesView.clickOnFileName(elementName);
		FileListView fileListViewAfterUploadFile = uploadFilesView.clickOnUploadButton();
		//TO DO. detect when the file is successfully uploaded
		Thread.sleep(15000);
		return fileListViewAfterUploadFile; 
	}


}
