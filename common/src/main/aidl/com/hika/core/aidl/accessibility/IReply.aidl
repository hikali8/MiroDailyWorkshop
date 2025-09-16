// IReply.aidl
package com.hika.core.aidl.accessibility;

// Declare any non-default types here with import statements

// report informations
interface IReply {
    oneway void reply(boolean isSure);
}
