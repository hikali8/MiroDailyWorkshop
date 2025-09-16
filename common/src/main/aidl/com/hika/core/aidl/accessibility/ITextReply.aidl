// ITextReply.aidl
package com.hika.core.aidl.accessibility;

// Declare any non-default types here with import statements
import com.hika.core.aidl.accessibility.ParcelableText;

// report informations
interface ITextReply {
    oneway void reply(in ParcelableText parcelizedMKText);
}