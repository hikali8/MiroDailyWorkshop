// IASReceiver.aidl
package com.hika.core.aidl.accessibility;

// Declare any non-default types here with import statements
import com.hika.core.aidl.accessibility.IAccessibilityService;

// from main app to accessibility service
interface IASReceiver {
    oneway void onASConnected(in IAccessibilityService iAccessibilityService);
}