package com.hika.mirodaily.core.data_extractors

import com.hika.core.aidl.accessibility.ParcelableSymbol
import com.hika.core.aidl.accessibility.ParcelableText

fun ParcelableText.matchSymbols(sequence: String): List<ParcelableSymbol> {
    if (this.isEmpty() || sequence !in this.text)
        return emptyList()
    for (block in this.textBlocks){
        val index = block.text.indexOf(sequence)
        if (index != -1)
            return block.symbols.subList(index, index + sequence.length)
    }
    return emptyList()
}

fun ParcelableText.containsAny(sequences: Array<String>): List<ParcelableSymbol> {
    if (this.isEmpty())
        return emptyList()
    for (sequence in sequences){
        if (sequence !in this.text)
            continue
        for (block in this.textBlocks){
            val index = block.text.indexOf(sequence)
            if (index != -1)
                return block.symbols.subList(index, index + sequence.length)
        }
    }
    return emptyList()
}