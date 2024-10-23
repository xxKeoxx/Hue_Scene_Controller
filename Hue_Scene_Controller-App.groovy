definition(
    name: "Hue Scene Control",
    namespace: "community",
    author: "Joe Rosiak",
    description: "Send API commands to Philips Hue",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
    section("Hue Bridge") {
        input "hueBridgeIp", "text", title: "Hue Bridge IP", required: true
        input "hueUsername", "password", title: "Hue API Username", required: true
    }
    section("Device Name") {
        input "deviceName", "text", title: "Device Name", required: true, defaultValue: "Hue Scene Device"
    }
}

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
    log.debug "Initializing Hue Scene Control"
    createChildDevice()
}

def uninstalled() {
    log.debug "Uninstalling App and Removing Child Device"
    deleteChildDevice()
}

def createChildDevice() {
    def existingDevice = getChildDevice("hueSceneDevice_${app.id}")
    
    if (!existingDevice) {
        try {
            def childDevice = addChildDevice("community", "Hue Scene Device", "hueSceneDevice_${app.id}", null, [
                label: settings.deviceName,
                name: settings.deviceName,
                isComponent: false
            ])
            log.debug "Child Device created with ID: hueSceneDevice_${app.id}"
        } catch (Exception e) {
            log.error "Error creating child device: ${e.message}"
        }
    } else {
        log.debug "Child Device already exists"
    }
}

def deleteChildDevice() {
    def childDevice = getChildDevice("hueSceneDevice_${app.id}")
    
    if (childDevice) {
        try {
            deleteChildDevice("hueSceneDevice_${app.id}")
            log.debug "Child Device with ID hueSceneDevice_${app.id} deleted"
        } catch (Exception e) {
            log.error "Error deleting child device: ${e.message}"
        }
    } else {
        log.debug "No child device to delete"
    }
}

// Helper to get zone ID based on name
def getZoneIdByName(zoneName) {
    def url = "http://${settings.hueBridgeIp}/api/${settings.hueUsername}/groups"
    
    try {
        httpGet([uri: url]) { resp ->
            def zones = resp.data
            def zone = zones.find { it.value.name == zoneName }
            return zone ? zone.key : null
        }
    } catch (Exception e) {
        log.error "Error fetching zones: ${e.message}"
        return null
    }
}

// Helper to get scene ID based on name
def getSceneIdByName(sceneName, zoneId) {
    def url = "http://${settings.hueBridgeIp}/api/${settings.hueUsername}/scenes"
    
    try {
        httpGet([uri: url]) { resp ->
            def scenes = resp.data
            def scene = scenes.find { it.value.name == sceneName && it.value.group == zoneId }
            return scene ? scene.key : null
        }
    } catch (Exception e) {
        log.error "Error fetching scenes: ${e.message}"
        return null
    }
}

// Send a command to the Hue API
def sendHueCommand(zoneName, sceneName, onOrOff) {
    def zoneId = getZoneIdByName(zoneName)
    
    if (!zoneId) {
        log.error "Zone with name ${zoneName} not found."
        return
    }
    
    if (onOrOff.toLowerCase() == "off") {
        // Turn off the entire zone
        turnOffZone(zoneId)
    } else {
        def sceneId = getSceneIdByName(sceneName, zoneId)
        
        if (!sceneId) {
            log.error "Scene with name ${sceneName} not found for zone ${zoneName}."
            return
        }

        activateScene(zoneId, sceneId)
    }
}

// Activate a scene in a zone
def activateScene(zoneId, sceneId) {
    def url = "http://${settings.hueBridgeIp}/api/${settings.hueUsername}/groups/${zoneId}/action"
    
    def body = [
        scene: sceneId,
        on: true
    ]
    
    def params = [
        uri: url,
        body: body
    ]
    
    try {
        httpPutJson(params) { resp ->
            log.debug "Hue API Response: ${resp.data}"
        }
    } catch (Exception e) {
        log.error "Error sending scene activation command: ${e.message}"
    }
}

// Turn off the entire zone
def turnOffZone(zoneId) {
    def url = "http://${settings.hueBridgeIp}/api/${settings.hueUsername}/groups/${zoneId}/action"
    
    def body = [
        on: false
    ]
    
    def params = [
        uri: url,
        body: body
    ]
    
    try {
        httpPutJson(params) { resp ->
            log.debug "Hue API Response (turn off zone): ${resp.data}"
        }
    } catch (Exception e) {
        log.error "Error sending zone off command: ${e.message}"
    }
}
