/**
 *  Copyright 2015 SmartThings
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
 *  Power Allowance
 *
 *  Author: SmartThings
 */
definition(
    name: "Return Home",
    namespace: "zeeMonkeez",
    author: "zeeMonkeez",
    description: "Turn on lights when coming home.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png",
    oauth: true
)

preferences {
    page(name: "firstPage", title: "Settings",
         nextPage: "otherSettings", uninstall: true, install: false) {
		section("When someone comes home ...") {
		    input "presence1", "capability.presenceSensor", title: "Who?", multiple: true, required: true
		}
    	section("Turn on these switches ...") {
			input "theSwitch", "capability.switch", multiple: true, title: "What?"
		}
    	section("Set dimmers to this value:") {
			input "dimmerV", "number", range: "0..99", title: "What level?", defaultValue: 50
		}
		section("Reset to old state how many minutes later?") {
			input "minutesLater", "number", title: "When?", defaultValue: 5
		}
        section("Night Mode:") {
			input "onlyAtNight", "bool", title: "Only turn on between sunset and sunrise?", defaultValue: true
        }
        section("Save energy:") {
            input "alwaysTurnOff", "bool", title: "Turn switches off even if they had been on?", defaultValue: false
        }
        section([mobileOnly:true]) {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)", required: false
        }

    }
    page(name: "otherSettings", title:"Other Settings", uninstall: true, install: true, nextPage:"firstPage") {
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
	}

}

def initialize() {
	subscribe(presence1, "presence.present", presence)
	subscribe(location, "position", locationPositionChange)
	subscribe(location, "sunriseTime", sunriseSunsetTimeHandler)
	subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)
	astroCheck()

}



def installed() {
	log.debug "Installed with settings: ${settings}"
   
    initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	unschedule()
	initialize()
	
}

def switchOnHandler(evt) {
	log.debug "Switch ${theSwitch} turned: ${evt.value}"
	def delay = minutesLater * 60
	log.debug "Turning off in ${minutesLater} minutes (${delay}seconds)"
	runIn(delay, turnOffSwitch)
}

def turnOffSwitch() {
	theSwitch.off()
}

def presence(evt)
{
    def map = [:]
    if (!onlyAtNight || enabled()) {
    theSwitch?.each {
		map[it.id] = [switch: it.currentSwitch, level: it.currentLevel]
        def isDimmer = it.capabilities.any{cap ->
        	cap.name == "Switch Level"}
        if (isDimmer) {
        	it.setLevel(dimmerV)
            log.info "Set ${it.displayName} to level $dimmerV"
        }
        it.on()
        log.info "Turn ${it.displayName} on"
        
    }
    state.beforeState = map
    def delay = minutesLater * 60
    log.info "Turn off in $minutesLater min"
	runIn(delay, restoreState)
    
    def devNames = theSwitch.collect({it.displayName}).join(', ')
    log.debug "recipients configured: $recipients"

    def message = "${evt.displayName} arrived home, turning on $devNames!"
    sendMessage(message)
	}
    else {
    	log.info "Presence detected, but not at night."
    }
}

def restoreState() {
	def map = state.beforeState
    if (alwaysTurnOff) {
    	log.info "Turn everything off"
    	theSwitch.off()
    }
    else {
		theSwitch?.each {
    	  	def value = map[it.id]
            log.debug value
            def level = value.level
				if (level) {
					log.info "setting $it.displayName level to $level"
					it.setLevel(level)
				}
            if (value?.switch == "off") {
            	log.info "Turning off ${it.displayName}"
            	it.off()
            }
    	}
    }
}

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

private enabled() {
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





