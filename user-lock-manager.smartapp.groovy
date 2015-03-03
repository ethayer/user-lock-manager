/**
 *  User Lock Manager
 *
 *  Copyright 2015 Erik Thayer
 *
 */
definition(
    name: "User Lock Manager",
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
  page(name: "notificationPage")
  page(name: "schedulingPage")
}

def rootPage() {
  dynamicPage(name: "rootPage", title: "", install: true, uninstall: true) {

    section("Which Locks?") {
      input "locks","capability.lockCodes", title: "Select Locks", required: true, multiple: true, submitOnChange: true
    }

    if (locks) {
      section {
        href(name: "toSetupPage", title: "User Settings", page: "setupPage", description: setupHrefDescription(), state: setupHrefDescription() ? "complete" : "")
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
      input(name: "userName", type: "text", title: "Name for User", required: true)
      input(name: "userSlot", type: "number", title: "User Slot (From 1 to 30) ", required: true)
      input(name: "userCode", type: "number", title: "Code (4 to 8 digits) or Blank to Delete", required: false)
      input(name: "burnCode", title: "Burn after use?", type: "bool", required: false)
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

def setupHrefDescription() {
  def title = ''
  if (userCode && userSlot) {
    title = "User ${userName} on slot ${userSlot}."
    if(burnCode) {
      title += ' Burning after each use.'
    }
  } else {
    return null
  }
  return title
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
    revokeAccess()
  }
}

def scheduledStart() {
  if (canStartAutomatically()) {
    grantAccess()
  }
}

def scheduledEnd() {
  revokeAccess()
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
  def codenumber = evt.data.replaceAll("\\D+","");
  if (evt.value == Integer.toString(userSlot)) {
    if (codenumber == "") {
      def message = "${userName} no longer has access to ${evt.displayName}"
      send(message)
    } else {
      def message = "${userName} now has access to ${evt.displayName}"
      send(message)
    }
  }
}

def codeUsed(evt) {
  if(evt.value == "unlocked" && evt.data) {
    def codeData = new JsonSlurper().parseText(evt.data)
    def message = "${evt.displayName} was unlocked by ${userName}"
    if(codeData.usedCode == userSlot) {
      if(burnCode) {
        revokeAccess()
        message += ".  Now burning code."
      }
      send(message)
    }
  }
}

def revokeAccess() {
  locks.deleteCode(userSlot)
}
def grantAccess() {
  if (userCode != null) {
    def newCode = Integer.toString(userCode)
    locks.setCode(userSlot, newCode)
  } else {
    revokeAccess()
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

