// IAccessibilityService.aidl
package com.hika.core.aidl.accessibility;

import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;

import com.hika.core.aidl.accessibility.IProjectionSuccess;
import com.hika.core.aidl.accessibility.IReply;
import com.hika.core.aidl.accessibility.DetectedObject;
import com.hika.core.aidl.accessibility.ParcelableText;
import com.hika.core.aidl.accessibility.ParcelableMotion;

// from accessibility service to main app
interface IAccessibilityService {
    boolean isProjectionStarted();
    oneway void stopProjection();
    oneway void setListenerOnProjectionSuccess(in IProjectionSuccess iProjectionSuccess);
    Point getScreenSize();

    oneway void setListenerOnActivityClassName(
        in String className,
        long maximalMillis,
        in IReply iReply
    );
    oneway void clearClassNameListeners();

    DetectedObject[] getObjectInRegion(in String detectorName, in @nullable Rect region); // implement later
    oneway void cancelAllObjectGetting();
    ParcelableText getTextInRegion(in @nullable Rect region);
    oneway void cancelAllTextGetting();

    oneway void click(in PointF point, long startTime, long duration);
    oneway void swipe(in PointF pointFrom, in PointF pointTo, long startTime, long duration);
    oneway void performAction(in int action);   //: AccessibilityService.GLOBAL_ACTION_XXX

    ParcelableMotion[] recordMotions();
    oneway void stopMotionRecording();
}