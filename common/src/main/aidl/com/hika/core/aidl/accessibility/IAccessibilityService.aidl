// IAccessibilityService.aidl
package com.hika.core.aidl.accessibility;

// Declare any non-default types here with import statements
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;

import com.hika.core.aidl.accessibility.IProjectionSuccess;
import com.hika.core.aidl.accessibility.IReply;
import com.hika.core.aidl.accessibility.ITextReply;
import com.hika.core.aidl.accessibility.ParcelableText;

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

//    oneway void getObjectInRegion(in @nullable Rect region); // implement later
//    oneway void cancelObjectGetting();
    oneway void getTextInRegion(in @nullable Rect region, in ITextReply iText);
    oneway void cancelAllTextGetting();
//    ParcelableText getTextInRegionSync(in @nullable Rect region);

    oneway void click(in PointF point, long startTime, long duration);
    oneway void swipe(in PointF pointFrom, in PointF pointTo, long startTime, long duration);
    oneway void performAction(in int action);   //: AccessibilityService.GLOBAL_ACTION_XXX
}