// IProjectionSuccess.aidl
package com.hika.core.aidl.accessibility;

// Declare any non-default types here with import statements
import android.graphics.Point;

// multi-called
interface IProjectionSuccess {
    oneway void onProjectionSuccess();
}