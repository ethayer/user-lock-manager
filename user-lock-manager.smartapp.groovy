/**
 *  User Lock Manager v3.7.8
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
  page(name: "calendarPage")
  page(name: "resetAllCodeUsagePage")
  page(name: "resetCodeUsagePage")
  page(name: "reEnableUserPage")
}

def rootPage() {
  //reset errors on each load
  dynamicPage(name: "rootPage", title: "", install: true, uninstall: true) {
    // check to see if maxUsers is still being used
    setMaxUserDefault()
    // have to do this in case the app is closed while modifying users
    state.cookieCurrentUser = false
    section("Which Locks?") {
      input "locks","capability.lockCodes", title: "Select Locks", required: true, multiple: true, submitOnChange: true
    }
    if (locks) {
      section {
        input name: "maxUsers", title: "Number of users", type: "number", multiple: false, refreshAfterSelection: true, submitOnChange: true
        //input name: "startSlot", title: "Lock code slot to start at.", type: "number", multiple: false, defaultValue: 1, refreshAfterSelection: true, submitOnChange: true
        //input name: "endSlot", title: "Lock code slot to end at.", type: "number", multiple: false, required: false, defaultValue: state.endSlot, refreshAfterSelection: true, submitOnChange: true
      }
    }

    //if ( settings?.startSlot > 0 && settings?.endSlot > settings?.startSlot ) {
      section {
        href(name: "toSetupPage", title: "User Settings", page: "setupPage", description: setupPageDescription(), state: setupPageDescription() ? "complete" : "")
        href(name: "toNotificationPage", page: "notificationPage", title: "Notification Settings", description: notificationPageDescription(), state: notificationPageDescription() ? "complete" : "")
        href(name: "toSchedulingPage", page: "schedulingPage", title: "Schedule (optional)", description: schedulingHrefDescription(), state: schedulingHrefDescription() ? "complete" : "")
        href(name: "toOnUnlockPage", page: "onUnlockPage", title: "Global Hello Home")
      }
    //}

    section {
        label(title: "Label this SmartApp", required: false, defaultValue: "")
    }
    section {
    	input name: "enableDebug", title: "Enable Debug Logging", type: "bool", required: false 
    }
  }
}

def setupPage() {
  dynamicPage(name:"setupPage", title:"User Settings") {
    checkRecentUser()
    // Set the current user to false since we already processed any previous ones
    state.cookieCurrentUser = false
    if (maxUsers > 0) {
      section('Users') {
        (1..maxUsers).each { user->
          if (!state."userState${user}") {
            //there's no values, so reset
            resetCodeUsage(user)
          }
          href(name: "toUserPage${user}", page: "userPage", params: [number: user], required: false, description: userHrefDescription(user), title: userHrefTitle(user), state: userPageState(user) )
        }
      }
      section {
        href(name: "toResetAllCodeUsage", title: "Reset Code Usage", page: "resetAllCodeUsagePage", description: "Tap to reset")
      }
    } else {
      section("Users") {
        paragraph "Users are set to zero.  Please go back to the main page and change the number of users to at least 1."
      }
    }
  }
}

def userPage(params) {
  dynamicPage(name:"userPage", title:"User Settings") {
    if (params?.number || params?.params?.number) {
      def i = 0

      // Assign params to i.  Sometimes parameters are double nested.
      if (params.number) {
        i = params.number
      } else {
        i = params.params.number
      }

      //Make sure i is a round number, not a float.
      if ( ! i.isNumber() ) {
        i = i.toInteger();
      } else if ( i.isNumber() ) {
        i = Math.round(i * 100) / 100
      }

      if (!state."userState${i}".enabled) {
        section {
          paragraph "This user has been disabled by the controller due to excessive failed set attempts! Please verify that the code is valid and does not conflict with another code.\n\nYou may attempt to delete the code field and re-enter it.\n\nTo re-enabled this slot, click 'Reset' link bellow."
          href(name: "toreEnableUserPage", title: "Reset User", page: "reEnableUserPage", params: [number: i], description: "Tap to reset")
        }
      }
      section("Code #${i}") {
        input(name: "userName${i}", type: "text", title: "Name for User", defaultValue: settings."userName${i}")
        input(name: "userCode${i}", type: "text", title: "Code (4 to 8 digits)", required: false, defaultValue: settings."userCode${i}")
        input(name: "userSlot${i}", type: "number", title: "Slot (1 through 30)", defaultValue: preSlectedCode(i))
      }
      section {
        input(name: "burnCode${i}", title: "Burn after use?", type: "bool", required: false, defaultValue: settings."burnCode${i}")
        input(name: "userEnabled${i}", title: "Enabled?", type: "bool", required: false, defaultValue: settings."userEnabled${i}")
        def phrases = location.helloHome?.getPhrases()*.label
        if (phrases) {
          phrases.sort()
          input name: "userHomePhrases${i}", type: "enum", title: "Hello Home Phrase", multiple: true,required: false, options: phrases, defaultValue: settings."userHomePhrases${i}", refreshAfterSelection: true
          input "userNoRunPresence${i}", "capability.presenceSensor", title: "Don't run Actions if any of these are present:", multiple: true, required: false, defaultValue: settings."userNoRunPresence${i}"
          input "userDoRunPresence${i}", "capability.presenceSensor", title: "Run Actions only if any of these are present:", multiple: true, required: false, defaultValue: settings."userDoRunPresence${i}"
        }
      }
      section {
        href(name: "toSetupPage", title: "Back To Users", page: "setupPage")
        href(name: "toResetCodeUsagePage", title: "Reset Code Usage", page: "resetCodeUsagePage", params: [number: i], description: "Tap to reset")
      }
    } else {
      section {
        paragraph "Page has been refreshed, please go back to users page."
        href(name: "toSetupPage", title: "Back", page: "setupPage")
      }
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
      input(name: "sendevent", type: "bool", title: "Send An Event Notification", description: "Event Notification", required: false, submitOnChange: true)
      if (phone != null || notification || sendevent) {
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
    if (!days) {
      section {
        href(name: "toCalendarPage", title: "Calendar", page: "calendarPage", description: calendarHrefDescription(), state: calendarHrefDescription() ? "complete" : "")
      }
    }
    if (!startDay && !startMonth && !startYear && !endDay && !endMonth && !endYear) {
      section {
        input(name: "days", type: "enum", title: "Allow User Access On These Days", description: "Every day", required: false, multiple: true, options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"], submitOnChange: true)
      }
      section {
        input(name: "modeStart", title: "Allow Access when in this mode", type: "mode", required: false, mutliple: false, submitOnChange: true)
      }
      section {
        if (modeStart) {
          input "andOrTime", "enum", title: "[And/Or] at a set time?", metadata:[values:["and", "or"]], required: false, submitOnChange: true
        }
        if ((modeStart == null) || andOrTime) {
          input(name: "startTime", type: "time", title: "Start Time", description: null, required: false)
          input(name: "endTime", type: "time", title: "End Time", description: null, required: false)
        }
      }
    }
  }
}

def calendarPage() {
  dynamicPage(name: "calendarPage", title: "Calendar Access") {
    section() {
      paragraph "This page is for advanced users only. You must enter each field carefully."
      paragraph "Calendar use does not support daily grant/deny OR Modes.  You cannot both have a date here, and allow access only on certain days/modes."
    }
    section("Start Date") {
      input name: "startDay", type: "number", title: "Day", required: false
      input name: "startMonth", type: "number", title: "Month", required: false
      input name: "startYear", type: "number", description: "Format(yyyy)", title: "Year", required: false
      input name: "startTime", type: "time", title: "Start Time", description: null, required: false
    }
    section("End Date") {
      input name: "endDay", type: "number", title: "Day", required: false
      input name: "endMonth", type: "number", title: "Month", required: false
      input name: "endYear", type: "number", description: "Format(yyyy)", title: "Year", required: false
      input name: "endTime", type: "time", title: "End Time", description: null, required: false
    }
  }
}

def onUnlockPage() {
  dynamicPage(name:"onUnlockPage", title:"Global Actions (Any Code)") {
    section("Actions") {
      def phrases = location.helloHome?.getPhrases()*.label
      if (phrases) {
        phrases.sort()
        input name: "homePhrases", type: "enum", title: "Home Mode Phrase", multiple: true,required: false, options: phrases, refreshAfterSelection: true, submitOnChange: true
        if (homePhrases) {
          input "noRunPresence", "capability.presenceSensor", title: "Don't run Actions if any of these are present:", multiple: true, required: false
          input "doRunPresence", "capability.presenceSensor", title: "Run Actions only if any of these are present:", multiple: true, required: false
          input name: "manualUnlock", title: "Initiate phrase on manual unlock also?", type: "bool", defaultValue: false, refreshAfterSelection: true
        }
      }
    }
  }
}

def resetCodeUsagePage(params) {
  // do reset
  resetCodeUsage(params.number)
  def i = params.number
  dynamicPage(name:"resetCodeUsagePage", title:"User Usage Reset") {
    section {
      paragraph "User code usage has been reset."
    }
    section {
      href(name: "toSetupPage", title: "Back To Users", page: "setupPage")
    }
  }
}
def resetAllCodeUsagePage() {
  // do resetAll
  resetAllCodeUsage()
  dynamicPage(name:"resetAllCodeUsagePage", title:"User Settings") {
    section {
      paragraph "All user code usages have been reset."
    }
    section("Users") {
      href(name: "toSetupPage", title: "Back to Users", page: "setupPage")
      href(name: "toRootPage", title: "Main Page", page: "rootPage")
    }
  }
}
def reEnableUserPage(params) {
  // do reset
  enableUser(params.number)
  lockErrorLoopReset()
  def i = params.number
  dynamicPage(name:"reEnableUserPage", title:"User re-enabled") {
    section {
      paragraph "User has been enabled."
    }
    section {
      href(name: "toSetupPage", title: "Back To Users", page: "setupPage")
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

def setupPageDescription(){
  def parts = []
  for (int i = 1; i <= settings.maxUsers; i++) {
    parts << settings."userName${i}"
  }
  return fancyString(parts)
}

def notificationPageDescription() {
    def parts = []
    def msg = ""
    if (settings.phone) {
        parts << "SMS to ${phone}"
    }
    if (settings.sendevent) {
        parts << "Event Notification"
    }
    if (settings.notification) {
        parts << "Push Notification"
    }
    msg += fancyString(parts)
    parts = []

    if (settings.notifyAccess) {
        parts << "on entry"
    }
    if (settings.notifyAccessStart) {
        parts << "when granting access"
    }
    if (settings.notifyAccessEnd) {
        parts << "when revoking access"
    }
    if (settings.notificationStartTime) {
        parts << "starting at ${settings.notificationStartTime}"
    }
    if (settings.notificationEndTime) {
        parts << "ending at ${settings.notificationEndTime}"
    }
    if (parts.size()) {
        msg += ": "
        msg += fancyString(parts)
    }
    return msg
}

def calendarHrefDescription() {
  def dateStart = startDateTime()
  def dateEnd = endDateTime()
  if (dateEnd && dateStart) {
    def startReadableTime = readableDateTime(dateStart)
    def endReadableTime = readableDateTime(dateEnd)
    return "Accessible from ${startReadableTime} until ${endReadableTime}"
  } else if (!dateEnd && dateStart) {
    def startReadableTime = readableDateTime(dateStart)
    return "Accessible on ${startReadableTime}"
  } else if (dateEnd && !dateStart){
    def endReadableTime = readableDateTime(dateEnd)
    return "Accessible until ${endReadableTime}"
  }
}

def readableDateTime(date) {
  new Date().parse(smartThingsDateFormat(), date.format(smartThingsDateFormat(), location.timeZone)).format("EEE, MMM d yyyy 'at' h:mma", location.timeZone)
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
  def usage = state."userState${i}".usage
  def description = ""
  if (us != null) {
    description += "Slot: ${us}"
  }
  if (uc != null) {
    description += " / ${uc}"
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
  if (settings."userCode${i}" && userIsEnabled(i)) {
    if (settings."burnCode${i}") {
      if (state.codeUsage."code${i}" > 0) {
        return 'incomplete'
      } else {
        return 'complete'
      }
    } else {
      return 'complete'
    }

  } else if (settings."userCode${i}" && !settings."userEnabled${i}") {
    return 'incomplete'
  } else {
    return 'incomplete'
  }
}

def userIsEnabled(i) {
  if (settings."userEnabled${i}" && (settings."userCode${i}" != null) && (state."userState${i}".enabled != false)) {
    return true
  } else {
    return false
  }
}

def fancyDeviceString(devices = []) {
  fancyString(devices.collect { deviceLabel(it) })
}

def deviceLabel(device) {
  return device.label ?: device.name
}

def fancyString(listOfStrings) {
  listOfStrings.removeAll([null])
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
  if (startDateTime() || endDateTime()) {
    calendarHrefDescription()
  } else {
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
  debugLog("Settings: ${settings}")
  unsubscribe()
  unschedule()
  if (startTime && !startDateTime()) {
    log.debug "scheduling access routine to run at ${startTime}"
    schedule(startTime, "reconcileCodesStart")
  } else if (startDateTime()) {
    // There's a start date, so let's run then
    log.debug "scheduling RUNONCE start"
    runOnce(startDateTime().format(smartThingsDateFormat(), location.timeZone), "reconcileCodesStart")
  }

  if (endTime && !endDateTime()) {
    log.debug "scheduling access denial routine to run at ${endTime}"
    schedule(endTime, "reconcileCodesEnd")
  } else if (endDateTime()) {
    // There's a end date, so let's run then
    log.debug "scheduling RUNONCE end"
    runOnce(endDateTime().format(smartThingsDateFormat(), location.timeZone), "reconcileCodesEnd")
  }

  subscribe(location, locationHandler)

  subscribe(locks, "codeReport", codereturn)
  subscribe(locks, "lock", codeUsed)
  subscribe(locks, "reportAllCodes", pollCodeReport, [filterEvents:false])

  revokeDisabledUsers()
  reconcileCodes()
  lockErrorLoopReset()
  log.debug "state: ${state}"
}

def resetAllCodeUsage() {
  for (int i = 1; i <= settings.maxUsers; i++) {
    lockErrorLoopReset()
    resetCodeUsage(i)
  }
  log.debug "reseting all code usage"
}
def resetCodeUsage(i) {
  if(state."userState${i}" == null) {
    state."userState${i}" = [:]
    state."userState${i}".enabled = true
  }
  state."userState${i}".usage = 0
}
def enableUser(i) {
  state."userState${i}".enabled = true
}
def lockErrorLoopReset() {
  state.error_loop_count = 0
  def i = 0
  locks.each { lock->
    i = i + 1
    state."lock${i}" = [:]
    state."lock${i}".error_loop = false
  }
}

def locationHandler(evt) {
  log.debug "locationHandler evt: ${evt.value}"
  if (!modeStart) {
    return
  }
  modeCheck()
}

def reconcileCodes() {
  if (isAbleToStart()) {
    grantAccess()
  } else {
    revokeAccess()
  }
}

def reconcileCodesStart() {
  // schedule start of reconcileCodes
  reconcileCodes()
}

def reconcileCodesEnd() {
  // schedule end of reconcileCodes
  reconcileCodes()
}

def isAbleToStart() {
  def dateStart = startDateTime()
  def dateEnd = endDateTime()

  if (dateStart || dateEnd) {
    // calendar schedule above all
    return checkCalendarSchedule(dateStart, dateEnd)
  } else if (modeStart || startTime || endTime || days) {
    // No calendar set, check daily schedule
    if (isCorrectDay()) {
      // it's the right day
      checkDailySchedule()
    } else {
      // it's the wrong day
      return false
    }
  } else {
    // no schedule
    return true
  }
}

def checkDailySchedule() {
  if (andOrTime && modeStart && (isCorrectMode() || isInScheduledTime())) {
    // in correct mode or time with and/or switch
    if (andOrTime == 'and') {
      // must be both
      if (isCorrectMode() && isInScheduledTime()) {
        // is both
        return true
      } else {
        // is not both
        return false
      }
    } else {
      // could be either
      if (isCorrectMode() || isInScheduledTime()) {
        // it is either mode or time
        return true
      } else {
        // is not either mode or time
        return false
      }
    }
  } else {
    // Allow either mode or time, no andOrTime is set
    if (isCorrectMode() || isInScheduledTime()) {
      // it is either mode or time
      return true
    } else {
      // is not either mode or time
      return false
    }
  }
}

def checkCalendarSchedule(dateStart, dateEnd) {
  def now = rightNow().getTime()
  if (dateStart && !dateEnd) {
    // There's a start time, but no end time.  Allow access after start
    if (dateStart.getTime() > now) {
      // It's after the start time
      return true
    } else {
      // It's before the start time
      return false
    }

  } else if (dateEnd && !dateStart) {
    // There's a end time, but no start time.  Allow access until end
    if (dateStart.getTime() > now) {
      // It's after the start time
      return true
    } else {
      // It's before the start time
      return false
    }

  } else {
    // There's both an end time, and a start time.  Allow access between them.
    if (dateStart.getTime() < now && dateEnd.getTime() > now) {
      // It's in calendar times
      return true
    } else {
      // It's not in calendar times
      return false
    }
  }
}

def isCorrectMode() {
  if (modeStart) {
    // mode check is on
    if (location.mode == modeStart) {
      // we're in the right one mode
      return true
    } else {
      // we're in the wrong mode
      return false
    }
  } else {
    // mode check is off
    return false
  }
}

def isInScheduledTime() {
  def now = new Date()
  if (startTime && endTime) {
    def start = timeToday(startTime)
    def stop = timeToday(endTime)

    // there's both start time and end time
    if (start.before(now) && stop.after(now)){
      // It's between the times
      return true
    } else {
      // It's not between the times
      return false
    }
  } else if (startTime && !endTime){
    // there's a start time, but no end time
    def start = timeToday(startTime)
    if (start.before(now)) {
      // it's after start time
      return true
    } else {
      //it's before start time
      return false
    }
  } else if (!startTime && endTime) {
    // there's an end time but no start time
    def stop = timeToday(endTime)
    if (stop.after(now)) {
      // it's still before end time
      return true
    } else {
      // it's after end time
      return false
    }
  } else {
    // there are no times
    return false
  }
}

def startDateTime() {
  if (startDay && startMonth && startYear && startTime) {
    def time = new Date().parse(smartThingsDateFormat(), startTime).format("'T'HH:mm:ss.SSSZ", timeZone(startTime))
    return Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", "${startYear}-${startMonth}-${startDay}${time}")
  } else {
    // Start Date Time not set
    return false
  }
}

def endDateTime() {
  if (endDay && endMonth && endYear && endTime) {
    def time = new Date().parse(smartThingsDateFormat(), endTime).format("'T'HH:mm:ss.SSSZ", timeZone(endTime))
    return Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", "${endYear}-${endMonth}-${endDay}${time}")
  } else {
    // End Date Time not set
    return false
  }
}

def rightNow() {
  def now = new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSSZ", location.timeZone)
  return Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", now)
}

def isCorrectDay() {
  def today = new Date().format("EEEE", location.timeZone)
  log.debug "today: ${today}, days: ${days}"
  if (!days || days.contains(today)) {
    // if no days, assume every day
    return true
  }
  log.trace "should not allow access - Not correct Day"
  return false
}

def userSlotArray() {
  def array = []
  for (int i = 1; i <= settings.maxUsers; i++) {
    if (settings."userSlot${i}") {
      array << settings."userSlot${i}".toInteger()
    }
  }
  return array
}

def enabledUsersArray() {
  def array = []
  for (int i = 1; i <= settings.maxUsers; i++) {
    if (userIsEnabled(i)) {
      array << i
    }
  }
  return array
}
def enabledUsersSlotArray() {
  def array = []
  for (int i = 1; i <= settings.maxUsers; i++) {
    if (userIsEnabled(i)) {
      def userSlot = settings."userSlot${i}"
      array << userSlot.toInteger()
    }
  }
  return array
}

def disabledUsersSlotArray() {
  def array = []
  for (int i = 1; i <= settings.maxUsers; i++) {
    if (!userIsEnabled(i)) {
      if (settings."userSlot${i}") {
        array << settings."userSlot${i}".toInteger()
      }
    }
  }
  return array
}

def codereturn(evt) {
  // move to JsonSlurper to support previous z-wave reporting device as well as the smartthings device and additional info added to data
  def codeNumber = codeData.code
  def codeSlot = evt.value
  log.debug "Received event for slot #$codeSlot with code #$codeNumber"
  if (notifyAccessEnd || notifyAccessStart) {
    if (userSlotArray().contains(evt.integerValue.toInteger())) {
      def userName = settings."userName${usedUserSlot(evt.integerValue)}"
      if (codeNumber == "") {
        if (notifyAccessEnd) {
          def message = "${userName} no longer has access to ${evt.displayName}"
          send(message)
        }
      } else {
        if (notifyAccessStart) {
          def message = "${userName} now has access to ${evt.displayName}"
          send(message)
        }
      }
    }
  }
}

def usedUserSlot(usedSlot) {
  def slot = ''
  for (int i = 1; i <= settings.maxUsers; i++) {
    if (settings."userSlot${i}".toInteger() == usedSlot.toInteger()) {
      return i
    }
  }
  return slot
}

def codeUsed(evt) {
  def codeData = new JsonSlurper().parseText(evt.data)
  if(evt.value == "unlocked" && evt.data) {
    codeData = new JsonSlurper().parseText(evt.data)
    if(userSlotArray().contains(codeData.usedCode.toInteger())) {
      def usedSlot = usedUserSlot(codeData.usedCode).toInteger()
      def unlockUserName = settings."userName${usedSlot}"
      def message = "${evt.displayName} was unlocked by ${unlockUserName}"
      // increment usage
      state."userState${usedSlot}".usage = state."userState${usedSlot}".usage + 1
      if(settings."userHomePhrases${usedSlot}") {
        // Specific User Hello Home
        if (settings."userNoRunPresence${usedSlot}" && settings."userDoRunPresence${usedSlot}" == null) {
          if (!anyoneHome(settings."userNoRunPresence${usedSlot}")) {
            location.helloHome.execute(settings."userHomePhrases${usedSlot}")
          }
        } else if (settings."userDoRunPresence${usedSlot}" && settings."userNoRunPresence${usedSlot}" == null) {
          if (anyoneHome(settings."userDoRunPresence${usedSlot}")) {
            location.helloHome.execute(settings."userHomePhrases${usedSlot}")
          }
        } else if (settings."userDoRunPresence${usedSlot}" && settings."userNoRunPresence${usedSlot}") {
          if (anyoneHome(settings."userDoRunPresence${usedSlot}") && !anyoneHome(settings."userNoRunPresence${usedSlot}")) {
            location.helloHome.execute(settings."userHomePhrases${usedSlot}")
          }
        } else {
          location.helloHome.execute(settings."userHomePhrases${usedSlot}")
        }
      }
      if(settings."burnCode${usedSlot}") {
        locks.deleteCode(codeData.usedCode)
        runIn(60*2, doPoll)
        message += ".  Now burning code."
      }
      send(message)
    }
  }
  if (homePhrases) {
    performActions(evt)
  }
}

def performActions(evt) {
  if(evt.value == "unlocked" && evt.data) {
    def codeData = new JsonSlurper().parseText(evt.data)
    if(enabledUsersArray().contains(codeData.usedCode) || isManualUnlock(codeData)) {
      // Global Hello Home
      if (noRunPresence && doRunPresence == null) {
        if (!anyoneHome(noRunPresence)) {
          location.helloHome.execute(homePhrases)
        }
      } else if (doRunPresence && noRunPresence == null) {
        if (anyoneHome(doRunPresence)) {
          location.helloHome.execute(homePhrases)
        }
      } else if (doRunPresence && noRunPresence) {
        if (anyoneHome(doRunPresence) && !anyoneHome(noRunPresence)) {
          location.helloHome.execute(homePhrases)
        }
      } else {
       location.helloHome.execute(homePhrases)
      }
    }
  }
}


def revokeDisabledUsers() {
  def array = []
  disabledUsersSlotArray().each { slot ->
    array << ["code${slot}", ""]
  }
  def json = new groovy.json.JsonBuilder(array).toString()
  if (json != '[]') {
    locks.updateCodes(json)
    runIn(60*2, doPoll)
  }
}

def doPoll() {
  // this gets codes if custom device is installed
  if (!allCodesDone()) {
    state.error_loop_count = state.error_loop_count + 1
  }
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
  def json = new groovy.json.JsonBuilder(array).toString()
  if (json != '[]') {
    locks.updateCodes(json)
    runIn(60*2, doPoll)
  }
}
def revokeAccess() {
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
  if (settings."burnCode${slot}" && state."userState${slot}".usage > 0) {
    return false
  } else {
    // not a burn code / not yet used
    return true
  }
}

def pollCodeReport(evt) {
  //log.debug "Door Manager: pollCodeReport"
  def active = isAbleToStart()
  def codeData = new JsonSlurper().parseText(evt.data)
  def numberOfCodes = codeData.codes
  def userSlots = userSlotArray()

  def array = []
  (1..maxUsers).each { user->
    def code = codeData."code${user}"
    def usedSlot = usedUserSlot(user)
    if (active) {
      if (userIsEnabled(usedSlot) && isActiveBurnCode(usedSlot)) {
        if (code == settings."userCode${usedSlot}") {
          // Code is Active, We should be active. Nothing to do
        } else {
          // Code is incorrect, We should be active.
          array << ["code${user}", settings."userCode${usedSlot}"]
        }
      } else {
        if (code != '') {
          // Code is set, user is disabled, We should be disabled.
          array << ["code${user}", ""]
        } else {
          // Code is not set, user is disabled. Nothing to do
        }
      }
    } else {
      if (code != '') {
        // Code is set, We should be disabled.
        array << ["code${user}", ""]
      } else {
        // Code is not active, We should be disabled. Nothing to do
      }
    }
  }
  def i = 0
  def currentLockNumber = 0
  def currentLock = [:]
  locks.each { lock->
    i = i + 1
    if (lock.id == evt.deviceId) {
      currentLock = lock
      currentLockNumber = i
    }
  }
  def json = new groovy.json.JsonBuilder(array).toString()
  if (json != '[]') {
    runIn(60*2, doPoll)

    //Lock is in an error state
    state."lock${currentLockNumber}".error_loop = true
    def error_number = state.error_loop_count + 1
    if (error_number <= 10) {
      log.debug "sendCodes fix is: ${json} Error: ${error_number}/10"
      currentLock.updateCodes(json)
    } else {
      log.debug "kill fix is: ${json}"
      currentLock.updateCodes(json)
      json = new JsonSlurper().parseText(json)
      def n = 0
      json.each { code ->
        n = code[0][4..-1].toInteger()
        def usedSlot = usedUserSlot(n)
        def name = settings."userName${usedSlot}"
        log.debug "disable: ${n}"
        if (state."userState${usedSlot}".enabled) {
          state."userState${usedSlot}".enabled = false
          send("Controller failed to set code for ${name}")
        }
      }
    }
  } else {
    state."lock${currentLockNumber}".error_loop = false
    if (allCodesDone) {
      lockErrorLoopReset()
    } else {
      runIn(60, doPoll)
    }
  }
}

def allCodesDone() {
  def i = 0
  def codeComplete = true
  locks.each { lock->
    i++
    if (state."lock${i}".error_loop == true) {
      codeComplete = false
    }
  }
  return codeComplete
}

private anyoneHome(sensors) {
  def result = false
  if(sensors.findAll { it?.currentPresence == "present" }) {
    result = true
  }
  result
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
  if (sendevent) {
    sendNotificationEvent(msg)
  }
}

def checkRecentUser() {
  if ( state.cookieCurrentUser ) {
    debugLog("setupPage: ${state.cookieCurrentUser}")
    def id = 0
    def code = null
    def enabled = false
    debugLog("User ${state.cookieCurrentUser} recently updated")
    if ( ! state.cookieCurrentUser.isNumber() ) {
    	debugLog("${state.cookieCurrentUser} is NOT a number")
        id = state.cookieCurrentUser.toInteger()
    } else {
        debugLog("${state.cookieCurrentUser} IS a number")
        id = state.cookieCurrentUser
    } 
    if ( id > 0 ) {
	debugLog("Processing slot #$id")
    	//processUserCode(id,settings."userCode${id}",settings."userEnabled${id}",settings."locks${id}")
    }
  }
}

def setMaxUserDefault() {
    debugLog("Checking maxUsers")
    if ( settings?.endSlot > 0 ) {
        debugLog("endSlot is set at ${settings.endSlot}")
    	state.endSlot = settings.endSlot
    } else if ( settings?.maxUsers > 0 ) {
        debugLog("maxUsers is set at ${settings.maxUsers}")
    	state.endSlot = settings.maxUsers
    }
}

def debugLog(msg) {
	if ( settings?.enableDebug ) {
		log.debug "USER-LOCK-MANAGER: $msg"
    }
}

