metadata {
    definition(name: "Hue Scene Device", namespace: "community", author: "Joe Rosiak") {
        capability "Switch"
        command "setHueScene", ["string", "string", "string"]
    }
}

def installed() {
    log.debug "Hue Scene Device Installed"
}

def updated() {
    log.debug "Hue Scene Device Updated"
}

def on() {
    log.debug "Switch On"
    // Optionally implement default on behavior
}

def off() {
    log.debug "Switch Off"
    // Optionally implement default off behavior
}

def setHueScene(zoneName, sceneName, onOrOff) {
    log.debug "Setting Hue Scene: Zone: ${zoneName}, Scene: ${sceneName}, On/Off: ${onOrOff}"
    
    // Send the command to the Hue Bridge through the parent app
    parent.sendHueCommand(zoneName, sceneName, onOrOff)
}