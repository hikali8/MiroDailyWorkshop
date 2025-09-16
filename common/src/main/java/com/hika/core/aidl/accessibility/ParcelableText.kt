package com.hika.core.aidl.accessibility

import android.graphics.Point
import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import com.google.mlkit.vision.text.Text
import kotlin.lazy


// 包装最外层的Text类
class ParcelableText(
    val text: String,
    val textBlocks: Array<ParcelableTextBlock>
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.createTypedArray(ParcelableTextBlock.CREATOR) ?: emptyArray()
    )

    constructor(text: Text?) : this(
        text?.text ?: "",
        text?.textBlocks?.run {
            val iter = this.iterator()
            Array(this.size){
                assert(iter.hasNext())
                ParcelableTextBlock(iter.next())
            }
        } ?: emptyArray()
    )

    constructor() : this(
        "",
        emptyArray()
    )

    fun isEmpty() = text.isEmpty()

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(text)
        parcel.writeTypedArray(textBlocks, 0)
    }

    companion object CREATOR : Parcelable.Creator<ParcelableText> {
        override fun createFromParcel(parcel: Parcel) = ParcelableText(parcel)
        override fun newArray(size: Int) = arrayOfNulls<ParcelableText>(size)
    }

    override fun describeContents() = 0
}

// 包装TextBlock
class ParcelableTextBlock : ParcelableTextBase {
    val lines: Array<ParcelableLine>
    val symbols: List<ParcelableSymbol> by lazy {
        lines.flatMap { it.elements.flatMap { it.symbols.asIterable() } }
    }

    constructor(parcel: Parcel) : super(parcel){
        lines = parcel.createTypedArray(ParcelableLine.CREATOR) ?: emptyArray()
    }

    constructor(textBlock: Text.TextBlock) : super(
        textBlock.text,
        textBlock.boundingBox,
        textBlock.cornerPoints,
        textBlock.recognizedLanguage
    ){
        lines = textBlock.lines.run {
            val iter = this.iterator()
            Array(this.size){
                assert(iter.hasNext())
                ParcelableLine(iter.next())
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeTypedArray(lines, 0)
    }

    companion object CREATOR : Parcelable.Creator<ParcelableTextBlock> {
        override fun createFromParcel(parcel: Parcel) = ParcelableTextBlock(parcel)
        override fun newArray(size: Int) = arrayOfNulls<ParcelableTextBlock>(size)
    }
}

// 包装Line
class ParcelableLine : ParcelableTextBase {
    val confidence: Float
    val angle: Float
    val elements: Array<ParcelableElement>

    constructor(parcel: Parcel) : super(parcel){
        confidence = parcel.readFloat()
        angle = parcel.readFloat()
        elements = parcel.createTypedArray(ParcelableElement.CREATOR) ?: emptyArray()
    }

    constructor(line: Text.Line) : super(
        line.text,
        line.boundingBox,
        line.cornerPoints,
        line.recognizedLanguage
    ){
        confidence = line.confidence
        angle = line.angle
        elements = line.elements.run {
            val iter = this.iterator()
            Array(this.size){
                assert(iter.hasNext())
                ParcelableElement(iter.next())
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeFloat(confidence)
        parcel.writeFloat(angle)
        parcel.writeTypedArray(elements, 0)
    }

    companion object CREATOR : Parcelable.Creator<ParcelableLine> {
        override fun createFromParcel(parcel: Parcel) = ParcelableLine(parcel)
        override fun newArray(size: Int) = arrayOfNulls<ParcelableLine>(size)
    }
}

// 包装Element
class ParcelableElement : ParcelableTextBase{
    val confidence: Float
    val angle: Float
    val symbols: Array<ParcelableSymbol>

    constructor(parcel: Parcel): super(parcel){
        confidence = parcel.readFloat()
        angle = parcel.readFloat()
        symbols = parcel.createTypedArray(ParcelableSymbol.CREATOR) ?: emptyArray()
    }

    constructor(element: Text.Element) : super(
        element.text,
        element.boundingBox,
        element.cornerPoints,
        element.recognizedLanguage
    ){
        confidence = element.confidence
        angle = element.angle
        symbols = element.symbols.run {
            val iter = this.iterator()
            Array(this.size){
                assert(iter.hasNext())
                ParcelableSymbol(iter.next())
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeFloat(confidence)
        parcel.writeFloat(angle)
        parcel.writeTypedArray(symbols, 0)
    }

    companion object CREATOR : Parcelable.Creator<ParcelableElement> {
        override fun createFromParcel(parcel: Parcel) = ParcelableElement(parcel)
        override fun newArray(size: Int) = arrayOfNulls<ParcelableElement>(size)
    }
}

// 包装Symbol
class ParcelableSymbol : ParcelableTextBase {
    val confidence: Float
    val angle: Float

    constructor(parcel: Parcel) : super(parcel)  {
        confidence = parcel.readFloat()
        angle = parcel.readFloat()
    }

    constructor(symbol: Text.Symbol) : super(
        symbol.text,
        symbol.boundingBox,
        symbol.cornerPoints,
        symbol.recognizedLanguage
    ){
        confidence = symbol.confidence
        angle = symbol.angle
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeFloat(confidence)
        parcel.writeFloat(angle)
    }

    companion object CREATOR : Parcelable.Creator<ParcelableSymbol> {
        override fun createFromParcel(parcel: Parcel) = ParcelableSymbol(parcel)
        override fun newArray(size: Int) = arrayOfNulls<ParcelableSymbol>(size)
    }

}


// 包装TextBase
open class ParcelableTextBase(
    val text: String,
    val boundingBox: Rect?,
    val cornerPoints: Array<Point>?,
    val recognizedLanguage: String
) : Parcelable {
    constructor(parcel: Parcel): this(
        parcel.readString() ?: "",
        parcel.readParcelable(Rect::class.java.classLoader),
        parcel.createTypedArray(Point.CREATOR),
        parcel.readString() ?: "",
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(text)
        parcel.writeParcelable(boundingBox, flags)
        parcel.writeTypedArray(cornerPoints, 0)
        parcel.writeString(recognizedLanguage)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<ParcelableSymbol> {
        override fun createFromParcel(parcel: Parcel) = ParcelableSymbol(parcel)
        override fun newArray(size: Int) = arrayOfNulls<ParcelableSymbol>(size)
    }
}


