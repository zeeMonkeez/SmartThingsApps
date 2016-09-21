/**
*  Return Home On Steroids
*
*  Copyright 2016 Jonas B. Zimmermann
*
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*/
definition(
name: "Return Home On Steroids",
namespace: "zeeMonkeez",
author: "Jonas Zimmermann",
description: "Upon detection of presence, turn on switches and dimmers. Allows switches to be only triggered at night, only if nobody had been home, and to set a delay for turning off. Settings can be overridden per device.",
category: "Convenience",
iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


// ========================================================
// PAGES
// ========================================================

preferences {
	page(name: "mainPage")
	page(name: "editElemPage")
	page(name: "extraOptionsPage")
}

def mainPage(params) {
	setupInit()
	log.debug "Main Page"

	dynamicPage(name: "mainPage", uninstall: state.installed, install: true) {
		section("When someone comes home ...") {
			input "presenceDevices", "capability.presenceSensor", title: "Who?", multiple: true, required: true
		}

		section("Turn on these devices:") {
			input "switches", "capability.switch", multiple: true, title: "What?", required: true
		}
		section("Set dimmers to this default value:") {
			input "switchLevel", "number", range: "1..99", title: "What level?", defaultValue: 50, required: true
		}
		section("Reset to old state how many minutes later?") {
			paragraph "State will not be reset if equal to or less than 0"
			input "switchOffDelay", "number", title: "delay in minutes", defaultValue: 5, required: true
		}
		section("Night Mode:") {
			input name: "onlyAtNight", type: "enum", title: "Only turn on between sunset and sunrise?", options: ["Yes", "No"], required: true, defaultValue: "Yes"
		}
		section("Save energy:") {
			input name: "switchAlwaysTurnOff", type: "enum", title: "Turn switches off even if they had been on?", options: ["Yes", "No"], defaultValue: "No", required: true
		}
		section("Only activate switch if nobody had been home at all.") {
			input name: "onlyIfHouseEmpty", type: "enum", title:"Perform only when house empty", defaultValue: "No", required: true, options: ["Yes", "No"]
		}

		section("Edit switch details") {
			href(
				name: "toEditElemPage",
				title: "Edit details ...",
				page: "editElemPage",
				description: "Select levels for dimmers and other settings",
				state: visitedPage("editElemPage")
			)

		}
		section("Other settings") {
			href(
				name: "toExtraOptionsPage",
				title: "Other settings ...",
				page: "extraOptionsPage",
				description: "Other settings",
				state: visitedPage("extraOptionsPage")
			)

		}
		section([mobileOnly:true]) {
			label title: "Assign a name", required: false
			mode title: "Set for specific mode(s)", required: false
		}
	}
}

def editElemPage() {
	state.visited["editElemPage"] = true
	log.debug "editElemPage params now: $settings.switches"
	dynamicPage(name: "editElemPage", uninstall: false) {
		settings.switches?.each {
			log.debug "Make section for $it"
			makeDeviceSection(it)
		}

		section("Save Settings ...") {
			href(
				name: "toMainPage",
				title: "Save and Return",
				page: "mainPage",
				description: "Save settings and return to main page"
			)
		}
	}
}

def extraOptionsPage() {
	state.visited["extraOptionsPage"] = true
	log.debug "extraOptionsPage"

	dynamicPage(name: "extraOptionsPage") {
		section ("Sunset offset (optional)...") {
			input "sunsetOffsetValue", "text", title: "HH:MM", required: false
			input "sunsetOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
		}
		section ("Sunrise offset (optional)...") {
			input "sunriseOffsetValue", "text", title: "HH:MM", required: false
			input "sunriseOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
		}

		section ("Zip code (optional, defaults to location coordinates when location services are enabled)...") {
			input "zipCode", "text", title: "Zip code", required: false
		}

		section("Via a push notification and/or an SMS message"){
			input("recipients", "contact", title: "Send notifications to") {
				input "phone", "phone", title: "Enter a phone number to get SMS", required: false
				paragraph "If outside the US please make sure to enter the proper country code"
				input "pushAndPhone", "enum", title: "Notify me via Push Notification", required: false, options: ["Yes", "No"]
			}
		}
		section("Save Settings ...") {
			href(
				name: "toMainPage",
				title: "Save and Return",
				page: "mainPage",
				description: "Save settings and return to main page"
			)
		}

	}
}


// ========================================================
// HELPERS
// ========================================================
def makeDeviceSection(dev) {
	log.debug "Making device section for ${dev.displayName}"
	return section(dev.displayName) {
		if (dev.hasCapability("Switch Level")) {
			input "switchLevel_${dev.id}", "number", range: "1..99", title: "What level?", required: false
			paragraph "Set dimmer to level 1..99"
		}

		input "switchOffDelay_${dev.id}", "number", title: "Delay in Minutes", required: false
		paragraph "Reset to old state how many minutes after arrival? State will not be reset if equal to or less than 0"

		input "onlyAtNight_${dev.id}", "enum", title: "After dark only?", options: ["Yes", "No"], required: false
		paragraph "Only trigger between sunset and sunrise."

		input "switchAlwaysTurnOff_${dev.id}", "enum", title: "Turn off even if switch had been on?", options: ["Yes", "No"], required: false
		paragraph "If “yes”, the previous state will be ignored and the switch will be turned off after the delay."

		input "onlyIfHouseEmpty_${dev.id}", "enum", title:"Perform only when house empty", required: false, options: ["Yes", "No"]
		paragraph("If “yes” and the current presence sensor is not the only one “present”, the device will not turn on.")

	}
}

def visitedPage(page) {
	state.visited[page] ? "complete": ""
}

def parseSettingsForDevice(dev) {
	def devId = dev.id
	state.devices[devId] = state.devices[devId]?: [:]
	state.devices[devId].isDimmer = dev.hasCapability("Switch Level")
	state.devices[devId].switchLevel = state.devices[devId].isDimmer ? settings["switchLevel_${devId}"] ?: settings.switchLevel : null
	state.devices[devId].switchOffDelay = settings["switchOffDelay_${devId}"] ?: settings.switchOffDelay
	state.devices[devId].onlyAtNight = (settings["onlyAtNight_${devId}"] ?: settings.onlyAtNight) == 'Yes'
	state.devices[devId].switchAlwaysTurnOff = (settings["switchAlwaysTurnOff_${devId}"] ?: settings.switchAlwaysTurnOff) == 'Yes'
	state.devices[devId].onlyIfHouseEmpty = (settings["onlyIfHouseEmpty_${devId}"] ?: settings.onlyIfHouseEmpty) == 'Yes'
    log.debug "settings.onlyAtNight ${settings.onlyAtNight}"
    log.debug "settings.switchAlwaysTurnOff ${settings.switchAlwaysTurnOff}"
    log.debug "settings.onlyIfHouseEmpty ${settings.onlyIfHouseEmpty}"


    def oihe = settings.onlyIfHouseEmpty
    def oihed = settings["onlyIfHouseEmpty_${devId}"]
    log.debug "$dev.displayName only if house empty: $oihe, $oihed -> ${state.devices[devId].onlyIfHouseEmpty}"
	state.devices[devId].hasBeenTriggered = false
	state.devices[devId].oldLevel = null
	state.devices[devId].oldSwitch = 'off'
	return state.devices[devId]
}


// ========================================================
// HANDLERS
// ========================================================

private def setupInit() {
	state.visited = state.visited ?: [:]
	if (state.installed == null) {
		state.installed = false
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	state.installed = true
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	state.devices = state?.devices ?: [:]
	settings.switches?.each {parseSettingsForDevice(it)}
	subscribe(presenceDevices, "presence.present", presence)
	subscribe(location, "position", locationPositionChange)
	subscribe(location, "sunriseTime", sunriseSunsetTimeHandler)
	subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)
	astroCheck()

}


def presence(evt) {
	// test if anybody was at home at all, or not. If there was, and option was set,
	// ignore event
	def someoneAtHome = presenceDevices?.find{
		it.id == evt.device.id ? false : it.currentPresence == 'present'
	}
	def atNight = isAtNight()
	def switchedSwitches = []
	settings.switches?.each { dev ->
		if (triggerDevice(dev, atNight: atNight, someoneAtHome: someoneAtHome)) {
			switchedSwitches << dev.displayName
		}
	}
	def message
    if (switchedSwitches.size() > 0) {
		def devNames = switchedSwitches.join(', ')
		message = "${evt.displayName} arrived at home, turning on $devNames!"
    }
    else {
    	message = "${evt.displayName} arrived at home, but no devices set to be turned on!"
    }
    log.debug "recipients configured: $recipients"
	sendMessage(message)
	sendNotificationEvent(message)
    
}

def triggerDevice(Map envinfo, dev) {
	def devState = state.devices[dev.id]
    def callScheduleList = []
	if (!envinfo.atNight && devState.onlyAtNight) {
		log.info "$dev.displayName only turns on at night."
		return false
	}
	if (envinfo.someoneAtHome && devState.onlyIfHouseEmpty) {
		log.info "$dev.displayName only turns on if house is empty."
		return false
	}
	if (!devState.hasBeenTriggered) {
		state.devices[dev.id].oldSwitch = dev.currentSwitch
		if (devState.isDimmer) {
			state.devices[dev.id].oldLevel = dev.currentLevel
			dev.setLevel(devState.switchLevel)
			log.info "Set ${dev.displayName} to level $devState.switchLevel"
		}
		dev.on()
		log.info "Turn $dev.displayName on."
		log.debug "Saving Old State ${state.devices[dev.id].oldSwitch}, ${state.devices[dev.id].oldLevel}."
		state.devices[dev.id].hasBeenTriggered = true
	}
	if (devState.switchOffDelay > 0) {
        if (state.devices[dev.id].oldLevel) {
        	callScheduleList << ['setLevel', [state.devices[dev.id].oldLevel]]
        }
    	if (state.devices[dev.id].switchAlwaysTurnOff) {
        	callScheduleList << ['off']
        }
        else {
        	callScheduleList << state.devices[dev.id].oldSwitch
        }
		def delay = devState.switchOffDelay * 60
		addToSchedule(dev.id, [methods: callScheduleList, inSeconds: delay])
		log.info "Turn off ${dev.displayName} in ${devState.switchOffDelay} min, will perform these actions: $callScheduleList"
	}
	return true
}


// Location + Sunset

def locationPositionChange(evt) {
	log.trace "locationChange()"
	astroCheck()
}

def sunriseSunsetTimeHandler(evt) {
	state.lastSunriseSunsetEvent = now()
	log.debug "SmartNightlight.sunriseSunsetTimeHandler($app.id)"
	astroCheck()
}

def astroCheck() {
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)
	state.riseTime = s.sunrise.time
	state.setTime = s.sunset.time
	log.debug "rise: ${new Date(state.riseTime)}($state.riseTime), set: ${new Date(state.setTime)}($state.setTime)"
}

private getSunriseOffset() {
	sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
}

private getSunsetOffset() {
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}

private isAtNight() {
	def result
	def t = now()
	result = t < state.riseTime || t > state.setTime
	result
}


private sendMessage(msg) {
	Map options = [:]

	log.debug "pushAndPhone:$pushAndPhone, '$msg'"

	if (location.contactBookEnabled) {
		sendNotificationToContacts(msg, recipients, options)
	} else {
		if (phone) {
			options.phone = phone
			if (pushAndPhone != 'No') {
				log.debug 'Sending push and SMS'
				options.method = 'both'
			} else {
				log.debug 'Sending SMS'
				options.method = 'phone'
			}
		} else if (pushAndPhone != 'No') {
			log.debug 'Sending push'
			options.method = 'push'
		} else {
			log.debug 'Sending nothing'
			options.method = 'none'
		}
		sendNotification(msg, options)
	}
}

// Implementation of our own scheduling system
// It allows overwriting of scheduled events, for individual devices

def addToSchedule(did, Map pmap) {
	state.schedule = state.schedule ?: [:]
	state.schedule[did] = pmap
	def t_now = now()
	def ptime = pmap.inSeconds?: (pmap.time? (pmap.time-t_now)/1e3 : 60)
	state.schedule[did].time = pmap.time ?: t_now + ptime*1e3

	// add 2 seconds to ensure handler will be called after event
	runIn(ptime + 2, scheduleHandler, [overwrite: false])
	log.info "Adding task $pmap to scheduler for $did"
}


def scheduleHandler(val) {
	state.schedule = state.schedule ?: [:]
	log.info "Working handler"
	log.debug "Pre-state.schedule: $state.schedule"
	def t_now = now()
	log.debug "Time now: $t_now"

	def due = state.schedule.findAll {it.value?.time < t_now}
	log.debug "Tasks due: $due"
	due.each {
		def mpar = it.value?.methods ?: [it.value?.method]
        log.debug "mpar $mpar"
		if (mpar instanceof String) {
			mpar = [mpar]
		}
        log.debug "mpar2 $mpar"
		def device = settings.switches.find {sw -> sw.id == it.key}

		mpar.each {mpa ->
	        log.debug "mpa $mpa"
			def m = mpa[0]
			def p = mpa[1]?:[]
			device?."${m}"(*p)
            def message = "Calling ${device.displayName}.${m}(${p})"
            log.debug message
            sendNotificationEvent(message)
		}
		state.schedule.remove(it.key)
	}
	log.debug "state.schedule is now $state.schedule"
}
