Welcome to Lock Manager!
=========================

### Now with custom device type!
* Use the attached device type zwave-lock.groovy for added functionality.
* Minimal changes to SmartThings code.
* More reliable code set/delete.
* Codes will be reconciled on each poll() event.
* App will function without custom device type, but not as well.

### Features:
* Assign Codes to Multiple Users.
* Manage Multiple Locks.
* See what codes are active.
* See how many times a code is used (reset usage manually)
* Be notified when a user uses their code.
* Delete codes after they are used*.
* Optionally Schedule Users so that they only have access during certain times, modes, days of the week, or a calendar range.
* Decide which notifications to receive in the event that the locks are too chatty.
* When a code is entered, perform Hello Home Actions.

\* Code is burned when a user enters it at any selected lock. Code will not become active again until code usage for that user is reset.

# How to use:

## How to...Install App

1. *Go to* **[Apps](https://graph.api.smartthings.com/ide/apps)** in IDE
1. *Click* "New App +"
1. *Click* "From Code"
1. *Copy* from **[github](https://raw.githubusercontent.com/ethayer/user-lock-manager/master/user-lock-manager.smartapp.groovy)**
1. *Paste* into textarea on SmartThings
1. *Click* Publish > For Me

## How to...Add Device Handler:
1. Navigate to: https://graph.api.smartthings.com/ide/devices
1. Click **'New SmartDevice'** in the upper right.
1. Click **'From Code'**
1. Paste the code from the link (github) above into the text area:
  * if you have a *schlage* lock, use @garyd9's handler: [here](https://raw.githubusercontent.com/garyd9/SmartThingsPublic/master/devicetypes/garyd9/zwave-schlage-touchscreen-lock.src/zwave-schlage-touchscreen-lock.groovy)
  * all other locks: [here](https://raw.githubusercontent.com/ethayer/user-lock-manager/master/zwave-lock.groovy)
1. Click **'Create'**
1. Click **'Save'**
1. Click **'Publish'** > **'For Me'**

## Change lock device handler to new code:
1. Navigate to **'My Devices'**:  https://graph.api.smartthings.com/device/list
1. Locate your lock and click on the **name** in the _first column_
1. Click **'Edit'** on the bottom of the page view
1. In the **'Type'** drop-down scroll to the bottom and select **'Z-Wave Lock Reporting'**
1. Click **'Update'** to save changes

#### Then continued on your smart device...

1. *Tap* Marketplace
1. *Tap* SmartApps
1. Scroll down to 'My Apps' / *Tap*
1.  *Tap* User Lock Manager
1. Fill out app options.

## NOTE:
Please use only the included device type or the default device type provided by SmartThings, or the schelage device type created by @garyd9 here:

## Confirmed Locks:
* Kwikset 910, 914
* Yale Security YRL-220-ZW-619
* Others?

USE AT YOUR OWN RISK
---------------------
It is possible that a user code deletion could fail.  Do not give door codes to people that you don't want access to your home or office, and watch notifications carefully.

### Donations

If this made your life easier, please consider donating.

* Paypal- <a href="https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=LDYNH7HNKBWXJ&lc=US&item_name=Lock%20Code%20Manager%20Donation&item_number=40123&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donate_SM%2egif%3aNonHosted"><img src="https://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif" alt="[paypal]" /></a>

* [Google Wallet-](https://www.google.com/wallet/) Send to: thayer.er@gmail.com
