package androidtest.models;

import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.PageFactory;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

public class LoginForm {
	final AndroidDriver driver;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"Server address\")")
	private AndroidElement hostUrlInput;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"Username\")")
	private AndroidElement userNameInput;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"Password\")")
	private AndroidElement passwordInput;
	
	@CacheLookup
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"Connect\")")
	private AndroidElement connectButton;
	
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"Testing connection\")")
	private AndroidElement serverStatusText;
	
	@AndroidFindBy(uiAutomator = "new UiSelector().description(\"Wrong username or password\")")
	private AndroidElement authStatusText;
	
	public LoginForm (AndroidDriver driver) {
		this.driver = driver;
		PageFactory.initElements(new AppiumFieldDecorator(driver), this);
	}

	public CertificatePopUp typeHostUrl (String hostUrl) {
		hostUrlInput.clear();
		hostUrlInput.sendKeys(hostUrl + "\n");
		CertificatePopUp certificatePopUp = new CertificatePopUp(driver);
		return certificatePopUp;
	}
	
	public void clickOnUserName () {
		userNameInput.click();
	}
	
	public void typeUserName (String userName) {
		userNameInput.clear();
		//using the \n , it not need to hide the keyboard which sometimes gives problems
		userNameInput.sendKeys(userName + "\n");
		//driver.hideKeyboard();
	}
	
	public void typePassword (String password) {
		passwordInput.clear();
		passwordInput.sendKeys(password + "\n");
		//driver.hideKeyboard();
	}
	
	public MainView clickOnConnectButton () {
		connectButton.click();
		MainView mainView = new MainView(driver);
		return mainView;
	}
	
	public AndroidElement gethostUrlInput () {
		return hostUrlInput;
	}
	
	public AndroidElement getUserNameInput () {
		return userNameInput;
	}
	
	public AndroidElement getPasswordInput () {
		return passwordInput;
	}
	
	
	public AndroidElement getServerStatusTextElement () {
		return serverStatusText;
	}
	
	public AndroidElement getAuthStatusText () {
		return authStatusText;
	}
}
