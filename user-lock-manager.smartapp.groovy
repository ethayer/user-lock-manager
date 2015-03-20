/**
 *  User Lock Manager v3
 *
 *  Copyright 2015 Erik Thayer
 *
 */
definition(
    name: "User Lock Manager TEST",
    namespace: "ethayer",
    author: "Erik Thayer",
    description: "This app allows you to change, delete, and schedule user access.",
    category: "Safety & Security",
    iconUrl: "https://dl.dropboxusercontent.com/u/54190708/LockManager/lockmanager.png",
    iconX2Url: "https://dl.dropboxusercontent.com/u/54190708/LockManager/lockmanagerx2.png",
    iconX3Url: "https://dl.dropboxusercontent.com/u/54190708/LockManager/lockmanagerx3.png")

 import groovy.json.JsonSlurper
 import groovy.json.JsonBuilder

 preferences {
  page(name: "rootPage")
  page(name: "setupPage")
  page(name: "userPage")
  page(name: "notificationPage")
  page(name: "onUnlockPage")
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
        href(name: "toOnUnlockPage", page: "onUnlockPage", title: "Actions after Unlock")
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
      input name: "maxUsers", title: "Number of users", type: "number", multiple: false, refreshAfterSelection: true
    }
    section("Users") {
      for (int i = 1; i <= settings.maxUsers; i++) {
        href(name: "toUserPage", page: "userPage", params: [number: i], required: false, description: userHrefDescription(i), title: userHrefTitle(i), state: userPageState(i) )
      }
    }
  }
}

def userPage(params) {
  dynamicPage(name:"setupPage", title:"User Settings") {
    def i = params.number
    section("Code #${i}") {
      input(name: "userName${i}", type: "text", title: "Name for User", required: true, defaultValue: settings."userName${i}")
      input(name: "userCode${i}", type: "text", title: "Code (4 to 8 digits) or Blank to Delete", required: false, defaultValue: settings."userCode${i}")
      input(name: "userSlot${i}", type: "number", title: "Slot (1 through 30)", required: true, defaultValue: preSlectedCode(i))
    }
    section {
      input(name: "burnCode${i}", title: "Burn after use?", type: "bool", required: false, defaultValue: settings."burnCode${i}")
      input(name: "userEnabled${i}", title: "Enabled?", type: "bool", required: false, defaultValue: settings."userEnabled${i}")
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
      if (modeStart) {
        input "andOrTime", "enum", title: "[And/Or] at a set time?", metadata:[values:["and", "or"]], required: false, submitOnChange: true
      }
      if ((modeStart == null) || andOrTime) {
        input(name: "startTime", type: "time", title: "Allow Access At This Time", description: null, required: false)
        input(name: "endTime", type: "time", title: "Deny Access At This Time", description: null, required: false)
      }
    }
  }
}


def onUnlockPage() {
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
  def usage = state.codeUsage["code${i}"]
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
  if (usage != null) {
    description += " [Usage: ${usage}]"
  }
  return description
}

def userPageState(i) {
  if (settings."userCode${i}" && settings."userEnabled${i}") {
    return 'complete'
  } else if (settings."userCode${i}" && !settings."userEnabled${i}") {
    return 'incomplete'
  } else {
    return 'incomplete'
  }
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
  if ((andOrTime != null) || (modeStart == null)) {
    if (startTime) {
      descriptionParts << "at ${humanReadableStartDate()}"
    }
    if (endTime) {
      descriptionParts << "until ${humanReadableEndDate()}"
    }
  }

  if (modeStart) {
    if (startTime && andOrTime) {
      descriptionParts << andOrTime
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
  unschedule()
  if (startTime != null) {
    log.debug "scheduling access routine to run at ${startTime}"
    schedule(startTime, "scheduledStart")
  }
  if (endTime != null) {
    log.debug "scheduling access denial routine to run at ${endTime}"
    schedule(endTime, "scheduleEndCheck")
  }

  subscribe(location, locationHandler)

  subscribe(locks, "codeReport", codereturn)
  subscribe(locks, "lock", codeUsed)
  subscribe(locks, "reportAllCodes", pollCodeReport, [filterEvents:false])
  if (homePhrases) {
    subscribe(locks, "lock", performActions)
  }
  revokeDisabledUsers()
  startSchedule()
}

def resetCodeUsage() {
  state.codeUsage = [:]
  for (int i = 1; i <= settings.maxUsers; i++) {
    state.codeUsage."code${i}" = 0
  }
}

def locationHandler(evt) {
  log.debug "locationHandler evt: ${evt.value}"
  if (!modeStart) {
    return
  }
  modeCheck()
}

def checkSechdule() {
  def scheduleCheck = false
  if (andOrTime && (isCorrectMode() || isInScheduledTime())) {
    if (andOrTime == 'and') {
      if (isCorrectMode() && isInScheduledTime()) {
        scheduleCheck = true
      }
    } else {
      if (isCorrectMode() || isInScheduledTime()) {
        scheduleCheck = true
      }
    }
  } else {
    if (isCorrectMode() || isInScheduledTime()) {
      scheduleCheck = true
    }
  }
  return scheduleCheck
}
def modeCheck() {
  if (modeStart || startTime || days) {
    def isSpecifiedMode = false
    if (modeStart && startTime) {
      isSpecifiedMode = checkSechdule()
    } else if (startTime && !modeStart) {
      isSpecifiedMode = isInScheduledTime()
    } else if (modeStart && !startTime) {
      isSpecifiedMode = isCorrectMode()
    } else {
      isSpecifiedMode = true
    }

    def modeStopIsTrue = (modeStop && modeStop != "false")

    if (isSpecifiedMode && canStartAutomatically()) {
      return true
    } else {
      return false
    }
  } else {
    //there's no schedule
    return true
  }
}

def startSchedule() {
  if (modeCheck()) {
    scheduledStart()
  } else {
    scheduledEnd()
  }
}

def isCorrectMode() {
  if (modeStart) {
    if (location.mode == modeStart) {
      return true
    } else {
      return false
    }
  } else {
    return false
  }
}

def isInScheduledTime() {
  if (startTime != null && endTime != null) {
    def start = timeToday(startTime)
    def stop = timeToday(endTime)
    def now = new Date()
    if (start.before(now) && stop.after(now)){
      return true
    } else {
      return false
    }
  } else {
    return false
  }
}

def scheduledStart() {
  if (andOrTime) {
    if (andOrTime == 'and' && isCorrectMode()) {
      grantIfCorrectDays()
    } else if (andOrTime == 'or') {
      grantIfCorrectDays()
    } else {
      // Do Nothing, We can't grant access now.
    }
  } else {
    grantIfCorrectDays()
  }
}
def grantIfCorrectDays() {
  if (canStartAutomatically()) {
    grantAccess()
  }
}

def scheduleEndCheck() {
  if (andOrTime) {
    if (andOrTime == 'and') {
      scheduledEnd()
    } else if (andOrTime == 'or' && modeStart && isCorrectMode()) {
      //do nothing, still in correct mode
    } else {
      scheduledEnd()
    }
  } else {
    scheduledEnd()
  }
}

def scheduledEnd() {
  def array = []
  enabledUsersArray().each { user->
    def userSlot = settings."userSlot${user}"
    array << ["code${userSlot}", ""]
  }
  def json = new groovy.json.JsonBuilder(array).toString()
  if (json != '[]') {
    locks.updateCodes(json)
    runIn(60*2, doPoll)
  }
}

def userSlotArray() {
  def array = []
  for (int i = 1; i <= settings.maxUsers; i++) {
    array << settings."userSlot${i}"
  }
  return array
}

def enabledUsersArray() {
  def array = []
  for (int i = 1; i <= settings.maxUsers; i++) {
    if (settings."userEnabled${i}" == true) {
      array << i
    }
  }
  return array
}
def enabledUsersSlotArray() {
  def array = []
  for (int i = 1; i <= settings.maxUsers; i++) {
    if (settings."userEnabled${i}" == true) {
      def userSlot = settings."userSlot${i}"
      array << userSlot
    }
  }
  return array
}

def disabledUsersSlotArray() {
  def array = []
  for (int i = 1; i <= settings.maxUsers; i++) {
    if (settings."userEnabled${i}" != true) {
      def userSlot = settings."userSlot${i}"
      array << userSlot
    }
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

def codereturn(evt) {
  def codeNumber = evt.data.replaceAll("\\D+","")
  def codeSlot = evt.value

  if (userSlotArray().contains(evt.integerValue)) {
    def userName = settings."userName${usedUserSlot(evt.integerValue)}"
    if (codeNumber == "") {
      def message = "${userName} no longer has access to ${evt.displayName}"
      send(message)
    } else {
      def message = "${userName} now has access to ${evt.displayName}"
      send(message)
    }
  }
}

def usedUserSlot(usedSlot) {
  def slot = ''
  for (int i = 1; i <= settings.maxUsers; i++) {
    if (settings."userSlot${i}" == usedSlot) {
      return i
    }
  }
  return slot
}

def codeUsed(evt) {
  def codeData = new JsonSlurper().parseText(evt.data)

  if(evt.value == "unlocked" && evt.data) {
    codeData = new JsonSlurper().parseText(evt.data)

    if(userSlotArray().contains(codeData.usedCode)) {
      def usedSlot = usedUserSlot(codeData.usedCode)
      def unlockUserName = settings."userName${usedSlot}"
      def message = "${evt.displayName} was unlocked by ${unlockUserName}"
      // increment usage
      state.codeUsage["code${usedSlot}"] = state.codeUsage["code${usedSlot}"] + 1

      if(settings."burnCode${usedSlot}") {
        locks.deleteCode(codeData.usedCode)
        runIn(60*2, doPoll)
        message += ".  Now burning code."
      }
      send(message)
    }
  }
}

def revokeDisabledUsers() {
  def array = []
  for (int i = 1; i <= settings.maxUsers; i++) {
    if (settings."userEnabled${i}" != true) {
      def userSlot = settings."userSlot${i}"
      array << ["code${userSlot}", ""]
    }
  }
  def json = new groovy.json.JsonBuilder(array).toString()
  if (json != '[]') {
    locks.updateCodes(json)
    runIn(60*2, doPoll)
  }
}

def doPoll() {
  // this gets codes if custom device is installed
  locks.poll()
}

def grantAccess() {
  def array = []
  enabledUsersArray().each { user->
    def userSlot = settings."userSlot${user}"
    if (settings."userCode${user}" != null) {
      def newCode = settings."userCode${user}"
      array << ["code${userSlot}", "${newCode}"]
    } else {
      array << ["code${userSlot}", ""]
    }
  }
  resetCodeUsage()
  def json = new groovy.json.JsonBuilder(array).toString()
  if (json != '[]') {
    locks.updateCodes(json)
    runIn(60*2, doPoll)
  }
}

def performActions(evt) {
  def message = ""
  if(evt.value == "unlocked" && evt.data) {
    def codeData = new JsonSlurper().parseText(evt.data)
    if(enabledUsersArray().contains(codeData.usedCode) || isManualUnlock(codeData)) {
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

def isActiveBurnCode(slot) {
  if (settings."burnCode${slot}" && state.codeUsage["code${slot}"] > 0) {
    return false
  } else {
    // not a burn code / not yet used
    return true
  }
}

def pollCodeReport(evt) {
  def active = modeCheck()
  def codeData = new JsonSlurper().parseText(evt.data)
  def numberOfCodes = codeData.codes
  def userSlots = userSlotArray()

  def array = []

  (1..numberOfCodes).each { n->
    def code = codeData."code${n}"
    def slot = n
    if (userSlots.contains(slot)) {
      def usedSlot = usedUserSlot(slot)

      if (active) {
        if (settings."userEnabled${usedSlot}" && isActiveBurnCode(usedSlot)) {
          if (code == settings."userCode${usedSlot}") {
            // "Code is Active, We should be active. Nothing to do"
          } else {
            // "Code is incorrect, We should be active."
            array << ["code${slot}", settings."userCode${usedSlot}"]
          }
        } else {
          if (code != '') {
            // "Code is set, user is disabled, We should be disabled."
            array << ["code${slot}", ""]
          } else {
            // "Code is not set, user is disabled. Nothing to do"
          }
        }
      } else {
        if (code != '') {
          // "Code is set, We should be disabled."
          array << ["code${slot}", ""]
        } else {
          // "Code is not active, We should be disabled. Nothing to do"
        }
      }
    }
  }
  def json = new groovy.json.JsonBuilder(array).toString()
  if (json != '[]') {
    locks.each() { lock ->
      if (lock.id == evt.deviceId) {
        log.debug "sendCodes fix is: ${json}"
        locks.updateCodes(json)
        runIn(60*2, doPoll)
      }
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

