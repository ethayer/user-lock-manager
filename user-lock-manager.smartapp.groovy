/**
 *  User Lock Manager2
 *
 *  Copyright 2015 Erik Thayer
 *
 */
definition(
    name: "User Lock Manager2",
    namespace: "ethayer",
    author: "Erik Thayer",
    description: "This app allows you to change, delete, and schedule user access.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

 import groovy.json.JsonSlurper

 preferences {
  page(name: "rootPage")
  page(name: "setupPage")
  page(name: "userPage")
  page(name: "notificationPage")
  page(name: "schedulingPage")
}

def rootPage() {
  dynamicPage(name: "rootPage", title: "", install: true, uninstall: true) {

    section("Which Locks?") {
      input "locks","capability.lockCodes", title: "Select Locks", required: true, multiple: true, submitOnChange: true
    }

    if (locks) {
      section("How many Users? (1-30)?") {
        input name: "maxUsers", title: "Number of users", type: "number", required: true, multiple: false,  refreshAfterSelection: true
      }
      section("How many Users Are in other installs? (0-29)?") {
        input name: "otherAppUsers", title: "Number of users", type: "number", defaultValue: "0", required: true, multiple: false,  refreshAfterSelection: true
      }
      section {
        href(name: "toSetupPage", title: "User Settings", page: "setupPage")
      }
      section {
        href(name: "toNotificationPage", page: "notificationPage", title: "Notification Settings", description: "", state: "")
      }
      section {
        href(name: "toSchedulingPage", page: "schedulingPage", title: "Schedule (optional)", description: schedulingHrefDescription(), state: schedulingHrefDescription() ? "complete" : "")
      }
      section {
        label(title: "Label this SmartApp", required: false, defaultValue: "")
      }
    }
  }
}

def setupPage() {
  dynamicPage(name:"setupPage", title:"User Settings") {
    section {
      for (int i = 1; i <= settings.maxUsers; i++) {
        href(name: "toUserPage", page: "userPage", params: [number: i], description: userHrefDescription(i), title: userHrefTitle(i))
      }
    }
  }
}

def userPage(params) {
  dynamicPage(name:"setupPage", title:"User Settings") {
    def i = params.number
    section("Code #${i}") {
      input(name: "userName${i}", type: "text", title: "Name for User", required: true, defaultValue: settings."userName${i}")
      input(name: "userCode${i}", type: "number", title: "Code (4 to 8 digits) or Blank to Delete", required: false, defaultValue: settings."userCode${i}")
      input(name: "burnCode${i}", title: "Burn after use?", type: "bool", required: false, defaultValue: settings."burnCode${i}")
    }
    section {
      href(name: "toSetupPage", title: "User Settings", page: "setupPage")
    }
  }
}


def notificationPage() {
  dynamicPage(name: "notificationPage", title: "Notification Settings") {

    section {
      input(name: "phone", type: "phone", title: "Text This Number", description: "Phone number", required: false, submitOnChange: true)
      input(name: "notification", type: "bool", title: "Send A Push Notification", description: "Notification", required: false, submitOnChange: true)
      if (phone != null || notification) {
        input(name: "notifyAccess", title: "on User Entry", type: "bool", required: false)
        input(name: "notifyAccessStart", title: "when granting access", type: "bool", required: false)
        input(name: "notifyAccessEnd", title: "when revoking access", type: "bool", required: false)
      }
    }

    section("Only During These Times (optional)") {
      input(name: "notificationStartTime", type: "time", title: "Notify Starting At This Time", description: null, required: false)
      input(name: "notificationEndTime", type: "time", title: "Notify Ending At This Time", description: null, required: false)
    }
  }
}
def schedulingPage() {
  dynamicPage(name: "schedulingPage", title: "Rules For Access Scheduling") {
    section {
      input(name: "days", type: "enum", title: "Allow User Access On These Days", description: "Every day", required: false, multiple: true, options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"])
    }
    section {
      input(name: "modeStart", title: "Allow Access when entering this mode", type: "mode", required: false, mutliple: false, submitOnChange: true)
      if (modeStart) {
        input(name: "modeStop", title: "Deny Access when leaving '${modeStart}' mode", type: "bool", required: false)
      }
    }
    section {
      input(name: "startTime", type: "time", title: "Allow Access At This Time", description: null, required: false)
      input(name: "endTime", type: "time", title: "Deny Access At This Time", description: null, required: false)
    }
  }
}

public smartThingsDateFormat() { "yyyy-MM-dd'T'HH:mm:ss.SSSZ" }

public humanReadableStartDate() {
  new Date().parse(smartThingsDateFormat(), startTime).format("h:mm a", timeZone(startTime))
}
public humanReadableEndDate() {
  new Date().parse(smartThingsDateFormat(), endTime).format("h:mm a", timeZone(endTime))
}

def userHrefTitle(i) {
  def title = "User ${i}"
  if (settings."userName${i}") {
    title = settings."userName${i}"
  }
  return title
}
def userHrefDescription(i) {
  def uc = settings."userCode${i}"
  def description = ""
  log.debug uc
  if (uc != null) {
    log.debug "in!"
    description = "Code: ${uc}"
    if(settings."burnCode${i}") {
      description += ' Single Use'
    }
  }
  log.debug description
  return description
}

def fancyDeviceString(devices = []) {
  fancyString(devices.collect { deviceLabel(it) })
}

def deviceLabel(device) {
  return device.label ?: device.name
}

def fancyString(listOfStrings) {

  def fancify = { list ->
    return list.collect {
      def label = it
      if (list.size() > 1 && it == list[-1]) {
        label = "and ${label}"
      }
      label
    }.join(", ")
  }

  return fancify(listOfStrings)
}

def schedulingHrefDescription() {

  def descriptionParts = []
  if (days) {
    descriptionParts << "On ${fancyString(days)},"
  }

  descriptionParts << "${fancyDeviceString(locks)} will be accessible"

  if (startTime) {
    descriptionParts << "at ${humanReadableStartDate()}"
  }
  if (endTime) {
    descriptionParts << "until ${humanReadableEndDate()}"
  }

  if (modeStart) {
    if (startTime) {
      descriptionParts << "or"
    }
    descriptionParts << "when ${location.name} enters '${modeStart}' mode"
  }

  if (descriptionParts.size() <= 1) {
    // locks will be in the list no matter what. No rules are set if only locks are in the list
    return null
  }

  return descriptionParts.join(" ")
}

def installed() {
  log.debug "Installing 'Locks' with settings: ${settings}"
  initialize()
}

def updated() {
  log.debug "Updating 'Locks' with settings: ${settings}"
  initialize()
}

private initialize() {
  unsubscribe()

  if (startTime != null) {
    log.debug "scheduling access routine to run at ${startTime}"
    schedule(startTime, "scheduledStart")
  }
  if (endTime != null) {
    log.debug "scheduling access denial routine to run at ${endTime}"
    schedule(endTime, "scheduledEnd")
  }

  subscribe(location, locationHandler)

  subscribe(app, appTouch)
  subscribe(locks, "codeReport", codereturn)
  subscribe(locks, "lock", codeUsed)
}

def locationHandler(evt) {
  log.debug "locationHandler evt: ${evt.value}"

  if (!modeStart) {
    return
  }

  def isSpecifiedMode = (evt.value == modeStart)
  def modeStopIsTrue = (modeStop && modeStop != "false")

  if (isSpecifiedMode && canStartAutomatically()) {
    grantAccess()
  } else if (!isSpecifiedMode && modeStopIsTrue) {
    scheduledEnd()
  }
}

def scheduledStart() {
  if (canStartAutomatically()) {
    grantAccess()
  }
}

def scheduledEnd() {
  for (int i = 1; i <= settings.maxUsers; i++) {
    revokeAccess(i)
  }
}


def canStartAutomatically() {
  def today = new Date().format("EEEE", location.timeZone)
  log.debug "today: ${today}, days: ${days}"

  if (!days || days.contains(today)) {// if no days, assume every day
    return true
  }

  log.trace "should not allow access"
  return false
}

def appTouch(evt) {
  grantAccess()
}


def codereturn(evt) {
  def codeNumber = evt.data.replaceAll("\\D+","");

  if (evt.value.toInteger() <= settings.maxUsers) {
    def usedUserName = settings."userName${evt.integerValue}"
    if (codeNumber == "") {
      def message = "${usedUserName} no longer has access to ${evt.displayName}"
      send(message)
    } else {
      def message = "${usedUserName} now has access to ${evt.displayName}"
      send(message)
    }
  }
}

def codeUsed(evt) {
  if(evt.value == "unlocked" && evt.data) {
    def codeData = new JsonSlurper().parseText(evt.data)

    if(codeData.usedCode && codeData.usedCode <= settings.maxUsers) {
      def unlockUserName = settings."userName${codeData.usedCode}"
      def message = "${evt.displayName} was unlocked by ${unlockUserName}"
      if(settings."burnCode${codeData.usedCode}") {
        revokeAccess(codeData.usedCode)
        message += ".  Now burning code."
      }
      send(message)
    }
  }
}

def revokeAccess(i) {
  locks.deleteCode(i)
}
def grantAccess() {
  for (int i = 1; i <= settings.maxUsers; i++) {
    if (settings."userCode${i}" != null) {
      def newCode = settings."userCode${i}"
      newCode = Integer.toString(newCode)
      locks.setCode(i, newCode)
    } else {
      revokeAccess(i)
    }
  }
}

private send(msg) {
  if (notificationStartTime != null && notificationEndTime != null) {
    def start = timeToday(notificationStartTime)
    def stop = timeToday(notificationEndTime)
    def now = new Date()
    if (start.before(now) && stop.after(now)){
      sendMessage(msg)
    }
  } else {
    sendMessage(msg)
  }
}
private sendMessage(msg) {
  if (notification) {
    sendPush(msg)
  }
  if (phone) {
    sendSms(phone, msg)
  }
}

