Welcome to Lock Manager!
=========================

###Now with custom device type!
* Use the attached device type zwave-lock.groovy for added functionality.
* Minimal changes to SmartThings code.
* More reliable code set/delete.
* Codes will be reconciled on each poll() event.
* App will function without custom device type, but not as well.

###Features:
* Assign Codes to Multiple Users.
* Manage Multiple Locks.
* See how many times a code is used (reset usage manually)
* Be notified when a user uses their code.
* Delete codes after they are used*.
* Optionally Schedule Users so that they only have access during certain times, modes, days of the week, or a calendar range.
* Decide which notifications to receive in the event that the locks are too chatty.
* When a code is entered, preform Hello Home Actions.

\* Code is burned when a user enters it at any selected lock. Code will not become active again until code usage for that user is reset.

##How to use:
1. Install the app via the SmartThings developer IDE.  Copy all code from the .groovy file on this repository.
  Having trouble with this step?  Use this [tutorial video](https://www.youtube.com/watch?v=D6rG4mk164M&feature=youtu.be) by user Scott in Pollock on youtube.
1. Navigate to My Apps on your smart device.  Follow the prompts on your new app.
1. When complete, either press the app icon to activate the code, or allow the schedule to run.  Pressing the app button outside of the set schedule will still allow the user access!

##NOTE:
Custom device types are not recommended.  Please use only the included device type or the default device type provided by SmartThings.

##Confirmed Locks:
* Kwikset 910, 914
* Yale Security YRL-220-ZW-619
* Others?

USE AT YOUR OWN RISK
---------------------
It is possible that a user code deletion could fail.  Do not give door codes to people that you don't want access to your home or office, and watch notifications carefully.
