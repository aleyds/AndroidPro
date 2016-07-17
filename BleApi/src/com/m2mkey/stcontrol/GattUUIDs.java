/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m2mkey.stcontrol;

import java.util.HashMap;
import java.util.UUID;

/**
 * This class is used to lookup the names of our GATT attributes.
 */
public class GattUUIDs {
	private static HashMap<UUID, String> attributes = new HashMap<UUID, String>();
    
	public static final int CHAR_MAXIMUM_BYTES = 20;
	
    // M2Mkey UUID portion
    private static String UUID_PREFIX = "0000";
    private static String UUID_POSTFIX = "-0000-1000-8000-00805f9b34fb";
    
    // Descriptors
    public static UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString(UUID_PREFIX + "2902" + UUID_POSTFIX);

    // Device Info Service
    public static UUID SVC_DEVICE_INFO = UUID.fromString(UUID_PREFIX + "180a" + UUID_POSTFIX);
    public static UUID INFO_HW_VERSION = UUID.fromString(UUID_PREFIX + "2a27" + UUID_POSTFIX);
    public static UUID INFO_SW_VERSION = UUID.fromString(UUID_PREFIX + "2a28" + UUID_POSTFIX);
    public static UUID CHAR_MANUFACTURER_NAME_STRING = UUID.fromString(UUID_PREFIX + "2a29" + UUID_POSTFIX);
    
    // iLock v2
    public static UUID SVC_ILOCK_V2 = UUID.fromString(UUID_PREFIX + "1851" + UUID_POSTFIX);
    public static UUID SVC_ILOCK_V2_ENC = UUID.fromString(UUID_PREFIX + "1852" + UUID_POSTFIX);
    public static UUID CHAR_SECURE_CMD = UUID.fromString(UUID_PREFIX + "2a3a" + UUID_POSTFIX);
    public static UUID CHAR_STATUS = UUID.fromString(UUID_PREFIX + "2a3b" + UUID_POSTFIX);
    public static UUID CHAR_NONCE = UUID.fromString(UUID_PREFIX + "2a3c" + UUID_POSTFIX);
    public static UUID CHAR_SESSION_START = UUID.fromString(UUID_PREFIX + "2a3d" + UUID_POSTFIX);
    
    
    static {
        // Device Info Service
        attributes.put(SVC_DEVICE_INFO, "Device Information Service");
        attributes.put(CHAR_MANUFACTURER_NAME_STRING, "Manufacturer Name String");
        
        // iLock v2 Service
        attributes.put(SVC_ILOCK_V2, "iLock V2 Service");
        attributes.put(CHAR_NONCE, "Nonce");
        attributes.put(CHAR_SESSION_START, "Session start");
        attributes.put(CHAR_SECURE_CMD, "Secure command");
        attributes.put(CHAR_STATUS, "Status");
    }

    public static String lookupUUID(UUID uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
