/**
 *  Example of passing params via href element to a dynamic page
 *
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
 */
definition(
    name: "href params example",
    namespace: "smartthings",
    author: "SmartThings",
    description: "passing params via href element to a dynamic page",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


// ========================================================
// PAGES
// ========================================================

preferences {
    page(name: "firstPage")
    page(name: "addElemPage")
	page(name: "editElemPage")
}

def firstPage(params) {
	log.debug "My params now: $params"
    if (params?.dev) {
    	saveDeviceChoice(params.dev)
    }
   	
    dynamicPage(name: "firstPage", uninstall: true) {
    
        
        if (state.devices) {
        	state.devices.each { did, dv ->
        	section {
            paragraph "We have dev $did with level $dv.level"
            href(
            	name: "Delete$did",
                title: "Edit",
                page: "editElemPage",
                description: "Edit this device",
                params: [editDev: did]
            )
            }
        }
        }
        
        section("Add new element") {
        	href(
            	name: "toaddElemPage",
                page: "addElemPage",
                title: "Add a new element",
                description: "Add a new element"

                )
        }
    }
}



def addElemPage() {
	dynamicPage(name: "addElemPage", uninstall: true, install:false) {
    	def hasDimmer = checkIfDimmer(theSwitches)
    	section("Turn on these switches ...") {
        	paragraph "Select dimmers individually to set different levels"
			input "theSwitches", "capability.switch", multiple: true, title: "What?", required: false, submitOnChange: true
		}
        if (hasDimmer) {
        
        log.debug "Got a dimmer"
        	section("Set to level (1..99)") {
	        	input "theLevel", "number", range: "1..99", title: "What level?", defaultValue: 50, submitOnChange: true
            }
        }
        if (theSwitches) {
        def devMap = [dev: makeDevMap(theSwitches, (hasDimmer ? theLevel : null))]
        log.debug "Got something in $theSwitches: $devMap"
        
        section("Done adding") {
        href(
            	name: "toSaveAddedElem",
                title: "Save new switches",
                page: "firstPage",
                description: "Save added element",
                params: devMap
        )
        }
}
 
    }
}

def editElemPage(params) {
	log.debug "editElemPage params now: $params"
 	def editDevId = params?.editDev
    if (editDevId) {
    //	deleteDeviceID(deleteDevId)
    }
   	
    dynamicPage(name: "editElemPage", uninstall: true) {
        def sectionTitle
        if (editDevId) {
        capability.switch
        	sectionTitle = "Successfully removed device"
        }
        else {
        	sectionTitle = "Return"
        }
        
        section(sectionTitle) {
        	href(
            	name: "toFirstPage",
                page: "firstPage",
                description: "Return"
                )
        }
    }
}



// ========================================================
// HELPERS
// ========================================================

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


def installed() {
    log.debug "Installed with settings: ${settings}"
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