/**
 *  User Lock Manager v1.2
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
  page(name: "userPage")
  page(name: "notificationPage")
  page(name: "onUnlock")
  page(name: "schedulingPage")
}

def rootPage() {
  dynamicPage(name: "rootPage", title: "", install: true, uninstall: true) {

    section("Which Locks?") {
      input "locks","capability.lockCodes", title: "Select Locks", required: true, multiple: true, submitOnChange: true
    }

    if (locks) {
      section {
        href(name: "toSetupPage", title: "User Settings", page: "setupPage")
        href(name: "toNotificationPage", page: "notificationPage", title: "Notification Settings", description: "", state: "")
        href(name: "toSchedulingPage", page: "schedulingPage", title: "Schedule (optional)", description: schedulingHrefDescription(), state: schedulingHrefDescription() ? "complete" : "")
        href(name: "toOnUnlockPage", page: "onUnlock", title: "Actions after Unlock")
      }
      section {
        label(title: "Label this SmartApp", required: false, defaultValue: "")
      }
    }
  }
}

def setupPage() {
  dynamicPage(name:"setupPage", title:"User Settings") {
    section("How many Users? (1-30)?") {
      input name: "maxUsers", title: "Number of users", type: "number", required: true, multiple: false, refreshAfterSelection: true
    }
    section("Users") {
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
      input(name: "userSlot${i}", type: "number", title: "Slot (1 through 30)", required: true, defaultValue: preSlectedCode(i))
      input(name: "burnCode${i}", title: "Burn after use?", type: "bool", required: false, defaultValue: settings."burnCode${i}")
    }
    section {
      href(name: "toSetupPage", title: "Back To Users", page: "setupPage")
    }
  }
}

def preSlectedCode(i) {
  if (settings."userSlot${i}" != null) {
    return settings."userSlot${i}"
  } else {
    return i
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


def onUnlock() {
  dynamicPage(name:"onUnlock", title:"Initiate Actions") {
    section("Actions") {
      def phrases = location.helloHome?.getPhrases()*.label
      if (phrases) {
        phrases.sort()
        input name: "homePhrases", type: "enum", title: "Home Mode Phrase", multiple: true,required: false, options: phrases, refreshAfterSelection: true
        if (homePhrases) {
          input name: "manualUnlock", title: "Initiate phrase on manual unlock also?", type: "bool", defaultValue: false, refreshAfterSelection: true
        }
      }
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
  def us = settings."userSlot${i}"
  def description = ""
  if (us != null) {
    description += "Slot: ${us}"
  }
  if (uc != null) {
    description += " // ${uc}"
    if(settings."burnCode${i}") {
      description += ' [Single Use]'
    }
  }
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
  if (homePhrases) {
    subscribe(locks, "lock", performActions)
  }
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
  userSlotArray().each { slot->
    revokeAccess(slot)
  }
}

def userSlotArray() {
  def array = []
  for (int i = 1; i <= settings.maxUsers; i++) {
    array << settings."userSlot${i}"
  }
  return array
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
  def codeNumber = evt.data.replaceAll("\\D+","")
  if (userSlotArray().contains(evt.integerValue)) {
    def userName = usedUserName(evt.integerValue)
    if (codeNumber == "") {
      def message = "${userName} no longer has access to ${evt.displayName}"
      send(message)
    } else {
      def message = "${userName} now has access to ${evt.displayName}"
      send(message)
    }
  }
}

def usedUserName(usedSlot) {
  def name = 'Unknown'
  for (int i = 1; i <= settings.maxUsers; i++) {
    if (settings."userSlot${i}" == usedSlot) {
      name = settings."userName${i}"
    }
  }
  return name
}

def codeUsed(evt) {
  if(evt.value == "unlocked" && evt.data) {
    def codeData = new JsonSlurper().parseText(evt.data)

    if(userSlotArray().contains(codeData.usedCode)) {
      def unlockUserName = usedUserName(codeData.usedCode)
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
    def userSlot = settings."userSlot${i}"
    if (settings."userCode${i}" != null) {
      def newCode = Integer.toString(settings."userCode${i}")
      locks.setCode(userSlot, newCode)
    } else {
      revokeAccess(userSlot)
    }
  }
}

def performActions(evt) {
  def message = ""
  if(evt.value == "unlocked" && evt.data) {
    def codeData = new JsonSlurper().parseText(evt.data)
    if(userSlotArray().contains(codeData.usedCode) || isManualUnlock(codeData)) {
      location.helloHome.execute(settings.homePhrases)
    }
  }
}

def isManualUnlock(codeData) {
  // check to see if the user wants this
  if (manualUnlock) {
    if ((codeData.usedCode == "") || (codeData.usedCode == null)) {
      // no code used on unlock!
      return true
    } else {
      // probably a code we're not dealing with here
      return false
    }
  } else {
    return false
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

