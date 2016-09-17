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
description: "Returning home made cool",
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
		section("Turn on these devices:") {
			input "switches", "capability.switch", multiple: true, title: "What?", required: true
		}
		section("Set dimmers to this default value:") {
			input "switchLevel", "number", range: "1..99", title: "What level?", defaultValue: 50
		}
		section("Reset to old state how many minutes later?") {
			paragraph "State will not be reset if equal to or less than 0"
			input "switchOffDelay", "number", title: "delay in minutes", defaultValue: 5, required: true
		}
		section("Night Mode:") {
			input "onlyAtNight", "enum", title: "Only turn on between sunset and sunrise?", defaultValue: "Yes", options: ["Yes", "No"]
		}
		section("Save energy:") {
			input "switchAlwaysTurnOff", "enum", title: "Turn switches off even if they had been on?", options: ["Yes", "No"], defaultValue: "No"
		}
		section("Only activate switch if nobody had been home at all.") {
			input "onlyIfHouseEmpty", "enum", title:"Perform only when house empty", defaultValue: "No", required: true, options: ["Yes", "No"]
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
			input "switchLevel_${dev.id}", "number", range: "1..99", title: "What level?", defaultValue: switchLevel, required: false
			paragraph "Set dimmer to level 1..99"
		}

		input "switchOffDelay_${dev.id}", "number", title: "Delay in Minutes", defaultValue: switchOffDelay, required: false
		paragraph "Reset to old state how many minutes after arrival? State will not be reset if equal to or less than 0"

		input "onlyAtNight_${dev.id}", "enum", title: "After dark only?", defaultValue: onlyAtNight, options: ["Yes", "No"], required: false
		paragraph "Only trigger between sunset and sunrise."

		input "switchAlwaysTurnOff_${dev.id}", "enum", title: "Turn off even if switch had been on?", defaultValue: switchAlwaysTurnOff, options: ["Yes", "No"], required: false
		paragraph "If “yes”, the previous state will be ignored and the switch will be turned off after the delay."

	}
}

def visitedPage(page) {
	state.visited[page] ? "complete": ""
}

		def saveDeviceChoice(devMap) {
			state.devices = state?.devices ?: [:]
			devMap?.each{ id, v ->
				state.devices[id] = v
			}
			//    state.devices[devMap.id] = [level: devMap.level]
			log.debug "adding device to list: $state.devices"
		}

		def deleteDeviceID(devID) {
			state.devices = state?.devices ?: [:]
			state.devices.remove(devID)
		}

		def checkIfDimmer(dev) {
			log.debug "Checking device(s): $dev"
			//log.debug dev.getClass() //(it instanceof Device) &&
			return   dev?.any{it.hasCapability("Switch Level")}
		}
		def makeDevMap(devs, lvl) {
			log.debug "Make map from $devs, $lvl"
			log.debug devs instanceof physicalgraph.app.DeviceWrapper
			if (devs instanceof physicalgraph.app.DeviceWrapper) {
				devs = [devs]
			}
			def myMap = devs.collectEntries({dev -> [(dev.id): [level: dev.capabilities.any{cap ->cap.name == "Switch Level"} ? lvl : null]]})
			log.debug "Finished map:  $myMap"
			return myMap
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
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
}

// TODO: implement event handlers
